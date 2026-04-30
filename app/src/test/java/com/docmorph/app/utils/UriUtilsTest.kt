package com.docmorph.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class UriUtilsTest {

    // ─── formatFileSize ───────────────────────────────────────────────────────

    @Test
    fun `formatFileSize formats bytes correctly`() {
        assertEquals("512 B",   formatFileSize(512L))
    }

    @Test
    fun `formatFileSize formats kilobytes correctly`() {
        assertEquals("1.0 KB",  formatFileSize(1_024L))
        assertEquals("512.0 KB", formatFileSize(524_288L))
    }

    @Test
    fun `formatFileSize formats megabytes correctly`() {
        assertEquals("1.0 MB",  formatFileSize(1_048_576L))
        assertEquals("9.5 MB",  formatFileSize(9_961_472L))
    }

    @Test
    fun `formatFileSize formats gigabytes correctly`() {
        assertEquals("1.00 GB", formatFileSize(1_073_741_824L))
    }

    // ─── formatTimestamp ──────────────────────────────────────────────────────

    @Test
    fun `formatTimestamp returns Just now for recent timestamps`() {
        val recent = System.currentTimeMillis() - 30_000L  // 30 seconds ago
        assertEquals("Just now", formatTimestamp(recent))
    }

    @Test
    fun `formatTimestamp returns minutes for timestamps 1-59 min ago`() {
        val fiveMinAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)
        assertEquals("5 min ago", formatTimestamp(fiveMinAgo))
    }

    @Test
    fun `formatTimestamp returns hours for timestamps 1-23 hr ago`() {
        val twoHoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)
        assertEquals("2 hr ago", formatTimestamp(twoHoursAgo))
    }

    @Test
    fun `formatTimestamp returns Yesterday for timestamps 24-48 hr ago`() {
        val yesterday = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(30)
        assertEquals("Yesterday", formatTimestamp(yesterday))
    }

    @Test
    fun `formatTimestamp returns days for timestamps 2-6 days ago`() {
        val threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        assertEquals("3 days ago", formatTimestamp(threeDaysAgo))
    }
}
