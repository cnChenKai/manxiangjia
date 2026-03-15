package com.mangahaven.data.local.repository

import com.mangahaven.data.files.ProgressRepository
import com.mangahaven.data.local.dao.ReadingProgressDao
import com.mangahaven.data.local.mapper.toEntity
import com.mangahaven.data.local.mapper.toModel
import com.mangahaven.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地阅读进度仓库实现。
 */
@Singleton
class LocalProgressRepository @Inject constructor(
    private val readingProgressDao: ReadingProgressDao,
) : ProgressRepository {

    override suspend fun getProgress(itemId: String): ReadingProgress? =
        readingProgressDao.getByItemId(itemId)?.toModel()

    override suspend fun saveProgress(progress: ReadingProgress) {
        readingProgressDao.insertOrUpdate(progress.toEntity())
    }

    /**
     * 观察特定条目的阅读进度变化。
     */
    fun observeProgress(itemId: String): Flow<ReadingProgress?> =
        readingProgressDao.observeByItemId(itemId).map { it?.toModel() }
}
