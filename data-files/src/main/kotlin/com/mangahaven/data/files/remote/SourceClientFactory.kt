package com.mangahaven.data.files.remote

import com.mangahaven.data.files.SourceClient
import com.mangahaven.model.Source
import com.mangahaven.model.SourceType
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 根据内容源类型，分发实例化对应的协议客户端。
 */
@Singleton
class SourceClientFactory @Inject constructor(
    private val httpClient: OkHttpClient,
) {
    /**
     * 根据 Source 生成可用的 SourceClient 接口。
     */
    fun create(source: Source): SourceClient {
        return when (source.type) {
            SourceType.WEBDAV -> WebDavSourceClient(source, httpClient)
            SourceType.SMB -> SmbSourceClient(source)
            SourceType.OPDS -> OpdsSourceClient(source, httpClient)
            else -> throw IllegalArgumentException("Unsupported remote source type: ${source.type}")
        }
    }
}
