package me.clearedSpore.sporeAPI

import org.bukkit.plugin.java.JavaPlugin

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeApi {

    @Volatile
    private var pluginRef: JavaPlugin? = null

    val plugin: JavaPlugin
        get() = pluginRef ?: error("SporeApi is not initialised yet - is your plugin extending SporePlugin?")

    val isInitialized: Boolean get() = pluginRef != null

    fun init(plugin: JavaPlugin) {
        pluginRef = plugin
    }
}
