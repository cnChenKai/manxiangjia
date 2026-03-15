package com.mangahaven.data.files.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.mangahaven.data.files.PageProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缩略图生成器。
 * 从 PageProvider 按指定尺寸生成缩略图并缓存到磁盘。
 */
@Singleton
class ThumbnailGenerator @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {
    companion object {
        private const val THUMBNAIL_WIDTH = 150
        private const val THUMBNAIL_HEIGHT = 200
        private const val THUMBNAIL_QUALITY = 75
    }

    private val thumbnailDir: File by lazy {
        File(context.cacheDir, "thumbnails").also { it.mkdirs() }
    }

    // 防止同一张缩略图被并发生成
    private val locks = mutableMapOf<String, Mutex>()
    private val locksMutex = Mutex()

    /**
     * 获取缩略图文件路径。如果缓存中没有，则按需生成。
     *
     * @param itemId 书架条目 ID
     * @param pageIndex 页码索引
     * @param pageProvider 页面数据提供者
     * @return 缩略图文件路径，生成失败返回 null
     */
    suspend fun getThumbnail(
        itemId: String,
        pageIndex: Int,
        pageProvider: PageProvider,
    ): String? = withContext(Dispatchers.IO) {
        val itemDir = File(thumbnailDir, itemId).also { it.mkdirs() }
        val thumbFile = File(itemDir, "page_$pageIndex.jpg")

        // 已存在直接返回
        if (thumbFile.exists() && thumbFile.length() > 0) {
            return@withContext thumbFile.absolutePath
        }

        // 获取或创建该 key 的锁
        val key = "$itemId/$pageIndex"
        val mutex = locksMutex.withLock {
            locks.getOrPut(key) { Mutex() }
        }

        // 加锁生成
        mutex.withLock {
            // 双重检查
            if (thumbFile.exists() && thumbFile.length() > 0) {
                return@withContext thumbFile.absolutePath
            }

            try {
                pageProvider.openPage(pageIndex).use { stream ->
                    // 先解码获取原始尺寸
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    // 读取到 byte array 以便两次解码
                    val bytes = stream.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                    // 计算采样率
                    options.inSampleSize = calculateInSampleSize(
                        options.outWidth, options.outHeight,
                        THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT
                    )
                    options.inJustDecodeBounds = false

                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        ?: return@withContext null

                    // 缩放到目标尺寸
                    val scaled = Bitmap.createScaledBitmap(
                        bitmap, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true
                    )

                    // 保存
                    thumbFile.outputStream().use { out ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
                    }

                    if (scaled !== bitmap) scaled.recycle()
                    bitmap.recycle()

                    thumbFile.absolutePath
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate thumbnail: $key")
                null
            }
        }
    }

    /**
     * 清理指定条目的缩略图缓存。
     */
    fun clearForItem(itemId: String) {
        File(thumbnailDir, itemId).deleteRecursively()
    }

    /**
     * 清理全部缩略图缓存。
     */
    fun clearAll() {
        thumbnailDir.deleteRecursively()
        thumbnailDir.mkdirs()
    }

    /**
     * 获取缓存大小（字节）。
     */
    fun getCacheSize(): Long {
        return thumbnailDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }

    private fun calculateInSampleSize(
        rawWidth: Int, rawHeight: Int,
        targetWidth: Int, targetHeight: Int,
    ): Int {
        var inSampleSize = 1
        if (rawHeight > targetHeight || rawWidth > targetWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while (halfHeight / inSampleSize >= targetHeight &&
                halfWidth / inSampleSize >= targetWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
