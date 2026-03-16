package com.mangahaven.data.files.container

import android.content.Context
import com.mangahaven.model.LibraryItemType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContainerReaderFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 根据书籍类型创建对应的 ContainerReader。
     */
    fun createReader(itemType: LibraryItemType): Any {
        return when (itemType) {
            LibraryItemType.FOLDER -> FolderContainerReader(context)
            LibraryItemType.ARCHIVE -> ArchiveContainerReader(context)
            LibraryItemType.EPUB -> EpubContainerReader(context)
            LibraryItemType.PDF -> PdfContainerReader(context)
            else -> throw IllegalArgumentException("Unsupported itemType: \$itemType")
        }
    }
}
