package me.clearedSpore.sporeAPI.task

import me.clearedSpore.sporeAPI.util.time.Duration
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class TaskBuilder(private val plugin: JavaPlugin) {

    private var async = false
    private var delay = Duration(0)
    private var period: Duration? = null
    private var entity: Entity? = null

    fun async() = apply { async = true }

    fun sync() = apply { async = false }

    fun delay(duration: Duration) = apply { delay = duration }

    fun repeat(period: Duration) = apply { this.period = period }

    fun immediate() = apply { delay = Duration(0) }

    fun forEntity(entity: Entity) = apply { this.entity = entity; async = false }

    fun run(block: () -> Unit): SporeScheduledTask {
        val delayTicks = delay.toMillis() / 50
        val periodTicks = period?.toMillis()?.div(50)
        val boundEntity = entity
        val runnable = Runnable(block)

        return when {
            boundEntity != null && periodTicks != null ->
                Tasks.runEntityTimer(boundEntity, delayTicks, periodTicks, runnable)

            boundEntity != null ->
                Tasks.runEntityLater(boundEntity, delayTicks, runnable)

            periodTicks != null && async ->
                Tasks.runTimerAsync(delayTicks, periodTicks, runnable)

            periodTicks != null ->
                Tasks.runTimer(delayTicks, periodTicks, runnable)

            async ->
                Tasks.runLaterAsync(delayTicks, runnable)

            else ->
                Tasks.runLater(delayTicks, runnable)
        }
    }
}
