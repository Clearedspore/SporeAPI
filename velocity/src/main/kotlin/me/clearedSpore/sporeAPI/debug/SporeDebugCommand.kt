package me.clearedSpore.sporeAPI.debug

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ConsoleCommandSource
import me.clearedSpore.sporeAPI.SporeVelocity
import me.clearedSpore.sporeAPI.command.cloud.SporeVelocityCommandManager
import net.kyori.adventure.text.Component
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.SuggestionProvider

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeDebugCommand {

    fun register(
        manager: SporeVelocityCommandManager,
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
                .handler { context -> context.sender().send(DebugViews.report(root, proxyLines())) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("io")
                .permission(permission)
                .handler { context -> context.sender().sendMessage(DebugViews.toggleIoGuard()) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("timings")
                .permission(permission)
                .handler { context -> context.sender().send(DebugViews.timings(root)) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("timings")
                .literal("clear")
                .permission(permission)
                .handler { context -> context.sender().sendMessage(DebugViews.clearTimings()) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("errors")
                .permission(permission)
                .handler { context -> context.sender().send(DebugViews.incidents(root)) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("errors")
                .literal("clear")
                .permission(permission)
                .handler { context -> context.sender().sendMessage(DebugViews.clearIncidents()) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("error")
                .required("id", StringParser.stringParser(), incidentIds())
                .permission(permission)
                .handler { context -> context.sender().send(DebugViews.incident(context.get("id"), root)) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("error")
                .required("id", StringParser.stringParser(), incidentIds())
                .literal("trace")
                .permission(permission)
                .handler { context ->
                    val sender = context.sender()
                    DebugViews.printTrace(context.get("id"), sender is ConsoleCommandSource)?.let(sender::sendMessage)
                }
        )
    }

    private fun incidentIds(): SuggestionProvider<CommandSource> =
        SuggestionProvider.blockingStrings { _, _ -> DebugViews.incidentIds() }

    private fun proxyLines(): List<String> {
        val server = SporeVelocity.server

        return listOf(
            "<gray>Players: <white>${server.playerCount}",
            "<gray>Servers: <white>${server.allServers.size}"
        )
    }

    private fun CommandSource.send(components: List<Component>) = components.forEach(::sendMessage)
}
