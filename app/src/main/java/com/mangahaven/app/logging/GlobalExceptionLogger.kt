package com.mangahaven.app.logging

import timber.log.Timber

class GlobalExceptionLogger(
    private val crashLogStore: CrashLogStore,
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            crashLogStore.append(throwable = throwable, threadName = thread.name)
        }.onFailure { error ->
            Timber.e(error, "Failed to persist crash log")
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
}
