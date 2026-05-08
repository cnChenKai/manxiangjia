package com.mangahaven.data.files.container

import android.content.Context
import com.mangahaven.data.files.ContainerReader
import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream

/**
 * MOBI 容器读取器。
 * 当前为一个占位符实现，尚未集成真正的 MOBI 解析库。
 */
class MobiContainerReader(
    @ApplicationContext private val context: Context,
) : ContainerReader {

    override suspend fun listPages(target: ContainerTarget): List<PageRef> =
        withContext(Dispatchers.IO) {
            Timber.w("MobiContainerReader: parsing MOBI files is not fully supported yet. Target: ${target.path}")
            // 在没有真实解析器的情况下，返回空列表
            emptyList()
        }

    override suspend fun openPage(pageRef: PageRef): InputStream =
        withContext(Dispatchers.IO) {
            throw UnsupportedOperationException("MOBI pages cannot be opened directly in this placeholder.")
        }

    override suspend fun extractCover(target: ContainerTarget): InputStream? =
        withContext(Dispatchers.IO) {
            Timber.w("MobiContainerReader: extracting cover from MOBI is not supported yet. Target: ${target.path}")
            null
        }
}
