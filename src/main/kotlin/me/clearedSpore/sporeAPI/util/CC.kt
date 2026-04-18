package me.clearedSpore.sporeAPI.util




import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.ChatColor
import net.kyori.adventure.text.Component
import java.util.regex.Pattern

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object CC {

    private val HEX_PATTERN: Pattern = Pattern.compile("(?<!\\\\)(?:\\\\\\\\)*&#[a-fA-F0-9]{6}")
    private val miniMessage = MiniMessage.miniMessage()
    private val legacySerializer = LegacyComponentSerializer.legacySection()

    fun String.translate(): String {
        var message = this

        message = message.replace("&cb", "&#1D91FF")
        message = message.replace("&cr", "&#F50000")

        val matcher = HEX_PATTERN.matcher(message)
        while (matcher.find()) {
            val hexCode = matcher.group().substring(1)
            message = message.replace(matcher.group(), ChatColor.of(hexCode).toString())
        }

        message = ChatColor.translateAlternateColorCodes('&', message)

        return try {
            val component: Component = miniMessage.deserialize(message)
            legacySerializer.serialize(component)
        } catch (ex: Exception) {
            message
        }
    }


    // General Colors
    fun String.white() = "&#E2E2E2$this".translate()
    fun String.blue() = "&#1D91FF$this".translate()
    fun String.orange() = "&#FF5733$this".translate()
    fun String.purple() = "&#9966CC$this".translate()
    fun String.yellow() = "&#E7FF00$this".translate()
    fun String.gray() = "&#AAAAAA$this".translate()
    fun String.gold() = "&#FFD700$this".translate()
    fun String.red() = "&#F50000$this".translate()
    fun String.green() = "&#4BFF2F$this".translate()
    fun String.aqua() = "&#46FC2A$this".translate()
    fun String.pink() = "&#F100FF$this".translate()
    fun String.darkGray() = "&#2B2B2B$this".translate()
    fun String.darkGreen() = "&#003508$this".translate()
    fun String.darkRed() = "&#640000$this".translate()
    fun String.darkBlue() = "&#000B64$this".translate()
    fun String.darkAqua() = "&#004840$this".translate()
    fun String.darkPurple() = "&#33007A$this".translate()

    // Command Colors
    fun String.accent() = "&#4DA3FF$this".translate()
    fun String.accentDark() = "&#2B6EDB$this".translate()
    fun String.warning() = "&#FFB84D$this".translate()
    fun String.error() = "&#FF5C5C$this".translate()
    fun String.success() = "&#4DFF88$this".translate()

    fun String.bold() = "&l".translate()
}