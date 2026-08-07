package me.clearedSpore.sporeAPI.util


import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.regex.Pattern

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object CC {

    private val HEX_PATTERN: Pattern = Pattern.compile("(?<!\\\\)(?:\\\\\\\\)*&#[a-fA-F0-9]{6}")
    private val miniMessage = MiniMessage.builder()
        .tags(
            TagResolver.builder()
                .resolver(StandardTags.defaults())
                .tag("s_blue", Tag.styling(TextColor.fromHexString("#1D91FF")!!))
                .tag("s_red", Tag.styling(TextColor.fromHexString("#F50000")!!))
                .build()
        )
        .build()

    private val legacySerializer = LegacyComponentSerializer.builder()
        .character(LegacyComponentSerializer.SECTION_CHAR)
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build()

    private val LEGACY_TAGS: Map<Char, String> = mapOf(
        '0' to "black", '1' to "dark_blue", '2' to "dark_green", '3' to "dark_aqua",
        '4' to "dark_red", '5' to "dark_purple", '6' to "gold", '7' to "gray",
        '8' to "dark_gray", '9' to "blue", 'a' to "green", 'b' to "aqua",
        'c' to "red", 'd' to "light_purple", 'e' to "yellow", 'f' to "white",
        'k' to "obfuscated", 'l' to "bold", 'm' to "strikethrough", 'n' to "underlined", 'o' to "italic", 'r' to "reset"
    )

    fun String.translate(): String {
        var message = this

        message = message.replace("&cb", "&#1D91FF")
        message = message.replace("&cr", "&#F50000")

        val matcher = HEX_PATTERN.matcher(message)
        while (matcher.find()) {
            val hex = matcher.group().takeLast(6)
            message = message.replace(matcher.group(), "<#$hex>")
        }

        message = translateLegacyCodes(message)

        return try {
            val component: Component = miniMessage.deserialize(message)
            legacySerializer.serialize(component)
        } catch (ex: Exception) {
            message
        }
    }

    private fun translateLegacyCodes(input: String): String {
        val builder = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '&' && i + 1 < input.length) {
                val tag = LEGACY_TAGS[input[i + 1].lowercaseChar()]
                if (tag != null) {
                    builder.append('<').append(tag).append('>')
                    i += 2
                    continue
                }
            }
            builder.append(c)
            i++
        }
        return builder.toString()
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
