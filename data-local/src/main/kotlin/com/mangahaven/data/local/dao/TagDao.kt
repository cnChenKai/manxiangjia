package com.mangahaven.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mangahaven.data.local.entity.LibraryItemTag
import com.mangahaven.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * 标签 DAO。
 */
@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity)

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: String)

    // --- 关联表操作 ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTag(crossRef: LibraryItemTag)

    @Delete
    suspend fun deleteItemTag(crossRef: LibraryItemTag)

    @Query("DELETE FROM library_item_tags WHERE itemId = :itemId AND tagId = :tagId")
    suspend fun deleteItemTagByIds(itemId: String, tagId: String)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN library_item_tags lit ON t.id = lit.tagId
        WHERE lit.itemId = :itemId
        ORDER BY t.name ASC
    """)
    fun observeTagsForItem(itemId: String): Flow<List<TagEntity>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN library_item_tags lit ON t.id = lit.tagId
        WHERE lit.itemId = :itemId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsForItem(itemId: String): List<TagEntity>

    @Query("SELECT itemId FROM library_item_tags WHERE tagId = :tagId")
    suspend fun getItemIdsByTag(tagId: String): List<String>
}
