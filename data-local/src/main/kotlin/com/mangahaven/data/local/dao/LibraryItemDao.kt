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
}
