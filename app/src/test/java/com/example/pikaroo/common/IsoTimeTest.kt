package com.example.pikaroo.common

import com.example.pikaroo.common.time.formatIsoUtc
import org.junit.Assert.assertEquals
import org.junit.Test

class IsoTimeTest {

    @Test
    fun `epoch zero is the start of 1970`() {
        assertEquals("1970-01-01T00:00:00Z", formatIsoUtc(0))
    }

    @Test
    fun `a known timestamp is formatted in UTC`() {
        assertEquals("2001-09-09T01:46:40Z", formatIsoUtc(1_000_000_000))
        assertEquals("2026-10-20T15:05:00Z", formatIsoUtc(1_792_508_700))
    }
}
