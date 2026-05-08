package com.mangahaven.data.files.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MobiMagicDetectionTest {

    @Test
    fun `detect JPEG`() {
        val magic = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertEquals("jpg", MobiContainerReader.detectImageExtension(magic, magic.size))
    }

    @Test
    fun `detect PNG`() {
        val magic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertEquals("png", MobiContainerReader.detectImageExtension(magic, magic.size))
    }

    @Test
    fun `detect GIF`() {
        val magic = "GIF89a".toByteArray()
        assertEquals("gif", MobiContainerReader.detectImageExtension(magic, magic.size))
    }

    @Test
    fun `detect BMP`() {
        val magic = byteArrayOf('B'.code.toByte(), 'M'.code.toByte(), 0, 0)
        assertEquals("bmp", MobiContainerReader.detectImageExtension(magic, magic.size))
    }

    @Test
    fun `detect WebP with full RIFF-WEBP check`() {
        // RIFF (4 bytes) + file size (4 bytes) + WEBP (4 bytes)
        val magic = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte(),
        )
        assertEquals("webp", MobiContainerReader.detectImageExtension(magic, magic.size))
    }

    @Test
    fun `reject RIFF without WEBP marker`() {
        // RIFF but not WEBP (e.g. RIFF audio file)
        val magic = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
        )
        assertNull("RIFF+WAVE should not be detected as WebP", MobiContainerReader.detectImageExtension(magic, magic.size))
    }

    @Test
    fun `reject RIFF with only 8 bytes`() {
        // Only RIFF header, not enough for WEBP check
        val magic = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
        )
        assertNull("RIFF with only 8 bytes should not be WebP", MobiContainerReader.detectImageExtension(magic, magic.size))
    }

    @Test
    fun `reject non-image data`() {
        val magic = "Hello world!".toByteArray()
        assertNull(MobiContainerReader.detectImageExtension(magic, magic.size))
    }

    @Test
    fun `reject too short data`() {
        val magic = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        assertNull("2 bytes should be insufficient", MobiContainerReader.detectImageExtension(magic, magic.size))
    }

    @Test
    fun `detect JPEG with EXIF marker`() {
        val magic = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte())
        assertEquals("jpg", MobiContainerReader.detectImageExtension(magic, magic.size))
    }
}
