package com.mangahaven.data.files.importer

import com.mangahaven.data.files.SourceClient
import com.mangahaven.data.files.container.ImageFileUtils
import com.mangahaven.data.local.repository.LibraryRepository
import com.mangahaven.model.LibraryItem
import com.mangahaven.model.LibraryItemType
import com.mangahaven.model.Source
import com.mangahaven.model.SourceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 远程资源扫描器。
 * 遍历 WebDAV 或 SMB 目录，发现合规漫画内容并导入到数据库。
 */
@Singleton
class RemoteScanner @Inject constructor(
    private val libraryRepository: LibraryRepository,
) {

    /**
     * 深度扫描起始路径。
     */
    suspend fun scanDirectory(
        source: Source,
        sourceClient: SourceClient,
        basePath: String,
        onProgress: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val existingIds = libraryRepository.getItemIdsBySource(source.id).toMutableSet()
        val newItemsToInsert = mutableListOf<LibraryItem>()

        val addedCount = scanDirectoryInternal(source, sourceClient, basePath, existingIds, newItemsToInsert, onProgress)

        if (newItemsToInsert.isNotEmpty()) {
            libraryRepository.addItems(newItemsToInsert)
            Timber.d("Batch inserted \${newItemsToInsert.size} remote items")
        }

        addedCount
    }

    private suspend fun scanDirectoryInternal(
        source: Source,
        sourceClient: SourceClient,
        basePath: String,
        existingIds: MutableSet<String>,
        newItemsToInsert: MutableList<LibraryItem>,
        onProgress: (String) -> Unit
    ): Int {
        var addedCount = 0
        try {
            val rootEntries = sourceClient.list(basePath)
            for (entry in rootEntries) {
                if (ImageFileUtils.shouldIgnore(entry.name)) continue

                if (entry.isDirectory) {
                    onProgress("Scanning: \${entry.path}")
                    // 检查此目录本身是否是一本漫画（即只包含图片）
                    val subEntries = sourceClient.list(entry.path)
                    val imgCount = subEntries.count { !it.isDirectory && ImageFileUtils.isImageFile(it.name) }
                    val dirCount = subEntries.count { it.isDirectory && !ImageFileUtils.shouldIgnore(it.name) }

                    if (imgCount > 0 && dirCount == 0) {
                        // 这是一个纯图片漫画目录，添加它
                        if (addRemoteEntry(source, entry, existingIds, newItemsToInsert)) {
                            addedCount++
                        }
                    } else if (dirCount > 0) {
                        // 包含子目录，继续递归探测
                        addedCount += scanDirectoryInternal(source, sourceClient, entry.path, existingIds, newItemsToInsert, onProgress)
                    }
                } else if (ImageFileUtils.isArchive(entry.name)) {
                    // 对于远程的 ZIP/CBZ 压缩包，当前 MVP (Phase 3) 也勉强算 REMOTE_ENTRY
                    // TODO: 等待下载缓存支持
                    if (addRemoteEntry(source, entry, existingIds, newItemsToInsert)) {
                        addedCount++
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning remote directory: \$basePath")
        }
        return addedCount
    }

    private fun addRemoteEntry(
        source: Source,
        entry: SourceEntry,
        existingIds: MutableSet<String>,
        newItemsToInsert: MutableList<LibraryItem>
    ): Boolean {
        // 判断库里是否已存在
        val existingHash = (source.id + entry.path).hashCode().toString()
        // 此查重比较粗糙，最好有唯一路径约束，这里简单生成个相对稳定的组合 id
        val itemId = UUID.nameUUIDFromBytes("\${source.id}:\${entry.path}".toByteArray()).toString()
        
        if (existingIds.contains(itemId)) return false

        val item = LibraryItem(
            id = itemId,
            sourceId = source.id,
            path = entry.path,
            title = entry.name,
            coverPath = null, // 后续按需提取
            itemType = LibraryItemType.REMOTE_ENTRY,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        existingIds.add(itemId)
        newItemsToInsert.add(item)
        Timber.d("Added remote item to batch: \${item.title}")
        return true
    }
}
