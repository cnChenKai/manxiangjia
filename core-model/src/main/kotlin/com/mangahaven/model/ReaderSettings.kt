package com.mangahaven.model

import kotlinx.serialization.Serializable

/**
 * 全局阅读设置。
 */
@Serializable
data class ReaderSettings(
    val readingMode: ReadingMode = ReadingMode.LEFT_TO_RIGHT,
    val enableCrop: Boolean = false,
    val enablePreload: Boolean = true,
    val doublePageMode: Boolean = false,
    val keepScreenOn: Boolean = true,
    val volumeKeysPaging: Boolean = true,
    val tapZoneProfile: TapZoneProfile = TapZoneProfile.DEFAULT,
)

/**
 * 点击区域配置。
 */
@Serializable
enum class TapZoneProfile {
    DEFAULT,
    LEFT_RIGHT,
    L_SHAPE,
    KINDLE_STYLE,
}

/**
 * 每本书单独覆盖的阅读设置。
 * 所有字段为 null 表示使用全局设置。
 */
@Serializable
data class ItemReaderSettingsOverride(
    val itemId: String,
    val readingMode: ReadingMode? = null,
    val cropEnabled: Boolean? = null,
    val doublePageMode: Boolean? = null,
    val volumeKeysPaging: Boolean? = null,
    val pageOffset: Int? = null,
)
