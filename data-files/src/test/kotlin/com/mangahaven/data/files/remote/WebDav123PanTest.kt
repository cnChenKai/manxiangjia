package com.mangahaven.data.files.remote

import com.mangahaven.model.Source
import com.mangahaven.model.SourceType
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

class WebDav123PanTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: WebDavSourceClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val source = Source(
            id = "1",
            name = "123Pan WebDAV",
            type = SourceType.WEBDAV,
            configJson = mockWebServer.url("/webdav").toString(),
            authRef = "user:pass"
        )

        client = WebDavSourceClient(source, OkHttpClient())
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `parse 123pan specific xml structure`() = runBlocking {
        val xmlResponse = """<?xml version="1.0" encoding="UTF-8"?>
<D:multistatus xmlns:D="DAV:">
  <D:response>
    <D:href>/webdav/</D:href>
    <D:propstat>
      <D:prop>
        <D:resourcetype><D:collection/></D:resourcetype>
        <D:getlastmodified>Mon, 01 Jan 0001 00:00:00 GMT</D:getlastmodified>
        <D:supportedlock>
          <D:lockentry>
            <D:lockscope><D:exclusive/></D:lockscope>
            <D:locktype><D:write/></D:locktype>
          </D:lockentry>
        </D:supportedlock>
        <D:displayname>root</D:displayname>
        <D:creationdate>Mon, 01 Jan 0001 00:00:00 GMT</D:creationdate>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
  <D:response>
    <D:href>/webdav/B%20%E7%AB%99%E4%BF%9D%E5%AD%98/</D:href>
    <D:propstat>
      <D:prop>
        <D:resourcetype><D:collection/></D:resourcetype>
        <D:getlastmodified>Thu, 15 Mar 2026 12:00:00 GMT</D:getlastmodified>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
  <D:response>
    <D:href>/webdav/test.txt</D:href>
    <D:propstat>
      <D:prop>
        <D:resourcetype/>
        <D:getlastmodified>Wed, 11 Sep 2024 14:00:00 GMT</D:getlastmodified>
        <D:getcontentlength>520</D:getcontentlength>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
</D:multistatus>"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(207)
                .setHeader("Content-Type", "text/xml; charset=utf-8")
                .setBody(xmlResponse)
        )

        val entries = client.list("/webdav")

        // Root entry /webdav/ should be filtered out
        assertEquals(2, entries.size)

        val folder = entries.find { it.name == "B 站保存" }
        assertTrue(folder != null)
        assertTrue(folder!!.isDirectory)
        assertEquals("/webdav/B 站保存", folder.path)

        val file = entries.find { it.name == "test.txt" }
        assertTrue(file != null)
        assertFalse(file!!.isDirectory)
        assertEquals(520L, file.sizeBytes)
        assertEquals("/webdav/test.txt", file.path)
    }
}
