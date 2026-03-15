package com.mangahaven.data.local.cover

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 封面管理器。
 * 负责提取容器封面并缓存到应用私有目录。
 */
@Singleton
class CoverManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val coverDir: File by lazy {
        File(context.cacheDir, "covers").also { it.mkdirs() }
    }

    /**
     * 保存封面图片到缓存。
     * @param itemId 书架条目 ID
     * @param coverStream 封面图片的输入流
     * @return 保存后的文件路径
     */
    suspend fun saveCover(itemId: String, coverStream: InputStream): String? {
        return try {
            val coverFile = File(coverDir, "$itemId.jpg")
            coverStream.use { input ->
                coverFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            coverFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to save cover for item: $itemId")
            null
        }
    }

    /**
     * 获取封面文件路径。
     * @return 如果封面文件存在返回路径，否则返回 null
     */
    fun getCoverPath(itemId: String): String? {
        val coverFile = File(coverDir, "$itemId.jpg")
        return if (coverFile.exists()) coverFile.absolutePath else null
    }

    /**
     * 删除封面文件。
     */
    fun deleteCover(itemId: String) {
        val coverFile = File(coverDir, "$itemId.jpg")
        if (coverFile.exists()) {
            coverFile.delete()
        }
    }

    /**
     * 清理所有封面缓存。
     */
    fun clearAll() {
        coverDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * 获取封面缓存总大小（字节）。
     */
    fun getCacheSize(): Long {
        return coverDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
