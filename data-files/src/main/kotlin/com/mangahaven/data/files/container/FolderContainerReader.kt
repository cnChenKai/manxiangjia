package com.mangahaven.data.files.container

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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

            try {
                // Determine if it's a tree URI or a document URI
                val isDocumentUri = DocumentsContract.isDocumentUri(context, uri)
                val documentId = if (isDocumentUri) {
                    DocumentsContract.getDocumentId(uri)
                } else {
                    DocumentsContract.getTreeDocumentId(uri)
                }

                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)

                val imageFiles = mutableListOf<DocumentFileInfo>()

                context.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val childDocId = cursor.getString(idIndex)
                        val name = cursor.getString(nameIndex)
                        val mimeType = cursor.getString(mimeIndex)
                        val size = if (!cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L

                        // skip directories based on mime type
                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) continue

                        if (name != null && ImageFileUtils.isImageFile(name) && !ImageFileUtils.shouldIgnore(name)) {
                            val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, childDocId)
                            imageFiles.add(DocumentFileInfo(name, childUri.toString(), mimeType, size))
                        }
                    }
                }

                val sortedFiles = imageFiles.sortedWith(compareBy(ImageFileUtils.naturalOrderComparator) { it.name })

                sortedFiles.mapIndexed { index, fileInfo ->
                    PageRef(
                        index = index,
                        name = fileInfo.name,
                        path = fileInfo.uriStr,
                        mimeType = ImageFileUtils.getMimeType(fileInfo.name) ?: fileInfo.mimeType,
                        sizeBytes = fileInfo.size,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list pages for directory: ${target.path}")
                emptyList()
            }
        }

    private data class DocumentFileInfo(
        val name: String,
        val uriStr: String,
        val mimeType: String?,
        val size: Long
    )

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
