package me.clearedSpore.sporeAPI.util

import me.clearedSpore.sporeAPI.event.PlayerPreLogEvent
import me.clearedSpore.sporeAPI.util.CC.blue
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


fun Logger.log(playerSuffix: String, sender: CommandSender, permission: String, message: String, includeSender: Boolean = true) {
    Bukkit.getOnlinePlayers()
        .filter { it.hasPermission(permission) && (includeSender || it != sender) }
        .forEach { player ->
            val event = PlayerPreLogEvent(player, permission, message, includeSender)
            Bukkit.getPluginManager().callEvent(event)
            if (!event.isCancelled) {
                player.sendMessage("$prefix&f${playerSuffix}${sender.name}&r has ${event.message}".blue())
            }
        }
}
