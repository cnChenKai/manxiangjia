package com.mangahaven.data.files

import com.mangahaven.model.ContainerTarget
import com.mangahaven.model.PageRef
import java.io.InputStream

/**
 * 统一容器读取接口。
 * 容器可以是目录、ZIP、CBZ 等。
 */
interface ContainerReader {

    /**
     * 列出容器中的所有页面。
     */
    suspend fun listPages(target: ContainerTarget): List<PageRef>

    /**
     * 打开指定页面的输入流。
     * 调用者负责关闭流。
     */
    suspend fun openPage(pageRef: PageRef): InputStream

    /**
     * 提取容器的封面图片。
     * 返回 null 表示无法提取封面。
     */
    suspend fun extractCover(target: ContainerTarget): InputStream?
}
