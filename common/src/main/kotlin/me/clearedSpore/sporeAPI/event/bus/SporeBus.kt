package me.clearedSpore.sporeAPI.event.bus

import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.DebugContext
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.debug.runDebugSuspending
import me.clearedSpore.sporeAPI.util.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


interface Cancellable {
    var isCancelled: Boolean
}

enum class BusPriority {
    LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR
}

class BusSubscription internal constructor(private val onUnregister: () -> Unit) {

    @Volatile
    var isActive: Boolean = true
        private set

    fun unregister() {
        if (!isActive) return
        isActive = false
        onUnregister()
    }
}

object SporeBus {

    private class Handler(
        val type: Class<*>,
        val priority: BusPriority,
        val ignoreCancelled: Boolean,
        val invoke: (Any) -> Unit
    )

    private val handlers = CopyOnWriteArrayList<Handler>()

    private val resolved = ConcurrentHashMap<Class<*>, List<Handler>>()

    fun <T : Any> register(
        type: Class<T>,
        priority: BusPriority = BusPriority.NORMAL,
        ignoreCancelled: Boolean = false,
        handler: (T) -> Unit
    ): BusSubscription {
        val entry = Handler(type, priority, ignoreCancelled) { event -> handler(type.cast(event)) }

        handlers += entry
        resolved.clear()

        return BusSubscription {
            handlers -= entry
            resolved.clear()
        }
    }

    fun <T : Any> registerSuspending(
        type: Class<T>,
        priority: BusPriority = BusPriority.NORMAL,
        ignoreCancelled: Boolean = false,
        handler: suspend (T) -> Unit
    ): BusSubscription {
        if (Cancellable::class.java.isAssignableFrom(type)) {
            Logger.warn(
                "A suspending bus handler is attached to ${type.simpleName}, which is cancellable. " +
                    "It cannot cancel the event, because post() returns before the handler finishes"
            )
        }

        return register(type, priority, ignoreCancelled) { event ->
            SporeCoroutines.launch { runDebugSuspending("bus.${type.simpleName}") { handler(event) } }
        }
    }

    fun <T : Any> post(event: T): T {
        val operation = "bus.${event.javaClass.simpleName}"

        handlersFor(event.javaClass).forEach { handler ->
            if (handler.ignoreCancelled && (event as? Cancellable)?.isCancelled == true) return@forEach

            try {
                DebugContext.inside(operation) { handler.invoke(event) }
            } catch (exception: Exception) {
                IncidentReporter.report(operation, exception, details = mapOf("handler" to handler.type.simpleName))
            }
        }

        return event
    }

    fun hasHandlers(type: Class<*>): Boolean = handlersFor(type).isNotEmpty()

    fun clear() {
        handlers.clear()
        resolved.clear()
    }

    private fun handlersFor(type: Class<*>): List<Handler> =
        resolved.computeIfAbsent(type) { eventType ->
            handlers.filter { it.type.isAssignableFrom(eventType) }.sortedBy { it.priority.ordinal }
        }

    inline fun <reified T : Any> on(
        priority: BusPriority = BusPriority.NORMAL,
        ignoreCancelled: Boolean = false,
        noinline handler: (T) -> Unit
    ): BusSubscription = register(T::class.java, priority, ignoreCancelled, handler)

    inline fun <reified T : Any> onAsync(
        priority: BusPriority = BusPriority.NORMAL,
        ignoreCancelled: Boolean = false,
        noinline handler: suspend (T) -> Unit
    ): BusSubscription = registerSuspending(T::class.java, priority, ignoreCancelled, handler)
}
