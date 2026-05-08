package com.mangahaven.data.files.provider

import com.mangahaven.data.files.PageProvider
import com.mangahaven.data.files.container.RemoteArchiveContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream

/**
 * 远程压缩包 PageProvider 实现。
 * 处理 SMB/WebDAV 上的 ZIP/CBZ/RAR 文件。
 *
 * 工作流程：
 * 1. 首次访问时触发下载（通过 RemoteArchiveContainerReader）
 * 2. 下载完成后使用本地读取器按页读取
 * 3. 后续访问直接使用缓存文件
 */
class RemoteArchivePageProvider(
    private val containerTarget: ContainerTarget,
    private val archiveReader: RemoteArchiveContainerReader,
) : PageProvider {

    private var pages: List<PageRef>? = null
    private val pagesMutex = Mutex()

    /**
     * 初始化页面列表（懒加载，线程安全）。
     * 首次调用时会触发远程文件下载。
     */
    private suspend fun ensurePages(): List<PageRef> {
        pages?.let { return it }
        return pagesMutex.withLock {
            // 双重检查：可能在等待锁期间已被其他协程加载
            pages?.let { return it }
            Timber.d("远程压缩包首次加载，开始下载: ${containerTarget.path}")
            val loadedPages = archiveReader.listPages(containerTarget)
            pages = loadedPages
            Timber.d("远程压缩包页面列表加载完成: ${loadedPages.size} 页")
            loadedPages
        }
    }

    override suspend fun getPageCount(): Int = ensurePages().size

    override suspend fun openPage(index: Int): InputStream =
        withContext(Dispatchers.IO) {
            val pageList = ensurePages()
            if (index < 0 || index >= pageList.size) {
                throw IndexOutOfBoundsException("页面索引 $index 超出范围 [0, ${pageList.size})")
            }
            val pageRef = pageList[index]

            // 使用 RemoteArchiveContainerReader 打开页面
            // 它会自动处理缓存文件的获取
            archiveReader.openPageFromArchive(pageRef.path)
        }

    override suspend fun preload(indices: List<Int>) {
        // 确保页面列表已加载（即文件已下载）
        ensurePages()
        Timber.d("远程压缩包预加载请求: $indices")
    }
}
