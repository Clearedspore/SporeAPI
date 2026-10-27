package me.clearedSpore.sporeAPI.tablist

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

abstract class Tablist {

    open fun header(viewer: Player): Component = Component.empty()

    open fun footer(viewer: Player): Component = Component.empty()

    open fun name(target: Player): Component? = null

    open fun order(target: Player): Int? = null

    open fun shouldShow(viewer: Player): Boolean = true

    open val updateIntervalTicks: Long = 20L

    internal open fun headerFor(viewer: Player, data: Any?): Component = header(viewer)
    internal open fun footerFor(viewer: Player, data: Any?): Component = footer(viewer)
}

abstract class AsyncTablist<T : Any> : Tablist() {

    abstract fun fetch(viewer: Player): T

    open fun header(viewer: Player, data: T): Component = Component.empty()

    open fun footer(viewer: Player, data: T): Component = Component.empty()

    open val fetchIntervalTicks: Long get() = updateIntervalTicks

    final override fun header(viewer: Player): Component = Component.empty()
    final override fun footer(viewer: Player): Component = Component.empty()

    internal fun fetchSnapshot(viewer: Player): Any = fetch(viewer)

    @Suppress("UNCHECKED_CAST")
    override fun headerFor(viewer: Player, data: Any?): Component = header(viewer, data as T)

    @Suppress("UNCHECKED_CAST")
    override fun footerFor(viewer: Player, data: Any?): Component = footer(viewer, data as T)
}
