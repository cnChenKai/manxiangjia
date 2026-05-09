package com.mangahaven.data.files.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * MobiContainerReader PDB 解析测试。
 *
 * 构造最小 PDB 二进制样本，通过 FileChannel 验证图片记录解析。
 * 测试覆盖：record 结构、magic byte 检测、边界情况。
 */
class MobiContainerReaderTest {

    // ── PDB binary construction helpers ──────────────────────────────

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        ((value shr 24) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

    private fun buildPdbFile(records: List<ByteArray>): File {
        val tempFile = File.createTempFile("test_mobi_", ".pdb")
        val pdbHeaderSize = 78
        val numRecords = records.size
        val recordEntrySize = 8 * numRecords

        // Calculate offsets
        val offsets = mutableListOf<Int>()
        var currentOffset = pdbHeaderSize + recordEntrySize
        for (record in records) {
            offsets.add(currentOffset)
            currentOffset += record.size
        }

        val baos = ByteArrayOutputStream()

        // ── PDB Header (78 bytes) ──
        val pdbHeader = ByteArray(78)
        "TestMobi".toByteArray().copyInto(pdbHeader, 0)
        // Type: BOOK
        pdbHeader[60] = 'B'.code.toByte(); pdbHeader[61] = 'O'.code.toByte()
        pdbHeader[62] = 'O'.code.toByte(); pdbHeader[63] = 'K'.code.toByte()
        // Creator: MOBI
        pdbHeader[64] = 'M'.code.toByte(); pdbHeader[65] = 'O'.code.toByte()
        pdbHeader[66] = 'B'.code.toByte(); pdbHeader[67] = 'I'.code.toByte()
        // Num records
        intToBytes(numRecords).copyInto(pdbHeader, 76)
        // Actually only 2 bytes for numRecords in PDB header (big-endian)
        pdbHeader[76] = ((numRecords shr 8) and 0xFF).toByte()
        pdbHeader[77] = (numRecords and 0xFF).toByte()
        baos.write(pdbHeader)

        // ── Record Entry List ──
        for (offset in offsets) {
            val entry = ByteArray(8)
            intToBytes(offset).copyInto(entry, 0)
            baos.write(entry)
        }

        // ── Records ──
        for (record in records) {
            baos.write(record)
        }

        tempFile.writeBytes(baos.toByteArray())
        return tempFile
    }

    private fun readAt(channel: FileChannel, offset: Int, count: Int): ByteArray {
        channel.position(offset.toLong())
        val buf = ByteBuffer.allocate(count)
        var total = 0
        while (total < count) {
            val n = channel.read(buf)
            if (n == -1) break
            total += n
        }
        buf.flip()
        val bytes = ByteArray(buf.remaining())
        buf.get(bytes)
        return bytes
    }

    // ── Tests ──────────────────────────────────────────────────────

    @Test
    fun `PDB with JPEG and PNG records has correct structure`() {
        val jpegData = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x01, 0x02, 0x03)
        val pngData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x04, 0x05, 0x06, 0x07)
        val headerData = ByteArray(32) { 0x42 }

