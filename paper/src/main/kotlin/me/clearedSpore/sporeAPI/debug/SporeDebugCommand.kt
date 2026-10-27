package me.clearedSpore.sporeAPI.debug

import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.command.cloud.SporeCloudCommandManager
import me.clearedSpore.sporeAPI.scoreboard.SidebarManager
import me.clearedSpore.sporeAPI.tablist.TablistManager
import me.clearedSpore.sporeAPI.util.FoliaUtil
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.SuggestionProvider

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeDebugCommand {

    fun register(
        manager: SporeCloudCommandManager,
        label: String,
        permission: String,
        notifyStaff: Boolean = false
    ) {
        val cloud = manager.manager
        val root = "/$label debug"

        IncidentReporter.detailCommand = "$root error"
        if (notifyStaff) IncidentReporter.notifyPermission = permission

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .permission(permission)
                .handler { context -> context.sender().sender.send(DebugViews.report(root, paperLines())) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("io")
                .permission(permission)
                .handler { context -> context.sender().sender.sendMessage(DebugViews.toggleIoGuard()) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("timings")
                .permission(permission)
                .handler { context -> context.sender().sender.send(DebugViews.timings(root)) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("timings")
                .literal("clear")
                .permission(permission)
                .handler { context -> context.sender().sender.sendMessage(DebugViews.clearTimings()) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("errors")
                .permission(permission)
                .handler { context -> context.sender().sender.send(DebugViews.incidents(root)) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("errors")
                .literal("clear")
                .permission(permission)
                .handler { context -> context.sender().sender.sendMessage(DebugViews.clearIncidents()) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("error")
                .required("id", StringParser.stringParser(), incidentIds())
                .permission(permission)
                .handler { context -> context.sender().sender.send(DebugViews.incident(context.get("id"), root)) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("error")
                .required("id", StringParser.stringParser(), incidentIds())
                .literal("trace")
                .permission(permission)
                .handler { context ->
                    val sender = context.sender().sender
                    DebugViews.printTrace(context.get("id"), sender is ConsoleCommandSender)?.let(sender::sendMessage)
                }
        )
    }

    private fun incidentIds(): SuggestionProvider<CommandSourceStack> =
        SuggestionProvider.blockingStrings { _, _ -> DebugViews.incidentIds() }

    private fun paperLines(): List<String> {
        val plugin = SporeApi.plugin

        val schedulerLine = if (FoliaUtil.isFolia) {
            "<white>Folia <gray>(per-region/entity/async schedulers)"
        } else {
            val scheduler = Bukkit.getScheduler()
            val pending = scheduler.pendingTasks.count { it.owner.name == plugin.name }
            val workers = scheduler.activeWorkers.count { it.owner.name == plugin.name }
            "<white>$pending <gray>pending, <white>$workers <gray>async worker(s)"
        }

        return listOf(
            "<gray>Scheduler: $schedulerLine",
            "<gray>Sidebars: <white>${SidebarManager.activeCount}",
            "<gray>Tablists: <white>${TablistManager.activeCount}"
        )
    }

    private fun CommandSender.send(components: List<Component>) = components.forEach(::sendMessage)
}
