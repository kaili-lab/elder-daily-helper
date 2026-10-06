package com.anxinkan.app

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class ClockDisplay(
    val date: String,
    val time: String,
    val weekday: String,
)

internal enum class ClockRefreshTrigger {
    Foreground,
    TimeChanged,
    DateChanged,
    TimezoneChanged,
    MinuteElapsed,
}

internal data class ClockRefreshResult(
    val display: ClockDisplay?,
    val nextDelayMillis: Long?,
)

internal object ClockDisplayModel {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun at(instant: Instant, zoneId: ZoneId): ClockDisplay {
        val dateTime = instant.atZone(zoneId)
        return ClockDisplay(
            date = dateTime.format(dateFormatter),
            time = dateTime.format(timeFormatter),
            weekday = "星期 ${dateTime.dayOfWeek.value}",
        )
    }

    fun refresh(
        trigger: ClockRefreshTrigger,
        instant: Instant,
        zoneId: ZoneId,
        started: Boolean,
    ): ClockRefreshResult {
        if (!started) return ClockRefreshResult(display = null, nextDelayMillis = null)
        return ClockRefreshResult(
            display = at(instant, zoneId),
            nextDelayMillis = nextLocalMinuteDelayMillis(instant, zoneId),
        )
    }

    fun nextLocalMinuteDelayMillis(instant: Instant, zoneId: ZoneId): Long {
        val nextMinute = instant
            .atZone(zoneId)
            .withSecond(0)
            .withNano(0)
            .plusMinutes(1)
            .toInstant()
        return Duration.between(instant, nextMinute).toMillis().coerceAtLeast(1L)
    }

    fun current(): ClockDisplay = at(Instant.now(), ZoneId.systemDefault())

    fun weekdayNumber(dayOfWeek: DayOfWeek): Int = dayOfWeek.value
}
