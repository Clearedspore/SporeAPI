package me.clearedSpore.sporeAPI.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import me.clearedSpore.sporeAPI.debug.runDebugSuspending
import kotlin.time.Duration

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


fun launch(block: suspend CoroutineScope.() -> Unit): Job = SporeCoroutines.launch(block)

fun launchAsync(block: suspend CoroutineScope.() -> Unit): Job = SporeCoroutines.launchAsync(block)

fun repeat(
    every: Duration,
    delay: Duration = Duration.ZERO,
    async: Boolean = false,
    name: String = "repeat",
    block: suspend CoroutineScope.() -> Unit
): Job {
    require(every.isPositive()) { "repeat interval must be positive, got $every" }

    return start(async) {
        kotlinx.coroutines.delay(delay)

        while (isActive) {
            runDebugSuspending(name) { block() }
            kotlinx.coroutines.delay(every)
        }
    }
}

fun delay(
    delay: Duration,
    async: Boolean = false,
    name: String = "delay",
    block: suspend CoroutineScope.() -> Unit
): Job = start(async) {
    kotlinx.coroutines.delay(delay)
    runDebugSuspending(name) { block() }
}

private fun start(async: Boolean, block: suspend CoroutineScope.() -> Unit): Job =
    if (async) SporeCoroutines.launchAsync(block) else SporeCoroutines.launch(block)
