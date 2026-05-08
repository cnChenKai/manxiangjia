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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

/**
 * MOBI/PDB 容器读取器。
 *
 * 解析 PalmDOC (PDB) 格式的 MOBI 文件，提取内嵌图片作为漫画页面。
 * 支持 MOBI header 中的 image offset/count 快速定位，以及 fallback 扫描。
 *
 * MOBI 文件结构：
 *   PDB Header (78 bytes)
 *   Record Entry List (8 bytes each)
 *   Record 0: MOBI header (包含 imageFirst/imageCount)
 *   Record 1: PalmDOC header (compression info)
 *   Record 2+: 内容记录（图片通常从 imageFirst 开始）
 */
class MobiContainerReader(
    @ApplicationContext private val context: Context,
) : ContainerReader {

    override suspend fun listPages(target: ContainerTarget): List<PageRef> =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(target.path)
            val entries = mutableListOf<MobiImageEntry>()

            try {
                // Copy to temp file for random access (SAF streams are sequential)
                val tempFile = java.io.File(context.cacheDir, "mobi_temp_${uri.hashCode()}.mobi")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@withContext emptyList()

                    RandomAccessFile(tempFile, "r").use { raf ->
                        entries.addAll(parseMobiImages(raf))
                    }
                } finally {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list pages in MOBI: ${target.path}")
                return@withContext emptyList()
            }

            entries
                .sortedWith(compareBy(ImageFileUtils.naturalOrderComparator) { it.name })
                .mapIndexed { index, entry ->
                    PageRef(
                        index = index,
                        name = entry.name,
                        path = "mobi://$index", // internal path reference
                        mimeType = ImageFileUtils.getMimeType(entry.name),
                        sizeBytes = entry.size.toLong(),
                    )
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
     * @param mobiUri MOBI 文件的 URI
     * @param pageIndex 页面索引
     */
    suspend fun openPageFromMobi(mobiUri: Uri, pageIndex: Int): InputStream =
        withContext(Dispatchers.IO) {
            val tempFile = java.io.File(context.cacheDir, "mobi_read_${mobiUri.hashCode()}.mobi")
            try {
                context.contentResolver.openInputStream(mobiUri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Cannot open MOBI file: $mobiUri")

                RandomAccessFile(tempFile, "r").use { raf ->
                    val entries = parseMobiImages(raf)
                    if (pageIndex < 0 || pageIndex >= entries.size) {
                        throw IndexOutOfBoundsException("Page index $pageIndex out of range [0, ${entries.size})")
                    }
                    val entry = entries[pageIndex]
                    raf.seek(entry.offset.toLong())
                    val bytes = ByteArray(entry.size)
                    raf.readFully(bytes)
                    return@withContext ByteArrayInputStream(bytes)
                }
            } finally {
                tempFile.delete()
            }
        }

    override suspend fun extractCover(target: ContainerTarget): InputStream? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(target.path)
                val tempFile = java.io.File(context.cacheDir, "mobi_cover_${uri.hashCode()}.mobi")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@withContext null

                    RandomAccessFile(tempFile, "r").use { raf ->
                        val entries = parseMobiImages(raf)
                        if (entries.isNotEmpty()) {
                            // Prefer cover image, fallback to first
                            val cover = entries.find { it.name.contains("cover", ignoreCase = true) } ?: entries.first()
                            raf.seek(cover.offset.toLong())
                            val bytes = ByteArray(cover.size)
                            raf.readFully(bytes)
                            return@withContext ByteArrayInputStream(bytes)
                        }
                    }
                } finally {
                    tempFile.delete()
                }
                null
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract cover from MOBI: ${target.path}")
                null
            }
        }

    // ── Internal parsing ──────────────────────────────────────────────

    private data class MobiImageEntry(
        val name: String,
        val offset: Int,
        val size: Int,
    )

    /**
     * 解析 MOBI 文件中的图片记录。
     * 优先使用 MOBI header 的 imageFirst/imageCount，
     * fallback 为逐记录扫描图片 magic bytes。
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

        // ── Try MOBI header (Record 0) for image info ──
        val imageEntries = tryParseFromMobiHeader(raf, recordOffsets, fileLength)

        if (imageEntries.isNotEmpty()) return imageEntries

        // ── Fallback: scan all records for image signatures ──
        return scanForImageRecords(raf, recordOffsets, fileLength)
    }

    /**
     * 尝试从 MOBI header 读取 imageFirst/imageCount。
     * MOBI header 位于 Record 0 的偏移 16 处。
     */
    private fun tryParseFromMobiHeader(
        raf: RandomAccessFile,
        recordOffsets: List<Int>,
        fileLength: Int,
    ): List<MobiImageEntry> {
        if (recordOffsets.size < 2) return emptyList()

        try {
            val record0Offset = recordOffsets[0]
            raf.seek((record0Offset + 16).toLong())
            val mobiMagic = ByteArray(4)
            raf.readFully(mobiMagic)
            if (String(mobiMagic, Charset.forName("ASCII")) != "MOBI") return emptyList()

            // MOBI header length at offset 4 (from record0 start + 16)
            raf.seek((record0Offset + 20).toLong())
            val headerLenBuf = ByteArray(4)
            raf.readFully(headerLenBuf)
            val headerLen = bufToInt(headerLenBuf)

            // imageFirst at MOBI header offset 108
            if (headerLen < 112) return emptyList()
            raf.seek((record0Offset + 16 + 108).toLong())
            val imgFirstBuf = ByteArray(4)
            raf.readFully(imgFirstBuf)
            val imageFirst = bufToInt(imgFirstBuf)

            // imageCount at MOBI header offset 112
            raf.seek((record0Offset + 16 + 112).toLong())
            val imgCountBuf = ByteArray(4)
            raf.readFully(imgCountBuf)
            val imageCount = bufToInt(imgCountBuf)

            if (imageFirst < 0 || imageFirst >= recordOffsets.size) return emptyList()
            if (imageCount <= 0 || imageCount > 100000) return emptyList()
            if (imageFirst + imageCount > recordOffsets.size) return emptyList()

            val entries = mutableListOf<MobiImageEntry>()
            for (i in 0 until imageCount) {
                val recIdx = imageFirst + i
                val start = recordOffsets[recIdx]
                val end = if (recIdx + 1 < recordOffsets.size) recordOffsets[recIdx + 1] else fileLength
                val size = end - start
                if (size <= 0) continue

                // Read first bytes to determine image type
                raf.seek(start.toLong())
                val magic = ByteArray(8)
                val readLen = minOf(magic.size, size)
                raf.read(magic, 0, readLen)

                val ext = detectImageExtension(magic, readLen)
                if (ext != null) {
                    entries.add(MobiImageEntry("image_${entries.size}.$ext", start, size))
                }
            }
            return entries
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse MOBI header for image info, falling back to scan")
            return emptyList()
        }
    }

    /**
     * 扫描所有 PDB 记录，通过 magic bytes 识别图片。
     */
    private fun scanForImageRecords(
        raf: RandomAccessFile,
        recordOffsets: List<Int>,
        fileLength: Int,
    ): List<MobiImageEntry> {
        val entries = mutableListOf<MobiImageEntry>()

        for (i in 1 until recordOffsets.size) { // skip record 0 (header)
            val start = recordOffsets[i]
            val end = if (i + 1 < recordOffsets.size) recordOffsets[i + 1] else fileLength
            val size = end - start
            if (size < 8) continue

            raf.seek(start.toLong())
            val magic = ByteArray(8)
            raf.readFully(magic)

            val ext = detectImageExtension(magic, magic.size)
            if (ext != null) {
                entries.add(MobiImageEntry("image_${entries.size}.$ext", start, size))
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
            // WebP: RIFF....WEBP
            if (len >= 4 && magic[0] == 'R'.code.toByte() && magic[1] == 'I'.code.toByte() &&
                magic[2] == 'F'.code.toByte() && magic[3] == 'F'.code.toByte()
            ) {
                return "webp"
            }
            return null
        }
    }
}
