package com.mangahaven.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mangahaven.data.local.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * 阅读进度 DAO。
 */
@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE itemId = :itemId")
    suspend fun getByItemId(itemId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE itemId = :itemId")
    fun observeByItemId(itemId: String): Flow<ReadingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)
}
