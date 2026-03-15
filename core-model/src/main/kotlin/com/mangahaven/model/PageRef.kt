package com.mangahaven.model

import kotlinx.serialization.Serializable

/**
 * 页面引用，表示容器中的单页。
 */
@Serializable
data class PageRef(
    val index: Int,
    val name: String,
    val path: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
)
