package me.clearedSpore.sporeAPI.task

import org.bukkit.scheduler.BukkitTask
// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class SporeTask {

    private var task: BukkitTask? = null

    abstract fun run()

    fun start() {
        if (task != null) return
        task = Tasks.run { run() }
    }

    fun startAsync() {
        if (task != null) return
        task = Tasks.runAsync { run() }
    }

    fun startLater(delayTicks: Long) {
        if (task != null) return
        task = Tasks.runLater(delayTicks) { run() }
    }

    fun startTimer(delayTicks: Long, periodTicks: Long) {
        if (task != null) return
        task = Tasks.runTimer(delayTicks, periodTicks) { run() }
    }

    fun startTimerAsync(delayTicks: Long, periodTicks: Long) {
        if (task != null) return
        task = Tasks.runTimerAsync(delayTicks, periodTicks) { run() }
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    fun isRunning(): Boolean = task != null
}