package me.clearedSpore.sporeAPI

import me.clearedSpore.sporeAPI.command.SporeCommand
import org.bukkit.event.Listener

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class SporeModule {

    lateinit var plugin: SporePlugin
        private set

    internal fun attach(plugin: SporePlugin) {
        this.plugin = plugin
    }

    open fun getCommands(): List<SporeCommand> = emptyList()
    open fun getListeners(): List<Listener> = emptyList()

    open fun onLoad() {}
    open fun onEnable() {}
    open fun onDisable() {}
}