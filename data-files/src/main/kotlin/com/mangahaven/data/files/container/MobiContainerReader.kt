package com.mangahaven.data.files.container

import android.content.Context
import android.net.Uri
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * MOBI/PDB 容器读取器。
 *
 * 解析 PalmDOC (PDB) 格式的 MOBI 文件，提取内嵌图片作为漫画页面。
 * 使用 ParcelFileDescriptor + FileChannel 实现随机访问，避免复制文件。
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
    private val indexCache = ConcurrentHashMap<String, List<MobiImageEntry>>()

    override suspend fun listPages(target: ContainerTarget): List<PageRef> =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(target.path)

            try {
                val entries = getOrLoadIndex(uri, target.path)
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
     * 使用 ParcelFileDescriptor 获取文件通道，避免复制文件。
     */
    suspend fun openPageFromMobi(mobiUri: Uri, pageIndex: Int): InputStream =
        withContext(Dispatchers.IO) {
            val entries = getOrLoadIndex(mobiUri, mobiUri.toString())

            if (pageIndex < 0 || pageIndex >= entries.size) {
                throw IndexOutOfBoundsException("Page index $pageIndex out of range [0, ${entries.size})")
            }
            val entry = entries[pageIndex]

            val pfd = context.contentResolver.openFileDescriptor(mobiUri, "r")
                ?: throw IllegalStateException("Cannot open MOBI file: $mobiUri")
            try {
                pfd.use { fd ->
                    java.io.FileInputStream(fd.fileDescriptor).channel.use { channel ->
                        channel.position(entry.offset.toLong())
                        val buf = ByteBuffer.allocate(entry.size)
                        channel.readFully(buf)
                        buf.flip()
                        val bytes = ByteArray(buf.remaining())
                        buf.get(bytes)
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
                val entries = getOrLoadIndex(uri, target.path)

                if (entries.isNotEmpty()) {
                    // Fallback to first image; MOBI cover metadata (EXTH) parsing
                    // would be needed for precise cover extraction.
                    val cover = entries.first()
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        ?: return@withContext null
                    pfd.use { fd ->
                        java.io.FileInputStream(fd.fileDescriptor).channel.use { channel ->
                            channel.position(cover.offset.toLong())
                            val buf = ByteBuffer.allocate(cover.size)
                            channel.readFully(buf)
                            buf.flip()
                            val bytes = ByteArray(buf.remaining())
                            buf.get(bytes)
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
     */
    private fun getOrLoadIndex(uri: Uri, uriStr: String): List<MobiImageEntry> {
        indexCache[uriStr]?.let { return it }

        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open MOBI file: $uri")
        pfd.use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).channel.use { channel ->
                val entries = parseMobiImages(channel)
                indexCache[uriStr] = entries
                return entries
            }
        }
    }

    /**
     * 清除指定 URI 的缓存。
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
    private fun parseMobiImages(channel: FileChannel): List<MobiImageEntry> {
        val fileLength = channel.size().toInt()
        if (fileLength < 78) return emptyList()

        // ── PDB Header ──
        channel.position(0)
        val pdbHeader = readBytes(channel, 78)

        val numRecords = ((pdbHeader[76].toInt() and 0xFF) shl 8) or (pdbHeader[77].toInt() and 0xFF)
        if (numRecords <= 0 || numRecords > 100000) return emptyList()

        // ── Record Entry List ──
        val recordOffsets = mutableListOf<Int>()
        val recordEntryStart = 78L
        channel.position(recordEntryStart)
        for (i in 0 until numRecords) {
            val buf = readBytes(channel, 8)
            val offset = ((buf[0].toInt() and 0xFF) shl 24) or
                    ((buf[1].toInt() and 0xFF) shl 16) or
                    ((buf[2].toInt() and 0xFF) shl 8) or
                    (buf[3].toInt() and 0xFF)
            recordOffsets.add(offset)
        }

        if (recordOffsets.isEmpty()) return emptyList()

        // ── Try to find imageFirst from MOBI header ──
        val imageFirst = tryGetImageFirst(channel, recordOffsets)

        // ── Scan for image records using magic bytes ──
        val scanStart = if (imageFirst in 1 until recordOffsets.size) imageFirst else 1
        return scanForImageRecords(channel, recordOffsets, fileLength, scanStart)
    }

    /**
     * 尝试从 MOBI header 读取 imageFirst。
     * 返回 -1 表示未找到。
     */
    private fun tryGetImageFirst(channel: FileChannel, recordOffsets: List<Int>): Int {
        if (recordOffsets.isEmpty()) return -1

        try {
            val record0Offset = recordOffsets[0]
            channel.position((record0Offset + 16).toLong())
            val mobiMagic = readBytes(channel, 4)
            if (String(mobiMagic, Charsets.US_ASCII) != "MOBI") return -1

            channel.position((record0Offset + 20).toLong())
            val headerLenBuf = readBytes(channel, 4)
            val headerLen = bufToInt(headerLenBuf)

            if (headerLen < 112) return -1
            channel.position((record0Offset + 16 + 108).toLong())
            val imgFirstBuf = readBytes(channel, 4)
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
     */
    private fun scanForImageRecords(
        channel: FileChannel,
        recordOffsets: List<Int>,
        fileLength: Int,
        startIndex: Int,
    ): List<MobiImageEntry> {
        val entries = mutableListOf<MobiImageEntry>()
        var consecutiveNonImages = 0
        val maxConsecutiveNonImages = 5

        for (i in startIndex until recordOffsets.size) {
            val start = recordOffsets[i]
            val end = if (i + 1 < recordOffsets.size) recordOffsets[i + 1] else fileLength
            val size = end - start
            if (size < 12) {
                consecutiveNonImages++
                if (consecutiveNonImages >= maxConsecutiveNonImages && entries.isNotEmpty()) break
                continue
            }

            channel.position(start.toLong())
            val magic = readBytes(channel, minOf(12, size))

            val ext = detectImageExtension(magic, magic.size)
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

    // ── Channel helpers ──────────────────────────────────────────────

    private fun readBytes(channel: ReadableByteChannel, count: Int): ByteArray {
        val buf = ByteBuffer.allocate(count)
        var totalRead = 0
        while (totalRead < count) {
            val n = channel.read(buf)
            if (n == -1) break
            totalRead += n
        }
        buf.flip()
        val bytes = ByteArray(buf.remaining())
        buf.get(bytes)
        return bytes
    }

    private fun FileChannel.readFully(buf: ByteBuffer) {
        var totalRead = 0
        while (totalRead < buf.capacity()) {
            val n = this.read(buf)
            if (n == -1) throw java.io.EOFException("Unexpected end of file")
            totalRead += n
        }
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
