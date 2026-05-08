package com.mangahaven.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room 实体：书架条目与标签的多对多关联表。
 */
@Entity(
    tableName = "library_item_tags",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("itemId"),
        Index("tagId"),
    ],
)
data class LibraryItemTag(
    val itemId: String,
    val tagId: String,
)
