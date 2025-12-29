package me.clearedSpore.sporeAPI.util

import io.papermc.paper.event.player.AsyncChatEvent
import me.clearedSpore.sporeAPI.util.CC.blue
import me.clearedSpore.sporeAPI.util.CC.red
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.function.Consumer

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class ChatInput(private val plugin: JavaPlugin) : Listener {

    private val awaitingInput = mutableMapOf<UUID, Consumer<String>>()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun awaitChatInput(player: Player, callback: Consumer<String>) {
        player.closeInventory()
        awaitingInput[player.uniqueId] = callback
        player.sendMessage("Please type your message in chat. Type 'cancel' to cancel.".blue())
    }

    fun cancelAwaitingInput(player: Player) {
        awaitingInput.remove(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val id = player.uniqueId
        if (awaitingInput.containsKey(id)) {
            event.isCancelled = true
            val message = event.message
            if (message.equals("cancel", ignoreCase = true)) {
                player.sendMessage("Input cancelled.".red())
                awaitingInput.remove(id)
                return
            }
            plugin.server.scheduler.runTask(plugin, Runnable {
                awaitingInput.remove(id)?.accept(message)
            })
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        awaitingInput.remove(event.player.uniqueId)
    }
}
