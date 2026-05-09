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
 * 扫描 SAF 目录中的漫画文件和图片，识别 ZIP、CBZ、EPUB、MOBI 和图片文件夹。
 */

@Singleton
class FileScanner @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {
    companion object {
        private val ARCHIVE_EXTENSIONS = setOf("zip", "cbz")
        private val RAR_EXTENSIONS = setOf("rar", "cbr")
        private val EBOOK_EXTENSIONS = setOf("epub")
        private val MOBI_EXTENSIONS = setOf("mobi", "prc")
        private val PDF_EXTENSIONS = setOf("pdf")

        private val ARCHIVE_MIME_TYPES = setOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/x-cbz-compressed",
            "application/vnd.comicbook+zip",
        )

        private val RAR_MIME_TYPES = setOf(
            "application/x-rar-compressed",
            "application/vnd.comicbook-rar",
            "application/x-cbr",
        )

        private val EPUB_MIME_TYPES = setOf(
            "application/epub+zip",
        )

        private val MOBI_MIME_TYPES = setOf(
            "application/x-mobipocket-ebook",
            "application/x-mobi",
        )

        private val PDF_MIME_TYPES = setOf(
            "application/pdf",
        )
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
                val name = child.name
                if (name != null && ImageFileUtils.shouldIgnore(name)) continue

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
                                    title = name ?: child.uri.lastPathSegment ?: "未命名目录",
                                    path = child.uri.toString(),
                                    itemType = LibraryItemType.FOLDER,
                                    pageCount = subImages,
                                )
                            )
                        }
                    }
                    // EPUB
                    child.isFile && isEbookFile(name, child.type) -> {
                        val title = name ?: child.uri.lastPathSegment ?: "未命名 EPUB"
                        results.add(
                            ScanResult(
                                title = title.substringBeforeLast('.'),
                                path = child.uri.toString(),
                                itemType = LibraryItemType.EPUB,
                                pageCount = null,
                            )
                        )
                    }
                    // MOBI/PRC
                    child.isFile && isMobiFile(name, child.type) -> {
                        val title = name ?: child.uri.lastPathSegment ?: "未命名 MOBI"
                        results.add(
                            ScanResult(
                                title = title.substringBeforeLast('.'),
                                path = child.uri.toString(),
                                itemType = LibraryItemType.MOBI,
                                pageCount = null,
                            )
                        )
                    }
                    // PDF
                    child.isFile && isPdfFile(name, child.type) -> {
                        val title = name ?: child.uri.lastPathSegment ?: "未命名 PDF"
                        results.add(
                            ScanResult(
                                title = title.substringBeforeLast('.'),
                                path = child.uri.toString(),
                                itemType = LibraryItemType.PDF,
                                pageCount = null,
                            )
                        )
                    }
                    // 压缩包 (ZIP/CBZ)
                    child.isFile && isArchiveFile(name, child.type) -> {
                        val title = name ?: child.uri.lastPathSegment ?: "未命名漫画"
                        results.add(
                            ScanResult(
                                title = title.substringBeforeLast('.'),
                                path = child.uri.toString(),
                                itemType = LibraryItemType.ARCHIVE,
                                pageCount = null, // 稍后按需计数
                            )
                        )
                    }
                    // RAR/CBR
                    child.isFile && isRarFile(name, child.type) -> {
                        val title = name ?: child.uri.lastPathSegment ?: "未命名 RAR"
                        results.add(
                            ScanResult(
                                title = title.substringBeforeLast('.'),
                                path = child.uri.toString(),
                                itemType = LibraryItemType.ARCHIVE,
                                pageCount = null,
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

            val name = file.name
            val mimeType = context.contentResolver.getType(fileUri)
            val title = name ?: fileUri.lastPathSegment ?: "未命名文件"

            when {
                isEbookFile(name, mimeType) -> ScanResult(
                    title = title.substringBeforeLast('.'),
                    path = fileUri.toString(),
                    itemType = LibraryItemType.EPUB,
                    pageCount = null,
                )
                isMobiFile(name, mimeType) -> ScanResult(
                    title = title.substringBeforeLast('.'),
                    path = fileUri.toString(),
                    itemType = LibraryItemType.MOBI,
                    pageCount = null,
                )
                isPdfFile(name, mimeType) -> ScanResult(
                    title = title.substringBeforeLast('.'),
                    path = fileUri.toString(),
                    itemType = LibraryItemType.PDF,
                    pageCount = null,
                )
                isArchiveFile(name, mimeType) -> ScanResult(
                    title = title.substringBeforeLast('.'),
                    path = fileUri.toString(),
                    itemType = LibraryItemType.ARCHIVE,
                    pageCount = null,
                )
                isRarFile(name, mimeType) -> ScanResult(
                    title = title.substringBeforeLast('.'),
                    path = fileUri.toString(),
                    itemType = LibraryItemType.ARCHIVE,
                    pageCount = null,
                )
                name != null && ImageFileUtils.isImageFile(name) -> {
                    Timber.w("Single image file imported, treating as 1-page book: $name")
                    ScanResult(
                        title = name.substringBeforeLast('.'),
                        path = fileUri.toString(),
                        itemType = LibraryItemType.FOLDER,
                        pageCount = 1,
                    )
                }
                else -> {
                    Timber.w("Unsupported file type: $name (mime: $mimeType)")
                    null
                }
            }
        }

    // ── Type detection helpers ──────────────────────────────────────

    private fun isArchiveFile(name: String?, mimeType: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase()
        if (ext in ARCHIVE_EXTENSIONS) return true
        return mimeType in ARCHIVE_MIME_TYPES
    }

    private fun isRarFile(name: String?, mimeType: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase()
        if (ext in RAR_EXTENSIONS) return true
        return mimeType in RAR_MIME_TYPES
    }

    private fun isEbookFile(name: String?, mimeType: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase()
        if (ext in EBOOK_EXTENSIONS) return true
        return mimeType in EPUB_MIME_TYPES
    }

    private fun isMobiFile(name: String?, mimeType: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase()
        if (ext in MOBI_EXTENSIONS) return true
        return mimeType in MOBI_MIME_TYPES
    }

    private fun isPdfFile(name: String?, mimeType: String?): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase()
        if (ext in PDF_EXTENSIONS) return true
        return mimeType in PDF_MIME_TYPES
    }
}
