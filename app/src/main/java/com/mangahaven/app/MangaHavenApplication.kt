package com.mangahaven.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * MangaHaven Application 入口。
 */
@HiltAndroidApp
class MangaHavenApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 仅在 debug 模式下启用 Timber 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
