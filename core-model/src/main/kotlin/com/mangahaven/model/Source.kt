package com.mangahaven.model

import kotlinx.serialization.Serializable

/**
 * 表示内容来源。
 * 可以是本地目录、SAF树、SMB、WebDAV 或 OPDS。
 */
@Serializable
data class Source(
    val id: String,
    val type: SourceType,
    val name: String,
    val configJson: String = "",
    val authRef: String? = null,
    val isWritable: Boolean = false,
    val lastSyncAt: Long? = null,
)

/**
 * 内容源类型枚举。
 */
@Serializable
enum class SourceType {
    LOCAL,
    SAF_TREE,
    SMB,
    WEBDAV,
    OPDS,
}
