package com.mangahaven.data.files.cache

import android.content.Context
import com.mangahaven.data.files.SourceClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 远程压缩包缓存管理器。
 * 负责将远程 ZIP/CBZ/RAR 文件下载到本地缓存目录，并提供缓存失效和清理能力。
 *
 * 缓存策略：
 * - 以 sourceId + 路径的哈希作为缓存文件名
 * - 缓存元数据文件记录远程文件的大小和最后修改时间
 * - 当远程文件的大小或修改时间变化时自动重新下载
 */
@Singleton
class RemoteArchiveCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** 缓存目录 */
    private val cacheDir: File by lazy {
        File(context.cacheDir, "remote_archives").also { it.mkdirs() }
    }

    /** 元数据目录（记录远程文件的大小和时间戳） */
    private val metaDir: File by lazy {
        File(cacheDir, ".meta").also { it.mkdirs() }
    }

    /** 下载进度：当前正在下载的 key -> 已下载字节数 */
    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()

    /** 按 key 的下载互斥锁，防止同一文件并发下载 */
    private val downloadMutexes = mutableMapOf<String, Mutex>()

    /**
     * 获取缓存文件。如果不存在或已过期，则从远程下载。
     *
     * @param sourceClient 远程源客户端
     * @param sourceId 内容源 ID
     * @param remotePath 远程文件路径
     * @param remoteFileSize 远程文件大小（字节），用于缓存失效检测。null 时跳过大小校验
     * @param remoteLastModified 远程文件最后修改时间（毫秒），用于缓存失效检测。null 时跳过时间校验
     * @return 本地缓存文件
     * @throws Exception 下载失败时抛出异常
     */
    suspend fun getCachedFile(
        sourceClient: SourceClient,
        sourceId: String,
        remotePath: String,
        remoteFileSize: Long? = null,
        remoteLastModified: Long? = null,
    ): File = withContext(Dispatchers.IO) {
        val cacheKey = computeCacheKey(sourceId, remotePath)
        val cachedFile = File(cacheDir, cacheKey)
        val metaFile = File(metaDir, "$cacheKey.meta")

        // 检查缓存是否有效
        if (cachedFile.exists() && metaFile.exists() && isCacheValid(metaFile, remoteFileSize, remoteLastModified)) {
            Timber.d("远程压缩包缓存命中: $remotePath")
            return@withContext cachedFile
        }

        // 缓存无效或不存在，需要下载
        // 每个文件一个互斥锁，避免重复下载
        val mutex = synchronized(downloadMutexes) {
            downloadMutexes.getOrPut(cacheKey) { Mutex() }
        }

        mutex.withLock {
            // 双重检查：其他协程可能已经下载完了
            if (cachedFile.exists() && metaFile.exists() && isCacheValid(metaFile, remoteFileSize, remoteLastModified)) {
                Timber.d("远程压缩包缓存命中（双重检查）: $remotePath")
                return@withContext cachedFile
            }

            // 执行下载
            downloadFile(sourceClient, remotePath, cachedFile, metaFile, remoteFileSize, remoteLastModified)
        }

        cachedFile
    }

    /**
     * 检查缓存文件是否存在。
     */
    fun isCached(sourceId: String, remotePath: String): Boolean {
        val cacheKey = computeCacheKey(sourceId, remotePath)
        return File(cacheDir, cacheKey).exists()
    }

    /**
     * 清理所有缓存。
     * 返回清理的字节数。
     */
    fun clearAll(): Long {
        var totalSize = 0L
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                totalSize += file.length()
                file.delete()
            }
        }
        metaDir.listFiles()?.forEach { it.delete() }
        Timber.d("已清理远程压缩包缓存: ${totalSize} 字节")
        return totalSize
    }

    /**
     * 获取缓存总大小（字节）。
     */
    fun getCacheSize(): Long {
        return cacheDir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }
            ?.sumOf { it.length() } ?: 0L
    }

    /**
     * 删除指定条目的缓存。
     */
    fun deleteCache(sourceId: String, remotePath: String) {
        val cacheKey = computeCacheKey(sourceId, remotePath)
        File(cacheDir, cacheKey).delete()
        File(metaDir, "$cacheKey.meta").delete()
    }

    /**
     * 执行文件下载。
     */
    private suspend fun downloadFile(
        sourceClient: SourceClient,
        remotePath: String,
        targetFile: File,
        metaFile: File,
        remoteFileSize: Long?,
        remoteLastModified: Long?,
    ) {
        val cacheKey = targetFile.name
        Timber.d("开始下载远程压缩包: $remotePath")

        // 更新下载进度
        updateProgress(cacheKey, DownloadProgress(0, remoteFileSize ?: -1, true))

        try {
            val tempFile = File(cacheDir, "$cacheKey.tmp")
            sourceClient.openStream(remotePath).use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalRead = 0L
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        // 每 256KB 更新一次进度
                        if (totalRead % (256 * 1024) < 8192) {
                            updateProgress(cacheKey, DownloadProgress(totalRead, remoteFileSize ?: -1, true))
                        }
                    }
                    // 最终进度
                    updateProgress(cacheKey, DownloadProgress(totalRead, totalRead, false))

                    // 写入元数据
                    writeMetaFile(metaFile, totalRead, remoteLastModified)

                    // 原子替换：先删旧文件再重命名
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)

                    Timber.d("远程压缩包下载完成: $remotePath ($totalRead 字节)")
                }
            }
        } catch (e: Exception) {
            // 清理临时文件
            File(cacheDir, "$cacheKey.tmp").delete()
            updateProgress(cacheKey, DownloadProgress(0, 0, false))
            Timber.e(e, "下载远程压缩包失败: $remotePath")
            throw e
        } finally {
            // 清除进度
            synchronized(_downloadProgress) {
                _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                    remove(cacheKey)
                }
            }
        }
    }

    /**
     * 检查缓存元数据是否与远程文件匹配。
     */
    private fun isCacheValid(metaFile: File, remoteFileSize: Long?, remoteLastModified: Long?): Boolean {
        return try {
            val meta = readMetaFile(metaFile) ?: return false

            // 如果远程信息都未知，认为缓存有效（避免每次重新下载）
            if (remoteFileSize == null && remoteLastModified == null) {
                return true
            }

            // 校验文件大小
            if (remoteFileSize != null && meta.fileSize != remoteFileSize) {
                Timber.d("缓存失效：文件大小变化 ${meta.fileSize} -> $remoteFileSize")
                return false
            }

            // 校验修改时间
            if (remoteLastModified != null && meta.lastModified != null && meta.lastModified != remoteLastModified) {
                Timber.d("缓存失效：修改时间变化 ${meta.lastModified} -> $remoteLastModified")
                return false
            }

            true
        } catch (e: Exception) {
            Timber.w(e, "读取缓存元数据失败，视为无效")
            false
        }
    }

    /**
     * 写入元数据文件。
     * 格式：第一行文件大小，第二行最后修改时间（可为 "null"）。
     */
    private fun writeMetaFile(metaFile: File, fileSize: Long, lastModified: Long?) {
        metaFile.writeText("$fileSize\n${lastModified?.toString() ?: "null"}\n")
    }

    /**
     * 读取元数据文件。
     */
    private fun readMetaFile(metaFile: File): CacheMeta? {
        if (!metaFile.exists()) return null
        val lines = metaFile.readLines()
        if (lines.size < 2) return null
        val fileSize = lines[0].toLongOrNull() ?: return null
        val lastModified = lines[1].toLongOrNull()
        return CacheMeta(fileSize, lastModified)
    }

    /**
     * 计算缓存键。使用 sourceId + path 的 MD5 哈希，确保文件名合法且唯一。
     */
    private fun computeCacheKey(sourceId: String, remotePath: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest("$sourceId::$remotePath".toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun updateProgress(key: String, progress: DownloadProgress) {
        synchronized(_downloadProgress) {
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                put(key, progress)
            }
        }
    }

    private data class CacheMeta(
        val fileSize: Long,
        val lastModified: Long?,
    )
}

/**
 * 下载进度数据。
 */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val isDownloading: Boolean,
) {
    /** 下载百分比（0-100），总大小未知时返回 -1 */
    val percent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else -1
}
