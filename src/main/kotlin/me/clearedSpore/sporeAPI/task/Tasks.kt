package me.clearedSpore.sporeAPI.task

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object Tasks {

    lateinit var instance: JavaPlugin
        private set

    fun onInitialize(plugin: JavaPlugin) {
        instance = plugin
    }

    fun run(runnable: Runnable) =
        Bukkit.getScheduler().runTask(instance, runnable)

    fun runAsync(runnable: Runnable) =
        Bukkit.getScheduler().runTaskAsynchronously(instance, runnable)

    fun runLater(delayTicks: Long, runnable: Runnable) =
        Bukkit.getScheduler().runTaskLater(instance, runnable, delayTicks)

    fun runLaterAsync(delayTicks: Long, runnable: Runnable) =
        Bukkit.getScheduler().runTaskLaterAsynchronously(instance, runnable, delayTicks)

    fun runTimer(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        Bukkit.getScheduler().runTaskTimer(instance, runnable, delayTicks, periodTicks)

    fun runTimerAsync(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, runnable, delayTicks, periodTicks)

    fun runRepeated(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        runTimer(delayTicks, periodTicks, runnable)

    fun runRepeatedAsync(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        runTimerAsync(delayTicks, periodTicks, runnable)

    fun cancelAll() {
        if (!::instance.isInitialized) return
        Bukkit.getScheduler().cancelTasks(instance)
    }
}
