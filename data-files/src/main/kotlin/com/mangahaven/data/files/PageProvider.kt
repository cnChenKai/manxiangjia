package com.mangahaven.data.files

import java.io.InputStream

/**
 * 阅读器按页访问接口。
 * 供阅读器 UI 使用。
 */
interface PageProvider {

    /**
     * 获取总页数。
     */
    suspend fun getPageCount(): Int

    /**
     * 打开指定索引页的输入流。
     * 调用者负责关闭流。
     */
    suspend fun openPage(index: Int): InputStream

    /**
     * 预加载指定索引的页面。
     */
    suspend fun preload(indices: List<Int>)
}