        val tempFile = buildPdbFile(listOf(headerData, jpegData, pngData))
        try {
            java.io.FileInputStream(tempFile).channel.use { channel ->
                // Verify PDB header: 3 records
                val header = readAt(channel, 0, 78)
                val numRecords = ((header[76].toInt() and 0xFF) shl 8) or (header[77].toInt() and 0xFF)
                assertEquals(3, numRecords)

                // Read record entry offsets
                channel.position(78)
                val offsets = mutableListOf<Int>()
                for (i in 0 until numRecords) {
                    val buf = readAt(channel, 78 + i * 8, 4)
                    offsets.add(((buf[0].toInt() and 0xFF) shl 24) or
                            ((buf[1].toInt() and 0xFF) shl 16) or
                            ((buf[2].toInt() and 0xFF) shl 8) or
                            (buf[3].toInt() and 0xFF))
                }

                // Record 0: non-image header
                val rec0 = readAt(channel, offsets[0], 4)
                assertEquals(0x42.toByte(), rec0[0]) // filler data
                assertNull(MobiContainerReader.detectImageExtension(rec0, rec0.size))

                // Record 1: JPEG
                val rec1 = readAt(channel, offsets[1], 4)
                assertEquals("jpg", MobiContainerReader.detectImageExtension(rec1, rec1.size))

                // Record 2: PNG
                val rec2 = readAt(channel, offsets[2], 8)
                assertEquals("png", MobiContainerReader.detectImageExtension(rec2, rec2.size))
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `PDB with no images has correct structure`() {
        val headerData = ByteArray(32) { 0x42 }
        val textData = ByteArray(16) { 'A'.code.toByte() }

        val tempFile = buildPdbFile(listOf(headerData, textData))
        try {
            java.io.FileInputStream(tempFile).channel.use { channel ->
                val header = readAt(channel, 0, 78)
                val numRecords = ((header[76].toInt() and 0xFF) shl 8) or (header[77].toInt() and 0xFF)
                assertEquals(2, numRecords)

                // Both records should be non-image
                channel.position(78)
                val offsets = mutableListOf<Int>()
                for (i in 0 until numRecords) {
                    val buf = readAt(channel, 78 + i * 8, 4)
                    offsets.add(((buf[0].toInt() and 0xFF) shl 24) or
                            ((buf[1].toInt() and 0xFF) shl 16) or
                            ((buf[2].toInt() and 0xFF) shl 8) or
                            (buf[3].toInt() and 0xFF))
                }

                for (offset in offsets) {
                    val magic = readAt(channel, offset, 4)
                    assertNull(MobiContainerReader.detectImageExtension(magic, magic.size))
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `PDB with mixed image and non-image records`() {
        val headerData = ByteArray(32) { 0x00 }
        val jpeg1 = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x01, 0x02, 0x03, 0x04)
        val textRecord = ByteArray(16) { 'X'.code.toByte() }
        val png1 = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x09, 0x0A, 0x0B, 0x0C)
        val gif1 = "GIF89a".toByteArray() + byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)

        val tempFile = buildPdbFile(listOf(headerData, jpeg1, textRecord, png1, gif1))
        try {
            java.io.FileInputStream(tempFile).channel.use { channel ->
                val header = readAt(channel, 0, 78)
                val numRecords = ((header[76].toInt() and 0xFF) shl 8) or (header[77].toInt() and 0xFF)
                assertEquals(5, numRecords)

                channel.position(78)
                val offsets = mutableListOf<Int>()
                for (i in 0 until numRecords) {
                    val buf = readAt(channel, 78 + i * 8, 4)
                    offsets.add(((buf[0].toInt() and 0xFF) shl 24) or
                            ((buf[1].toInt() and 0xFF) shl 16) or
                            ((buf[2].toInt() and 0xFF) shl 8) or
                            (buf[3].toInt() and 0xFF))
                }

                // Record 0: header (non-image)
                assertNull(MobiContainerReader.detectImageExtension(readAt(channel, offsets[0], 4), 4))
                // Record 1: JPEG
                assertEquals("jpg", MobiContainerReader.detectImageExtension(readAt(channel, offsets[1], 4), 4))
                // Record 2: text (non-image)
                assertNull(MobiContainerReader.detectImageExtension(readAt(channel, offsets[2], 4), 4))
                // Record 3: PNG
                assertEquals("png", MobiContainerReader.detectImageExtension(readAt(channel, offsets[3], 8), 8))
                // Record 4: GIF
                assertEquals("gif", MobiContainerReader.detectImageExtension(readAt(channel, offsets[4], 6), 6))
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `detectImageExtension handles all formats`() {
        assertEquals("jpg", MobiContainerReader.detectImageExtension(
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()), 4))
        assertEquals("png", MobiContainerReader.detectImageExtension(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), 4))
        assertEquals("gif", MobiContainerReader.detectImageExtension("GIF89a".toByteArray(), 6))
        assertEquals("bmp", MobiContainerReader.detectImageExtension(
            byteArrayOf('B'.code.toByte(), 'M'.code.toByte()), 2))

        val webpMagic = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte(),
        )
        assertEquals("webp", MobiContainerReader.detectImageExtension(webpMagic, 12))

        val riffWave = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
        )
        assertNull(MobiContainerReader.detectImageExtension(riffWave, 12))
    }
}
