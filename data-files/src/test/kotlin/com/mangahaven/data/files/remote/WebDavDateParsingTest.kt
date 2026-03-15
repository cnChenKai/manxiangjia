package com.mangahaven.data.files.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class WebDavDateParsingTest {

    @Test
    fun testRfc1123DateParsing() {
        val dateStr = "Thu, 13 Feb 2025 10:44:48 GMT"
        val lastModified = try {
            ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            null
        }

        assertNotNull(lastModified)
        // 1739443488000 is Thu Feb 13 2025 10:44:48 UTC
        assertEquals(1739443488000L, lastModified)
    }

    @Test
    fun testAnotherRfc1123DateParsing() {
        val dateStr = "Wed, 01 Jan 2020 00:00:00 GMT"
        val lastModified = try {
            ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            null
        }

        assertNotNull(lastModified)
        assertEquals(1577836800000L, lastModified)
    }
}
