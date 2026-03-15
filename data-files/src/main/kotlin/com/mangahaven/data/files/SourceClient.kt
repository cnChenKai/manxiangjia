package com.mangahaven.data.files

import com.mangahaven.model.SourceEntry
import java.io.InputStream

/**
 * 统一所有内容源的访问接口。
 * 不同的内容源（本地、WebDAV、SMB 等）都实现此接口。
 */
interface SourceClient {

    /**
     * 列出指定路径下的文件和目录。
     */
    suspend fun list(path: String): List<SourceEntry>

    /**
     * 获取指定路径的文件信息。
     */
    suspend fun stat(path: String): SourceEntry?

    /**
     * 打开指定路径文件的输入流。
     * 调用者负责关闭流。
     */
    suspend fun openStream(path: String): InputStream

    /**
     * 检查指定路径是否存在。
     */
    suspend fun exists(path: String): Boolean
}
