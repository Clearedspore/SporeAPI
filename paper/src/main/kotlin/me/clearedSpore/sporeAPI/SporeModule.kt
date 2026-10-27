package me.clearedSpore.sporeAPI

import org.bukkit.event.Listener

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class SporeModule {

    lateinit var plugin: SporePlugin
        private set

    internal fun attach(plugin: SporePlugin) {
        this.plugin = plugin
    }

    // Any command type: Cloud commands, or ACF SporeCommands when using sporeapi-acf
    open fun getCommands(): List<Any> = emptyList()
    open fun getCloudCommands(): List<Any> = emptyList()
    open fun getListeners(): List<Listener> = emptyList()

    open fun onLoad() {}
    open fun onEnable() {}
    open fun onDisable() {}
}