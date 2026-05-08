package com.mangahaven.data.files.container

import android.content.Context
import android.net.Uri
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.data.files.SourceClient
import com.mangahaven.data.files.cache.RemoteArchiveCacheManager
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.InputStream

/**
 * 远程压缩包容器读取器。
 * 通过下载远程 ZIP/CBZ/RAR 文件到本地缓存，再委托给本地读取器读取。
 *
 * 工作流程：
 * 1. 首次访问时，通过 SourceClient 下载远程文件到缓存目录
 * 2. 后续访问直接使用缓存文件
 * 3. 当远程文件大小或修改时间变化时自动重新下载
 */
class RemoteArchiveContainerReader(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val sourceClient: SourceClient,
    private val cacheManager: RemoteArchiveCacheManager,
    private val sourceId: String,
    private val remotePath: String,
    private val remoteFileSize: Long? = null,
    private val remoteLastModified: Long? = null,
) : ContainerReader {

    /** 本地 ZIP/CBZ 读取器 */
    private val archiveReader by lazy { ArchiveContainerReader(context) }

    /** 本地 RAR 读取器 */
    private val rarReader by lazy { RarArchiveContainerReader(context) }

    /**
     * 判断远程文件是否为 RAR/CBR 格式。
     */
    private fun isRarArchive(path: String): Boolean {
        val lowerPath = path.lowercase()
        return lowerPath.endsWith(".rar") || lowerPath.endsWith(".cbr")
    }

    /**
     * 获取缓存后的本地文件。如果未缓存则下载。
     */
    suspend fun getCachedFile(): File {
        return cacheManager.getCachedFile(
            sourceClient = sourceClient,
            sourceId = sourceId,
            remotePath = remotePath,
            remoteFileSize = remoteFileSize,
            remoteLastModified = remoteLastModified,
        )
    }

    /**
     * 获取本地缓存文件的 URI。
     * 用于委托给本地读取器。
     */
    private suspend fun getLocalUri(): Uri {
        val file = getCachedFile()
        return Uri.fromFile(file)
    }

    override suspend fun listPages(target: ContainerTarget): List<PageRef> =
        withContext(Dispatchers.IO) {
            try {
                val localUri = getLocalUri()
                val localTarget = target.copy(path = localUri.toString())

                if (isRarArchive(remotePath)) {
                    rarReader.listPages(localTarget)
                } else {
                    archiveReader.listPages(localTarget)
                }
            } catch (e: Exception) {
                Timber.e(e, "列出远程压缩包页面失败: $remotePath")
                emptyList()
            }
        }

    override suspend fun openPage(pageRef: PageRef): InputStream =
        withContext(Dispatchers.IO) {
            // 直接通过 openPageFromArchive 打开，绕过 ContainerReader.openPage 的限制
            openPageFromArchive(pageRef.path)
        }

    /**
     * 从缓存的压缩包中打开指定页面。
     * @param entryPath 压缩包内的文件路径
     */
    suspend fun openPageFromArchive(entryPath: String): InputStream =
        withContext(Dispatchers.IO) {
            val localUri = getLocalUri()
            if (isRarArchive(remotePath)) {
                rarReader.openPageFromArchive(localUri, entryPath)
            } else {
                archiveReader.openPageFromArchive(localUri, entryPath)
            }
        }

    override suspend fun extractCover(target: ContainerTarget): InputStream? =
        withContext(Dispatchers.IO) {
            try {
                val localUri = getLocalUri()
                val localTarget = target.copy(path = localUri.toString())

                if (isRarArchive(remotePath)) {
                    rarReader.extractCover(localTarget)
                } else {
                    archiveReader.extractCover(localTarget)
                }
            } catch (e: Exception) {
                Timber.e(e, "提取远程压缩包封面失败: $remotePath")
                null
            }
        }
}
