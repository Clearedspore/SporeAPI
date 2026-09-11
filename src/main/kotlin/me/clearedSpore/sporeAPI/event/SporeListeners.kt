package me.clearedSpore.sporeAPI.event

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.DebugContext
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeListeners {

    // Every handler goes through registerMethod instead of Bukkit's registerEvents, so one that
    // throws becomes an incident rather than Bukkit's "Could not pass event" dump.
    fun register(listener: Listener) {
        handlerMethods(listener.javaClass).forEach { method -> registerMethod(listener, method) }
    }

    private fun handlerMethods(type: Class<*>): List<Method> {
        val methods = mutableListOf<Method>()
        val seen = mutableSetOf<String>()
        var current: Class<*>? = type

        while (current != null && current != Any::class.java) {
            current.declaredMethods
                .filter { it.isAnnotationPresent(EventHandler::class.java) && !it.isBridge && !it.isSynthetic }
                .filter { seen.add(it.name + it.parameterTypes.contentToString()) }
                .forEach { methods += it }

            current = current.superclass
        }

        return methods
    }

    private fun Method.isSuspending(): Boolean =
        parameterCount == 2 && parameterTypes[1] == Continuation::class.java

    private fun registerMethod(listener: Listener, method: Method) {
        val annotation = method.getAnnotation(EventHandler::class.java) ?: return
        val suspending = method.isSuspending()

        val expectedParameters = if (suspending) 2 else 1
        if (method.parameterCount != expectedParameters || !Event::class.java.isAssignableFrom(method.parameterTypes[0])) {
            Logger.warn(
                "${listener.javaClass.simpleName}.${method.name} is annotated with @EventHandler " +
                    "but does not take a single event parameter"
            )
            return
        }

        @Suppress("UNCHECKED_CAST")
        val eventType = method.parameterTypes[0] as Class<out Event>
        method.isAccessible = true

        if (suspending) SporeEvents.warnIfCancellable(eventType)

        val executor = EventExecutor { _, event ->
            if (!eventType.isInstance(event)) return@EventExecutor

            if (suspending) {
                SporeCoroutines.launch { invokeSuspending(method, listener, event) }
            } else {
                invokeDirect(method, listener, event)
            }
        }

        Bukkit.getPluginManager().registerEvent(
            eventType,
            listener,
            annotation.priority,
            executor,
            SporeApi.plugin,
            annotation.ignoreCancelled
        )
    }

    private fun invokeDirect(method: Method, listener: Listener, event: Event) {
        try {
            DebugContext.inside(operation(method, listener)) { method.invoke(listener, event) }
        } catch (exception: InvocationTargetException) {
            report(method, listener, exception.cause ?: exception)
        } catch (exception: Exception) {
            report(method, listener, exception)
        }
    }

    private suspend fun invokeSuspending(method: Method, listener: Listener, event: Event) {
        try {
            DebugContext.insideSuspending(operation(method, listener)) {
                suspendCoroutine<Any?> { continuation ->
                    val result = try {
                        method.invoke(listener, event, continuation)
                    } catch (exception: InvocationTargetException) {
                        continuation.resumeWithException(exception.cause ?: exception)
                        return@suspendCoroutine
                    }

                    if (result !== COROUTINE_SUSPENDED) continuation.resume(result)
                }
            }
        } catch (cancelled: CancellationException) {
            if (!currentCoroutineContext().isActive) throw cancelled
            report(method, listener, cancelled)
        } catch (exception: Exception) {
            report(method, listener, exception)
        }
    }

    private fun operation(method: Method, listener: Listener): String =
        "listener.${listener.javaClass.simpleName}.${method.name}"

    private fun report(method: Method, listener: Listener, throwable: Throwable) {
        IncidentReporter.report(
            operation = operation(method, listener),
            error = throwable,
            severity = Severity.ERROR,
            details = mapOf("event" to (method.parameterTypes.firstOrNull()?.simpleName ?: "unknown"))
        )
    }
}
