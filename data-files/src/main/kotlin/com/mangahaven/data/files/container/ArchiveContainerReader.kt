package com.mangahaven.data.files.container

import android.content.Context
import android.net.Uri
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * ZIP/CBZ 容器读取器。
 * 使用 java.util.zip 读取压缩包中的图片页面。
 * 不做全量解压，按需读取。
 */
class ArchiveContainerReader(
    private val context: Context,
) : ContainerReader {

    override suspend fun listPages(target: ContainerTarget): List<PageRef> =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(target.path)
            val entries = mutableListOf<ZipEntryInfo>()

            try {
                context.contentResolver.openInputStream(uri)?.use { rawStream ->
                    ZipInputStream(rawStream).use { zis ->
                        var entry: ZipEntry? = zis.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory &&
                                ImageFileUtils.isImageFile(entry.name) &&
                                !ImageFileUtils.shouldIgnore(entry.name)
                            ) {
                                entries.add(
                                    ZipEntryInfo(
                                        name = entry.name.substringAfterLast('/'),
                                        fullPath = entry.name,
                                        size = entry.size,
                                    )
                                )
                            }
                            entry = zis.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list pages in archive: ${target.path}")
                return@withContext emptyList()
            }

            entries
                .sortedWith(compareBy(ImageFileUtils.naturalOrderComparator) { it.name })
                .mapIndexed { index, info ->
                    PageRef(
                        index = index,
                        name = info.name,
                        path = info.fullPath, // ZIP 内部路径
                        mimeType = ImageFileUtils.getMimeType(info.name),
                        sizeBytes = if (info.size >= 0) info.size else null,
                    )
                }
        }

    override suspend fun openPage(pageRef: PageRef): InputStream =
        withContext(Dispatchers.IO) {
            // pageRef.path 是 ZIP 内部路径
            // 我们需要从 ContainerTarget 获取 ZIP 文件 URI
            // 但 openPage 只接收 PageRef，所以需要另一种方式传递 ZIP URI
            // 解决方案：将 ZIP URI 编码到一个临时上下文中
            // 实际使用中通过 PageProvider 包装来传递
            throw UnsupportedOperationException(
                "Use ArchivePageProvider.openPage() instead. " +
                "Direct ArchiveContainerReader.openPage() requires the archive URI context."
            )
        }

    /**
     * 从指定 ZIP URI 中打开特定页面。
     * @param archiveUri ZIP 文件的 URI
     * @param entryPath ZIP 内的文件路径
     */
    suspend fun openPageFromArchive(archiveUri: Uri, entryPath: String): InputStream =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(archiveUri)?.use { rawStream ->
                ZipInputStream(rawStream).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == entryPath) {
                            // 读取到内存（ZIP 流是顺序的，无法随机访问）
                            val baos = ByteArrayOutputStream()
                            zis.copyTo(baos)
                            return@withContext ByteArrayInputStream(baos.toByteArray())
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            throw IllegalStateException("Entry not found in archive: $entryPath")
        }

    override suspend fun extractCover(target: ContainerTarget): InputStream? =
        withContext(Dispatchers.IO) {
            try {
                val pages = listPages(target)
                if (pages.isNotEmpty()) {
                    val uri = Uri.parse(target.path)
                    openPageFromArchive(uri, pages.first().path)
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract cover from archive: ${target.path}")
                null
            }
        }

    private data class ZipEntryInfo(
        val name: String,
        val fullPath: String,
        val size: Long,
    )
}
