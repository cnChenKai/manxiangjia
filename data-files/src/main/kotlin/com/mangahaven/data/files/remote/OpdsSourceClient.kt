package com.mangahaven.data.files.remote

import android.net.Uri
import com.mangahaven.data.files.SourceClient
import com.mangahaven.model.Source
import com.mangahaven.model.SourceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import okhttp3.Response
import timber.log.Timber
import java.io.InputStream
import java.io.IOException
import java.io.StringReader

/**
 * OPDS 协议的内容源客户端。
 * 满足不给设备带来存储负担的要求，OPDS 源是纯动态的，我们仅请求并解析 XML 获取书目，
 * 不将源下载至本地，除非用户实际打开页面，符合 "Library Management" Phase 4/Phase 0 强化要求。
 */
class OpdsSourceClient(
    private val source: Source,
    private val okHttpClient: OkHttpClient
) : SourceClient {

    private val baseUrl: String = source.configJson

    override suspend fun list(path: String): List<SourceEntry> =
        withContext(Dispatchers.IO) {
            val requestUrl = if (path.isEmpty() || path == "/") baseUrl else path

            val request = Request.Builder()
                .url(requestUrl)
                .build()

            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("OPDS server returned HTTP ${response.code}: ${response.message}")
                    }
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.isEmpty()) {
                        return@withContext emptyList()
                    }
                    parseOpdsXml(bodyString, requestUrl)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to list OPDS path: $path")
                throw e
            }
        }

    override suspend fun stat(path: String): SourceEntry? =
        withContext(Dispatchers.IO) {
            // OPDS generally doesn't support specific stats unless fetching the feed.
            // We can just try to see if it's a valid link.
            try {
                val request = Request.Builder().url(path).head().build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        SourceEntry(
                            name = Uri.parse(path).lastPathSegment ?: "opds_entry",
                            path = path,
                            isDirectory = response.header("Content-Type")?.contains("application/atom+xml") == true,
                            sizeBytes = response.header("Content-Length")?.toLongOrNull(),
                            lastModified = System.currentTimeMillis() // Simple fallback
                        )
                    } else null
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to stat OPDS path: $path")
                null
            }
        }

    override suspend fun openStream(path: String): InputStream =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(path).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                throw IOException("OPDS server returned HTTP ${response.code}")
            }
            val body = response.body ?: run { response.close(); throw IOException("Empty OPDS body for $path") }
            // 包装流，关闭时自动释放 Response 连接
            ResponseClosingInputStream(body.byteStream(), response)
        }

    override suspend fun exists(path: String): Boolean =
        withContext(Dispatchers.IO) {
            stat(path) != null
        }

    private fun parseOpdsXml(xmlString: String, currentUrl: String): List<SourceEntry> {
        val entries = mutableListOf<SourceEntry>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xmlString))

            var eventType = xpp.eventType
            var inEntry = false
            var currentTitle = ""
            var currentLink = ""
            var isDir = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (xpp.name) {
                            "entry" -> {
                                inEntry = true
                                currentTitle = ""
                                currentLink = ""
                                isDir = false
                            }
                            "title" -> {
                                if (inEntry) {
                                    currentTitle = xpp.nextText()
                                }
                            }
                            "link" -> {
                                if (inEntry) {
                                    val rel = xpp.getAttributeValue(null, "rel")
                                    val type = xpp.getAttributeValue(null, "type")
                                    val href = xpp.getAttributeValue(null, "href")

                                    // Identify if it's a catalog (directory) or acquisition (file)
                                    if (rel?.contains("acquisition") == true) {
                                        currentLink = href
                                        isDir = false
                                    } else if (rel?.contains("subsection") == true || type?.contains("application/atom+xml") == true) {
                                        currentLink = href
                                        isDir = true
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (xpp.name == "entry" && currentLink.isNotEmpty()) {
                            // Resolve relative links
                            val resolvedPath = if (currentLink.startsWith("http")) {
                                currentLink
                            } else {
                                Uri.withAppendedPath(Uri.parse(currentUrl), currentLink).toString()
                            }

                            entries.add(
                                SourceEntry(
                                    name = currentTitle.ifEmpty { "Unknown" },
                                    path = resolvedPath,
                                    isDirectory = isDir,
                                    sizeBytes = null,
                                    lastModified = System.currentTimeMillis()
                                )
                            )
                            inEntry = false
                        }
                    }
                }
                eventType = xpp.next()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing OPDS XML")
        }
        return entries
    }
}
