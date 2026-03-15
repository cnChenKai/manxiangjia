package com.mangahaven.data.local.repository

import com.mangahaven.data.local.SettingsDataStore
import com.mangahaven.data.local.dao.ItemSettingsDao
import com.mangahaven.data.local.entity.ItemSettingsEntity
import com.mangahaven.model.ItemReaderSettingsOverride
import com.mangahaven.model.ReadingMode
import com.mangahaven.model.ReaderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每本书独立设置仓库。
 * 提供合并逻辑：全局设置 + 独立覆盖 = 最终生效设置。
 */
@Singleton
class ItemSettingsRepository @Inject constructor(
    private val itemSettingsDao: ItemSettingsDao,
    private val settingsDataStore: SettingsDataStore,
) {
    /**
     * 获取某本书的独立设置。
     */
    suspend fun getOverride(itemId: String): ItemReaderSettingsOverride? {
        return itemSettingsDao.getByItemId(itemId)?.toModel()
    }

    /**
     * 观察某本书的独立设置。
     */
    fun observeOverride(itemId: String): Flow<ItemReaderSettingsOverride?> {
        return itemSettingsDao.observeByItemId(itemId).map { it?.toModel() }
    }

    /**
     * 保存某本书的独立设置。
     */
    suspend fun saveOverride(override: ItemReaderSettingsOverride) {
        itemSettingsDao.insertOrUpdate(override.toEntity())
    }

    /**
     * 删除某本书的独立设置（恢复为跟随全局）。
     */
    suspend fun resetToGlobal(itemId: String) {
        itemSettingsDao.deleteByItemId(itemId)
    }

    /**
     * 解析最终生效的阅读设置：独立设置覆盖全局设置。
     */
    fun resolveSettings(itemId: String): Flow<ResolvedReaderSettings> {
        return combine(
            settingsDataStore.settingsFlow,
            itemSettingsDao.observeByItemId(itemId)
        ) { global, override ->
            ResolvedReaderSettings(
                readingMode = override?.readingMode?.let {
                    runCatching { ReadingMode.valueOf(it) }.getOrNull()
                } ?: global.readingMode,
                enableCrop = override?.cropEnabled ?: global.enableCrop,
                doublePageMode = override?.doublePageMode ?: global.doublePageMode,
                pageOffset = override?.pageOffset ?: 0,
                keepScreenOn = global.keepScreenOn,
                enablePreload = global.enablePreload,
                isOverridden = override != null,
            )
        }
    }

    // --- 映射函数 ---

    private fun ItemSettingsEntity.toModel(): ItemReaderSettingsOverride {
        return ItemReaderSettingsOverride(
            itemId = itemId,
            readingMode = readingMode?.let {
                runCatching { ReadingMode.valueOf(it) }.getOrNull()
            },
            cropEnabled = cropEnabled,
            doublePageMode = doublePageMode,
            pageOffset = pageOffset,
        )
    }

    private fun ItemReaderSettingsOverride.toEntity(): ItemSettingsEntity {
        return ItemSettingsEntity(
            itemId = itemId,
            readingMode = readingMode?.name,
            cropEnabled = cropEnabled,
            doublePageMode = doublePageMode,
            pageOffset = pageOffset,
        )
    }
}

/**
 * 合并后的最终生效阅读设置。
 */
data class ResolvedReaderSettings(
    val readingMode: ReadingMode,
    val enableCrop: Boolean,
    val doublePageMode: Boolean,
    val pageOffset: Int,
    val keepScreenOn: Boolean,
    val enablePreload: Boolean,
    val isOverridden: Boolean,
)
