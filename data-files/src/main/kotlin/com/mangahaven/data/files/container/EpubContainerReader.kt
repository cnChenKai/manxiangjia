package com.mangahaven.data.files.container

import android.content.Context
import android.net.Uri
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * EPUB 容器读取器。
 * 纯原生 Kotlin 解析 EPUB（由于 EPUB 结构是一个 ZIP 文件）。
 * 我们只会抽取内部的图片文件（不渲染 HTML 文本，主要面向连环画/漫画 EPUB）。
 */
class EpubContainerReader(
    @ApplicationContext private val context: Context,
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
                                !ImageFileUtils.shouldIgnore(entry.name) &&
                                !entry.name.contains("..")
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
                Timber.e(e, "Failed to list image pages in EPUB: \${target.path}")
                return@withContext emptyList()
            }

            // 对找到的所有图片进行排序
            entries
                .sortedWith(compareBy(ImageFileUtils.naturalOrderComparator) { it.name })
                .mapIndexed { index, info ->
                    PageRef(
                        index = index,
                        name = info.name,
                        path = info.fullPath,
                        mimeType = ImageFileUtils.getMimeType(info.name),
                        sizeBytes = if (info.size >= 0) info.size else null,
                    )
                }
        }

    override suspend fun openPage(pageRef: PageRef): InputStream =
        withContext(Dispatchers.IO) {
            throw UnsupportedOperationException(
                "Use LocalPageProvider with EPUB target. " +
                "Direct openPage requires URI context."
            )
        }

    suspend fun openPageFromArchive(archiveUri: Uri, entryPath: String): InputStream =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(archiveUri)?.use { rawStream ->
                ZipInputStream(rawStream).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == entryPath) {
                            val baos = ByteArrayOutputStream()
                            zis.copyTo(baos)
                            return@withContext ByteArrayInputStream(baos.toByteArray())
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            throw IllegalStateException("Image entry not found in EPUB: \$entryPath")
        }

    override suspend fun extractCover(target: ContainerTarget): InputStream? =
        withContext(Dispatchers.IO) {
            try {
                // Heuristic: pick the first image or specifically look for "cover" named images
                val pages = listPages(target)
                if (pages.isNotEmpty()) {
                    val uri = Uri.parse(target.path)
                    val coverPage = pages.find { it.name.contains("cover", ignoreCase = true) } ?: pages.first()
                    openPageFromArchive(uri, coverPage.path)
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract cover from EPUB: \${target.path}")
                null
            }
        }

    private data class ZipEntryInfo(
        val name: String,
        val fullPath: String,
        val size: Long,
    )
}
