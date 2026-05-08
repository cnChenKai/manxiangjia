package com.mangahaven.data.files.provider

import android.content.Context
import com.mangahaven.data.files.container.ContainerReaderFactory
import com.mangahaven.data.files.container.RemoteFolderContainerReader
import com.mangahaven.data.files.remote.SourceClientFactory
import com.mangahaven.data.local.dao.SourceDao
import com.mangahaven.data.local.mapper.toModel
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.LibraryItemType
import com.mangahaven.model.SourceType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PageProvider 工厂。
 * 根据书架条目创建对应的 PageProvider。
 */
@Singleton
class PageProviderFactory @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val sourceDao: SourceDao,
    private val sourceClientFactory: SourceClientFactory,
    private val containerReaderFactory: ContainerReaderFactory,
) {
    /**
     * 为指定条目创建 PageProvider。
     */
    suspend fun create(itemId: String, sourceId: String, path: String, itemType: LibraryItemType): com.mangahaven.data.files.PageProvider {
        val target = ContainerTarget(
            sourceId = sourceId,
            path = path,
            itemType = itemType,
        )

        val sourceEntity = sourceDao.getById(sourceId)
        val source = sourceEntity?.toModel()

        return if (source != null && (source.type == SourceType.WEBDAV || source.type == SourceType.SMB)) {
            val client = sourceClientFactory.create(source)

            // 判断是否为远程压缩包文件
            if (ContainerReaderFactory.isArchiveFile(path)) {
                Timber.d("检测到远程压缩包: $path，使用 RemoteArchivePageProvider")
                // 获取远程文件元信息用于缓存失效检测
                val entry = try { client.stat(path) } catch (e: Exception) {
                    Timber.w(e, "获取远程文件信息失败，将跳过缓存校验: $path")
                    null
                }
                val reader = containerReaderFactory.createRemoteArchiveReader(
                    sourceClient = client,
                    sourceId = sourceId,
                    remotePath = path,
                    remoteFileSize = entry?.sizeBytes,
                    remoteLastModified = entry?.lastModified,
                )
                RemoteArchivePageProvider(target, reader)
            } else {
                // 文件夹类型的远程条目
                val reader = RemoteFolderContainerReader(client)
                RemotePageProvider(target, reader)
            }
        } else {
            LocalPageProvider(context, target)
        }
    }
}
