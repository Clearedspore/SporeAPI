package me.clearedSpore.sporeAPI.util

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object StringUtil {

    fun String.capitalizeFirstLetter(): String? {
        val input = this
        if (input.isNullOrEmpty()) return input
        val lower = input.lowercase()
        return lower.replaceFirstChar(Char::uppercaseChar)
    }

    fun String.joinWithSpaces(vararg parts: String?): String {
        return listOf(this, *parts).filterNotNull().joinToString(" ")
    }

    fun String.splitPipe(): List<String> {
        return this.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun String.firstPart(): String = this.splitPipe().firstOrNull() ?: this

    fun String.hasFlag(flag: String): Boolean {
        return this.splitPipe().any { it.equals(flag, ignoreCase = true) }
    }

}
