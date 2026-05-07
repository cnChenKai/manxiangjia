package com.mangahaven.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.mangahaven.data.local.AppSettingsDataStore
import com.mangahaven.data.local.SettingsDataStore
import com.mangahaven.data.local.cover.CoverManager
import com.mangahaven.model.ReaderSettings
import com.mangahaven.model.ReadingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val settingsDataStore: SettingsDataStore,
    private val coverManager: CoverManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

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

    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    init {
        updateCacheSize()
    }

    fun updateCacheSize() {
        viewModelScope.launch {
            val coverCacheSize = coverManager.getCacheSize()
            val coilCacheSize = context.imageLoader.diskCache?.size ?: 0L
            val totalBytes = coverCacheSize + coilCacheSize
            _cacheSize.value = formatSize(totalBytes)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            coverManager.clearAll()
            context.imageLoader.diskCache?.clear()
            context.imageLoader.memoryCache?.clear()
            updateCacheSize()
        }
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}
