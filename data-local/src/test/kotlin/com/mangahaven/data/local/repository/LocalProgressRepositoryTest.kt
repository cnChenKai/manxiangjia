package com.mangahaven.data.local.repository

import com.mangahaven.data.local.dao.ReadingProgressDao
import com.mangahaven.data.local.entity.ReadingProgressEntity
import com.mangahaven.model.ReadingMode
import com.mangahaven.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * [LocalProgressRepository] 的测试类。
 */
class LocalProgressRepositoryTest {

    private lateinit var fakeDao: FakeReadingProgressDao
    private lateinit var repository: LocalProgressRepository

    @Before
    fun setup() {
        fakeDao = FakeReadingProgressDao()
        repository = LocalProgressRepository(fakeDao)
    }

    @Test
    fun saveProgress_savesToDao() = runTest {
        val progress = ReadingProgress(
            itemId = "item1",
            currentPage = 5,
            totalPages = 100,
            scrollOffset = 0.5f,
            readingMode = ReadingMode.VERTICAL,
            updatedAt = 123456789L
        )

        repository.saveProgress(progress)

        val savedEntity = fakeDao.getByItemId("item1")
        assertNotNull(savedEntity)
        assertEquals("item1", savedEntity?.itemId)
        assertEquals(5, savedEntity?.currentPage)
        assertEquals(100, savedEntity?.totalPages)
        assertEquals(0.5f, savedEntity?.scrollOffset)
        assertEquals("VERTICAL", savedEntity?.readingMode)
        assertEquals(123456789L, savedEntity?.updatedAt)
    }

    @Test
    fun getProgress_returnsFromDao() = runTest {
        val entity = ReadingProgressEntity(
            itemId = "item1",
            currentPage = 10,
            totalPages = 50,
            scrollOffset = null,
            readingMode = "RIGHT_TO_LEFT",
            updatedAt = 987654321L
        )
        fakeDao.insertOrUpdate(entity)

        val result = repository.getProgress("item1")

        assertNotNull(result)
        assertEquals("item1", result?.itemId)
        assertEquals(10, result?.currentPage)
        assertEquals(50, result?.totalPages)
        assertEquals(null, result?.scrollOffset)
        assertEquals(ReadingMode.RIGHT_TO_LEFT, result?.readingMode)
        assertEquals(987654321L, result?.updatedAt)
    }

    @Test
    fun getProgress_returnsNullIfNotFound() = runTest {
        val result = repository.getProgress("non-existent")
        assertNull(result)
    }
}

/**
 * [ReadingProgressDao] 的伪造实现，用于测试。
 */
class FakeReadingProgressDao : ReadingProgressDao {
    private val progressMap = mutableMapOf<String, ReadingProgressEntity>()
    private val flows = mutableMapOf<String, MutableStateFlow<ReadingProgressEntity?>>()

    override suspend fun getByItemId(itemId: String): ReadingProgressEntity? {
        return progressMap[itemId]
    }

    override fun observeByItemId(itemId: String): Flow<ReadingProgressEntity?> {
        return flows.getOrPut(itemId) { MutableStateFlow(progressMap[itemId]) }.asStateFlow()
    }

    override suspend fun insertOrUpdate(progress: ReadingProgressEntity) {
        progressMap[progress.itemId] = progress
        flows[progress.itemId]?.value = progress
    }

    override suspend fun deleteByItemId(itemId: String) {
        progressMap.remove(itemId)
        flows[itemId]?.value = null
    }
}
