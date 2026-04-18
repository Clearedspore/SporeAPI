package me.clearedSpore.sporeAPI.util.time

import java.util.concurrent.TimeUnit

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object TimeUtil {

    private val timeRegex = "(\\d+)([smhd])".toRegex(RegexOption.IGNORE_CASE)

    enum class TimeUnitStyle { SHORT, LONG, COMPACT, MINIMAL }

    fun parse(input: String): Duration {
        var total = 0L

        timeRegex.findAll(input).forEach { match ->
            val (amountStr, unit) = match.destructured
            val amount = amountStr.toLongOrNull() ?: return@forEach

            total += when (unit.lowercase()) {
                "s" -> amount * 1000
                "m" -> amount * 60_000
                "h" -> amount * 3_600_000
                "d" -> amount * 86_400_000
                else -> 0L
            }
        }

        return Duration(total)
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

        val days = remaining / 86_400_000
        remaining %= 86_400_000

        val hours = remaining / 3_600_000
        remaining %= 3_600_000

        val minutes = remaining / 60_000
        remaining %= 60_000

        val seconds = remaining / 1000

        val units = listOf(
            "d" to days,
            "h" to hours,
            "m" to minutes,
            "s" to seconds
        )

        val parts = units
            .filter { it.second > 0 || showZero }
            .map { formatUnit(it.second, it.first, style) }

        return if (parts.isEmpty()) formatUnit(0, "s", style)
        else parts.take(maxUnits).joinToString(separator)
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
    fun seconds(value: Long) = Duration(value * 1000)
    fun minutes(value: Long) = Duration(value * 60_000)
    fun hours(value: Long) = Duration(value * 3_600_000)
    fun days(value: Long) = Duration(value * 86_400_000)

    val Int.seconds get() = Duration(this * 1000L)
    val Int.minutes get() = Duration(this * 60_000L)
    val Int.hours get() = Duration(this * 3_600_000L)
    val Int.days get() = Duration(this * 86_400_000L)
    fun now() = System.currentTimeMillis()

    /**
     * Adds two durations in milliseconds.
     */
    fun add(duration1: Long, duration2: Long) = duration1 + duration2

    /**
     * Subtracts duration2 from duration1.
     */
    fun subtract(duration1: Long, duration2: Long) = duration1 - duration2
}