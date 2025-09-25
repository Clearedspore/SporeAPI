package me.clearedSpore.sporeAPI.util

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object StringUtil {

    fun String.capitalizeFirstLetter(input: String?): String? {
        if (input.isNullOrEmpty()) return input
        val lower = input.lowercase()
        return lower.replaceFirstChar(Char::uppercaseChar)
    }

    fun String.joinWithSpaces(vararg parts: String?): String =
        parts.filterNotNull().joinToString(" ")
}
