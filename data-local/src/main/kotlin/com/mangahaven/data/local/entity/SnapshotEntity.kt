package com.mangahaven.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room 实体：远程书库搜索快照。
 * 记录远程源的条目快照，用于增量同步和变更检测。
 */
@Entity(
    tableName = "snapshots",
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
        Index("normalizedTitle"),
    ],
)
data class SnapshotEntity(
    @PrimaryKey
    val id: String,
    val sourceId: String,
    val path: String,
    val title: String,
    val normalizedTitle: String,
    /** 标签列表，JSON 序列化存储 */
    val tagsJson: String = "[]",
    val pageCount: Int? = null,
    val updatedAt: Long,
)
