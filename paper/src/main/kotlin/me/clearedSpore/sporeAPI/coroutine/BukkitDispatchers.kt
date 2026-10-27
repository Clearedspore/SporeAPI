package me.clearedSpore.sporeAPI.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import me.clearedSpore.sporeAPI.task.Tasks
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.IllegalPluginAccessException
import org.bukkit.plugin.Plugin
import kotlin.coroutines.CoroutineContext

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class BukkitMainDispatcher(private val plugin: Plugin) : CoroutineDispatcher() {

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = !Bukkit.isPrimaryThread()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (!plugin.isEnabled) {
            runIfOnMainThread(block)
            return
        }

        try {
            Tasks.run(block)
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

    override fun toString(): String = "Bukkit.main(${plugin.name})"
}


class EntityDispatcher(private val entity: Entity) : CoroutineDispatcher() {

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Tasks.runEntity(entity, block)
    }

    override fun toString(): String = "Bukkit.entity(${entity.uniqueId})"
}

suspend fun <T> withEntityCtx(entity: Entity, block: suspend CoroutineScope.() -> T): T =
    withContext(EntityDispatcher(entity), block)
