package com.mangahaven.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 每本书独立阅读设置。
 * 所有字段为 null 表示跟随全局设置。
 */
@Entity(
    tableName = "item_settings",
    foreignKeys = [
        ForeignKey(
            entity = LibraryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class ItemSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: String,

    @ColumnInfo(name = "reading_mode")
    val readingMode: String? = null,

    @ColumnInfo(name = "crop_enabled")
    val cropEnabled: Boolean? = null,

    @ColumnInfo(name = "double_page_mode")
    val doublePageMode: Boolean? = null,

    @ColumnInfo(name = "page_offset")
    val pageOffset: Int? = null,

    @ColumnInfo(name = "volume_keys_paging")
    val volumeKeysPaging: Boolean? = null,
)
