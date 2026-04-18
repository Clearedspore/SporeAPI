package me.clearedSpore.sporeAPI.task

import me.clearedSpore.sporeAPI.util.time.Duration
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class TaskBuilder(private val plugin: JavaPlugin) {

    private var async = false
    private var delay = Duration(0)
    private var period: Duration? = null

    fun async() = apply { async = true }

    fun sync() = apply { async = false }

    fun delay(duration: Duration) = apply { delay = duration }

    fun repeat(period: Duration) = apply { this.period = period }

    fun immediate() = apply { delay = Duration(0) }

    fun run(block: () -> Unit): BukkitTask {
        val scheduler = Bukkit.getScheduler()

        val delayTicks = delay.toMillis() / 50
        val periodTicks = period?.toMillis()?.div(50)

        return when {
            periodTicks != null && async ->
                scheduler.runTaskTimerAsynchronously(plugin, block, delayTicks, periodTicks)

            periodTicks != null ->
                scheduler.runTaskTimer(plugin, block, delayTicks, periodTicks)

            async ->
                scheduler.runTaskLaterAsynchronously(plugin, block, delayTicks)

            else ->
                scheduler.runTaskLater(plugin, block, delayTicks)
        }
    }
}