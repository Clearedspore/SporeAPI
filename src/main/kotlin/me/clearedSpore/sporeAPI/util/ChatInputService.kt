package me.clearedSpore.sporeAPI.util

import me.clearedSpore.sporeAPI.util.Message.sendSuccessMessage
import org.bukkit.entity.Player
import java.util.UUID
import java.util.function.Consumer

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object ChatInputService {

    private val awaitingInput = mutableMapOf<UUID, Consumer<String>>()

    fun begin(player: Player, silent: Boolean = false, callback: Consumer<String>) {
        player.closeInventory()
        awaitingInput[player.uniqueId] = callback
        if (!silent) {
            player.sendSuccessMessage("Please type your message in chat. Type 'cancel' to cancel.")
        }
    }

    fun cancel(player: Player) {
        awaitingInput.remove(player.uniqueId)
    }

    fun has(player: Player): Boolean {
        return awaitingInput.containsKey(player.uniqueId)
    }

    fun consume(player: Player, message: String): Boolean {
        val callback = awaitingInput.remove(player.uniqueId) ?: return false
        callback.accept(message)
        return true
    }

    fun clear() {
        awaitingInput.clear()
    }
}
