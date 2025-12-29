package me.clearedSpore.sporeAPI.util

import io.papermc.paper.event.player.AsyncChatEvent
import me.clearedSpore.sporeAPI.util.CC.blue
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.UUID
import java.util.function.Consumer

class ChatInput(private val plugin: JavaPlugin) : Listener {

    private val awaitingInput = mutableMapOf<UUID, Consumer<String>>()
    private val plainSerializer = PlainTextComponentSerializer.plainText()

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

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val callback = awaitingInput[player.uniqueId] ?: return

        event.isCancelled = true
        val msg = plainSerializer.serialize(event.message())

        Task.run(Runnable {
            awaitingInput.remove(player.uniqueId)?.accept(msg)
        })


    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        awaitingInput.remove(event.player.uniqueId)
    }
}
