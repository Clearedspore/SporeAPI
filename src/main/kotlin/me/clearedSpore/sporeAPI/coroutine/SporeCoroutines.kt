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
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import kotlin.coroutines.CoroutineContext

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class BukkitMainDispatcher(private val plugin: Plugin) : CoroutineDispatcher() {

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = !Bukkit.isPrimaryThread()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (!plugin.isEnabled) {
            block.run()
            return
        }

        Bukkit.getScheduler().runTask(plugin, block)
    }

    override fun toString(): String = "Bukkit.main"
}

object SporeCoroutines {

    lateinit var main: CoroutineDispatcher
        private set

    val async: CoroutineDispatcher = Dispatchers.IO

    lateinit var scope: CoroutineScope
        private set

    // Without this, a launched coroutine that throws only reaches the thread's default handler.
    private val incidents = CoroutineExceptionHandler { _, throwable ->
        IncidentReporter.report("coroutine.uncaught", throwable)
    }

    fun init(plugin: JavaPlugin) {
        main = BukkitMainDispatcher(plugin)
        scope = CoroutineScope(SupervisorJob() + main + CoroutineName(plugin.name) + incidents)
    }

    fun shutdown() {
        if (!::scope.isInitialized) return
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
