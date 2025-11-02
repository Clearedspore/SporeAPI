package me.clearedSpore.sporeAPI.util

import me.clearedSpore.sporeAPI.util.CC.blue
import me.clearedSpore.sporeAPI.util.CC.red
import me.clearedSpore.sporeAPI.util.CC.translate
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

    private var usePrefix: Boolean = false
    private var prefix = Logger.pluginName

    fun init(prefix: Boolean){
        usePrefix = prefix
    }

    fun broadcastMessage(message: String) = Bukkit.broadcastMessage(message.translate())
    fun broadcastMessageWithPermission(message: String, permission: String) =
        Bukkit.getOnlinePlayers().filter { it.hasPermission(permission) }.forEach { it.sendMessage(message) }

    fun Player.sendBossBar(title: String, progress: Float) =
        createBossBar(title, progress).addPlayer(this.player!!)

    fun broadcastBossBar(title: String, progress: Float) =
        createBossBar(title, progress).also { Bukkit.getOnlinePlayers().forEach(it::addPlayer) }

    fun broadcastBossBarWithPermission(title: String, progress: Float, permission: String) =
        createBossBar(title, progress).also {
            Bukkit.getOnlinePlayers().filter { p -> p.hasPermission(permission) }.forEach(it::addPlayer)
        }


    fun Player.endTimedBossBar(
        plugin: JavaPlugin,
        title: String,
        progress: Float,
        duration: Long
    ) {
        val bossBar = createBossBar(title, progress)
        bossBar.addPlayer(this.player!!)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            bossBar.removePlayer(this.player!!)
        }, duration)
    }


    fun broadcastActionBar(message: String) = Bukkit.getOnlinePlayers().forEach { it.sendActionBar(message) }

    fun Player.sendSuccessMessage(message: String) {
        if(usePrefix) {
            this.sendMessage("$prefix » ✔ | $message".blue())
        } else {
            this.sendMessage("✔ | $message".blue())
        }
        this.playSound(this.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
    }

    fun Player.sendErrorMessage(message: String) {
        if(usePrefix) {
            this.sendMessage("$prefix » ✖ | $message".red())
        } else {
            this.sendMessage("✖ | $message".red())
        }
        this.playSound(this.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
    }

    private fun createBossBar(title: String, progress: Float): BossBar =
        Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SOLID)
}
