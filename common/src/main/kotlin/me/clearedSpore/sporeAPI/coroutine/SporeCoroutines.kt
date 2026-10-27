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
import kotlin.coroutines.CoroutineContext

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class MainDispatcher : CoroutineDispatcher() {

    @Volatile
    var delegate: CoroutineDispatcher = Dispatchers.Default
        internal set

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = delegate.isDispatchNeeded(context)

    override fun dispatch(context: CoroutineContext, block: Runnable) = delegate.dispatch(context, block)

    override fun toString(): String = "Spore.main($delegate)"
}

object SporeCoroutines {

    val main: MainDispatcher = MainDispatcher()

    val async: CoroutineDispatcher = Dispatchers.IO

    private val incidents = CoroutineExceptionHandler { _, throwable ->
        IncidentReporter.report("coroutine.uncaught", throwable)
    }

    val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + main + CoroutineName("SporeAPI") + incidents)

    fun useMainDispatcher(dispatcher: CoroutineDispatcher) {
        main.delegate = dispatcher
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
