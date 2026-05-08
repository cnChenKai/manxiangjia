package com.mangahaven.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.mangahaven.data.files.thumbnail.ThumbnailGenerator
import com.mangahaven.data.local.AppSettingsDataStore
import com.mangahaven.data.local.SettingsBackupManager
import com.mangahaven.data.local.SettingsDataStore
import com.mangahaven.data.local.cover.CoverManager
import com.mangahaven.model.ReaderSettings
import com.mangahaven.model.ReadingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val settingsDataStore: SettingsDataStore,
    private val coverManager: CoverManager,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val backupManager: SettingsBackupManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    init {
        updateCacheSize()
    }

    val privacyLockEnabled: StateFlow<Boolean> = appSettingsDataStore.privacyLockEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val readerSettings: StateFlow<ReaderSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReaderSettings()
        )

    fun togglePrivacyLock(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.setPrivacyLockEnabled(enabled)
        }
    }

    fun updateReadingMode() {
        viewModelScope.launch {
            val current = readerSettings.value.readingMode
            val next = if (current == ReadingMode.LEFT_TO_RIGHT) ReadingMode.RIGHT_TO_LEFT else ReadingMode.LEFT_TO_RIGHT
            settingsDataStore.updateSettings(readerSettings.value.copy(readingMode = next))
        }
    }

    fun toggleDoublePageMode(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateSettings(readerSettings.value.copy(doublePageMode = enabled)) }
    }

    fun toggleCropEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateSettings(readerSettings.value.copy(enableCrop = enabled)) }
    }

    fun togglePreload(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateSettings(readerSettings.value.copy(enablePreload = enabled)) }
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateSettings(readerSettings.value.copy(keepScreenOn = enabled)) }
    }

    /**
     * 导出设置到指定 URI。
     */
    fun exportSettings(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = backupManager.exportToJson()
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("无法打开导出文件")
                    output.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                _backupMessage.value = "设置已导出"
            } catch (e: Exception) {
                Timber.e(e, "导出设置失败")
                _backupMessage.value = "导出失败: ${e.message}"
            }
        }
    }

    /**
     * 从指定 URI 导入设置。
     */
    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("无法读取文件")
                    input.use { it.readBytes().toString(Charsets.UTF_8) }
                }
                val result = backupManager.importFromJson(json)
                _backupMessage.value = "导入成功：新增 ${result.insertedSources} 个远程源，更新 ${result.updatedSources} 个远程源"
            } catch (e: Exception) {
                Timber.e(e, "导入设置失败")
                _backupMessage.value = "导入失败: ${e.message}"
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                coverManager.clearAll()
                thumbnailGenerator.clearAll()
                context.imageLoader.diskCache?.clear()
                context.imageLoader.memoryCache?.clear()
            }
            updateCacheSize()
        }
    }

    private fun updateCacheSize() {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) {
                val coverSize = coverManager.getCacheSize()
                val thumbnailSize = thumbnailGenerator.getCacheSize()
                val coilSize = context.imageLoader.diskCache?.size ?: 0L
                coverSize + thumbnailSize + coilSize
            }
            _cacheSize.value = formatSize(size)
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.getDefault(), "%.2f MB", mb)
    }
}
