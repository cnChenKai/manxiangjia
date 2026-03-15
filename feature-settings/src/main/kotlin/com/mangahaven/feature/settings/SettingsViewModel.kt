package com.mangahaven.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangahaven.data.local.AppSettingsDataStore
import com.mangahaven.data.local.SettingsDataStore
import com.mangahaven.model.ReaderSettings
import com.mangahaven.model.ReadingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val settingsDataStore: SettingsDataStore
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
}
