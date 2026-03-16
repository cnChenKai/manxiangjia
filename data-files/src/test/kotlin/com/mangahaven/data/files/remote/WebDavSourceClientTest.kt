package com.mangahaven.data.files.remote

import com.mangahaven.model.Source
import com.mangahaven.model.SourceEntry
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class WebDavSourceClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: WebDavSourceClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val source = Source(
            id = "1",
            name = "Test WebDAV",
            type = com.mangahaven.model.SourceType.WEBDAV,
            configJson = mockWebServer.url("/").toString(),
            authRef = "user:password"
        )

        client = WebDavSourceClient(source, OkHttpClient())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `list root returns expected entries`() = runBlocking {
        val xmlResponse = """<?xml version="1.0" encoding="utf-8"?>
<D:multistatus xmlns:D="DAV:">
  <D:response>
    <D:href>/</D:href>
    <D:propstat>
      <D:prop>
        <D:getlastmodified>Tue, 10 Sep 2024 12:00:00 GMT</D:getlastmodified>
        <D:resourcetype><D:collection/></D:resourcetype>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
  <D:response>
    <D:href>/folder1/</D:href>
    <D:propstat>
      <D:prop>
        <D:getlastmodified>Tue, 10 Sep 2024 13:00:00 GMT</D:getlastmodified>
        <D:resourcetype><D:collection/></D:resourcetype>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
  <D:response>
    <D:href>/file.cbz</D:href>
    <D:propstat>
      <D:prop>
        <D:getlastmodified>Wed, 11 Sep 2024 14:00:00 GMT</D:getlastmodified>
        <D:getcontentlength>1024</D:getcontentlength>
        <D:resourcetype/>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
</D:multistatus>"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setHeader("Content-Type", "application/xml; charset=utf-8")
                .setBody(xmlResponse)
        )

        val entries = client.list("/")

        assertEquals(2, entries.size)

        val folder = entries.find { it.name == "folder1" }
        assertTrue(folder != null)
        assertTrue(folder!!.isDirectory)
        assertEquals("/folder1", folder.path)

        val file = entries.find { it.name == "file.cbz" }
        assertTrue(file != null)
        assertFalse(file!!.isDirectory)
        assertEquals(1024L, file.sizeBytes)
        assertEquals("/file.cbz", file.path)

        val request = mockWebServer.takeRequest()
        assertEquals("PROPFIND", request.method)
        assertEquals("1", request.getHeader("Depth"))
    }

    @Test(expected = IOException::class)
    fun `list root throws IOException on 401 Unauthorized`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        client.list("/")
        Unit
    }

    @Test
    fun `list root with tricky paths`() = runBlocking {
        val xmlResponse = """<?xml version="1.0" encoding="utf-8"?>
<D:multistatus xmlns:D="DAV:">
  <D:response>
    <D:href>/my%20folder/</D:href>
    <D:propstat>
      <D:prop>
        <D:getlastmodified>Tue, 10 Sep 2024 12:00:00 GMT</D:getlastmodified>
        <D:resourcetype><D:collection/></D:resourcetype>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
</D:multistatus>"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setHeader("Content-Type", "application/xml; charset=utf-8")
                .setBody(xmlResponse)
        )

        val entries = client.list("/")

        assertEquals(1, entries.size)
        assertEquals("my folder", entries[0].name)
        assertEquals("/my folder", entries[0].path)
    }
}
