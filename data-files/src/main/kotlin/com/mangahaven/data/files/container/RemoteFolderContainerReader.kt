package com.mangahaven.data.files.container

import com.mangahaven.data.files.ContainerReader
import com.mangahaven.data.files.SourceClient
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * 读取远程（SMB、WebDAV 等）的图片目录容器。
 */
class RemoteFolderContainerReader(
    private val sourceClient: SourceClient,
) : ContainerReader {

    override suspend fun listPages(target: ContainerTarget): List<PageRef> = withContext(Dispatchers.IO) {
        val entries = sourceClient.list(target.path)
        
        entries
            .filter { !it.isDirectory }
            .filter { ImageFileUtils.isImageFile(it.name) }
            .filter { !ImageFileUtils.isIgnoredEntry(it.name) }
            .sortedWith { a, b -> ImageFileUtils.naturalCompare(a.name, b.name) }
            .mapIndexed { index, entry ->
                PageRef(
                    index = index,
                    id = entry.path,
                    name = entry.name,
                    sizeBytes = entry.sizeBytes ?: 0L,
                )
            }
    }

    override suspend fun openPage(pageRef: PageRef): InputStream = withContext(Dispatchers.IO) {
        // pageRef.id 里存了我们能请求的绝对子路径
        sourceClient.openStream(pageRef.id)
    }

    override suspend fun extractCover(target: ContainerTarget): InputStream? = withContext(Dispatchers.IO) {
        val pages = listPages(target)
        val firstPage = pages.firstOrNull() ?: return@withContext null
        openPage(firstPage)
    }
}
