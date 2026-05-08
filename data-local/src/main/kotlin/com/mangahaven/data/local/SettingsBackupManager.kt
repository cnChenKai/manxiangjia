package com.mangahaven.data.local

import com.mangahaven.data.local.dao.SourceDao
import com.mangahaven.data.local.mapper.toEntity
import com.mangahaven.data.local.mapper.toModel
import com.mangahaven.model.AppSettings
import com.mangahaven.model.ReaderSettings
import com.mangahaven.model.SettingsExportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置备份管理器，负责收集、序列化和恢复应用设置。
 */
@Singleton
class SettingsBackupManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val sourceDao: SourceDao,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * 收集当前所有设置并序列化为 JSON 字符串。
     */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val readerSettings = settingsDataStore.settingsFlow.first()
        val privacyLock = appSettingsDataStore.privacyLockEnabledFlow.first()
        val sources = sourceDao.getAll().map { it.toModel() }

        val data = SettingsExportData(
            readerSettings = readerSettings,
            appSettings = AppSettings(privacyLockEnabled = privacyLock),
            sources = sources,
        )
        json.encodeToString(data)
    }

    /**
     * 从 JSON 字符串导入设置并写入各存储。
     * @return 导入的源数量
     */
    suspend fun importFromJson(jsonStr: String): Int = withContext(Dispatchers.IO) {
        val data = json.decodeFromString<SettingsExportData>(jsonStr)

        settingsDataStore.updateSettings(data.readerSettings)
        appSettingsDataStore.setPrivacyLockEnabled(data.appSettings.privacyLockEnabled)

        // 导入远程源（WebDAV/SMB 等），覆盖写入
        for (source in data.sources) {
            sourceDao.insert(source.toEntity())
        }

        data.sources.size
    }
}
