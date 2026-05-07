package com.mangahaven.app.logging

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(context: Context) : Timber.DebugTree() {
    private val logFile: File

    init {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        logFile = File(logDir, "log_$dateString.txt")
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)

        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
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

            FileWriter(logFile, true).use { writer ->
                writer.append(logMessage)
                t?.let {
                    writer.append(Log.getStackTraceString(it))
                    writer.append("\n")
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Error writing to log file", e)
        }
    }
}
