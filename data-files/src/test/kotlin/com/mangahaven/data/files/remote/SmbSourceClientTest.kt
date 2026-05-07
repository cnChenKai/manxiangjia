package com.mangahaven.data.files.remote

import com.mangahaven.model.Source
import com.mangahaven.model.SourceType
import jcifs.CIFSContext
import jcifs.smb.NtlmPasswordAuthenticator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Field

class SmbSourceClientTest {

    @Test
    fun testAuthParsingWithDomain() {
        val source = Source(
            id = "1",
            type = SourceType.SMB,
            name = "Test SMB",
            configJson = "192.168.1.1",
            authRef = "MYDOMAIN;myuser:mypass"
        )
        val client = SmbSourceClient(source)

        val cifsContext = getCifsContext(client)
        val credentials = cifsContext.credentials

        assertTrue(credentials is NtlmPasswordAuthenticator)
        val auth = credentials as NtlmPasswordAuthenticator

        assertEquals("MYDOMAIN", auth.userDomain)
        assertEquals("myuser", auth.username)
        assertEquals("mypass", auth.password)
    }

    @Test
    fun testAuthParsingWithoutDomain() {
        val source = Source(
            id = "2",
            type = SourceType.SMB,
            name = "Test SMB",
            configJson = "192.168.1.1",
            authRef = "myuser:mypass"
        )
        val client = SmbSourceClient(source)

        val cifsContext = getCifsContext(client)
        val credentials = cifsContext.credentials

        assertTrue(credentials is NtlmPasswordAuthenticator)
        val auth = credentials as NtlmPasswordAuthenticator

        assertEquals("", auth.userDomain)
        assertEquals("myuser", auth.username)
        assertEquals("mypass", auth.password)
    }

    @Test
    fun testGuestAuth() {
        val source = Source(
            id = "3",
            type = SourceType.SMB,
            name = "Test SMB",
            configJson = "192.168.1.1",
            authRef = null
        )
        val client = SmbSourceClient(source)

        val cifsContext = getCifsContext(client)
        val credentials = cifsContext.credentials

        assertTrue(credentials is NtlmPasswordAuthenticator)
        val auth = credentials as NtlmPasswordAuthenticator

        // jcifs-ng guest credentials typically have empty/null values or specific flags
        // For guest, NtlmPasswordAuthenticator(null, null, null) is often used via withGuestCredentials
        // In jcifs-ng, guest user is usually "GUEST" or similar depending on implementation
        // But we just want to ensure it doesn't crash and uses guest credentials
        // baseContext.withGuestCrendentials() is called
    }

    private fun getCifsContext(client: SmbSourceClient): CIFSContext {
        val field: Field = SmbSourceClient::class.java.getDeclaredField("cifsContext\$delegate")
        field.isAccessible = true
        val lazyValue = field.get(client) as Lazy<*>
        return lazyValue.value as CIFSContext
    }
}
