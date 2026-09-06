package me.clearedSpore.sporeAPI.scoreboard

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class AsyncSidebar<T : Any> : Sidebar() {

    abstract fun fetch(player: Player): T

    abstract fun title(player: Player, data: T): Component

    abstract fun lines(player: Player, data: T): List<Component>

    open val fetchIntervalTicks: Long get() = updateIntervalTicks

    final override fun title(player: Player): Component = Component.empty()

    final override fun lines(player: Player): List<Component> = emptyList()

    internal fun fetchSnapshot(player: Player): Any = fetch(player)

    @Suppress("UNCHECKED_CAST")
    override fun titleFor(player: Player, data: Any?): Component = title(player, data as T)

    @Suppress("UNCHECKED_CAST")
    override fun linesFor(player: Player, data: Any?): List<Component> = lines(player, data as T)
}
