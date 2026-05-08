package com.mangahaven.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mangahaven.data.local.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow

/**
 * 快照 DAO。
 */
@Dao
interface SnapshotDao {

    @Query("SELECT * FROM snapshots WHERE sourceId = :sourceId ORDER BY updatedAt DESC")
    fun observeBySource(sourceId: String): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots WHERE sourceId = :sourceId ORDER BY updatedAt DESC")
    suspend fun getBySource(sourceId: String): List<SnapshotEntity>

    @Query("SELECT * FROM snapshots WHERE id = :id")
    suspend fun getById(id: String): SnapshotEntity?

    @Query("SELECT * FROM snapshots WHERE sourceId = :sourceId AND normalizedTitle = :normalizedTitle LIMIT 1")
    suspend fun findByNormalizedTitle(sourceId: String, normalizedTitle: String): SnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: SnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<SnapshotEntity>)

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM snapshots WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    @Query("SELECT COUNT(*) FROM snapshots WHERE sourceId = :sourceId")
    suspend fun countBySource(sourceId: String): Int

    @Query("SELECT * FROM snapshots WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    suspend fun searchByTitle(query: String): List<SnapshotEntity>
}
