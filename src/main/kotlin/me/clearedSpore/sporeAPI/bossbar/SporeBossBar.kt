package me.clearedSpore.sporeAPI.bossbar

import me.clearedSpore.sporeAPI.task.SporeScheduler
import me.clearedSpore.sporeAPI.task.Tickable
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import java.util.*

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class SporeBossBar(
    val id: UUID = UUID.randomUUID(),
    text: Component,
    color: BarColor,
    style: BarStyle,
    private val condition: (() -> Boolean)? = null,
    private val permission: String? = null,
    private val durationTicks: Long? = null,
    private val onFinish: (() -> Unit)? = null,
    private val viewerCondition: ((Player) -> Boolean)? = null
) : Tickable {

    private val bar = BossBar.bossBar(text, 1f, color.toAdventure(), style.toAdventure())
    private var remaining = durationTicks ?: -1

    private val viewers = mutableSetOf<UUID>()
    private var active = false

    var text: Component = text
        set(value) {
            field = value
            bar.name(value)
        }

    var color: BarColor = color
        set(value) {
            field = value
            bar.color(value.toAdventure())
        }

    var style: BarStyle = style
        set(value) {
            field = value
            bar.overlay(value.toAdventure())
        }

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
        player.showBossBar(bar)
    }

    fun removePlayer(player: Player) {
        viewers.remove(player.uniqueId)
        player.hideBossBar(bar)
    }

    fun show() {
        Bukkit.getOnlinePlayers().forEach {
            if (shouldShow(it)) addPlayer(it)
        }
    }

    fun hide() {
        viewers.forEach { Bukkit.getPlayer(it)?.hideBossBar(bar) }
        viewers.clear()
    }

    override fun tick() {
        viewers.removeIf { Bukkit.getPlayer(it) == null }

        Bukkit.getOnlinePlayers().forEach { player ->
            if (shouldShow(player)) {
                if (!viewers.contains(player.uniqueId)) addPlayer(player)
            } else if (viewers.contains(player.uniqueId)) {
                removePlayer(player)
            }
        }

        if (remaining > 0) {
            remaining--

            val max = durationTicks ?: 1
            updateProgress(remaining.toDouble() / max.toDouble())

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
        if (viewerCondition != null && !viewerCondition.invoke(player)) return false
        return true
    }

    fun updateText(text: Component) {
        this.text = text
    }

    fun updateText(text: String) {
        this.text = text.fromLegacy()
    }

    fun updateColor(color: BarColor) {
        this.color = color
    }

    fun updateProgress(progress: Double) {
        bar.progress(progress.coerceIn(0.0, 1.0).toFloat())
    }

    companion object {
        private val legacySerializer = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build()

        internal fun String.fromLegacy(): Component = legacySerializer.deserialize(this)

        private fun BarColor.toAdventure(): BossBar.Color = when (this) {
            BarColor.PINK -> BossBar.Color.PINK
            BarColor.BLUE -> BossBar.Color.BLUE
            BarColor.RED -> BossBar.Color.RED
            BarColor.GREEN -> BossBar.Color.GREEN
            BarColor.YELLOW -> BossBar.Color.YELLOW
            BarColor.PURPLE -> BossBar.Color.PURPLE
            BarColor.WHITE -> BossBar.Color.WHITE
        }

        private fun BarStyle.toAdventure(): BossBar.Overlay = when (this) {
            BarStyle.SOLID -> BossBar.Overlay.PROGRESS
            BarStyle.SEGMENTED_6 -> BossBar.Overlay.NOTCHED_6
            BarStyle.SEGMENTED_10 -> BossBar.Overlay.NOTCHED_10
            BarStyle.SEGMENTED_12 -> BossBar.Overlay.NOTCHED_12
            BarStyle.SEGMENTED_20 -> BossBar.Overlay.NOTCHED_20
        }
    }
}
