package com.mangahaven.app.logging

import android.content.Context
import com.mangahaven.app.BuildConfig
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CrashLogStore(
    private val context: Context,
) {
    private val logsDir: File
        get() = File(context.filesDir, LOG_DIR).apply { mkdirs() }

    fun append(throwable: Throwable, threadName: String) {
        val timestamp = Instant.now()
        val file = File(logsDir, "crash-${FILE_TS.format(timestamp)}.log")

        file.bufferedWriter().use { writer ->
            writer.appendLine("timestamp=${DISPLAY_TS.format(timestamp)}")
            writer.appendLine("thread=$threadName")
            writer.appendLine("appVersion=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            writer.appendLine("message=${throwable.message.orEmpty()}")
            writer.appendLine()
            writer.appendLine("stacktrace:")
            writer.appendLine(throwable.stackTraceToString())
        }

        pruneOldFiles()
    }

    fun latestLogFile(): File? = logsDir
        .listFiles { file -> file.extension == "log" }
        ?.maxByOrNull { it.lastModified() }

    fun logsDirectoryPath(): String = logsDir.absolutePath

    private fun pruneOldFiles() {
        val files = logsDir.listFiles { file -> file.extension == "log" }?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_LOG_FILES).forEach { it.delete() }
    }

    companion object {
        private const val LOG_DIR = "crash-logs"
        private const val MAX_LOG_FILES = 20
        private val FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
        private val DISPLAY_TS = DateTimeFormatter.ISO_INSTANT
    }
}
