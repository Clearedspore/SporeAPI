package me.clearedSpore.sporeAPI.scoreboard

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class Sidebar {

    abstract fun title(player: Player): Component

    abstract fun lines(player: Player): List<Component>

    open fun shouldShow(player: Player): Boolean = true

    open val updateIntervalTicks: Long = 20L

    internal open fun titleFor(player: Player, data: Any?): Component = title(player)

    internal open fun linesFor(player: Player, data: Any?): List<Component> = lines(player)
}
