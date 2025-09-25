package me.clearedSpore.sporeAPI.util

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object Confirmation : Listener {

    private val pendingConfirmations = mutableSetOf<UUID>()

    fun addPlayer(playerId: UUID) = pendingConfirmations.add(playerId)
    fun removePlayer(playerId: UUID) = pendingConfirmations.remove(playerId)
    fun isPlayerPending(playerId: UUID) = pendingConfirmations.contains(playerId)

    @EventHandler
    fun onPlayerLeave(event: PlayerQuitEvent) {
        removePlayer(event.player.uniqueId)
    }
}
