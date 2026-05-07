package com.mangahaven.data.local.cover

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    fun testSaveCoverSuccess() = runBlocking {
        val itemId = "item1"
        val content = "test content".toByteArray()
        val inputStream = ByteArrayInputStream(content)

        val path = coverManager.saveCover(itemId, inputStream)

        assertNotNull(path)
        val savedFile = File(path!!)
        assertTrue(savedFile.exists())
        assertEquals(content.size.toLong(), savedFile.length())
        assertEquals("$itemId.jpg", savedFile.name)
    }

    @Test
    fun testSaveCoverError() = runBlocking {
        val itemId = "item_error"
        val inputStream = object : InputStream() {
            override fun read(): Int = throw IOException("Mocked IO failure")
            override fun read(b: ByteArray, off: Int, len: Int): Int = throw IOException("Mocked IO failure")
        }

        val path = coverManager.saveCover(itemId, inputStream)

        assertNull(path)
        val coverFile = File(coverDir, "$itemId.jpg")
        // The file might be created but should be empty or partially written if it failed during copy
        // In our case, copyTo will fail immediately on first read.
    }

    @Test
    fun testGetCoverPath() = runBlocking {
        val itemId = "item2"
        assertNull(coverManager.getCoverPath(itemId))

        val content = "test content".toByteArray()
        coverManager.saveCover(itemId, ByteArrayInputStream(content))

        val path = coverManager.getCoverPath(itemId)
        assertNotNull(path)
        assertTrue(File(path!!).exists())
    }

    @Test
    fun testDeleteCover() = runBlocking {
        val itemId = "item3"
        coverManager.saveCover(itemId, ByteArrayInputStream("test".toByteArray()))
        assertNotNull(coverManager.getCoverPath(itemId))

        coverManager.deleteCover(itemId)
        assertNull(coverManager.getCoverPath(itemId))
    }

    @Test
    fun testClearAll() = runBlocking {
        coverManager.saveCover("1", ByteArrayInputStream("1".toByteArray()))
        coverManager.saveCover("2", ByteArrayInputStream("2".toByteArray()))
        assertEquals(2, coverDir.listFiles()?.size)

        coverManager.clearAll()
        assertEquals(0, coverDir.listFiles()?.size)
    }

    @Test
    fun testGetCacheSize() = runBlocking {
        val data1 = "abc".toByteArray()
        val data2 = "defgh".toByteArray()
        coverManager.saveCover("1", ByteArrayInputStream(data1))
        coverManager.saveCover("2", ByteArrayInputStream(data2))

        assertEquals((data1.size + data2.size).toLong(), coverManager.getCacheSize())
    }
}
