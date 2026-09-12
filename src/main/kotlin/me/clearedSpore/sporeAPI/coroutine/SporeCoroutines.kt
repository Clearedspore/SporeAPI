package me.clearedSpore.sporeAPI.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.Bukkit
import org.bukkit.plugin.IllegalPluginAccessException
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import kotlin.coroutines.CoroutineContext

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class BukkitMainDispatcher : CoroutineDispatcher() {

    @Volatile
    private var plugin: Plugin? = null

    internal fun bind(plugin: Plugin) {
        this.plugin = plugin
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = !Bukkit.isPrimaryThread()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val plugin = this.plugin

        if (plugin == null || !plugin.isEnabled) {
            runIfOnMainThread(block)
            return
        }

        try {
            Bukkit.getScheduler().runTask(plugin, block)
        } catch (rejected: IllegalPluginAccessException) {
            runIfOnMainThread(block)
        }
    }

    private fun runIfOnMainThread(block: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            block.run()
        } else {
            Logger.warn("Dropped a main-thread continuation: plugin is disabled")
        }
    }

    override fun toString(): String = "Bukkit.main(${plugin?.name ?: "unbound"})"
}

object SporeCoroutines {

    val main: BukkitMainDispatcher = BukkitMainDispatcher()

    val async: CoroutineDispatcher = Dispatchers.IO

    private val incidents = CoroutineExceptionHandler { _, throwable ->
        IncidentReporter.report("coroutine.uncaught", throwable)
    }

    val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + main + CoroutineName("SporeAPI") + incidents)

    fun init(plugin: JavaPlugin) {
        main.bind(plugin)
    }

    fun shutdown() {
        scope.cancel("Plugin is disabling")
    }


    fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        scope.launch(block = block)

    fun launchAsync(block: suspend CoroutineScope.() -> Unit): Job =
        scope.launch(async, block = block)
}

suspend fun <T> withAsyncCtx(block: suspend CoroutineScope.() -> T): T =
    withContext(SporeCoroutines.async, block)


suspend fun <T> withRunCtx(block: suspend CoroutineScope.() -> T): T =
    withContext(SporeCoroutines.main, block)

suspend fun delayTicks(ticks: Long) = delay(ticks * 50L)
