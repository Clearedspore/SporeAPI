package me.clearedSpore.sporeAPI.task

import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap
// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeScheduler {

    private lateinit var plugin: JavaPlugin
    private var task: BukkitTask? = null

    private val tickables = ConcurrentHashMap.newKeySet<Tickable>()

    fun init(plugin: JavaPlugin) {
        this.plugin = plugin

        task = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { tick() },
            1L,
            1L
        )

        Logger.info("SporeScheduler initialized")
    }

    fun register(tickable: Tickable) {
        tickables += tickable
    }

    fun unregister(tickable: Tickable) {
        tickables -= tickable
    }

    private fun tick() {
        if (tickables.isEmpty()) return

        val iterator = tickables.iterator()

        while (iterator.hasNext()) {
            val t = iterator.next()

            if (t.isFinished()) {
                iterator.remove()
                continue
            }

            t.tick()
        }
    }

    fun shutdown() {
        task?.cancel()
        task = null
        tickables.clear()
    }
}