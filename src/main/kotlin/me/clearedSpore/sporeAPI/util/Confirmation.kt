package me.clearedSpore.sporeAPI.util

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
        player.sendMessage("         &bWARNING!!         ".red())
        player.sendMessage("Are you sure you want to do this?".red())
        player.sendMessage("This action cannot be undone".red())
        player.sendMessage("Run the command again to confirm!".red())
    }

    fun removePlayer(playerId: UUID) = pendingConfirmations.remove(playerId)
    fun isPlayerPending(playerId: UUID) = pendingConfirmations.contains(playerId)

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        removePlayer(event.player.uniqueId)
    }
}
