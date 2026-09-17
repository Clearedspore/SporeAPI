package me.clearedSpore.sporeAPI.task

import me.clearedSpore.sporeAPI.util.FoliaUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object Tasks {

    private const val MILLIS_PER_TICK = 50L

    lateinit var instance: JavaPlugin
        private set

    fun onInitialize(plugin: JavaPlugin) {
        instance = plugin
    }


    fun run(runnable: Runnable): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            Bukkit.getGlobalRegionScheduler().run(instance) { runnable.run() }.toSpore()
        else
            Bukkit.getScheduler().runTask(instance, runnable).toSpore()

    fun runLater(delayTicks: Long, runnable: Runnable): SporeScheduledTask =
        if (FoliaUtil.isFolia) {
            if (delayTicks <= 0)
                Bukkit.getGlobalRegionScheduler().run(instance) { runnable.run() }.toSpore()
            else
                Bukkit.getGlobalRegionScheduler().runDelayed(instance, { runnable.run() }, delayTicks).toSpore()
        } else
            Bukkit.getScheduler().runTaskLater(instance, runnable, delayTicks).toSpore()

    fun runTimer(delayTicks: Long, periodTicks: Long, runnable: Runnable): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                instance, { runnable.run() }, delayTicks.coerceAtLeast(1L), periodTicks.coerceAtLeast(1L)
            ).toSpore()
        else
            Bukkit.getScheduler().runTaskTimer(instance, runnable, delayTicks, periodTicks).toSpore()


    fun runAsync(runnable: Runnable): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            Bukkit.getAsyncScheduler().runNow(instance) { runnable.run() }.toSpore()
        else
            Bukkit.getScheduler().runTaskAsynchronously(instance, runnable).toSpore()

    fun runLaterAsync(delayTicks: Long, runnable: Runnable): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            Bukkit.getAsyncScheduler().runDelayed(
                instance, { runnable.run() }, delayTicks.coerceAtLeast(1L) * MILLIS_PER_TICK, TimeUnit.MILLISECONDS
            ).toSpore()
        else
            Bukkit.getScheduler().runTaskLaterAsynchronously(instance, runnable, delayTicks).toSpore()

    fun runTimerAsync(delayTicks: Long, periodTicks: Long, runnable: Runnable): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            Bukkit.getAsyncScheduler().runAtFixedRate(
                instance,
                { runnable.run() },
                delayTicks.coerceAtLeast(1L) * MILLIS_PER_TICK,
                periodTicks.coerceAtLeast(1L) * MILLIS_PER_TICK,
                TimeUnit.MILLISECONDS
            ).toSpore()
        else
            Bukkit.getScheduler().runTaskTimerAsynchronously(instance, runnable, delayTicks, periodTicks).toSpore()

    fun runRepeated(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        runTimer(delayTicks, periodTicks, runnable)

    fun runRepeatedAsync(delayTicks: Long, periodTicks: Long, runnable: Runnable) =
        runTimerAsync(delayTicks, periodTicks, runnable)

    fun runEntity(entity: Entity, runnable: Runnable, retired: Runnable? = null): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            entity.scheduler.run(instance, { runnable.run() }, retired)?.toSpore() ?: SporeScheduledTask.NOOP
        else
            Bukkit.getScheduler().runTask(instance, runnable).toSpore()

    fun runEntityLater(entity: Entity, delayTicks: Long, runnable: Runnable, retired: Runnable? = null): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            entity.scheduler.runDelayed(instance, { runnable.run() }, retired, delayTicks.coerceAtLeast(1L))
                ?.toSpore() ?: SporeScheduledTask.NOOP
        else
            Bukkit.getScheduler().runTaskLater(instance, runnable, delayTicks).toSpore()

    fun runEntityTimer(
        entity: Entity,
        delayTicks: Long,
        periodTicks: Long,
        runnable: Runnable,
        retired: Runnable? = null
    ): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            entity.scheduler.runAtFixedRate(
                instance, { runnable.run() }, retired, delayTicks.coerceAtLeast(1L), periodTicks.coerceAtLeast(1L)
            )?.toSpore() ?: SporeScheduledTask.NOOP
        else
            Bukkit.getScheduler().runTaskTimer(instance, runnable, delayTicks, periodTicks).toSpore()

    fun runAtLocation(location: Location, runnable: Runnable): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            Bukkit.getRegionScheduler().run(instance, location) { runnable.run() }.toSpore()
        else
            Bukkit.getScheduler().runTask(instance, runnable).toSpore()

    fun runAtLocationLater(location: Location, delayTicks: Long, runnable: Runnable): SporeScheduledTask =
        if (FoliaUtil.isFolia) {
            if (delayTicks <= 0)
                Bukkit.getRegionScheduler().run(instance, location) { runnable.run() }.toSpore()
            else
                Bukkit.getRegionScheduler().runDelayed(instance, location, { runnable.run() }, delayTicks).toSpore()
        } else
            Bukkit.getScheduler().runTaskLater(instance, runnable, delayTicks).toSpore()

    fun runAtLocationTimer(location: Location, delayTicks: Long, periodTicks: Long, runnable: Runnable): SporeScheduledTask =
        if (FoliaUtil.isFolia)
            Bukkit.getRegionScheduler().runAtFixedRate(
                instance, location, { runnable.run() }, delayTicks.coerceAtLeast(1L), periodTicks.coerceAtLeast(1L)
            ).toSpore()
        else
            Bukkit.getScheduler().runTaskTimer(instance, runnable, delayTicks, periodTicks).toSpore()

    fun cancelAll() {
        if (!::instance.isInitialized) return

        if (FoliaUtil.isFolia) {
            Bukkit.getGlobalRegionScheduler().cancelTasks(instance)
            Bukkit.getAsyncScheduler().cancelTasks(instance)
        } else {
            Bukkit.getScheduler().cancelTasks(instance)
        }
    }

    private fun BukkitTask.toSpore(): SporeScheduledTask = SporeScheduledTask { cancel() }
    private fun ScheduledTask.toSpore(): SporeScheduledTask = SporeScheduledTask { cancel() }
}
