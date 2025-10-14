package me.clearedSpore.sporeAPI.event

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class PlayerPreLogEvent(
    val sender: CommandSender,
    val permission: String,
    var message: String,
    var includeSender: Boolean
) : Event(!Bukkit.isPrimaryThread()), Cancellable {

    private var cancelled = false
    override fun isCancelled() = cancelled
    override fun setCancelled(cancel: Boolean) { cancelled = cancel }

    override fun getHandlers() = handlerList

    companion object {
        @JvmStatic val handlerList = HandlerList()
    }
}
