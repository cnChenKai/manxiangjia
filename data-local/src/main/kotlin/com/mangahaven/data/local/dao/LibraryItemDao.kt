package com.mangahaven.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mangahaven.data.local.entity.LibraryItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书架条目 DAO。
 */
@Dao
interface LibraryItemDao {

    @Query("SELECT * FROM library_items ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE id = :id")
    suspend fun getById(id: String): LibraryItemEntity?

    @Query("SELECT * FROM library_items WHERE sourceId = :sourceId ORDER BY title ASC")
    fun observeBySource(sourceId: String): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT :limit")
    fun observeRecentlyRead(limit: Int = 10): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE readingStatus = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: String): Flow<List<LibraryItemEntity>>

    @Query("""
        SELECT * FROM library_items 
        WHERE (:query IS NULL OR title LIKE '%' || :query || '%')
        AND (:status IS NULL OR readingStatus = :status)
        AND (:isFavorite IS NULL OR isFavorite = :isFavorite)
        ORDER BY 
            CASE WHEN :sortBy = 'RECENT_READ' THEN lastReadAt END DESC,
            CASE WHEN :sortBy = 'RECENT_ADDED' THEN createdAt END DESC,
            CASE WHEN :sortBy = 'TITLE' THEN title END ASC,
            updatedAt DESC
    """)
    fun searchAndFilter(
        query: String?,
        status: String?,
        isFavorite: Boolean?,
        sortBy: String
    ): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchByTitle(query: String): Flow<List<LibraryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LibraryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LibraryItemEntity>)

    @Update
    suspend fun update(item: LibraryItemEntity)

    @Delete
    suspend fun delete(item: LibraryItemEntity)

    @Query("DELETE FROM library_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM library_items")
    suspend fun count(): Int

    /** 按阅读状态统计数量 */
    @Query("SELECT COUNT(*) FROM library_items WHERE readingStatus = :status")
    suspend fun countByStatus(status: String): Int

    /** 收藏条目数量 */
    @Query("SELECT COUNT(*) FROM library_items WHERE isFavorite = 1")
    suspend fun countFavorites(): Int

    /** 远程条目数量 */
    @Query("SELECT COUNT(*) FROM library_items WHERE itemType = 'REMOTE_ENTRY'")
    suspend fun countRemoteItems(): Int
}
