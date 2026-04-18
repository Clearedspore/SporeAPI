package me.clearedSpore.sporeAPI.task

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object Tasks {

    lateinit var instance: JavaPlugin
        private set

    private val tasks = mutableSetOf<BukkitTask>()

    fun onInitialize(plugin: JavaPlugin) {
        instance = plugin
    }

    private fun track(task: BukkitTask): BukkitTask {
        tasks += task
        return task
    }

    fun run(runnable: Runnable) =
        track(Bukkit.getScheduler().runTask(instance, runnable))

    fun runAsync(runnable: Runnable) =
        track(Bukkit.getScheduler().runTaskAsynchronously(instance, runnable))

    fun runLater(delayTicks: Long, runnable: Runnable) =
        track(Bukkit.getScheduler().runTaskLater(instance, runnable, delayTicks))

    fun runLaterAsync(delayTicks: Long, runnable: Runnable) =
        track(Bukkit.getScheduler().runTaskLaterAsynchronously(instance, runnable, delayTicks))

    fun runTimer(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        track(Bukkit.getScheduler().runTaskTimer(instance, runnable, delayTicks, periodTicks))

    fun runTimerAsync(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        track(Bukkit.getScheduler().runTaskTimerAsynchronously(instance, runnable, delayTicks, periodTicks))

    fun runRepeated(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        runTimer(delayTicks, periodTicks, runnable)

    fun runRepeatedAsync(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        runTimerAsync(delayTicks, periodTicks, runnable)

    fun cancelAll() {
        tasks.forEach { it.cancel() }
        tasks.clear()
    }
}