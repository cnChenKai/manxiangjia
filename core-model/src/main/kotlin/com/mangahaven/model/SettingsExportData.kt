package com.mangahaven.model

import kotlinx.serialization.Serializable

/**
 * 设置导出数据，包含所有可导出的配置项。
 * 用于一键导出/导入功能。
 */
@Serializable
data class SettingsExportData(
    val version: Int = 1,
    val readerSettings: ReaderSettings = ReaderSettings(),
    val appSettings: AppSettings = AppSettings(),
    val sources: List<Source> = emptyList(),
)

/**
 * 应用级设置。
 */
@Serializable
data class AppSettings(
    val privacyLockEnabled: Boolean = false,
)
