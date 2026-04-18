package me.clearedSpore.sporeAPI.bossbar

import me.clearedSpore.sporeAPI.task.SporeScheduler
import me.clearedSpore.sporeAPI.task.Tickable
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import java.util.*

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class SporeBossBar(
    val id: UUID = UUID.randomUUID(),
    var text: String,
    var color: BarColor,
    var style: BarStyle,
    private val condition: (() -> Boolean)? = null,
    private val permission: String? = null,
    private val durationTicks: Long? = null,
    private val onFinish: (() -> Unit)? = null
) : Tickable {

    private val bar = Bukkit.createBossBar(text, color, style)
    private var remaining = durationTicks ?: -1

    private val viewers = mutableSetOf<UUID>()
    private var active = false

    fun start() {
        if (active) return
        active = true
        SporeScheduler.register(this)
    }

    fun stop() {
        if (!active) return
        active = false
        hide()
        SporeScheduler.unregister(this)
    }

    fun addPlayer(player: Player) {
        viewers.add(player.uniqueId)
        bar.addPlayer(player)
    }

    fun removePlayer(player: Player) {
        viewers.remove(player.uniqueId)
        bar.removePlayer(player)
    }

    fun show() {
        Bukkit.getOnlinePlayers().forEach {
            if (shouldShow(it)) addPlayer(it)
        }
    }

    fun hide() {
        bar.removeAll()
        viewers.clear()
    }

    override fun tick() {
        Bukkit.getOnlinePlayers().forEach { player ->
            if (shouldShow(player)) {
                if (!viewers.contains(player.uniqueId)) addPlayer(player)
            } else {
                removePlayer(player)
            }
        }

        if (remaining > 0) {
            remaining--

            val max = durationTicks ?: 1
            bar.progress = remaining.toDouble() / max.toDouble()

            if (remaining <= 0) {
                onFinish?.invoke()
                stop()
            }
        }
    }

    override fun isFinished(): Boolean {
        return remaining == 0L
    }

    private fun shouldShow(player: Player): Boolean {
        if (permission != null && !player.hasPermission(permission)) return false
        if (condition != null && !condition.invoke()) return false
        return true
    }

    fun updateText(text: String) {
        this.text = text
        bar.setTitle(text)
    }

    fun updateProgress(progress: Double) {
        bar.progress = progress.coerceIn(0.0, 1.0)
    }
}