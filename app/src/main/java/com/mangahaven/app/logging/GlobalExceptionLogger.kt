package com.mangahaven.app.logging

import timber.log.Timber
import com.mangahaven.data.files.remote.CrashUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GlobalExceptionLogger(
    private val crashLogStore: CrashLogStore,
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()


    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            crashLogStore.append(throwable = throwable, threadName = thread.name)

            // Try to upload the latest log file if possible
            val latestLog = crashLogStore.latestLogFile()
            if (latestLog != null) {
                // We use global scope here because the app is crashing
                CoroutineScope(Dispatchers.IO).launch {
                    val result = CrashUploader.uploadLogFile(context = crashLogStore.getContext(), logFile = latestLog)
                    if (result.isSuccess) {
                        Timber.i("Crash log successfully uploaded before process death: %s", result.getOrNull())
                    } else {
                        Timber.e("Failed to upload crash log before process death")
                    }
                }
            }
        }.onFailure { error ->
            Timber.e(error, "Failed to persist crash log")
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }


    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
}
