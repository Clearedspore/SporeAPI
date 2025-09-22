package me.clearedSpore.sporeAPI.util

object TimeService {

    fun formatTimeLong(totalSeconds: Int): String {
        val (d, h, m, s) = breakdown(totalSeconds)
        return "${d} days ${h} hours ${m} minutes ${s} seconds"
    }

    fun formatTimeShort(totalSeconds: Int): String {
        val (d, h, m, s) = breakdown(totalSeconds)
        return "${d}d ${h}h ${m}m ${s}s"
    }

    fun formatTimeShortSmart(totalSeconds: Int) = formatTime(totalSeconds, shortFormat = true)
    fun formatTimeLongSmart(totalSeconds: Int) = formatTime(totalSeconds, shortFormat = false)

    private fun formatUnit(value: Int, longName: String, shortName: String, shortFormat: Boolean) =
        if (shortFormat) "$value$shortName" else "$value $longName${if (value == 1) "" else "s"}"

    fun formatTime(totalSeconds: Int, shortFormat: Boolean): String {
        val (days, hours, minutes, seconds) = breakdown(totalSeconds)
        return buildString {
            if (days > 0) append(formatUnit(days, "day", "d", shortFormat)).append(" ")
            if (hours > 0) append(formatUnit(hours, "hour", "h", shortFormat)).append(" ")
            if (minutes > 0) append(formatUnit(minutes, "minute", "m", shortFormat)).append(" ")
            if (seconds > 0 || isEmpty()) append(formatUnit(seconds, "second", "s", shortFormat))
        }.trim()
    }

    private fun breakdown(totalSeconds: Int): List<Int> {
        var remaining = totalSeconds
        val days = remaining / 86400; remaining %= 86400
        val hours = remaining / 3600; remaining %= 3600
        val minutes = remaining / 60; val seconds = remaining % 60
        return listOf(days, hours, minutes, seconds)
    }

    fun parseTime(timeString: String): Long {
        var total = 0L
        var number = StringBuilder()
        for (c in timeString) {
            if (c.isDigit()) number.append(c)
            else if (number.isNotEmpty()) {
                val value = number.toString().toLong()
                total += when (c) {
                    's' -> value
                    'm' -> value * 60
                    'h' -> value * 3600
                    'd' -> value * 86400
                    else -> throw IllegalArgumentException("Invalid time unit: $c")
                }
                number = StringBuilder()
            }
        }
        return total * 1000
    }

    fun convertToMilliseconds(timeString: String): Long = parseTime(timeString)
    fun convertFromMilliseconds(ms: Long, longFormat: Boolean) = formatTime((ms / 1000).toInt(), !longFormat)
}
