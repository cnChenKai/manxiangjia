package com.mangahaven.data.files.container

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

/**
 * MOBI/PDB 容器读取器。
 *
 * 解析 PalmDOC (PDB) 格式的 MOBI 文件，提取内嵌图片作为漫画页面。
 * 使用 ParcelFileDescriptor 实现随机访问，避免重复复制文件。
 *
 * MOBI 文件结构：
 *   PDB Header (78 bytes)
 *   Record Entry List (8 bytes each)
 *   Record 0: MOBI header
 *   Record 1: PalmDOC header (compression info)
 *   Record 2+: 内容记录（图片通常从 imageFirst 开始）
 */
class MobiContainerReader(
    @ApplicationContext private val context: Context,
) : ContainerReader {

    /**
     * 缓存已解析的 MOBI 索引，key 为 URI 字符串。
     * 生命周期与 reader 实例一致（由 LocalPageProvider 持有）。
     */
    private val indexCache = ConcurrentHashMap<String, CachedMobiIndex>()

    private data class CachedMobiIndex(
        val entries: List<MobiImageEntry>,
        val fileLength: Long,
    )

    override suspend fun listPages(target: ContainerTarget): List<PageRef> =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(target.path)
            val uriStr = target.path

            try {
                val entries = getOrLoadIndex(uri, uriStr)
                entries
                    .sortedWith(compareBy(ImageFileUtils.naturalOrderComparator) { it.name })
                    .mapIndexed { index, entry ->
                        PageRef(
                            index = index,
                            name = entry.name,
                            path = "mobi://$index",
                            mimeType = ImageFileUtils.getMimeType(entry.name),
                            sizeBytes = entry.size.toLong(),
                        )
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list pages in MOBI: ${target.path}")
                emptyList()
            }
        }

    override suspend fun openPage(pageRef: PageRef): InputStream =
        withContext(Dispatchers.IO) {
            throw UnsupportedOperationException(
                "Use MobiContainerReader.openPageFromMobi() with the source URI."
            )
        }

    /**
     * 从 MOBI 文件中读取指定页面。
     * 使用 ParcelFileDescriptor 获取可随机访问的文件描述符，避免复制文件。
     * @param mobiUri MOBI 文件的 URI
     * @param pageIndex 页面索引
     */
    suspend fun openPageFromMobi(mobiUri: Uri, pageIndex: Int): InputStream =
        withContext(Dispatchers.IO) {
            val uriStr = mobiUri.toString()
            val entries = getOrLoadIndex(mobiUri, uriStr)

            if (pageIndex < 0 || pageIndex >= entries.size) {
                throw IndexOutOfBoundsException("Page index $pageIndex out of range [0, ${entries.size})")
            }
            val entry = entries[pageIndex]

            val pfd = context.contentResolver.openFileDescriptor(mobiUri, "r")
                ?: throw IllegalStateException("Cannot open MOBI file: $mobiUri")
            try {
                pfd.use { fd ->
                    RandomAccessFile(fd.fileDescriptor).use { raf ->
                        raf.seek(entry.offset.toLong())
                        val bytes = ByteArray(entry.size)
                        raf.readFully(bytes)
                        return@withContext ByteArrayInputStream(bytes)
                    }
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to read page $pageIndex from MOBI: $mobiUri", e)
            }
        }

    override suspend fun extractCover(target: ContainerTarget): InputStream? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(target.path)
                val uriStr = target.path
                val entries = getOrLoadIndex(uri, uriStr)

                if (entries.isNotEmpty()) {
                    // Fallback to first image; MOBI cover metadata (EXTH) parsing
                    // would be needed for precise cover extraction.
                    val cover = entries.first()
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        ?: return@withContext null
                    pfd.use { fd ->
                        RandomAccessFile(fd.fileDescriptor).use { raf ->
                            raf.seek(cover.offset.toLong())
                            val bytes = ByteArray(cover.size)
                            raf.readFully(bytes)
                            return@withContext ByteArrayInputStream(bytes)
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract cover from MOBI: ${target.path}")
                null
            }
        }

    /**
     * 获取或加载 MOBI 索引缓存。
     * 使用 openFileDescriptor 避免复制文件。
     */
    private fun getOrLoadIndex(uri: Uri, uriStr: String): List<MobiImageEntry> {
        indexCache[uriStr]?.let { return it.entries }

        val entries = mutableListOf<MobiImageEntry>()
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open MOBI file: $uri")
        pfd.use { fd ->
            RandomAccessFile(fd.fileDescriptor).use { raf ->
                entries.addAll(parseMobiImages(raf))
            }
        }

        indexCache[uriStr] = CachedMobiIndex(entries, 0)
        return entries
    }

    /**
     * 清除指定 URI 的缓存（文件被替换时调用）。
     */
    fun invalidateCache(uri: String) {
        indexCache.remove(uri)
    }

    /**
     * 清除所有缓存。
     */
    fun clearCache() {
        indexCache.clear()
    }

    // ── Internal parsing ──────────────────────────────────────────────

    private data class MobiImageEntry(
        val name: String,
        val offset: Int,
        val size: Int,
    )

    /**
     * 解析 MOBI 文件中的图片记录。
     *
     * 策略：以 magic byte 扫描为主路径，确保正确性。
     * 先尝试从 MOBI header 读取 imageFirst 作为优化起点，
     * 但不依赖不可靠的 imageCount 字段。
     */
    private fun parseMobiImages(raf: RandomAccessFile): List<MobiImageEntry> {
        val fileLength = raf.length().toInt()
        if (fileLength < 78) return emptyList()

        // ── PDB Header ──
        raf.seek(0)
        val pdbHeader = ByteArray(78)
        raf.readFully(pdbHeader)

        val numRecords = ((pdbHeader[76].toInt() and 0xFF) shl 8) or (pdbHeader[77].toInt() and 0xFF)
        if (numRecords <= 0 || numRecords > 100000) return emptyList()

        // ── Record Entry List ──
        val recordOffsets = mutableListOf<Int>()
        val recordEntryStart = 78
        for (i in 0 until numRecords) {
            val base = recordEntryStart + i * 8
            if (base + 8 > fileLength) break
            raf.seek(base.toLong())
            val buf = ByteArray(4)
            raf.readFully(buf)
            val offset = ((buf[0].toInt() and 0xFF) shl 24) or
                    ((buf[1].toInt() and 0xFF) shl 16) or
                    ((buf[2].toInt() and 0xFF) shl 8) or
                    (buf[3].toInt() and 0xFF)
            recordOffsets.add(offset)
        }

        if (recordOffsets.isEmpty()) return emptyList()

        // ── Try to find imageFirst from MOBI header ──
        val imageFirst = tryGetImageFirst(raf, recordOffsets)

        // ── Scan for image records using magic bytes ──
        // Start from imageFirst if available, otherwise scan from record 1
        val scanStart = if (imageFirst in 1 until recordOffsets.size) imageFirst else 1
        val entries = scanForImageRecords(raf, recordOffsets, fileLength, scanStart)

        return entries
    }

    /**
     * 尝试从 MOBI header 读取 imageFirst（第一个图片记录的索引）。
     * 不读取 imageCount，因为其偏移量在不同 MOBI 版本中不一致。
     * 返回 -1 表示未找到。
     */
    private fun tryGetImageFirst(raf: RandomAccessFile, recordOffsets: List<Int>): Int {
        if (recordOffsets.isEmpty()) return -1

        try {
            val record0Offset = recordOffsets[0]
            raf.seek((record0Offset + 16).toLong())
            val mobiMagic = ByteArray(4)
            raf.readFully(mobiMagic)
            if (String(mobiMagic, Charsets.US_ASCII) != "MOBI") return -1

            // MOBI header length at record0 + 20
            raf.seek((record0Offset + 20).toLong())
            val headerLenBuf = ByteArray(4)
            raf.readFully(headerLenBuf)
            val headerLen = bufToInt(headerLenBuf)

            // imageFirst at MOBI header offset 108 (record0 + 16 + 108)
            if (headerLen < 112) return -1
            raf.seek((record0Offset + 16 + 108).toLong())
            val imgFirstBuf = ByteArray(4)
            raf.readFully(imgFirstBuf)
            val imageFirst = bufToInt(imgFirstBuf)

            if (imageFirst < 1 || imageFirst >= recordOffsets.size) return -1
            return imageFirst
        } catch (e: Exception) {
            Timber.w(e, "Failed to read imageFirst from MOBI header")
            return -1
        }
    }

    /**
     * 从指定起始位置扫描 PDB 记录，通过 magic bytes 识别图片。
     * 连续遇到非图片记录时停止扫描。
     */
    private fun scanForImageRecords(
        raf: RandomAccessFile,
        recordOffsets: List<Int>,
        fileLength: Int,
        startIndex: Int,
    ): List<MobiImageEntry> {
        val entries = mutableListOf<MobiImageEntry>()
        var consecutiveNonImages = 0
        val maxConsecutiveNonImages = 5 // stop after 5 consecutive non-image records

        for (i in startIndex until recordOffsets.size) {
            val start = recordOffsets[i]
            val end = if (i + 1 < recordOffsets.size) recordOffsets[i + 1] else fileLength
            val size = end - start
            if (size < 12) { // need at least 12 bytes for reliable magic detection
                consecutiveNonImages++
                if (consecutiveNonImages >= maxConsecutiveNonImages && entries.isNotEmpty()) break
                continue
            }

            raf.seek(start.toLong())
            val magic = ByteArray(12) // read 12 bytes for RIFF....WEBP detection
            val readLen = minOf(magic.size, size)
            raf.read(magic, 0, readLen)

            val ext = detectImageExtension(magic, readLen)
            if (ext != null) {
                entries.add(MobiImageEntry("image_${entries.size}.$ext", start, size))
                consecutiveNonImages = 0
            } else {
                consecutiveNonImages++
                if (consecutiveNonImages >= maxConsecutiveNonImages && entries.isNotEmpty()) break
            }
        }
        return entries
    }

    private fun bufToInt(buf: ByteArray): Int {
        return ((buf[0].toInt() and 0xFF) shl 24) or
                ((buf[1].toInt() and 0xFF) shl 16) or
                ((buf[2].toInt() and 0xFF) shl 8) or
                (buf[3].toInt() and 0xFF)
    }

    companion object {
        /**
         * 通过 magic bytes 检测图片格式，返回文件扩展名或 null。
         * 需要至少 12 字节的 buffer 以正确识别 WebP (RIFF....WEBP)。
         */
        fun detectImageExtension(magic: ByteArray, len: Int): String? {
            if (len < 4) return null

            // JPEG: FF D8 FF
            if (len >= 3 && magic[0] == 0xFF.toByte() && magic[1] == 0xD8.toByte() && magic[2] == 0xFF.toByte()) {
                return "jpg"
            }
            // PNG: 89 50 4E 47
            if (len >= 4 && magic[0] == 0x89.toByte() && magic[1] == 0x50.toByte() &&
                magic[2] == 0x4E.toByte() && magic[3] == 0x47.toByte()
            ) {
                return "png"
            }
            // GIF: GIF8
            if (len >= 4 && magic[0] == 'G'.code.toByte() && magic[1] == 'I'.code.toByte() &&
                magic[2] == 'F'.code.toByte() && magic[3] == '8'.code.toByte()
            ) {
                return "gif"
            }
            // BMP: BM
            if (len >= 2 && magic[0] == 'B'.code.toByte() && magic[1] == 'M'.code.toByte()) {
                return "bmp"
            }
            // WebP: RIFF (bytes 0-3) + WEBP (bytes 8-11)
            if (len >= 12 && magic[0] == 'R'.code.toByte() && magic[1] == 'I'.code.toByte() &&
                magic[2] == 'F'.code.toByte() && magic[3] == 'F'.code.toByte() &&
                magic[8] == 'W'.code.toByte() && magic[9] == 'E'.code.toByte() &&
                magic[10] == 'B'.code.toByte() && magic[11] == 'P'.code.toByte()
            ) {
                return "webp"
            }
            return null
        }
    }
}
