package me.clearedSpore.sporeAPI.util.time

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

data class Duration(val millis: Long) {

    fun toMillis() = millis
    fun toSeconds() = millis / 1000
    fun toMinutes() = millis / 60_000
    fun toHours() = millis / 3_600_000
    fun toDays() = millis / 86_400_000

    operator fun plus(other: Duration) = Duration(millis + other.millis)
    operator fun minus(other: Duration) = Duration(millis - other.millis)

    fun format(
        style: TimeUtil.TimeUnitStyle = TimeUtil.TimeUnitStyle.SHORT,
        maxUnits: Int = Int.MAX_VALUE,
        showZero: Boolean = false,
        separator: String = " "
    ): String {
        return TimeUtil.formatDuration(millis, style, maxUnits, showZero, separator)
    }
}