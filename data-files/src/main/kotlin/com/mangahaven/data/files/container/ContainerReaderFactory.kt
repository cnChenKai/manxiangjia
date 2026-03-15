package com.mangahaven.data.files.container

import android.content.Context
import com.mangahaven.model.LibraryItemType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 容器读取器工厂。
 * 根据条目类型选择正确的 ContainerReader 实现。
 */
@Singleton
class ContainerReaderFactory @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {

    /**
     * 创建适合指定条目类型的 ContainerReader。
     */
    fun createReader(itemType: LibraryItemType): Any {
        return when (itemType) {
            LibraryItemType.FOLDER -> FolderContainerReader(context)
            LibraryItemType.ARCHIVE -> ArchiveContainerReader(context)
            else -> throw IllegalArgumentException("Unsupported item type: $itemType")
        }
    }

    fun createFolderReader(): FolderContainerReader = FolderContainerReader(context)
    fun createArchiveReader(): ArchiveContainerReader = ArchiveContainerReader(context)
}
