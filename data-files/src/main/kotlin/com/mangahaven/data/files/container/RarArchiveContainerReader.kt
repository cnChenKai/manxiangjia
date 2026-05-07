package com.mangahaven.data.files.container

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * RAR/CBR 容器读取器。
 * 使用 junrar 读取压缩包中的图片页面。
 */
class RarArchiveContainerReader(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) : ContainerReader {

    private data class CacheEntry(
        val entryCount: Int,
        val indexMap: Map<String, Int>
    )

    // Cache entry lookup metadata: archive Uri string -> (entryPath -> index in fileHeaders)
    private val headerIndexCache = ConcurrentHashMap<String, CacheEntry>()

    // Helper to get a File from URI since Junrar works best with random access File objects
    private suspend fun getTempFile(uri: Uri): File = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("temp_rar_", ".rar", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    }

    override suspend fun listPages(target: ContainerTarget): List<PageRef> =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(target.path)
            val uriString = uri.toString()
            val entries = mutableListOf<RarEntryInfo>()
            var tempFile: File? = null

            try {
                tempFile = getTempFile(uri)
                val archive = Archive(tempFile)
                val fileHeaders = archive.fileHeaders

                val indexMap = mutableMapOf<String, Int>()

                for (i in fileHeaders.indices) {
                    val header = fileHeaders[i]
                    val name = header.fileName ?: header.fileNameW

                    if (name != null) {
                        indexMap[name] = i
                    }

                    if (!header.isDirectory) {
                        if (name != null &&
                            ImageFileUtils.isImageFile(name) &&
                            !ImageFileUtils.shouldIgnore(name) &&
                            !name.contains("..")
                        ) {
                            entries.add(
                                RarEntryInfo(
                                    name = name.substringAfterLast('\\').substringAfterLast('/'),
                                    fullPath = name,
                                    size = header.fullUnpackSize,
                                )
                            )
                        }
                    }
                }

                headerIndexCache[uriString] = CacheEntry(fileHeaders.size, indexMap)
            } catch (e: Exception) {
                Timber.e(e, "Failed to list pages in RAR archive: ${target.path}")
                return@withContext emptyList()
            } finally {
                tempFile?.delete()
            }

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
                "Use ArchivePageProvider.openPage() instead. " +
                "Direct RarArchiveContainerReader.openPage() requires the archive URI context."
            )
        }

    /**
     * 从指定 RAR URI 中打开特定页面。
     */
    suspend fun openPageFromArchive(archiveUri: Uri, entryPath: String): InputStream =
        withContext(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                tempFile = getTempFile(archiveUri)
                val archive = Archive(tempFile)
                val fileHeaders = archive.fileHeaders
                val uriString = archiveUri.toString()

                // Fast path: cached index lookup
                val cachedEntry = headerIndexCache[uriString]
                if (cachedEntry != null) {
                    if (cachedEntry.entryCount != fileHeaders.size) {
                        headerIndexCache.remove(uriString)
                    } else {
                        val index = cachedEntry.indexMap[entryPath]
                        if (index != null && index >= 0 && index < fileHeaders.size) {
                            val header = fileHeaders[index]
                            val name = header.fileName ?: header.fileNameW
                            if (name == entryPath) {
                                val baos = ByteArrayOutputStream()
                                archive.extractFile(header, baos)
                                return@withContext ByteArrayInputStream(baos.toByteArray())
                            }
                        }
                    }
                }

                // Fallback path: linear scan
                for (i in fileHeaders.indices) {
                    val header = fileHeaders[i]
                    val name = header.fileName ?: header.fileNameW
                    if (name == entryPath) {
                        // Opportunistic cache update for the single entry if cache didn't exist or was wrong
                        // Note: A full listPages call is needed to fully populate the cache, but this helps
                        // if listPages wasn't called.
                        val currentEntry = headerIndexCache[uriString]
                        val currentMap = currentEntry?.indexMap?.toMutableMap() ?: mutableMapOf()
                        currentMap[entryPath] = i
                        headerIndexCache[uriString] = CacheEntry(fileHeaders.size, currentMap)

                        val baos = ByteArrayOutputStream()
                        archive.extractFile(header, baos)
                        return@withContext ByteArrayInputStream(baos.toByteArray())
                    }
                }
            } finally {
                tempFile?.delete()
            }
            throw IllegalStateException("Entry not found in RAR archive: $entryPath")
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
                Timber.e(e, "Failed to extract cover from RAR archive: ${target.path}")
                null
            }
        }

    fun clearCache() {
        headerIndexCache.clear()
    }

    private data class RarEntryInfo(
        val name: String,
        val fullPath: String,
        val size: Long,
    )
}
