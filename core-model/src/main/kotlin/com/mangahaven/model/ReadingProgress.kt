package com.mangahaven.model

import kotlinx.serialization.Serializable

/**
 * 阅读进度。
 */
@Serializable
data class ReadingProgress(
    val itemId: String,
    val currentPage: Int,
    val totalPages: Int,
    val scrollOffset: Float? = null,
    val readingMode: ReadingMode = ReadingMode.LEFT_TO_RIGHT,
    val updatedAt: Long,
)

/**
 * 阅读模式枚举。
 */
@Serializable
enum class ReadingMode {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    VERTICAL,
    CONTINUOUS_VERTICAL,
}
