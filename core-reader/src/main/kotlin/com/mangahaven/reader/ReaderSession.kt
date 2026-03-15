package com.mangahaven.reader

import com.mangahaven.model.PageRef

/**
 * 阅读会话接口。
 * 管理一次阅读的生命周期。
 */
interface ReaderSession {

    /**
     * 打开一本漫画。
     */
    suspend fun open(itemId: String)

    /**
     * 获取当前页面列表。
     */
    suspend fun getPages(): List<PageRef>

    /**
     * 获取总页数。
     */
    suspend fun getPageCount(): Int

    /**
     * 关闭会话，释放资源。
     */
    suspend fun close()
}
