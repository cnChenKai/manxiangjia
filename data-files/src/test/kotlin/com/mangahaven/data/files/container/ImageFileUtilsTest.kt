package com.mangahaven.data.files.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageFileUtilsTest {

    @Test
    fun testIsImageFile() {
        assertTrue(ImageFileUtils.isImageFile("test.jpg"))
        assertTrue(ImageFileUtils.isImageFile("test.JPEG"))
        assertTrue(ImageFileUtils.isImageFile("test.png"))
        assertTrue(ImageFileUtils.isImageFile("test.webp"))
        assertTrue(ImageFileUtils.isImageFile("test.gif"))
        assertTrue(ImageFileUtils.isImageFile("test.bmp"))
        assertTrue(ImageFileUtils.isImageFile("test.avif"))

        assertFalse(ImageFileUtils.isImageFile("test.txt"))
        assertFalse(ImageFileUtils.isImageFile("test.zip"))
        assertFalse(ImageFileUtils.isImageFile("test"))
        assertFalse(ImageFileUtils.isImageFile("test."))
    }

    @Test
    fun testShouldIgnore() {
        assertTrue(ImageFileUtils.shouldIgnore("__MACOSX/image.jpg"))
        assertTrue(ImageFileUtils.shouldIgnore("some/path/.DS_Store"))
        assertTrue(ImageFileUtils.shouldIgnore("Thumbs.db"))
        assertTrue(ImageFileUtils.shouldIgnore("path/to/desktop.ini"))
        assertTrue(ImageFileUtils.shouldIgnore("._.DS_Store"))
        assertTrue(ImageFileUtils.shouldIgnore("THUMBS.DB"))

        assertFalse(ImageFileUtils.shouldIgnore("normal_folder/image.jpg"))
        assertFalse(ImageFileUtils.shouldIgnore("my_manga/chapter1/001.png"))
    }

    @Test
    fun testGetMimeType() {
        assertEquals("image/jpeg", ImageFileUtils.getMimeType("test.jpg"))
        assertEquals("image/jpeg", ImageFileUtils.getMimeType("test.jpeg"))
        assertEquals("IMAGE/JPEG", "image/jpeg", ImageFileUtils.getMimeType("test.JPEG"))
        assertEquals("image/png", ImageFileUtils.getMimeType("test.png"))
        assertEquals("image/webp", ImageFileUtils.getMimeType("test.webp"))
        assertEquals("image/gif", ImageFileUtils.getMimeType("test.gif"))
        assertEquals("image/bmp", ImageFileUtils.getMimeType("test.bmp"))
        assertEquals("image/avif", ImageFileUtils.getMimeType("test.avif"))

        assertNull(ImageFileUtils.getMimeType("test.txt"))
        assertNull(ImageFileUtils.getMimeType("test.zip"))
    }

    @Test
    fun testIsArchive() {
        assertTrue(ImageFileUtils.isArchive("test.zip"))
        assertTrue(ImageFileUtils.isArchive("test.cbz"))
        assertTrue(ImageFileUtils.isArchive("test.rar"))
        assertTrue(ImageFileUtils.isArchive("test.cbr"))
        assertTrue(ImageFileUtils.isArchive("TEST.ZIP"))

        assertFalse(ImageFileUtils.isArchive("test.jpg"))
        assertFalse(ImageFileUtils.isArchive("test.txt"))
    }

    @Test
    fun testNaturalOrderComparator() {
        val list = listOf("page10.jpg", "page2.jpg", "page1.jpg")
        val sorted = list.sortedWith(ImageFileUtils.naturalOrderComparator)
        assertEquals(listOf("page1.jpg", "page2.jpg", "page10.jpg"), sorted)

        val listWithLeadingZeros = listOf("01.jpg", "1.jpg", "001.jpg")
        val sortedLeadingZeros = listWithLeadingZeros.sortedWith(ImageFileUtils.naturalOrderComparator)
        assertEquals(listOf("1.jpg", "01.jpg", "001.jpg"), sortedLeadingZeros)

        val mixed = listOf("a2.jpg", "a10.jpg", "a1.jpg", "b1.jpg")
        val sortedMixed = mixed.sortedWith(ImageFileUtils.naturalOrderComparator)
        assertEquals(listOf("a1.jpg", "a2.jpg", "a10.jpg", "b1.jpg"), sortedMixed)

        // Case insensitive sorting for non-numeric parts
        val cases = listOf("B.jpg", "a.jpg")
        val sortedCases = cases.sortedWith(ImageFileUtils.naturalOrderComparator)
        assertEquals(listOf("a.jpg", "B.jpg"), sortedCases)
    }
}
