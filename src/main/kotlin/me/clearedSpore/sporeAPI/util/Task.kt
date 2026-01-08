package me.clearedSpore.sporeAPI.util

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.*

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object Task {

    lateinit var instance: JavaPlugin
    
    fun onInitialize(plugin: JavaPlugin){
        instance = plugin
    }

    fun run(runnable: Runnable): BukkitTask =
        Bukkit.getScheduler().runTask(instance, runnable)

    fun runSync(runnable: Runnable): BukkitTask =
        Bukkit.getScheduler().runTask(instance, runnable)

    fun runAsync(runnable: Runnable): BukkitTask =
        Bukkit.getScheduler().runTaskAsynchronously(instance, runnable)

    fun runLater(runnable: Runnable, delay: Long, unit: TimeUnit = TimeUnit.SECONDS): BukkitTask =
        Bukkit.getScheduler().runTaskLater(
            instance,
            runnable,
            unit.toSeconds(delay) * 20L
        )

    fun runLaterAsync(runnable: Runnable, delay: Long, unit: TimeUnit = TimeUnit.SECONDS): BukkitTask =
        Bukkit.getScheduler().runTaskLaterAsynchronously(
            instance,
            runnable,
            unit.toSeconds(delay) * 20L
        )

    fun runRepeated(runnable: Runnable, delay: Long, interval: Long, unit: TimeUnit = TimeUnit.SECONDS): BukkitTask =
        Bukkit.getScheduler().runTaskTimer(
            instance,
            runnable,
            unit.toSeconds(delay) * 20L,
            unit.toSeconds(interval) * 20L
        )

    fun runRepeatedAsync(
        runnable: Runnable,
        delay: Long,
        interval: Long,
        unit: TimeUnit = TimeUnit.SECONDS
    ): BukkitTask =
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            instance,
            runnable,
            unit.toSeconds(delay) * 20L,
            unit.toSeconds(interval) * 20L
        )

    fun cancel(task: BukkitTask) {
        task.cancel()
    }

}
