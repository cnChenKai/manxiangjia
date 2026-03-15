package com.mangahaven.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mangahaven.model.ReadingMode
import com.mangahaven.model.ReaderSettings
import com.mangahaven.model.TapZoneProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reader_settings"
)

/**
 * DataStore 包装类，用于持久化全局阅读设置。
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context,
) {
    private object Keys {
        val READING_MODE = stringPreferencesKey("reading_mode")
        val ENABLE_CROP = booleanPreferencesKey("enable_crop")
        val ENABLE_PRELOAD = booleanPreferencesKey("enable_preload")
        val DOUBLE_PAGE_MODE = booleanPreferencesKey("double_page_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val VOLUME_KEYS_PAGING = booleanPreferencesKey("volume_keys_paging")
        val TAP_ZONE_PROFILE = stringPreferencesKey("tap_zone_profile")
    }

    /**
     * 观察阅读设置变化。
     */
    val settingsFlow: Flow<ReaderSettings> = context.settingsDataStore.data.map { prefs ->
        ReaderSettings(
            readingMode = prefs[Keys.READING_MODE]?.let {
                runCatching { ReadingMode.valueOf(it) }.getOrNull()
            } ?: ReadingMode.LEFT_TO_RIGHT,
            enableCrop = prefs[Keys.ENABLE_CROP] ?: false,
            enablePreload = prefs[Keys.ENABLE_PRELOAD] ?: true,
            doublePageMode = prefs[Keys.DOUBLE_PAGE_MODE] ?: false,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            volumeKeysPaging = prefs[Keys.VOLUME_KEYS_PAGING] ?: true,
            tapZoneProfile = prefs[Keys.TAP_ZONE_PROFILE]?.let {
                runCatching { TapZoneProfile.valueOf(it) }.getOrNull()
            } ?: TapZoneProfile.DEFAULT,
        )
    }

    /**
     * 更新阅读设置。
     */
    suspend fun updateSettings(settings: ReaderSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.READING_MODE] = settings.readingMode.name
            prefs[Keys.ENABLE_CROP] = settings.enableCrop
            prefs[Keys.ENABLE_PRELOAD] = settings.enablePreload
            prefs[Keys.DOUBLE_PAGE_MODE] = settings.doublePageMode
            prefs[Keys.KEEP_SCREEN_ON] = settings.keepScreenOn
            prefs[Keys.VOLUME_KEYS_PAGING] = settings.volumeKeysPaging
            prefs[Keys.TAP_ZONE_PROFILE] = settings.tapZoneProfile.name
        }
    }
}
