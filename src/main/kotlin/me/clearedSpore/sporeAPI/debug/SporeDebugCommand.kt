package me.clearedSpore.sporeAPI.debug

import kotlinx.coroutines.job
import me.clearedSpore.sporeAPI.Extension.error
import me.clearedSpore.sporeAPI.Extension.success
import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.command.cloud.SporeCloudCommandManager
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.IncidentFormat.esc
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.registry.RegistryIndex
import me.clearedSpore.sporeAPI.repository.SporeMongo
import me.clearedSpore.sporeAPI.scoreboard.SidebarManager
import me.clearedSpore.sporeAPI.util.CC.mm
import me.clearedSpore.sporeAPI.util.Logger
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.SuggestionProvider

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeDebugCommand {

    private const val LIST_SIZE = 10
    private const val CHAT_TRACE_DEPTH = 6

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
                .handler { context -> report(context.sender().sender, root) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("io")
                .permission(permission)
                .handler { context -> toggleIoGuard(context.sender().sender) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("errors")
                .permission(permission)
                .handler { context -> listIncidents(context.sender().sender, root) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("errors")
                .literal("clear")
                .permission(permission)
                .handler { context -> clearIncidents(context.sender().sender) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("error")
                .required("id", StringParser.stringParser(), incidentIds())
                .permission(permission)
                .handler { context -> showIncident(context.sender().sender, context.get("id"), root) }
        )

        cloud.command(
            cloud.commandBuilder(label)
                .literal("debug")
                .literal("error")
                .required("id", StringParser.stringParser(), incidentIds())
                .literal("trace")
                .permission(permission)
                .handler { context -> printTrace(context.sender().sender, context.get("id")) }
        )
    }

    private fun incidentIds(): SuggestionProvider<CommandSourceStack> =
        SuggestionProvider.blockingStrings { _, _ -> IncidentReporter.recent().asReversed().map { it.id } }

    private fun toggleIoGuard(sender: CommandSender) {
        val silenced = SporeDebug.mainThreadIoPolicy == MainThreadIoPolicy.WARN

        SporeDebug.mainThreadIoPolicy =
            if (silenced) MainThreadIoPolicy.IGNORE else MainThreadIoPolicy.WARN

        val state = if (silenced) "<s_red>silenced" else "<s_green>warning"
        sender.success("Main-thread IO guard is now $state<white>.")
    }

    private fun report(sender: CommandSender, root: String) {
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

        lines += ""
        lines.forEach { sender.sendMessage(it.mm()) }

        val incidents = IncidentReporter.recent()
        if (incidents.isEmpty()) {
            sender.sendMessage("<gray>Incidents: <s_green>none recorded".mm())
            return
        }

        val errors = incidents.count { it.severity != Severity.WARNING }
        sender.sendMessage(
            IncidentFormat.clickable(
                "<gray>Incidents: <white>${incidents.size} <gray>(<s_red>$errors<gray> error(s), " +
                    "<gold>${incidents.size - errors}<gray> warning(s)) <s_blue>[view]",
                ClickEvent.runCommand("$root errors"),
                "<gray>Click to list them"
            )
        )
    }

    private fun listIncidents(sender: CommandSender, root: String) {
        val incidents = IncidentReporter.recent().asReversed()

        if (incidents.isEmpty()) {
            sender.success("No incidents recorded.")
            return
        }

        sender.sendMessage("<s_blue>${esc(SporeApi.plugin.name)} <white>incidents <gray> Newest first, click one for details".mm())

        incidents.take(LIST_SIZE).forEach { incident ->
            val stats = IncidentReporter.stats(incident)
            val type = TraceAnalyzer.rootCause(incident.error).javaClass.simpleName

            sender.sendMessage(
                IncidentFormat.clickable(
                    "<dark_gray>${IncidentFormat.time(stats.lastSeen)} ${IncidentFormat.severity(incident.severity)} " +
                        "<white>${esc(incident.operation)} <gray>${esc(type)} <white>x${stats.count} <s_blue>[${incident.id}]",
                    ClickEvent.runCommand("$root error ${incident.id}"),
                    "<white>${esc(IncidentFormat.what(incident.error))}" +
                        "<newline><gray>${esc(IncidentFormat.where(incident))}" +
                        "<newline><s_blue>Click for details"
                )
            )
        }

        if (incidents.size > LIST_SIZE) {
            sender.sendMessage("<gray>...and <white>${incidents.size - LIST_SIZE} <gray>older.".mm())
        }

        sender.sendMessage(
            IncidentFormat.clickable(
                "<s_red>[clear all]",
                ClickEvent.suggestCommand("$root errors clear"),
                "<gray>Forget every recorded incident"
            )
        )
    }

    private fun showIncident(sender: CommandSender, id: String, root: String) {
        val incident = IncidentReporter.find(id)
        if (incident == null) {
            sender.error("No incident with ref <white>${esc(id)}<s_red>. It may have been cleared, or pushed out by newer ones.")
            return
        }

        val stats = IncidentReporter.stats(incident)
        val chain = TraceAnalyzer.causeChain(incident.error)
        val cause = chain.last()
        val pluginPackage = IncidentReporter.pluginPackage()

        val lines = mutableListOf<String>()

        lines += "<s_blue>Incident ${incident.id} <dark_gray>- ${IncidentFormat.severity(incident.severity)}"
        lines += field("Operation", incident.operation)
        lines += field("What", IncidentFormat.what(incident.error))

        incident.explanation?.let { explanation ->
            lines += field("Why", explanation.summary)
            explanation.hint?.let { lines += "<gray>Hint: <s_green>${esc(it)}" }
        }

        lines += field("Where", IncidentFormat.where(incident))
        if (incident.breadcrumbs.isNotEmpty()) lines += field("Inside", IncidentFormat.breadcrumbs(incident))
        if (incident.details.isNotEmpty()) lines += field("Details", IncidentFormat.details(incident))
        lines += "<gray>Seen: <white>${stats.count}x <gray> first at <white>${IncidentFormat.time(stats.firstSeen)}" +
            "<gray>, last <white>${IncidentFormat.ago(stats.lastSeen)}"
        lines += field("Thread", IncidentFormat.thread(incident))

        if (chain.size > 1) {
            lines += "<gray>Chain: <white>" + chain.joinToString(" <dark_gray>→ <white>") { esc(it.javaClass.simpleName) }
        }

        lines += "<gray>Trace:"
        cause.stackTrace.take(CHAT_TRACE_DEPTH).forEach { frame ->
            val colour = if (pluginPackage != null && TraceAnalyzer.isPluginFrame(frame, pluginPackage)) "<white>" else "<dark_gray>"
            lines += "  $colour${esc(IncidentFormat.frame(frame))}"
        }
        if (cause.stackTrace.size > CHAT_TRACE_DEPTH) {
            lines += "  <dark_gray>... ${cause.stackTrace.size - CHAT_TRACE_DEPTH} more"
        }

        lines.forEach { sender.sendMessage(it.mm()) }

        sender.sendMessage(
            IncidentFormat.clickable(
                "<s_blue>[print full trace to console]",
                ClickEvent.runCommand("$root error ${incident.id} trace"),
                "<gray>Prints every line of the stack trace to the server console"
            )
        )
    }

    private fun printTrace(sender: CommandSender, id: String) {
        val incident = IncidentReporter.find(id)
        if (incident == null) {
            sender.error("No incident with ref <white>${esc(id)}<s_red>.")
            return
        }

        Logger.error("Full trace for incident ${incident.id} (${incident.operation}):")
        IncidentFormat.fullTrace(incident.error).forEach { Logger.error(it) }

        if (sender !is ConsoleCommandSender) {
            sender.success("Printed the full trace for <white>${incident.id} <s_blue>to the console.")
        }
    }

    private fun clearIncidents(sender: CommandSender) {
        IncidentReporter.clear()
        sender.success("Cleared all incidents.")
    }

    private fun field(name: String, value: String) = "<gray>$name: <white>${esc(value)}"
}
