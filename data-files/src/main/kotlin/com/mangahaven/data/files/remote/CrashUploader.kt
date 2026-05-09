package com.mangahaven.data.files.remote

import android.content.Intent
import timber.log.Timber
import java.io.File

object CrashUploader {

    // 开发/测试用占位邮箱，正式发布前应替换为真实的客服邮箱
    private const val EMAIL_ADDRESS = "dev@example.com"
    private const val MAX_LOG_CHARS = 50_000

    /**
     * 读取日志文件并构建分享 Intent。
     * 内部仅读取前 [MAX_LOG_CHARS] 个字符，不会将整个文件加载到内存。
     * 应在 IO 线程调用。
     */
    fun buildLogIntent(logFile: File): Intent {
        val content = readLogPreview(logFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, "MangaHaven Crash Log / Debug Report")
            putExtra(Intent.EXTRA_TEXT, "Log content:\n\n$content")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * 读取日志文件前 [MAX_LOG_CHARS] 个字符；超出时追加截断标记。
     * 使用 buffered reader 逐块读取，不加载完整文件到内存。
     * 应在 IO 线程调用。
     */
    private fun readLogPreview(logFile: File): String {
        val buffer = CharArray(MAX_LOG_CHARS)
        var totalRead = 0
        logFile.bufferedReader().use { reader ->
            totalRead = reader.read(buffer, 0, MAX_LOG_CHARS)
        }
        if (totalRead < 0) return ""
        val preview = String(buffer, 0, totalRead)
        // 若读满了缓冲区，说明文件内容超出限制
        return if (totalRead == MAX_LOG_CHARS) {
            "$preview\n\n[truncated — full log is ~${logFile.length() / 1024}KB]"
        } else {
            preview
        }
    }

}
