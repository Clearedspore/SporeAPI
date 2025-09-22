package me.clearedSpore.sporeAPI.util

object StringUtil {

    fun String.capitalizeFirstLetter(input: String?): String? {
        if (input.isNullOrEmpty()) return input
        val lower = input.lowercase()
        return lower.replaceFirstChar(Char::uppercaseChar)
    }

    fun String.joinWithSpaces(vararg parts: String?): String =
        parts.filterNotNull().joinToString(" ")
}
