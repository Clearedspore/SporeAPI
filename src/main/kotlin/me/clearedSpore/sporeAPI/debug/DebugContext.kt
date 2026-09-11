package me.clearedSpore.sporeAPI.debug

import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object DebugContext {

    private const val MAX_DEPTH = 16

    private val trail: ThreadLocal<List<String>> = ThreadLocal.withInitial { emptyList() }

    fun current(): List<String> = trail.get()

    fun <T> inside(operation: String, block: () -> T): T {
        val previous = trail.get()
        trail.set(extend(previous, operation))

        try {
            return block()
        } finally {
            trail.set(previous)
        }
    }

    suspend fun <T> insideSuspending(operation: String, block: suspend () -> T): T =
        withContext(trail.asContextElement(extend(current(), operation))) { block() }

    private fun extend(trail: List<String>, operation: String): List<String> =
        if (trail.lastOrNull() == operation) trail else (trail + operation).takeLast(MAX_DEPTH)
}
