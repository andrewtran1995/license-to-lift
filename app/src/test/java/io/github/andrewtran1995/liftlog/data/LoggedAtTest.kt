package io.github.andrewtran1995.liftlog.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class LoggedAtTest {
    private val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val at = 1_795_290_000_000L // 2026-11-21 ~ some time UTC
    private fun label(ageMs: Long) = formatLoggedAt(at, at + ageMs, ZoneOffset.UTC, clock)

    @Test fun secondsForTheFirstMinute() {
        assertEquals("0s ago", label(0))
        assertEquals("0s ago", label(999))
        assertEquals("12s ago", label(12_400))
        assertEquals("59s ago", label(59_999))
    }

    @Test fun minutesForTheFirstHour() {
        assertEquals("1m ago", label(60_000))
        assertEquals("59m ago", label(3_599_999))
    }

    @Test fun clockTimeAfterAnHour() {
        val expected = java.time.Instant.ofEpochMilli(at).atZone(ZoneOffset.UTC).format(clock)
        assertEquals(expected, label(3_600_000))
    }

    @Test fun futureTimeReadsZero() {
        assertEquals("0s ago", label(-5_000))
    }

    @Test fun nextChangeTicksBySecondThenMinute() {
        assertEquals(at + 1_000, nextLoggedAtChange(listOf(at), at))
        assertEquals(at + 13_000, nextLoggedAtChange(listOf(at), at + 12_400))
        assertEquals(at + 120_000, nextLoggedAtChange(listOf(at), at + 90_000))
        // The soonest change across all sets wins.
        assertEquals(at + 91_000, nextLoggedAtChange(listOf(at, at + 90_000), at + 90_000))
    }

    @Test fun nextChangeStopsOnceEverySetShowsAClock() {
        assertEquals(at + 3_600_000, nextLoggedAtChange(listOf(at), at + 3_599_000))
        assertNull(nextLoggedAtChange(listOf(at), at + 3_600_000))
        assertNull(nextLoggedAtChange(emptyList(), at))
    }
}
