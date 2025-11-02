package me.clearedSpore.sporeAPI.util

import me.clearedSpore.sporeAPI.util.CC.blue
import me.clearedSpore.sporeAPI.util.CC.red
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object Confirmation : Listener {

    private val pendingConfirmations = mutableSetOf<UUID>()

    fun addPlayer(playerId: UUID){
        val player = Bukkit.getPlayer(playerId)
        pendingConfirmations.add(playerId)
        player!!.sendMessage("".red())
        player.sendMessage("         &lWARNING!!         ".red())
        player.sendMessage("Are you sure you want to do this?".blue())
        player.sendMessage("This action cannot be undone".blue())
        player.sendMessage("Run the command again to confirm!".blue())
    }

    fun removePlayer(playerId: UUID) = pendingConfirmations.remove(playerId)
    fun isPlayerPending(playerId: UUID) = pendingConfirmations.contains(playerId)

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        removePlayer(event.player.uniqueId)
    }
}
