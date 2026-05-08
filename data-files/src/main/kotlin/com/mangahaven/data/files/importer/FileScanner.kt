package com.mangahaven.data.files.importer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mangahaven.data.files.container.ImageFileUtils
import com.mangahaven.model.LibraryItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件扫描结果。
 */
data class ScanResult(
    val title: String,
    val path: String,
    val itemType: LibraryItemType,
    val pageCount: Int?,
)

/**
 * 文件扫描器。
 * 扫描 SAF 目录中的漫画文件和图片，识别 ZIP、CBZ 和图片文件夹。
 */

@Singleton
class FileScanner @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {
    companion object {
        private val ARCHIVE_EXTENSIONS = setOf("zip", "cbz", "rar", "cbr")
    }

    /**
     * 扫描目录，返回该目录中发现的漫画条目列表。
     * 如果该目录本身就包含图片文件，则将整个目录作为一本漫画。
     * 如果该目录包含子目录或压缩包，将它们逐个识别。
     */
    suspend fun scanDirectory(directoryUri: Uri): List<ScanResult> =
        withContext(Dispatchers.IO) {
            val directory = DocumentFile.fromTreeUri(context, directoryUri)
            if (directory == null || !directory.exists() || !directory.isDirectory) {
                Timber.e("Not a valid directory: $directoryUri")
                return@withContext emptyList()
            }

            val results = mutableListOf<ScanResult>()
            val children = directory.listFiles()

            // 检查目录本身是否是一本图片漫画
            val imageFiles = children.filter { file ->
                file.isFile &&
                file.name != null &&
                ImageFileUtils.isImageFile(file.name!!)
            }

            if (imageFiles.isNotEmpty()) {
                // 目录本身包含图片，整个目录作为一本漫画
                results.add(
                    ScanResult(
                        title = directory.name ?: "未知漫画",
                        path = directoryUri.toString(),
                        itemType = LibraryItemType.FOLDER,
                        pageCount = imageFiles.size,
                    )
                )
                return@withContext results
            }

            // 扫描子项
            for (child in children) {
                val name = child.name ?: continue
                if (ImageFileUtils.shouldIgnore(name)) continue

                when {
                    // 子目录：检查是否包含图片
                    child.isDirectory -> {
                        val subImages = child.listFiles().count { subFile ->
                            subFile.isFile &&
                            subFile.name != null &&
                            ImageFileUtils.isImageFile(subFile.name!!)
                        }
                        if (subImages > 0) {
                            results.add(
                                ScanResult(
                                    title = name,
                                    path = child.uri.toString(),
                                    itemType = LibraryItemType.FOLDER,
                                    pageCount = subImages,
                                )
                            )
                        }
                    }
                    // 压缩包
                    child.isFile && isArchiveFile(name) -> {
                        results.add(
                            ScanResult(
                                title = name.substringBeforeLast('.'),
                                path = child.uri.toString(),
                                itemType = LibraryItemType.ARCHIVE,
                                pageCount = null, // 稍后按需计数
                            )
                        )
                    }
                }
            }

            results.sortedWith(compareBy(ImageFileUtils.naturalOrderComparator) { it.title })
        }

    /**
     * 扫描单个文件。
     */
    suspend fun scanSingleFile(fileUri: Uri): ScanResult? =
        withContext(Dispatchers.IO) {
            val file = DocumentFile.fromSingleUri(context, fileUri)
            if (file == null || !file.exists()) {
                Timber.e("File not found: $fileUri")
                return@withContext null
            }

            val name = file.name ?: return@withContext null

            when {
                isArchiveFile(name) -> ScanResult(
                    title = name.substringBeforeLast('.'),
                    path = fileUri.toString(),
                    itemType = LibraryItemType.ARCHIVE,
                    pageCount = null,
                )
                ImageFileUtils.isImageFile(name) -> {
                    Timber.w("Single image file imported, treating as 1-page book: $name")
                    ScanResult(
                        title = name.substringBeforeLast('.'),
                        path = fileUri.toString(),
                        itemType = LibraryItemType.FOLDER,
                        pageCount = 1,
                    )
                }
                else -> {
                    Timber.w("Unsupported file type: $name")
                    null
                }
            }
        }

    private fun isArchiveFile(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in ARCHIVE_EXTENSIONS
    }
}
