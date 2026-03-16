package com.mangahaven.data.files.container

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.File

/**
 * PDF 容器读取器。
 * 使用 Android 原生的 PdfRenderer 读取 PDF。
 */
class PdfContainerReader(
    @ApplicationContext private val context: Context,
) : ContainerReader {

    override suspend fun listPages(target: ContainerTarget): List<PageRef> =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(target.path)
            try {
                // PDF renderer requires seekable file descriptor, so we need a local file or SAF descriptor
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext emptyList()
                pfd.use { fd ->
                    val renderer = PdfRenderer(fd)
                    val count = renderer.pageCount
                    val pages = mutableListOf<PageRef>()
                    for (i in 0 until count) {
                        pages.add(
                            PageRef(
                                index = i,
                                name = "page_\${i}.png",
                                path = i.toString(), // We use the path field as the page index for PDF
                                mimeType = "image/png",
                                sizeBytes = null
                            )
                        )
                    }
                    renderer.close()
                    pages
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list pages in PDF: \${target.path}")
                emptyList()
            }
        }

    override suspend fun openPage(pageRef: PageRef): InputStream =
        withContext(Dispatchers.IO) {
            throw UnsupportedOperationException(
                "Use LocalPageProvider with PDF target. " +
                "Direct openPage requires URI context."
            )
        }

    suspend fun openPageFromPdf(pdfUri: Uri, pageIndex: Int): InputStream =
        withContext(Dispatchers.IO) {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: throw IllegalStateException("Could not open PDF file descriptor")
            pfd.use { fd ->
                val renderer = PdfRenderer(fd)
                try {
                    val page = renderer.openPage(pageIndex)
                    // Scale factor for reasonable quality
                    val densityDpi = context.resources.displayMetrics.densityDpi
                    val width = (page.width * densityDpi / 72.0).toInt()
                    val height = (page.height * densityDpi / 72.0).toInt()

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // Make background white
                    bitmap.eraseColor(android.graphics.Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    bitmap.recycle()
                    ByteArrayInputStream(baos.toByteArray())
                } finally {
                    renderer.close()
                }
            }
        }

    override suspend fun extractCover(target: ContainerTarget): InputStream? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(target.path)
                openPageFromPdf(uri, 0)
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract cover from PDF: \${target.path}")
                null
            }
        }
}
