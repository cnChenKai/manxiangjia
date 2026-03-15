package com.mangahaven.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * DataStore 包装类，用于持久化 APP 级别的配置（例如应用锁）。
 */
@Singleton
class AppSettingsDataStore @Inject constructor(
    private val context: Context,
) {
    private object Keys {
        val PRIVACY_LOCK_ENABLED = booleanPreferencesKey("privacy_lock_enabled")
    }

    /**
     * 隐私锁配置流。
     */
    val privacyLockEnabledFlow: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[Keys.PRIVACY_LOCK_ENABLED] ?: false
    }

    /**
     * 更新隐私锁配置。
     */
    suspend fun setPrivacyLockEnabled(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[Keys.PRIVACY_LOCK_ENABLED] = enabled
        }
    }
}
