package com.mangahaven.data.files.provider

import android.content.Context
import android.net.Uri
import com.mangahaven.data.files.PageProvider
import com.mangahaven.data.files.container.ArchiveContainerReader
import com.mangahaven.data.files.container.FolderContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.LibraryItemType
import com.mangahaven.model.PageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream

/**
 * 本地 PageProvider 实现。
 * 基于 ContainerReader 提供按页访问漫画内容的能力。
 */
class LocalPageProvider(
    private val context: Context,
    private val containerTarget: ContainerTarget,
) : PageProvider {

    private var pages: List<PageRef>? = null
    private val folderReader by lazy { FolderContainerReader(context) }
    private val archiveReader by lazy { ArchiveContainerReader(context) }

    /**
     * 初始化页面列表（懒加载）。
     */
    private suspend fun ensurePages(): List<PageRef> {
        return pages ?: run {
            val loadedPages = when (containerTarget.itemType) {
                LibraryItemType.FOLDER -> folderReader.listPages(containerTarget)
                LibraryItemType.ARCHIVE -> archiveReader.listPages(containerTarget)
                else -> emptyList()
            }
            pages = loadedPages
            loadedPages
        }
    }

    override suspend fun getPageCount(): Int = ensurePages().size

    override suspend fun openPage(index: Int): InputStream =
        withContext(Dispatchers.IO) {
            val pageList = ensurePages()
            if (index < 0 || index >= pageList.size) {
                throw IndexOutOfBoundsException("Page index $index out of range [0, ${pageList.size})")
            }
            val pageRef = pageList[index]

            when (containerTarget.itemType) {
                LibraryItemType.FOLDER -> folderReader.openPage(pageRef)
                LibraryItemType.ARCHIVE -> {
                    val archiveUri = Uri.parse(containerTarget.path)
                    archiveReader.openPageFromArchive(archiveUri, pageRef.path)
                }
                else -> throw IllegalStateException("Unsupported item type: ${containerTarget.itemType}")
            }
        }

    override suspend fun preload(indices: List<Int>) {
        // Phase 1 基础预加载：仅确保页面列表已加载
        // 实际的图片预解码在后续阶段使用 Coil 的预加载能力
        ensurePages()
        Timber.d("Preload requested for indices: $indices (page list ready)")
    }
}
