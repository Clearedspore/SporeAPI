package me.clearedSpore.sporeAPI.bossbar

import org.bukkit.Bukkit
import org.bukkit.boss.BossBar
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object BossBarManager {

    private val bars = mutableMapOf<UUID, SporeBossBar>()
    lateinit var plugin: JavaPlugin

    fun init(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    fun add(bar: SporeBossBar) {
        bars[bar.id] = bar
        bar.start()
        bar.show()
    }

    fun remove(id: UUID) {
        bars.remove(id)?.stop()
    }

    fun tick() {
        val iterator = bars.values.iterator()

        while (iterator.hasNext()) {
            val bar = iterator.next()

            if (bar.isFinished()) {
                bar.stop()
                iterator.remove()
                continue
            }

            bar.tick()
        }
    }
}