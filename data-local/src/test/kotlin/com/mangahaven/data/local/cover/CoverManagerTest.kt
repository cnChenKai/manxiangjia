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

    private lateinit var coverManager: CoverManager
    private lateinit var baseDir: File

    @Before
    fun setup() {
        baseDir = tempFolder.newFolder("cache")
        coverManager = CoverManager(baseDir)
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
        assertEquals(File(baseDir, "covers/$itemId.jpg").absolutePath, path)
    }

    @Test
    fun `saveCover returns null when inputStream throws exception`() = runTest {
        val itemId = "error_item"
        val inputStream = object : InputStream() {
            override fun read(): Int = throw IOException("Simulated error")
        }

        val path = coverManager.saveCover(itemId, inputStream)

        assertNull(path)
    }

    @Test
    fun `getCoverPath returns correct path when file exists`() = runTest {
        val itemId = "existing_item"
        val coversDir = File(baseDir, "covers")
        coversDir.mkdirs()
        val coverFile = File(coversDir, "$itemId.jpg")
        coverFile.writeText("content")

        val path = coverManager.getCoverPath(itemId)

        assertEquals(coverFile.absolutePath, path)
    }

    @Test
    fun `getCoverPath returns null when file does not exist`() {
        val path = coverManager.getCoverPath("non_existent")
        assertNull(path)
    }

    @Test
    fun `deleteCover removes the file`() {
        val itemId = "delete_item"
        val coversDir = File(baseDir, "covers")
        coversDir.mkdirs()
        val coverFile = File(coversDir, "$itemId.jpg")
        coverFile.writeText("content")
        assertTrue(coverFile.exists())

        coverManager.deleteCover(itemId)

        assertFalse(coverFile.exists())
    }

    @Test
    fun `clearAll deletes all files in covers directory`() {
        val coversDir = File(baseDir, "covers")
        coversDir.mkdirs()
        File(coversDir, "1.jpg").writeText("1")
        File(coversDir, "2.jpg").writeText("2")
        assertEquals(2, coversDir.listFiles()?.size)

        coverManager.clearAll()

        assertEquals(0, coversDir.listFiles()?.size)
    }

    @Test
    fun `getCacheSize returns sum of file sizes`() {
        val coversDir = File(baseDir, "covers")
        coversDir.mkdirs()
        val f1 = File(coversDir, "1.jpg")
        f1.writeText("123") // 3 bytes
        val f2 = File(coversDir, "2.jpg")
        f2.writeText("12345") // 5 bytes

        val size = coverManager.getCacheSize()

        assertEquals(8L, size)
    }
}
