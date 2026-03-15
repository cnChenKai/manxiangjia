package com.mangahaven.data.files.remote

import com.mangahaven.data.files.SourceClient
import com.mangahaven.model.Source
import com.mangahaven.model.SourceEntry
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.MalformedURLException

/**
 * 基于 jcifs-ng 的 SMB 客户端。
 */
class SmbSourceClient(
    private val source: Source,
) : SourceClient {

    private val cifsContext: CIFSContext by lazy {
        val baseContext = SingletonContext.getInstance()
        val authRef = source.authRef
        if (authRef != null && authRef.contains(":")) {
            val parts = authRef.split(":", limit = 2)
            // 简单支持 username:password
            val auth = NtlmPasswordAuthenticator("", parts[0], parts[1])
            baseContext.withCredentials(auth)
        } else {
            // Guest 匿名访问
            baseContext.withGuestCrendentials()
        }
    }

    private fun buildSmbUrl(path: String): String {
        // SMB url 必须以 smb:// 开头，并且目录以 / 结尾
        var baseUrl = source.configJson.replace("\\", "/")
        if (!baseUrl.startsWith("smb://")) {
            baseUrl = "smb://$baseUrl"
        }
        baseUrl = baseUrl.trimEnd('/')
        
        val subPath = path.trimStart('/')
        
        return if (subPath.isEmpty()) "$baseUrl/" else "$baseUrl/$subPath"
    }

    @Throws(MalformedURLException::class)
    private suspend fun getSmbFile(path: String): SmbFile = withContext(Dispatchers.IO) {
        val url = buildSmbUrl(path)
        SmbFile(url, cifsContext)
    }

    override suspend fun list(path: String): List<SourceEntry> = withContext(Dispatchers.IO) {
        val smbFile = getSmbFile(path)
        
        if (!smbFile.exists() || !smbFile.isDirectory) {
            return@withContext emptyList()
        }

        smbFile.listFiles()?.map { file ->
            SourceEntry(
                name = file.name.trimEnd('/'),
                path = file.path.substringAfter(source.configJson).trimEnd('/'),
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isFile) file.length() else null,
                lastModified = file.lastModified()
            )
        } ?: emptyList()
    }

    override suspend fun stat(path: String): SourceEntry? = withContext(Dispatchers.IO) {
        val smbFile = getSmbFile(path)
        if (!smbFile.exists()) return@withContext null
        
        SourceEntry(
            name = smbFile.name.trimEnd('/'),
            path = path,
            isDirectory = smbFile.isDirectory,
            sizeBytes = if (smbFile.isFile) smbFile.length() else null,
            lastModified = smbFile.lastModified()
        )
    }

    override suspend fun openStream(path: String): InputStream = withContext(Dispatchers.IO) {
        val smbFile = getSmbFile(path)
        if (!smbFile.exists() || smbFile.isDirectory) {
            throw IllegalArgumentException("Cannot open stream for directory or non-existent file: $path")
        }
        smbFile.inputStream
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        getSmbFile(path).exists()
    }
}
