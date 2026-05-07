package com.mangahaven.data.local.cover

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class CoverManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var coverDir: File
    private lateinit var coverManager: CoverManager

    @Before
    fun setup() {
        coverDir = tempFolder.newFolder("covers")
        coverManager = CoverManager(context = null, overrideCoverDir = coverDir)
    }

    @Test
    fun `saveCover successfully saves a cover`() = runTest {
        val itemId = "test_item"
        val content = "fake image content".toByteArray()
        val inputStream = ByteArrayInputStream(content)

        val path = coverManager.saveCover(itemId, inputStream)

        assertNotNull(path)
        val savedFile = File(path!!)
        assertTrue(savedFile.exists())
        assertEquals(content.size.toLong(), savedFile.length())
        assertEquals("$itemId.jpg", savedFile.name)
    }

    @Test
    fun `saveCover returns null when inputStream throws exception`() = runTest {
        val itemId = "error_item"
        val inputStream = object : InputStream() {
            override fun read(): Int = throw IOException("Simulated error")
            override fun read(b: ByteArray, off: Int, len: Int): Int = throw IOException("Simulated error")
        }

        val path = coverManager.saveCover(itemId, inputStream)

        assertNull(path)
    }

    @Test
    fun `getCoverPath returns correct path when file exists`() = runTest {
        val itemId = "existing_item"
        coverManager.saveCover(itemId, ByteArrayInputStream("content".toByteArray()))

        val path = coverManager.getCoverPath(itemId)

        assertNotNull(path)
        assertTrue(File(path!!).exists())
    }

    @Test
    fun `getCoverPath returns null when file does not exist`() {
        val path = coverManager.getCoverPath("non_existent")
        assertNull(path)
    }

    @Test
    fun `deleteCover removes the file`() = runTest {
        val itemId = "delete_item"
        coverManager.saveCover(itemId, ByteArrayInputStream("content".toByteArray()))
        assertNotNull(coverManager.getCoverPath(itemId))

        coverManager.deleteCover(itemId)

        assertNull(coverManager.getCoverPath(itemId))
    }

    @Test
    fun `clearAll deletes all files in covers directory`() = runTest {
        coverManager.saveCover("1", ByteArrayInputStream("1".toByteArray()))
        coverManager.saveCover("2", ByteArrayInputStream("2".toByteArray()))
        assertEquals(2, coverDir.listFiles()?.size)

        coverManager.clearAll()

        assertEquals(0, coverDir.listFiles()?.size)
    }

    @Test
    fun `getCacheSize returns sum of file sizes`() = runTest {
        val data1 = "abc".toByteArray()
        val data2 = "defgh".toByteArray()
        coverManager.saveCover("1", ByteArrayInputStream(data1))
        coverManager.saveCover("2", ByteArrayInputStream(data2))

        assertEquals((data1.size + data2.size).toLong(), coverManager.getCacheSize())
    }
}
