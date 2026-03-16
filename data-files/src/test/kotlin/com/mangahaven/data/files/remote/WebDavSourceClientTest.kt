package com.mangahaven.data.files.remote

import com.mangahaven.model.Source
import com.mangahaven.model.SourceEntry
import com.mangahaven.model.SourceType
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import org.junit.Assert.assertThrows

class WebDavSourceClientTest {

    @Test
    fun testParsePropfindResponse() { runBlocking {
        val xml = """<?xml version="1.0" encoding="utf-8"?>
<D:multistatus xmlns:D="DAV:">
  <D:response>
    <D:href>/dav/</D:href>
    <D:propstat>
      <D:prop>
        <D:getlastmodified>Thu, 13 Feb 2025 10:44:48 GMT</D:getlastmodified>
        <D:resourcetype><D:collection/></D:resourcetype>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
  <D:response>
    <D:href>/dav/test%20space.txt</D:href>
    <D:propstat>
      <D:prop>
        <D:getlastmodified>Thu, 13 Feb 2025 10:44:48 GMT</D:getlastmodified>
        <D:getcontentlength>123</D:getcontentlength>
        <D:resourcetype/>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
</D:multistatus>"""

        val source = Source(id = "1", name = "Test", type = SourceType.WEBDAV, configJson = "http://localhost/dav", authRef = null)

        val mockHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(207)
                .message("Multi-Status")
                .body(xml.toResponseBody("application/xml".toMediaType()))
                .build()
        }.build()

        val client = WebDavSourceClient(source, mockHttpClient)
        val entries = client.list("/")

        assertEquals(1, entries.size)
        assertEquals("test space.txt", entries[0].name)
        assertEquals(123L, entries[0].sizeBytes)
        assertEquals(false, entries[0].isDirectory)
    } }

    @Test
    fun testErrorResponseThrowsException() { runBlocking {
        val source = Source(id = "1", name = "Test", type = SourceType.WEBDAV, configJson = "http://localhost/dav", authRef = null)

        val mockHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body("".toResponseBody("application/xml".toMediaType()))
                .build()
        }.build()

        val client = WebDavSourceClient(source, mockHttpClient)

        assertThrows(IOException::class.java) {
            runBlocking {
                client.list("/")
            }
        }
    } }
}
