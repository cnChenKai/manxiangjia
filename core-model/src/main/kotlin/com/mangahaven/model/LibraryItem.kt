package com.mangahaven.model

import kotlinx.serialization.Serializable

/**
 * 表示书架中的作品或条目。
 */
@Serializable
data class LibraryItem(
    val id: String,
    val sourceId: String,
    val path: String,
    val title: String,
    val coverPath: String? = null,
    val itemType: LibraryItemType,
    val pageCount: Int? = null,
    val readingStatus: ReadingStatus = ReadingStatus.UNREAD,
    val lastReadAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * 书架条目类型。
 */
@Serializable
enum class LibraryItemType {
    FOLDER,
    ARCHIVE,
    BOOK,
    REMOTE_ENTRY,
}

/**
 * 阅读状态。
 */
@Serializable
enum class ReadingStatus {
    UNREAD,
    READING,
    COMPLETED,
}
