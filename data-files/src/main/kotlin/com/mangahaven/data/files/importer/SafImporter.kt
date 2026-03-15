package com.mangahaven.data.files.importer

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mangahaven.data.files.container.ArchiveContainerReader
import com.mangahaven.data.files.container.FolderContainerReader
import com.mangahaven.data.local.cover.CoverManager
import com.mangahaven.data.local.repository.LibraryRepository
import com.mangahaven.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SAF 文件导入器。
 * 处理从 Android Storage Access Framework 导入漫画文件或目录。
 */
@Singleton
class SafImporter @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val fileScanner: FileScanner,
    private val libraryRepository: LibraryRepository,
    private val coverManager: CoverManager,
) {

    companion object {
        private const val LOCAL_SOURCE_ID = "local"
    }

    /**
     * 确保本地 Source 存在。
     */
    private suspend fun ensureLocalSource() {
        val existing = libraryRepository.getSource(LOCAL_SOURCE_ID)
        if (existing == null) {
            libraryRepository.addSource(
                Source(
                    id = LOCAL_SOURCE_ID,
                    type = SourceType.LOCAL,
                    name = "本地文件",
                )
            )
        }
    }

    /**
     * 导入单个文件（ZIP/CBZ）。
     * @param fileUri SAF 返回的文件 URI
     * @return 导入成功的 LibraryItem，失败返回 null
     */
    suspend fun importFile(fileUri: Uri): LibraryItem? = withContext(Dispatchers.IO) {
        try {
            // 持久化 URI 权限
            persistPermission(fileUri)

            ensureLocalSource()

            val scanResult = fileScanner.scanSingleFile(fileUri) ?: return@withContext null
            val item = createLibraryItem(scanResult)

            // 提取封面
            extractAndSaveCover(item, scanResult)

            // 保存到数据库
            libraryRepository.addItem(item)

            Timber.i("Successfully imported file: ${item.title}")
            item
        } catch (e: Exception) {
            Timber.e(e, "Failed to import file: $fileUri")
            null
        }
    }

    /**
     * 导入目录。
     * @param directoryUri SAF 返回的目录 URI
     * @return 导入成功的 LibraryItem 列表
     */
    suspend fun importDirectory(directoryUri: Uri): List<LibraryItem> =
        withContext(Dispatchers.IO) {
            try {
                // 持久化 URI 权限
                persistPermission(directoryUri)

                ensureLocalSource()

                val scanResults = fileScanner.scanDirectory(directoryUri)
                if (scanResults.isEmpty()) {
                    Timber.w("No manga found in directory: $directoryUri")
                    return@withContext emptyList()
                }

                val items = scanResults.map { result ->
                    val item = createLibraryItem(result)
                    // 提取封面
                    extractAndSaveCover(item, result)
                    item
                }

                // 批量保存到数据库
                libraryRepository.addItems(items)

                Timber.i("Successfully imported ${items.size} items from directory")
                items
            } catch (e: Exception) {
                Timber.e(e, "Failed to import directory: $directoryUri")
                emptyList()
            }
        }

    /**
     * 持久化 URI 权限，确保应用重启后仍可访问。
     */
    private fun persistPermission(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            Timber.d("Persisted URI permission: $uri")
        } catch (e: SecurityException) {
            Timber.w(e, "Could not persist URI permission: $uri")
        }
    }

    /**
     * 从扫描结果创建 LibraryItem。
     */
    private fun createLibraryItem(scanResult: ScanResult): LibraryItem {
        val now = System.currentTimeMillis()
        return LibraryItem(
            id = UUID.randomUUID().toString(),
            sourceId = LOCAL_SOURCE_ID,
            path = scanResult.path,
            title = scanResult.title,
            coverPath = null, // 稍后填充
            itemType = scanResult.itemType,
            pageCount = scanResult.pageCount,
            readingStatus = ReadingStatus.UNREAD,
            lastReadAt = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    /**
     * 提取并保存封面图片。
     */
    private suspend fun extractAndSaveCover(
        item: LibraryItem,
        scanResult: ScanResult,
    ): LibraryItem {
        val target = ContainerTarget(
            sourceId = LOCAL_SOURCE_ID,
            path = scanResult.path,
            itemType = scanResult.itemType,
        )

        val coverStream = when (scanResult.itemType) {
            LibraryItemType.FOLDER -> FolderContainerReader(context).extractCover(target)
            LibraryItemType.ARCHIVE -> {
                val lowerPath = target.path.lowercase()
                if (lowerPath.endsWith(".rar") || lowerPath.endsWith(".cbr")) {
                    com.mangahaven.data.files.container.RarArchiveContainerReader(context).extractCover(target)
                } else {
                    ArchiveContainerReader(context).extractCover(target)
                }
            }
            else -> null
        }

        val coverPath = coverStream?.let { stream ->
            coverManager.saveCover(item.id, stream)
        }

        return if (coverPath != null) {
            val updated = item.copy(coverPath = coverPath)
            updated
        } else {
            item
        }
    }
}
