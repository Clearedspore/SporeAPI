package me.clearedSpore.sporeAPI.bossbar

import me.clearedSpore.sporeAPI.bossbar.SporeBossBar.Companion.fromLegacy
import net.kyori.adventure.text.Component
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import java.util.*
// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class BossBarBuilder {

    private var text: Component = Component.empty()
    private var color: BarColor = BarColor.WHITE
    private var style: BarStyle = BarStyle.SOLID

    private var permission: String? = null
    private var condition: (() -> Boolean)? = null
    private var viewerCondition: ((Player) -> Boolean)? = null
    private var durationTicks: Long? = null
    private var onFinish: (() -> Unit)? = null

    fun text(text: Component) = apply { this.text = text }
    fun text(text: String) = apply { this.text = text.fromLegacy() }
    fun color(color: BarColor) = apply { this.color = color }
    fun style(style: BarStyle) = apply { this.style = style }

    fun permission(permission: String) = apply { this.permission = permission }
    fun condition(condition: () -> Boolean) = apply { this.condition = condition }
    fun viewerCondition(condition: (Player) -> Boolean) = apply { this.viewerCondition = condition }
    fun viewer(player: Player) = apply {
        val uuid = player.uniqueId
        this.viewerCondition = { it.uniqueId == uuid }
    }

    fun durationTicks(ticks: Long) = apply { this.durationTicks = ticks }
    fun onFinish(action: () -> Unit) = apply { this.onFinish = action }

    fun build(): SporeBossBar {
        return SporeBossBar(
            id = UUID.randomUUID(),
            text = text,
            color = color,
            style = style,
            condition = condition,
            permission = permission,
            durationTicks = durationTicks,
            onFinish = onFinish,
            viewerCondition = viewerCondition
        )
    }
}
