package com.mangahaven.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 实体：内容源。
 */
@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String,
    val configJson: String = "",
    val authRef: String? = null,
    val isWritable: Boolean = false,
    val lastSyncAt: Long? = null,
)
