package com.mangahaven.data.files.remote

import android.content.Context
import android.content.Intent
import timber.log.Timber
import java.io.File

object CrashUploader {

    // 开发/测试用占位邮箱，正式发布前应替换为真实的客服邮箱
    private const val EMAIL_ADDRESS = "dev@example.com"

    fun exportLogFile(context: Context, logFile: File) {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, "MangaHaven Crash Log / Debug Report")
            putExtra(Intent.EXTRA_TEXT, "Please find the attached log file.")

            // To attach the file, we need a FileProvider in a real app,
            // but for simple text logs, we can just append the text if it's small,
            // or use standard ContentProvider approach.
            // For simplicity in this early stage, let's just append the content if it's not huge.
            try {
                val fullContent = logFile.readText()
                val content = if (fullContent.length > 50000) {
                    fullContent.take(50000) + "\n\n[truncated — full log is ${fullContent.length} chars]"
                } else {
                    fullContent
                }
                putExtra(Intent.EXTRA_TEXT, "Log content:\n\n$content")
            } catch (e: Exception) {
                Timber.e(e, "Failed to read log file for email")
            }

            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Export Log").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Timber.e(e, "Failed to start export intent")
        }
    }
}
