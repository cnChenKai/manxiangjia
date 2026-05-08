package com.mangahaven.data.files.remote

import okhttp3.Response
import java.io.InputStream

/**
 * 包装 OkHttp Response 的 InputStream，关闭流时同时关闭底层 Response，避免连接泄漏。
 */
internal class ResponseClosingInputStream(
    private val delegate: InputStream,
    private val response: Response,
) : InputStream() {
    override fun read(): Int = delegate.read()
    override fun read(b: ByteArray): Int = delegate.read(b)
    override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
    override fun available(): Int = delegate.available()
    override fun close() {
        try { delegate.close() } finally { response.close() }
    }
    override fun mark(readlimit: Int) = delegate.mark(readlimit)
    override fun reset() = delegate.reset()
    override fun markSupported(): Boolean = delegate.markSupported()
    override fun skip(n: Long): Long = delegate.skip(n)
}
