package com.mangahaven.data.files.provider

import com.mangahaven.data.files.PageProvider
import com.mangahaven.data.files.container.RemoteFolderContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.LibraryItemType
import com.mangahaven.model.PageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream

/**
 * 远程 PageProvider 实现。
 * 读取 WebDAV 或 SMB 目录下的漫画页。
 */
class RemotePageProvider(
    private val containerTarget: ContainerTarget,
    private val remoteFolderReader: RemoteFolderContainerReader,
) : PageProvider {

    private var pages: List<PageRef>? = null

    /**
     * 初始化页面列表（懒加载）。
     */
    private suspend fun ensurePages(): List<PageRef> {
        return pages ?: run {
            val loadedPages = when (containerTarget.itemType) {
                LibraryItemType.REMOTE_ENTRY -> remoteFolderReader.listPages(containerTarget)
                else -> {
                    Timber.w("RemotePageProvider currently only deeply supports REMOTE_ENTRY pseudo-folders")
                    emptyList()
                }
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
                LibraryItemType.REMOTE_ENTRY -> remoteFolderReader.openPage(pageRef)
                else -> throw IllegalStateException("Unsupported item type for remote: ${containerTarget.itemType}")
            }
        }

    override suspend fun preload(indices: List<Int>) {
        ensurePages()
        Timber.d("Preload requested for remote indices: $indices")
    }
}
