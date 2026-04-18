package me.clearedSpore.sporeAPI

import me.clearedSpore.sporeAPI.util.CC.blue
import me.clearedSpore.sporeAPI.util.CC.red
import me.clearedSpore.sporeAPI.util.Cooldown
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale
import java.util.UUID

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object Extension {

    private val CONSOLE_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun CommandSender.success(message: String) {
        val prefix = Logger.pluginName
        val finalMessage = if (this is Player) {
            "$prefix » ✔ | $message".blue()
        } else {
            "✔ | $message".blue()
        }

        sendMessage(finalMessage)
    }

    fun CommandSender.error(message: String) {
        val prefix = Logger.pluginName
        val finalMessage = if (this is Player) {
            "$prefix » ✖ | $message".red()
        } else {
            "✖ | $message".red()
        }

        sendMessage(finalMessage)
    }

    val CommandSender.uuid: UUID
        get() = if (this is Player) uniqueId else CONSOLE_UUID

    val CommandSender.uuidStr: String
        get() = uuid.toString()

    fun CommandSender.hasCooldown(id: String): Boolean {
        return Cooldown.isOnCooldown(id, this.uuid)
    }

    fun CommandSender.applyCooldown(id: String, seconds: Long) {
        Cooldown.createCooldown(id, seconds)
        Cooldown.addCooldown(id, this.uuid)
    }

    fun CommandSender.cooldownRemaining(id: String): Long {
        return Cooldown.getTimeLeft(id, this.uuid)
    }

    fun CommandSender.clearCooldown(id: String) {
        Cooldown.removeCooldown(id, this.uuid)
    }

    fun String.niceName(): String {
        val string = this
        val words = string.replace("_", " ").lowercase(Locale.getDefault()).split(" ")
        return words.joinToString(" ") { it.replaceFirstChar { c -> c.titlecaseChar() } }
    }

}