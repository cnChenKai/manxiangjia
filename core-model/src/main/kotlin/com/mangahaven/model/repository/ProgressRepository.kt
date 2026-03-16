package com.mangahaven.model.repository

import com.mangahaven.model.ReadingProgress

/**
 * 阅读进度仓库接口。
 */
interface ProgressRepository {

    /**
     * 获取指定条目的阅读进度。
     */
    suspend fun getProgress(itemId: String): ReadingProgress?

    /**
     * 保存阅读进度。
     */
    suspend fun saveProgress(progress: ReadingProgress)
}
