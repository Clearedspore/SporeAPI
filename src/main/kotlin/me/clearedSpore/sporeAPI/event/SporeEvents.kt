package me.clearedSpore.sporeAPI.event

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.DebugContext
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.debug.runDebugSuspending
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.Bukkit
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class EventSubscription internal constructor(private val listener: Listener) {

    @Volatile
    var isActive: Boolean = true
        private set

    fun unregister() {
        if (!isActive) return
        isActive = false
        HandlerList.unregisterAll(listener)
    }
}

object SporeEvents {

    fun <T : Event> register(
        type: Class<T>,
        priority: EventPriority,
        ignoreCancelled: Boolean,
        handler: (T) -> Unit
    ): EventSubscription {
        val listener = object : Listener {}
        val operation = "event.${type.simpleName}"

        val executor = EventExecutor { _, event ->
            if (!type.isInstance(event)) return@EventExecutor

            try {
                DebugContext.inside(operation) { handler(type.cast(event)) }
            } catch (exception: Exception) {
                IncidentReporter.report(
                    operation = operation,
                    error = exception,
                    severity = Severity.ERROR
                )
            }
        }

        Bukkit.getPluginManager().registerEvent(
            type,
            listener,
            priority,
            executor,
            SporeApi.plugin,
            ignoreCancelled
        )

        return EventSubscription(listener)
    }

    fun <T : Event> registerSuspending(
        type: Class<T>,
        priority: EventPriority,
        ignoreCancelled: Boolean,
        handler: suspend (T) -> Unit
    ): EventSubscription {
        warnIfCancellable(type)

        return register(type, priority, ignoreCancelled) { event ->
            SporeCoroutines.launch { runDebugSuspending("event.${type.simpleName}") { handler(event) } }
        }
    }

    internal fun warnIfCancellable(type: Class<out Event>) {
        if (!Cancellable::class.java.isAssignableFrom(type)) return

        Logger.warn(
            "A suspending handler is attached to ${type.simpleName}, which is cancellable " +
                "It cannot cancel the event, because the event completes before the handler does"
        )
    }

    suspend fun <T : Event> await(
        type: Class<T>,
        timeoutTicks: Long,
        priority: EventPriority,
        predicate: (T) -> Boolean
    ): T? {
        suspend fun listen(): T = suspendCancellableCoroutine { continuation ->
            val holder = AtomicReference<EventSubscription?>(null)
            val fired = AtomicBoolean(false)

            val subscription = register(type, priority, false) { event ->
                if (!predicate(event)) return@register
                if (!fired.compareAndSet(false, true)) return@register

                holder.get()?.unregister()
                if (continuation.isActive) continuation.resume(event)
            }

            holder.set(subscription)

            if (fired.get()) subscription.unregister()

            continuation.invokeOnCancellation { subscription.unregister() }
        }

        return if (timeoutTicks <= 0) listen() else withTimeoutOrNull(timeoutTicks * 50L) { listen() }
    }
}

inline fun <reified T : Event> on(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    noinline handler: (T) -> Unit
) {
    SporeEvents.register(T::class.java, priority, ignoreCancelled, handler)
}

inline fun <reified T : Event> subscribe(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    noinline handler: (T) -> Unit
): EventSubscription = SporeEvents.register(T::class.java, priority, ignoreCancelled, handler)

inline fun <reified T : Event> onAsync(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    noinline handler: suspend (T) -> Unit
) {
    SporeEvents.registerSuspending(T::class.java, priority, ignoreCancelled, handler)
}

inline fun <reified T : Event> subscribeAsync(
    priority: EventPriority = EventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    noinline handler: suspend (T) -> Unit
): EventSubscription =
    SporeEvents.registerSuspending(T::class.java, priority, ignoreCancelled, handler)

suspend inline fun <reified T : Event> awaitEvent(
    timeoutTicks: Long = 0,
    priority: EventPriority = EventPriority.NORMAL,
    noinline predicate: (T) -> Boolean = { true }
): T? = SporeEvents.await(T::class.java, timeoutTicks, priority, predicate)
