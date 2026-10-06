package com.anxinkan.app

import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockDisplayModelTest {
    private val shanghai = ZoneId.of("Asia/Shanghai")

    @Test
    fun formatsEveryWeekdayWithFixedNumbers() {
        val expected = listOf("星期 1", "星期 2", "星期 3", "星期 4", "星期 5", "星期 6", "星期 7")
        for ((index, label) in expected.withIndex()) {
            val instant = LocalDate.of(2026, 1, 5)
                .plusDays(index.toLong())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
            val display = ClockDisplayModel.at(instant, ZoneOffset.UTC)
            assertEquals(label, display.weekday)
        }
    }

    @Test
    fun formatsDateAndTimeWithLeadingZerosAnd24HourClock() {
        val display = ClockDisplayModel.at(
            Instant.parse("2026-03-07T00:05:00Z"),
            ZoneOffset.UTC,
        )

        assertEquals("2026-03-07", display.date)
        assertEquals("00:05", display.time)
        assertEquals("星期 6", display.weekday)
    }

    @Test
    fun outputDoesNotDependOnDefaultLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val us = ClockDisplayModel.at(Instant.parse("2026-10-04T06:35:00Z"), shanghai)
            Locale.setDefault(Locale.JAPAN)
            val japan = ClockDisplayModel.at(Instant.parse("2026-10-04T06:35:00Z"), shanghai)

            assertEquals(us, japan)
            assertEquals("2026-10-04", us.date)
            assertEquals("14:35", us.time)
            assertEquals("星期 7", us.weekday)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun dateAndWeekdayChangeTogetherAcrossMidnight() {
        val before = ClockDisplayModel.at(Instant.parse("2026-10-03T15:59:00Z"), shanghai)
        val after = ClockDisplayModel.at(Instant.parse("2026-10-03T16:00:00Z"), shanghai)

        assertEquals("2026-10-03", before.date)
        assertEquals("23:59", before.time)
        assertEquals("星期 6", before.weekday)
        assertEquals("2026-10-04", after.date)
        assertEquals("00:00", after.time)
        assertEquals("星期 7", after.weekday)
    }
    @Test
    fun nextMinuteDelayUsesLocalMinuteBoundary() {
        val before = ClockDisplayModel.nextLocalMinuteDelayMillis(
            Instant.parse("2026-10-03T15:59:30Z"),
            shanghai,
        )
        val after = ClockDisplayModel.nextLocalMinuteDelayMillis(
            Instant.parse("2026-10-03T16:00:00.250Z"),
            shanghai,
        )

        assertEquals(30_000L, before)
        assertEquals(59_750L, after)
    }
    @Test
    fun everyStartedRefreshTriggerRecomputesTheWholeDisplay() {
        val instant = Instant.parse("2026-10-04T06:35:35Z")
        val expected = ClockDisplayModel.at(instant, shanghai)

        for (trigger in ClockRefreshTrigger.entries) {
            val result = ClockDisplayModel.refresh(trigger, instant, shanghai, started = true)
            assertEquals(expected, result.display)
            assertEquals(25_000L, result.nextDelayMillis)
        }
    }

    @Test
    fun stoppedRefreshDoesNotUpdateOrSchedule() {
        val result = ClockDisplayModel.refresh(
            ClockRefreshTrigger.TimezoneChanged,
            Instant.parse("2026-10-04T06:35:00Z"),
            shanghai,
            started = false,
        )

        assertEquals(null, result.display)
        assertEquals(null, result.nextDelayMillis)
    }

    @Test
    fun timezoneRefreshUsesNewZoneForDateTimeAndWeekday() {
        val instant = Instant.parse("2026-10-03T16:00:00Z")
        val result = ClockDisplayModel.refresh(
            ClockRefreshTrigger.TimezoneChanged,
            instant,
            ZoneId.of("America/Los_Angeles"),
            started = true,
        )

        assertEquals("2026-10-03", result.display?.date)
        assertEquals("09:00", result.display?.time)
        assertEquals("星期 6", result.display?.weekday)
    }
}
