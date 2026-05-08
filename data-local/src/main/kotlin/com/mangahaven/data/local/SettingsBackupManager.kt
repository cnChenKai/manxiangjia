package com.mangahaven.data.local

import com.mangahaven.data.local.dao.SourceDao
import com.mangahaven.data.local.mapper.toEntity
import com.mangahaven.data.local.mapper.toModel
import com.mangahaven.model.AppSettings
import com.mangahaven.model.SettingsExportData
import com.mangahaven.model.SettingsImportResult
import com.mangahaven.model.SourceType
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
    private val settingsDataStore: ISettingsDataStore,
    private val appSettingsDataStore: IAppSettingsDataStore,
    private val sourceDao: SourceDao,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    companion object {
        /** 可迁移的远程源类型。 */
        private val MIGRATABLE_TYPES = setOf(
            SourceType.WEBDAV,
            SourceType.SMB,
            SourceType.OPDS,
        )

        /** 判断源是否可迁移（远程源）。 */
        fun isMigratable(sourceType: SourceType): Boolean =
            sourceType in MIGRATABLE_TYPES
    }

    /**
     * 收集当前设置并序列化为 JSON 字符串。
     * 只导出可迁移的远程源（WEBDAV/SMB/OPDS），不导出 LOCAL/SAF_TREE。
     */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val readerSettings = settingsDataStore.settingsFlow.first()
        val privacyLock = appSettingsDataStore.privacyLockEnabledFlow.first()
        val sources = sourceDao.getAll()
            .map { it.toModel() }
            .filter { isMigratable(it.type) }

        val data = SettingsExportData(
            readerSettings = readerSettings,
            appSettings = AppSettings(privacyLockEnabled = privacyLock),
            sources = sources,
        )
        json.encodeToString(data)
    }

    /**
     * 从 JSON 字符串导入设置并写入各存储。
     * 按 type + configJson 去重：已存在的源更新，不存在的源插入。
     * @return 导入结果，包含新增和更新的源数量
     */
    suspend fun importFromJson(jsonStr: String): SettingsImportResult = withContext(Dispatchers.IO) {
        val data = json.decodeFromString<SettingsExportData>(jsonStr)

        settingsDataStore.updateSettings(data.readerSettings)
        appSettingsDataStore.setPrivacyLockEnabled(data.appSettings.privacyLockEnabled)

        var inserted = 0
        var updated = 0

        // 只处理可迁移的远程源，忽略 LOCAL/SAF_TREE
        for (source in data.sources.filter { isMigratable(it.type) }) {
            val existing = sourceDao.findByTypeAndConfig(source.type.name, source.configJson)
            if (existing != null) {
                // 已存在同类型同配置的源，保留原 id 更新
                sourceDao.insert(source.copy(id = existing.id).toEntity())
                updated++
            } else {
                sourceDao.insert(source.toEntity())
                inserted++
            }
        }

        SettingsImportResult(insertedSources = inserted, updatedSources = updated)
    }
}
