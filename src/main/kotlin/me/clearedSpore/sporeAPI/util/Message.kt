package me.clearedSpore.sporeAPI.util

import me.clearedSpore.sporeAPI.util.CC.blue
import me.clearedSpore.sporeAPI.util.CC.red
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object Message {

    fun broadcastMessage(message: String) = Bukkit.getOnlinePlayers().forEach { it.sendMessage(message) }
    fun broadcastMessageWithPermission(message: String, permission: String) =
        Bukkit.getOnlinePlayers().filter { it.hasPermission(permission) }.forEach { it.sendMessage(message) }

    fun Player.sendBossBar(player: Player, title: String, progress: Float) =
        createBossBar(title, progress).addPlayer(player)

    fun broadcastBossBar(title: String, progress: Float) =
        createBossBar(title, progress).also { Bukkit.getOnlinePlayers().forEach(it::addPlayer) }

    fun broadcastBossBarWithPermission(title: String, progress: Float, permission: String) =
        createBossBar(title, progress).also {
            Bukkit.getOnlinePlayers().filter { p -> p.hasPermission(permission) }.forEach(it::addPlayer)
        }

    fun Player.sendMessageWithTitle(player: Player, title: String, subtitle: String) =
        player.sendTitle(title, subtitle, 10, 70, 20)

    fun Player.endTimedBossBar(
        plugin: JavaPlugin,
        player: Player,
        title: String,
        progress: Float,
        duration: Long
    ) {
        val bossBar = createBossBar(title, progress)
        bossBar.addPlayer(player)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            bossBar.removePlayer(player)
        }, duration)
    }

    fun Player.sendActionBar(player: Player, message: String) =
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent(message)
        )

    fun broadcastActionBar(message: String) = Bukkit.getOnlinePlayers().forEach { it.sendActionBar(it, message) }
    fun Player.sendSuccessMessage(sender: CommandSender, message: String) {
        sender.sendMessage("✔ | $message".blue())
        if (sender is Player) sender.playSound(sender.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
    }

    fun Player.sendErrorMessage(sender: CommandSender, message: String) {
        sender.sendMessage("✖ | $message".red())
        if (sender is Player) sender.playSound(sender.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
    }

    private fun createBossBar(title: String, progress: Float): BossBar =
        Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SOLID)
}
