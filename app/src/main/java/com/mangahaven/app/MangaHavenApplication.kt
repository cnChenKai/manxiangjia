package com.mangahaven.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.mangahaven.app.logging.CrashLogStore
import com.mangahaven.app.logging.GlobalExceptionLogger
import com.mangahaven.data.files.worker.LibrarySyncWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.mangahaven.app.logging.FileLoggingTree
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * MangaHaven Application 入口。
 */
@HiltAndroidApp
class MangaHavenApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // 仅在 debug 模式下启用 Timber 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(FileLoggingTree(this))
        }


        val crashLogStore = CrashLogStore(this)
        GlobalExceptionLogger(crashLogStore).install()
        Timber.i("Crash logs directory: %s", crashLogStore.logsDirectoryPath())

        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(appLockManager)

        scheduleBackgroundSync()
    }

    @Inject
    lateinit var appLockManager: AppLockManager

    private fun scheduleBackgroundSync() {
        // 配置环境约束：网络畅通时才同步
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<LibrarySyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LibrarySyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
