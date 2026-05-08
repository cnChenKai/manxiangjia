package com.mangahaven.data.files.remote

import com.mangahaven.data.files.SourceClient
import com.mangahaven.model.Source
import com.mangahaven.model.SourceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import timber.log.Timber
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 基于 OkHttp 和内置 XML 解析的轻量级 WebDAV 客户端。
 */
class WebDavSourceClient(
    private val source: Source,
    private val httpClient: OkHttpClient,
) : SourceClient {

    private val authHeader: String? = source.authRef?.takeIf { it.contains(":") }?.let {
        val parts = it.split(":", limit = 2)
        Credentials.basic(parts[0], parts[1])
    }

    private fun Request.Builder.addAuth(): Request.Builder {
        authHeader?.let { header("Authorization", it) }
        return this
    }

    override suspend fun list(path: String): List<SourceEntry> = withContext(Dispatchers.IO) {
        val url = buildUrl(path)
        
        // WebDAV PROPFIND，Depth = 1 查询子目录
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", """<?xml version="1.0" encoding="utf-8"?><D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>""".toRequestBody("text/xml".toMediaType()))
            .header("Depth", "1")
            .addAuth()
            .build()
            
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("WebDAV list failed: ${response.code} ${response.message}")
            }
            
            parsePropfindResponse(response.body?.string() ?: "", path)
        }
    }

    override suspend fun stat(path: String): SourceEntry? = withContext(Dispatchers.IO) {
        val url = buildUrl(path)
        
        // WebDAV PROPFIND，Depth = 0 查询自己
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", """<?xml version="1.0" encoding="utf-8"?><D:propfind xmlns:D="DAV:"><D:allprop/></D:propfind>""".toRequestBody("text/xml".toMediaType()))
            .header("Depth", "0")
            .addAuth()
            .build()
            
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw java.io.IOException("WebDAV stat failed: ${response.code} ${response.message}")
            parsePropfindResponse(response.body?.string() ?: "", path).firstOrNull()
        }
    }

    override suspend fun openStream(path: String): InputStream = withContext(Dispatchers.IO) {
        val url = buildUrl(path)
        val request = Request.Builder()
            .url(url)
            .get()
            .addAuth()
            .build()
            
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IllegalStateException("Failed to GET WebDAV stream: ${response.code}")
        }
        
        val body = response.body ?: run { response.close(); throw IllegalStateException("Empty WebDAV body") }
        // 包装流，关闭时自动释放 Response 连接
        ResponseClosingInputStream(body.byteStream(), response)
    }

    override suspend fun exists(path: String): Boolean {
        return stat(path) != null
    }

    private fun buildUrl(path: String): String {
        val baseUrl = source.configJson.trimEnd('/')
        val subPath = path.trimStart('/')
        return if (subPath.isEmpty()) baseUrl else "$baseUrl/$subPath"
    }

    private fun parsePropfindResponse(xmlContent: String, requestPath: String): List<SourceEntry> {
        val entries = mutableListOf<SourceEntry>()
        if (xmlContent.isEmpty()) return entries

        try {
            val factory = DocumentBuilderFactory.newInstance()
            // XXE Security Fix: Disable DTDs and external entities
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            factory.isXIncludeAware = false
            factory.isExpandEntityReferences = false

            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(xmlContent.byteInputStream())
            
            val responses: org.w3c.dom.NodeList = getElementsByLocalName(doc, "response")
            
            for (i in 0 until responses.length) {
                val responseNode = responses.item(i) as org.w3c.dom.Element
                val hrefNode = getElementsByLocalName(responseNode, "href").item(0)
                val href = hrefNode?.textContent?.trim() ?: continue
                
                val normalizedHref = href.trimEnd('/')
                val reqSubPath = requestPath.trimStart('/').trimEnd('/')

                // Skip self. If request is /webdav, and href is /webdav/
                if (reqSubPath.isNotEmpty() && normalizedHref.endsWith(reqSubPath)) {
                    if (responses.length > 1 && normalizedHref.endsWith(reqSubPath)) continue
                } else if (reqSubPath.isEmpty() && (normalizedHref.isEmpty() || normalizedHref == "/")) {
                    if (responses.length > 1) continue
                }

                val propstats = getElementsByLocalName(responseNode, "propstat")
                var propNode: org.w3c.dom.Element? = null

                for (j in 0 until propstats.length) {
                    val propstatNode = propstats.item(j) as org.w3c.dom.Element
                    val statusNode = getElementsByLocalName(propstatNode, "status").item(0)
                    val status = statusNode?.textContent ?: ""
                    if (status.contains("200")) {
                        propNode = getElementsByLocalName(propstatNode, "prop").item(0) as? org.w3c.dom.Element
                        break
                    }
                }

                if (propNode == null && propstats.length > 0) {
                    val propstatNode = propstats.item(0) as org.w3c.dom.Element
                    propNode = getElementsByLocalName(propstatNode, "prop").item(0) as? org.w3c.dom.Element
                }
                
                if (propNode != null) {
                    // Check for collection tag inside resourcetype or anywhere in prop
                    val isDir = getElementsByLocalName(propNode, "collection").length > 0 || normalizedHref.endsWith("/")
                    
                    val contentLengthNode = getElementsByLocalName(propNode, "getcontentlength").item(0)
                    val sizeBytes = contentLengthNode?.textContent?.toLongOrNull() ?: 0L
                    
                    val lastModifiedNode = getElementsByLocalName(propNode, "getlastmodified").item(0)
                    var lastModified: Long? = null

                    if (lastModifiedNode != null && !lastModifiedNode.textContent.isNullOrBlank()) {

                        val dateStr = lastModifiedNode.textContent
                        try {
                            if (dateStr.contains("0001 00:00:00")) {
                                lastModified = 0L // 123Pan invalid epoch fallback
                            } else {
                                lastModified = java.time.ZonedDateTime.parse(dateStr, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                                    .toInstant()
                                    .toEpochMilli()
                            }
                        } catch (e: Exception) {
                            Timber.w("Failed to parse lastModified date: $dateStr")
                        }

                    }
                    
                    val decodedHref = try {
                        java.net.URLDecoder.decode(normalizedHref, "UTF-8")
                    } catch (e: Exception) { normalizedHref }

                    // We need the raw name. Example: "/webdav/B 站保存" -> "B 站保存"
                    val name = decodedHref.substringAfterLast('/')

                    if (name.isNotEmpty()) {
                        entries.add(
                            SourceEntry(
                                name = name,
                                path = decodedHref,
                                isDirectory = isDir,
                                sizeBytes = sizeBytes,
                                lastModified = lastModified
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse WebDAV XML. Content was: \n$xmlContent")
            throw java.io.IOException("Failed to parse WebDAV XML", e)
        }
        return entries
    }

    private fun getElementsByLocalName(doc: org.w3c.dom.Document, localName: String): org.w3c.dom.NodeList {
        var nodes = doc.getElementsByTagNameNS("*", localName)
        if (nodes.length > 0) return nodes

        nodes = doc.getElementsByTagName(localName)
        if (nodes.length > 0) return nodes

        nodes = doc.getElementsByTagName("d:$localName")
        if (nodes.length > 0) return nodes

        return doc.getElementsByTagName("D:$localName")
    }

    private fun getElementsByLocalName(element: org.w3c.dom.Element, localName: String): org.w3c.dom.NodeList {
        var nodes = element.getElementsByTagNameNS("*", localName)
        if (nodes.length > 0) return nodes

        nodes = element.getElementsByTagName(localName)
        if (nodes.length > 0) return nodes

        nodes = element.getElementsByTagName("d:$localName")
        if (nodes.length > 0) return nodes

        return element.getElementsByTagName("D:$localName")
    }
}
