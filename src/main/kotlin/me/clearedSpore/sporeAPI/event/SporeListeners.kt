package me.clearedSpore.sporeAPI.event

import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeListeners {

    fun register(listener: Listener) {
        val methods = handlerMethods(listener.javaClass)

        if (methods.none { it.isSuspending() }) {
            Bukkit.getPluginManager().registerEvents(listener, SporeApi.plugin)
            return
        }

        methods.forEach { method -> registerMethod(listener, method) }
    }

    private fun handlerMethods(type: Class<*>): List<Method> {
        val methods = mutableListOf<Method>()
        var current: Class<*>? = type

        while (current != null && current != Any::class.java) {
            current.declaredMethods
                .filter { it.isAnnotationPresent(EventHandler::class.java) }
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
            method.invoke(listener, event)
        } catch (exception: InvocationTargetException) {
            report(method, listener, exception.cause ?: exception)
        } catch (exception: Exception) {
            report(method, listener, exception)
        }
    }

    private suspend fun invokeSuspending(method: Method, listener: Listener, event: Event) {
        try {
            suspendCoroutine<Any?> { continuation ->
                val result = try {
                    method.invoke(listener, event, continuation)
                } catch (exception: InvocationTargetException) {
                    continuation.resumeWithException(exception.cause ?: exception)
                    return@suspendCoroutine
                }

                if (result !== COROUTINE_SUSPENDED) continuation.resume(result)
            }
        } catch (exception: Exception) {
            report(method, listener, exception)
        }
    }

    private fun report(method: Method, listener: Listener, throwable: Throwable) {
        Logger.error("${listener.javaClass.simpleName}.${method.name} threw: ${throwable.message}")
        throwable.printStackTrace()
    }
}
