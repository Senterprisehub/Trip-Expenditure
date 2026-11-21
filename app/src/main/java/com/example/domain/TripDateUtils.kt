package com.example.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * UTC-safe date formatting and trip duration calculation helpers for Material 3 DatePicker values.
 *
 * Material 3 DatePickerState always represents selected dates as UTC midnight epoch milliseconds.
 * Performing both formatting and arithmetic strictly in UTC prevents timezone and DST off-by-one
 * discrepancies across all devices.
 */
object TripDateUtils {
    private const val MILLIS_PER_DAY = 86_400_000L
    const val DISPLAY_DATE_PATTERN = "dd MMM yyyy"

    /**
     * Formats a UTC epoch timestamp into `DD MMM YYYY` (e.g., `20 Nov 2026`).
     */
    fun formatDisplayDate(utcMillis: Long): String {
        val formatter = SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(utcMillis))
    }

    /**
     * Normalizes any UTC epoch millisecond value to 00:00:00.000 UTC of that calendar day.
     */
    fun normalizeToUtcDayStart(utcMillis: Long): Long {
        return Math.floorDiv(utcMillis, MILLIS_PER_DAY) * MILLIS_PER_DAY
    }

    /**
     * Calculates inclusive trip days between [startUtcMillis] and [endUtcMillis].
     * For example, `20 Nov 2026` to `24 Nov 2026` returns `5` days; same-day returns `1` day.
     * Reversed ranges are safely clamped to `1`.
     */
    fun calculateInclusiveDays(startUtcMillis: Long, endUtcMillis: Long): Int {
        val startDay = Math.floorDiv(startUtcMillis, MILLIS_PER_DAY)
        val endDay = Math.floorDiv(endUtcMillis, MILLIS_PER_DAY)
        val inclusiveDays = (endDay - startDay) + 1L
        return inclusiveDays.coerceAtLeast(1L).toInt()
    }

    /**
     * Calculates the number of nights from an inclusive day count (`days - 1`, minimum `0`).
     */
    fun calculateNightsFromDays(daysCount: Int): Int {
        return (daysCount - 1).coerceAtLeast(0)
    }

    /**
     * Calculates the number of nights directly between [startUtcMillis] and [endUtcMillis].
     */
    fun calculateNights(startUtcMillis: Long, endUtcMillis: Long): Int {
        return calculateNightsFromDays(calculateInclusiveDays(startUtcMillis, endUtcMillis))
    }

    /**
     * Formats a concise label displaying both Days and Nights (e.g., `5 Days / 4 Nights`).
     */
    fun formatDurationLabel(daysCount: Int): String {
        val safeDays = daysCount.coerceAtLeast(1)
        val nights = calculateNightsFromDays(safeDays)
        val dayUnit = if (safeDays == 1) "Day" else "Days"
        val nightUnit = if (nights == 1) "Night" else "Nights"
        return "$safeDays $dayUnit / $nights $nightUnit"
    }
}
