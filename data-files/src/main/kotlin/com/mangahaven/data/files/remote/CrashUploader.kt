package com.mangahaven.data.files.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.IOException

object CrashUploader {

    // 开发/测试用占位值，正式发布前应替换为实际的崩溃上报服务地址
    private const val UPLOAD_URL = "https://0x0.st"
    // 开发/测试用占位邮箱，正式发布前应替换为真实的客服邮箱
    private const val EMAIL_ADDRESS = "dev@example.com"

    suspend fun uploadLogFile(context: Context, logFile: File): Result<String> = withContext(Dispatchers.IO) {
        if (!logFile.exists()) {
            return@withContext Result.failure(Exception("Log file does not exist: ${logFile.absolutePath}"))
        }

        val client = OkHttpClient.Builder().build()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                logFile.name,
                logFile.asRequestBody("text/plain".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = "Upload failed with code ${response.code}: ${response.message}"
                    Timber.e(errorMsg)
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val url = response.body?.string()?.trim()
                if (url.isNullOrBlank()) {
                    val errorMsg = "Upload succeeded but returned empty URL"
                    Timber.e(errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                Timber.i("Successfully uploaded log file to: $url")
                return@withContext Result.success(url)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error uploading log file")
            return@withContext Result.failure(e)
        }
    }

    fun sendLogFileViaEmail(context: Context, logFile: File) {
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
                val content = logFile.readText().take(50000) // limit to 50k chars
                putExtra(Intent.EXTRA_TEXT, "Log content:\n\n$content")
            } catch (e: Exception) {
                Timber.e(e, "Failed to read log file for email")
            }

            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Send Log via Email").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Timber.e(e, "Failed to start email intent")
        }
    }
}
