package com.mangahaven.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room 实体：书架条目。
 */
@Entity(
    tableName = "library_items",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("sourceId"),
        Index("title"),
        Index("lastReadAt"),
        Index("updatedAt"),
    ],
)
data class LibraryItemEntity(
    @PrimaryKey
    val id: String,
    val sourceId: String,
    val path: String,
    val title: String,
    val coverPath: String? = null,
    val itemType: String,
    val pageCount: Int? = null,
    val readingStatus: String = "UNREAD",
    val lastReadAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
