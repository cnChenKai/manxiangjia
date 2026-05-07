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
        assertTrue(ImageFileUtils.isImageFile("image.png"))
        assertTrue(ImageFileUtils.isImageFile("picture.webp"))
        assertTrue(ImageFileUtils.isImageFile("anim.gif"))
        assertTrue(ImageFileUtils.isImageFile("raw.bmp"))
        assertTrue(ImageFileUtils.isImageFile("modern.avif"))

        assertFalse(ImageFileUtils.isImageFile("test.txt"))
        assertFalse(ImageFileUtils.isImageFile("archive.zip"))
        assertFalse(ImageFileUtils.isImageFile("no_extension"))
    }

    @Test
    fun testShouldIgnore() {
        assertTrue(ImageFileUtils.shouldIgnore("__MACOSX/image.jpg"))
        assertTrue(ImageFileUtils.shouldIgnore(".DS_Store"))
        assertTrue(ImageFileUtils.shouldIgnore("Thumbs.db"))
        assertTrue(ImageFileUtils.shouldIgnore("desktop.ini"))
        assertTrue(ImageFileUtils.shouldIgnore("._.DS_Store"))

        assertFalse(ImageFileUtils.shouldIgnore("normal_file.jpg"))
        assertFalse(ImageFileUtils.shouldIgnore("folder/page1.png"))
    }

    @Test
    fun testGetMimeType() {
        assertEquals("image/jpeg", ImageFileUtils.getMimeType("test.jpg"))
        assertEquals("image/jpeg", ImageFileUtils.getMimeType("test.jpeg"))
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

        assertFalse(ImageFileUtils.isArchive("test.jpg"))
        assertFalse(ImageFileUtils.isArchive("test.pdf"))
    }

    @Test
    fun testNaturalOrderComparator() {
        val comparator = ImageFileUtils.naturalOrderComparator

        // Basic string comparison (no numbers)
        assertTrue(comparator.compare("a", "b") < 0)
        assertTrue(comparator.compare("b", "a") > 0)
        assertTrue(comparator.compare("a", "a") == 0)

        // Simple numeric comparison
        assertTrue(comparator.compare("2", "10") < 0)
        assertTrue(comparator.compare("10", "2") > 0)

        // Numeric comparison with text
        assertTrue(comparator.compare("page2", "page10") < 0)
        assertTrue(comparator.compare("page10", "page2") > 0)
        assertTrue(comparator.compare("2.jpg", "10.jpg") < 0)

        // Leading zeros (current implementation: page01 and page1 are equal if numeric part is 1)
        // Actually, let's see how current implementation handles it:
        // "page01" vs "page1"
        // numA = 1, numB = 1 -> cmp = 0. But after the while loops, i and j are at the end.
        // Wait, if numA and numB are equal, it doesn't return, it continues the loop.
        // In "page01" vs "page1":
        // p=p, a=a, g=g, e=e. Then i=4 ('0'), j=4 ('1').
        // ca='0', cb='1'. Both are digits.
        // numA: '0' -> 0, '1' -> 1. numA = 1. i = 6.
        // numB: '1' -> 1. numB = 1. j = 5.
        // cmp = 0. Loop continues.
        // a.length = 6, b.length = 5.
        // Returns 6 - 5 = 1. So "page01" > "page1".
        assertTrue(comparator.compare("page01", "page1") > 0)
        assertTrue(comparator.compare("page1", "page01") < 0)

        // Multiple numeric blocks
        assertTrue(comparator.compare("vol1_page10", "vol2_page1") < 0)
        assertTrue(comparator.compare("vol1_page1", "vol1_page10") < 0)

        // Case insensitivity
        assertTrue(comparator.compare("Page2", "page10") < 0)
        assertTrue(comparator.compare("page2", "Page10") < 0)
        assertTrue(comparator.compare("a", "A") == 0)

        // Empty strings
        assertTrue(comparator.compare("", "a") < 0)
        assertTrue(comparator.compare("a", "") > 0)
        assertTrue(comparator.compare("", "") == 0)

        // Strings of different lengths
        assertTrue(comparator.compare("abc", "abcd") < 0)
        assertTrue(comparator.compare("abcd", "abc") > 0)

        // Large numbers (Long.MAX_VALUE is about 9e18)
        // Our implementation uses Long, so it should handle up to 18-19 digits.
        assertTrue(comparator.compare("1000000000000000000", "2000000000000000000") < 0)
    }
}
