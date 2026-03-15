package com.mangahaven.data.files.provider

import android.content.Context
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.LibraryItemType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PageProvider 工厂。
 * 根据书架条目创建对应的 PageProvider。
 */
@Singleton
class PageProviderFactory @Inject constructor(
    private val context: Context,
) {
    /**
     * 为指定条目创建 PageProvider。
     */
    fun create(itemId: String, path: String, itemType: LibraryItemType): LocalPageProvider {
        val target = ContainerTarget(
            sourceId = "local",
            path = path,
            itemType = itemType,
        )
        return LocalPageProvider(context, target)
    }
}
