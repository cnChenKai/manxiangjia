package com.mangahaven.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 实体：标签。
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    /** 标签颜色，十六进制格式 */
    val color: String = "#607D8B",
)
