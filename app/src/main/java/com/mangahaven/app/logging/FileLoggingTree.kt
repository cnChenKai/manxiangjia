package com.mangahaven.app.logging

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import java.io.BufferedWriter
import java.util.concurrent.atomic.AtomicInteger

class FileLoggingTree(context: Context) : Timber.DebugTree() {
    private val logFile: File
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private var writer: BufferedWriter? = null
    private val logCount = AtomicInteger(0)

    init {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        logFile = File(logDir, "log_$dateString.txt")
        try {
            writer = BufferedWriter(FileWriter(logFile, true))
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Error initializing writer", e)
        }
    }

    @Synchronized
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)

        try {
            val timestamp = timeFormat.format(Date())
            val priorityStr = when (priority) {
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }

            val logMessage = "$timestamp $priorityStr/$tag: $message\n"

            writer?.let { w ->
                w.append(logMessage)
                t?.let {
                    w.append(Log.getStackTraceString(it))
                    w.append("\n")
                }

                // Periodically flush (e.g. every 50 logs)
                if (logCount.incrementAndGet() % 50 == 0) {
                    w.flush()
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Error writing to log file", e)
        }
    }

    @Synchronized
    fun close() {
        try {
            writer?.flush()
            writer?.close()
            writer = null
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Error closing writer", e)
        }
    }
}
