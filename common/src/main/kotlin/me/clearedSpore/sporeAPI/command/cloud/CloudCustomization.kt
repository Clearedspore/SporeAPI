package me.clearedSpore.sporeAPI.command.cloud

import net.kyori.adventure.audience.Audience
import org.incendo.cloud.caption.Caption

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class CloudCustomization {

    val theme = CloudTheme()
    val messages = CloudMessages()
    val captions = CloudCaptions()
    val help = CloudHelp()

    val placeholders = linkedMapOf<String, String>()

    var overrideExistingCommands: Boolean? = null

    internal val handlers = mutableListOf<CloudHandler>()

    fun theme(block: CloudTheme.() -> Unit) = theme.block()
    fun messages(block: CloudMessages.() -> Unit) = messages.block()
    fun captions(block: CloudCaptions.() -> Unit) = captions.block()
    fun help(block: CloudHelp.() -> Unit) = help.block()
    fun placeholders(block: MutableMap<String, String>.() -> Unit) = placeholders.block()

    inline fun <reified T : Throwable> handle(noinline handler: (sender: Audience, exception: T) -> Unit) {
        addHandler(T::class.java, handler)
    }

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Throwable> addHandler(type: Class<T>, handler: (Audience, T) -> Unit) {
        handlers += CloudHandler(type as Class<Throwable>, handler as (Audience, Throwable) -> Unit)
    }
}

internal class CloudHandler(val type: Class<Throwable>, val handler: (Audience, Throwable) -> Unit)

class CloudTheme {
    var prefix: String = ""
    var primary: String = "#4DA3FF"
    var accent: String = "#2B6EDB"
    var error: String = "#F50000"
    var muted: String = "#AAAAAA"
}

class CloudMessages {
    var noPermission: String? = null

    var invalidSyntax: String? = null

    var unknownCommand: String? = null

    var argumentError: String? = null

    var playerOnly: String = "<error>Only players can run this command."

    var internalError: String = "<error>An internal error occurred while running that command."
}

class CloudCaptions {
    internal val entries = linkedMapOf<Caption, String>()

    fun set(key: Caption, message: String) {
        entries[key] = message
    }
}

class CloudHelp {
    var enabled: Boolean = false

    var root: String? = null
    var label: String = "help"
    var permission: String? = null
    var perPage: Int = 8

    var showOnInvalidSyntax: Boolean = false

    internal val messages = linkedMapOf<String, String>()

    fun message(key: String, value: String) {
        messages[key] = value
    }
}
