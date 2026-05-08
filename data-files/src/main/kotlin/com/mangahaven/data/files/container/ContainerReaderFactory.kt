package com.mangahaven.data.files.container

import android.content.Context
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.data.files.SourceClient
import com.mangahaven.data.files.cache.RemoteArchiveCacheManager
import com.mangahaven.model.LibraryItemType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContainerReaderFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheManager: RemoteArchiveCacheManager,
) {
    /**
     * 根据书籍类型创建对应的 ContainerReader。
     */
    fun createReader(itemType: LibraryItemType): ContainerReader {
        return when (itemType) {
            LibraryItemType.FOLDER -> FolderContainerReader(context)
            LibraryItemType.ARCHIVE -> ArchiveContainerReader(context)
            LibraryItemType.EPUB -> EpubContainerReader(context)
            LibraryItemType.MOBI -> MobiContainerReader(context)
            LibraryItemType.PDF -> PdfContainerReader(context)
            else -> throw IllegalArgumentException("Unsupported itemType: $itemType")
        }
    }

    /**
     * 为远程压缩包条目创建 RemoteArchiveContainerReader。
     *
     * @param sourceClient 远程源客户端
     * @param sourceId 内容源 ID
     * @param remotePath 远程文件路径
     * @param remoteFileSize 远程文件大小（用于缓存失效检测）
     * @param remoteLastModified 远程文件最后修改时间（用于缓存失效检测）
     */
    fun createRemoteArchiveReader(
        sourceClient: SourceClient,
        sourceId: String,
        remotePath: String,
        remoteFileSize: Long? = null,
        remoteLastModified: Long? = null,
    ): RemoteArchiveContainerReader {
        return RemoteArchiveContainerReader(
            context = context,
            sourceClient = sourceClient,
            cacheManager = cacheManager,
            sourceId = sourceId,
            remotePath = remotePath,
            remoteFileSize = remoteFileSize,
            remoteLastModified = remoteLastModified,
        )
    }

    companion object {
        /**
         * 判断文件名是否为归档压缩包（ZIP/CBZ/RAR/CBR）。
         */
        fun isArchiveFile(name: String): Boolean {
            val extension = name.substringAfterLast('.', "").lowercase()
            return extension in setOf("zip", "cbz", "rar", "cbr")
        }
    }
}
