package me.clearedSpore.sporeAPI.debug

import kotlinx.coroutines.job
import me.clearedSpore.sporeAPI.Extension.success
import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.command.cloud.SporeCloudCommandManager
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.registry.RegistryIndex
import me.clearedSpore.sporeAPI.repository.SporeMongo
import me.clearedSpore.sporeAPI.scoreboard.SidebarManager
import me.clearedSpore.sporeAPI.util.CC.mm
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeDebugCommand {

    fun register(manager: SporeCloudCommandManager, label: String, permission: String) {
        val cloud = manager.manager

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .permission(permission)
                .handler { context -> report(context.sender().sender) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("io")
                .permission(permission)
                .handler { context -> toggleIoGuard(context.sender().sender) }
        )
    }

    private fun toggleIoGuard(sender: CommandSender) {
        val silenced = SporeDebug.mainThreadIoPolicy == MainThreadIoPolicy.WARN

        SporeDebug.mainThreadIoPolicy =
            if (silenced) MainThreadIoPolicy.IGNORE else MainThreadIoPolicy.WARN

        val state = if (silenced) "<s_red>silenced" else "<s_green>warning"
        sender.success("Main-thread IO guard is now $state<white>.")
    }

    private fun report(sender: CommandSender) {
        val plugin = SporeApi.plugin
        val scheduler = Bukkit.getScheduler()

        val pending = scheduler.pendingTasks.count { it.owner.name == plugin.name }
        val workers = scheduler.activeWorkers.count { it.owner.name == plugin.name }
        val coroutines = runCatching {
            SporeCoroutines.scope.coroutineContext.job.children.count()
        }.getOrDefault(0)

        val lines = mutableListOf<String>()

        lines += "<s_blue>${plugin.name} <white>debug"
        lines += ""
        lines += "<gray>Scheduler: <white>$pending <gray>pending, <white>$workers <gray>async worker(s)"
        lines += "<gray>Coroutines: <white>$coroutines <gray>active"
        lines += "<gray>Sidebars: <white>${SidebarManager.activeCount}"
        lines += "<gray>Mongo: " + if (SporeMongo.isInitialized) "<s_green>connected" else "<gray>not configured"

        val registries = RegistryIndex.all()
        if (registries.isNotEmpty()) {
            lines += ""
            lines += "<gray>Registries:"
            registries.sortedBy { it.name }.forEach {
                lines += "  <s_blue>${it.name} <white>${it.size}"
            }
        }

        lines += ""
        lines += when (SporeDebug.mainThreadIoPolicy) {
            MainThreadIoPolicy.IGNORE -> "<gray>IO guard: <s_red>silenced"
            MainThreadIoPolicy.WARN -> "<gray>IO guard: <s_green>warning"
            MainThreadIoPolicy.THROW -> "<gray>IO guard: <s_red>throwing"
        }

        val warnings = SporeDebug.warnings()
        if (warnings.isEmpty()) {
            lines += "<gray>No main-thread IO recorded."
        } else {
            lines += "<gray>Last <white>${warnings.size} <gray>main-thread call(s):"
            warnings.reversed().forEach { warning ->
                lines += "  <s_red>${warning.operation} <gray>${warning.time} <white>x${warning.count}"
                warning.trace.firstOrNull()?.let { lines += "    <gray>$it" }
            }
        }

        lines.forEach { sender.sendMessage(it.mm()) }
    }
}
