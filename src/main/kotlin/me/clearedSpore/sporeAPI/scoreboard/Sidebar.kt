package me.clearedSpore.sporeAPI.scoreboard

import org.bukkit.entity.Player

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.



abstract class Sidebar {

    abstract fun title(player: Player): String

    abstract fun lines(player: Player): List<String>

    open fun shouldShow(player: Player): Boolean = true

    open val updateIntervalTicks: Long = 20L
}
