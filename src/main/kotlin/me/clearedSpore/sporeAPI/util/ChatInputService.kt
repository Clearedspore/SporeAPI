package me.clearedSpore.sporeAPI.util

import me.clearedSpore.sporeAPI.util.Message.sendSuccessMessage
import org.bukkit.entity.Player
import java.util.UUID
import java.util.function.Consumer

object ChatInputService {

    private val awaitingInput = mutableMapOf<UUID, Consumer<String>>()

    fun begin(player: Player, callback: Consumer<String>) {
        player.closeInventory()
        awaitingInput[player.uniqueId] = callback
        player.sendSuccessMessage("Please type your message in chat. Type 'cancel' to cancel.")
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
