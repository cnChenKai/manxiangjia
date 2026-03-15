package com.mangahaven.data.files.container

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream

/**
 * 目录型 ContainerReader。
 * 从 SAF DocumentFile 目录中读取图片文件作为漫画页面。
 */
class FolderContainerReader(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) : ContainerReader {

    override suspend fun listPages(target: ContainerTarget): List<PageRef> =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(target.path)
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            if (documentFile == null || !documentFile.exists()) {
                Timber.e("Directory not found: ${target.path}")
                return@withContext emptyList()
            }

            val imageFiles = documentFile.listFiles()
                .filter { file ->
                    file.isFile &&
                    file.name != null &&
                    ImageFileUtils.isImageFile(file.name!!) &&
                    !ImageFileUtils.shouldIgnore(file.name!!)
                }
                .sortedWith(compareBy(ImageFileUtils.naturalOrderComparator) { it.name ?: "" })

            imageFiles.mapIndexed { index, file ->
                PageRef(
                    index = index,
                    name = file.name ?: "page_$index",
                    path = file.uri.toString(),
                    mimeType = ImageFileUtils.getMimeType(file.name ?: ""),
                    sizeBytes = file.length(),
                )
            }
        }

    override suspend fun openPage(pageRef: PageRef): InputStream =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(pageRef.path)
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open stream for: ${pageRef.path}")
        }

    override suspend fun extractCover(target: ContainerTarget): InputStream? =
        withContext(Dispatchers.IO) {
            try {
                val pages = listPages(target)
                if (pages.isNotEmpty()) {
                    openPage(pages.first())
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract cover from folder: ${target.path}")
                null
            }
        }
}
