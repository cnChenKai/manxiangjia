package com.mangahaven.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mangahaven.data.local.entity.ItemSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * 每本书独立设置 DAO。
 */
@Dao
interface ItemSettingsDao {

    @Query("SELECT * FROM item_settings WHERE item_id = :itemId")
    suspend fun getByItemId(itemId: String): ItemSettingsEntity?

    @Query("SELECT * FROM item_settings WHERE item_id = :itemId")
    fun observeByItemId(itemId: String): Flow<ItemSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ItemSettingsEntity)

    @Query("DELETE FROM item_settings WHERE item_id = :itemId")
    suspend fun deleteByItemId(itemId: String)
}
