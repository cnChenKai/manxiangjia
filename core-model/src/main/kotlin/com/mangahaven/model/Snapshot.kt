package com.mangahaven.model

import kotlinx.serialization.Serializable

/**
 * 远程书库的搜索快照。
 * Phase 4 使用，Phase 0 先定义数据结构。
 */
@Serializable
data class Snapshot(
    val id: String,
    val sourceId: String,
    val path: String,
    val title: String,
    val normalizedTitle: String,
    val tags: List<String> = emptyList(),
    val pageCount: Int? = null,
    val updatedAt: Long,
)

/**
 * 内容源目录条目。
 * 表示 SourceClient 返回的文件/目录信息。
 */
@Serializable
data class SourceEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long? = null,
    val lastModified: Long? = null,
    val mimeType: String? = null,
)

/**
 * 容器目标。
 * 表示一个待读取的漫画容器（目录或压缩包）。
 */
@Serializable
data class ContainerTarget(
    val sourceId: String,
    val path: String,
    val itemType: LibraryItemType,
)
