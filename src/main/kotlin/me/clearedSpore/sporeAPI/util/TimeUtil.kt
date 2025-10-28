package me.clearedSpore.sporeAPI.util

import java.util.concurrent.TimeUnit
import kotlin.math.floor

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object TimeUtil {

    private val timeRegex = "(\\d+)([smhd])".toRegex(RegexOption.IGNORE_CASE)

    enum class TimeUnitStyle { SHORT, LONG, COMPACT, MINIMAL }

    /**
     * Parses a duration string like "1h30m", "2d", "45s" into milliseconds.
     * Returns 0 for invalid formats.
     */
    fun parseDuration(input: String): Long {
        var total = 0L
        timeRegex.findAll(input).forEach { match ->
            val (amountStr, unit) = match.destructured
            val amount = amountStr.toLongOrNull() ?: return@forEach
            total += when (unit.lowercase()) {
                "s" -> TimeUnit.SECONDS.toMillis(amount)
                "m" -> TimeUnit.MINUTES.toMillis(amount)
                "h" -> TimeUnit.HOURS.toMillis(amount)
                "d" -> TimeUnit.DAYS.toMillis(amount)
                else -> 0L
            }
        }
        return total
    }

    /**
     * Formats a duration (in ms) into a human-readable string.
     *
     * @param durationMs Duration in milliseconds.
     * @param style Display style.
     * @param maxUnits Max number of units to display (e.g., 2 → "1d 2h").
     * @param showZero Whether to include zero units.
     * @param separator String between units, e.g., " " or ", ".
     */
    fun formatDuration(
        durationMs: Long,
        style: TimeUnitStyle = TimeUnitStyle.SHORT,
        maxUnits: Int = Int.MAX_VALUE,
        showZero: Boolean = false,
        separator: String = " "
    ): String {
        var remaining = durationMs
        val days = TimeUnit.MILLISECONDS.toDays(remaining)
        remaining -= TimeUnit.DAYS.toMillis(days)
        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
        remaining -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
        remaining -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining)

        val units = listOf(
            "d" to days,
            "h" to hours,
            "m" to minutes,
            "s" to seconds
        )

        val builder = mutableListOf<String>()
        for ((unit, value) in units) {
            if (value > 0 || showZero) {
                builder.add(formatUnit(value, unit, style))
            }
        }

        return if (builder.isEmpty()) formatUnit(0, "s", style)
        else builder.take(maxUnits).joinToString(separator)
    }

    private fun formatUnit(value: Long, unit: String, style: TimeUnitStyle): String {
        return when (style) {
            TimeUnitStyle.SHORT -> "$value$unit"
            TimeUnitStyle.LONG -> {
                val full = when (unit) {
                    "d" -> if (value == 1L) "day" else "days"
                    "h" -> if (value == 1L) "hour" else "hours"
                    "m" -> if (value == 1L) "minute" else "minutes"
                    "s" -> if (value == 1L) "second" else "seconds"
                    else -> unit
                }
                "$value $full"
            }
            TimeUnitStyle.COMPACT -> "$value$unit"
            TimeUnitStyle.MINIMAL -> String.format("%02d", value)
        }
    }

    /**
     * Converts a duration in milliseconds into a total in seconds/minutes/etc.
     */
    fun toSeconds(durationMs: Long) = TimeUnit.MILLISECONDS.toSeconds(durationMs)
    fun toMinutes(durationMs: Long) = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    fun toHours(durationMs: Long) = TimeUnit.MILLISECONDS.toHours(durationMs)
    fun toDays(durationMs: Long) = TimeUnit.MILLISECONDS.toDays(durationMs)

    /**
     * Adds two durations in milliseconds.
     */
    fun add(duration1: Long, duration2: Long) = duration1 + duration2

    /**
     * Subtracts duration2 from duration1.
     */
    fun subtract(duration1: Long, duration2: Long) = duration1 - duration2
}
