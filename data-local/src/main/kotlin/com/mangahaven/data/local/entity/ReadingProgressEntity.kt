package com.mangahaven.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Room 实体：阅读进度。
 */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = LibraryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReadingProgressEntity(
    @PrimaryKey
    val itemId: String,
    val currentPage: Int,
    val totalPages: Int,
    val scrollOffset: Float? = null,
    val readingMode: String = "LEFT_TO_RIGHT",
    val updatedAt: Long,
)
