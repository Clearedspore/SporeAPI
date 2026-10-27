package me.clearedSpore.sporeAPI.debug

import kotlinx.coroutines.job
import me.clearedSpore.sporeAPI.SporeCore
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.IncidentFormat.esc
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.registry.RegistryIndex
import me.clearedSpore.sporeAPI.util.CC.mm
import me.clearedSpore.sporeAPI.util.Logger
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object DebugViews {

    private const val LIST_SIZE = 10
    private const val CHAT_TRACE_DEPTH = 6
    private const val TIMINGS_IN_REPORT = 5
    private const val TIMINGS_LIST_SIZE = 15

    fun success(message: String): Component = "<s_blue>✔ | $message".mm()

    fun error(message: String): Component = "<s_red>✖ | $message".mm()

    fun report(root: String, platformLines: List<String> = emptyList()): List<Component> {
        val coroutines = runCatching {
            SporeCoroutines.scope.coroutineContext.job.children.count()
        }.getOrDefault(0)

        val lines = mutableListOf<String>()

        lines += "<s_blue>${esc(platformName())} <white>debug"
        lines += ""
        lines += platformLines
        lines += "<gray>Coroutines: <white>$coroutines <gray>active"
        SporeDebug.statuses().forEach { (name, value) -> lines += "<gray>$name: $value" }

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

        val components = lines.map { it.mm() }.toMutableList()

        val slowest = Timings.slowest(TIMINGS_IN_REPORT)
        if (slowest.isNotEmpty()) {
            components += Component.empty()
            components += IncidentFormat.clickable(
                "<gray>Slowest timings: <s_blue>[view all]",
                ClickEvent.runCommand("$root timings"),
                "<gray>Click to list every timed operation"
            )
            slowest.forEach { (name, stats) -> components += timingLine(name, stats).mm() }
        }

        components += Component.empty()

        val incidents = IncidentReporter.recent()
        if (incidents.isEmpty()) {
            components += "<gray>Incidents: <s_green>none recorded".mm()
            return components
        }

        val errors = incidents.count { it.severity != Severity.WARNING }
        components += IncidentFormat.clickable(
            "<gray>Incidents: <white>${incidents.size} <gray>(<s_red>$errors<gray> error(s), " +
                "<gold>${incidents.size - errors}<gray> warning(s)) <s_blue>[view]",
            ClickEvent.runCommand("$root errors"),
            "<gray>Click to list them"
        )

        return components
    }

    fun toggleIoGuard(): Component {
        val silenced = SporeDebug.mainThreadIoPolicy == MainThreadIoPolicy.WARN

        SporeDebug.mainThreadIoPolicy =
            if (silenced) MainThreadIoPolicy.IGNORE else MainThreadIoPolicy.WARN

        val state = if (silenced) "<s_red>silenced" else "<s_green>warning"
        return success("Main-thread IO guard is now $state<white>.")
    }

    fun timings(root: String): List<Component> {
        val stats = Timings.slowest(TIMINGS_LIST_SIZE)

        if (stats.isEmpty()) return listOf(success("Nothing has been timed yet."))

        val components = mutableListOf<Component>()
        components += "<s_blue>${esc(platformName())} <white>timings <gray>Slowest average first".mm()
        stats.forEach { (name, stat) -> components += timingLine(name, stat).mm() }
        components += IncidentFormat.clickable(
            "<s_red>[clear all]",
            ClickEvent.suggestCommand("$root timings clear"),
            "<gray>Forget every recorded timing"
        )

        return components
    }

    fun clearTimings(): Component {
        Timings.clear()
        return success("Cleared all timings.")
    }

    fun incidents(root: String): List<Component> {
        val incidents = IncidentReporter.recent().asReversed()

        if (incidents.isEmpty()) return listOf(success("No incidents recorded."))

        val components = mutableListOf<Component>()
        components += "<s_blue>${esc(platformName())} <white>incidents <gray> Newest first, click one for details".mm()

        incidents.take(LIST_SIZE).forEach { incident ->
            val stats = IncidentReporter.stats(incident)
            val type = TraceAnalyzer.rootCause(incident.error).javaClass.simpleName

            components += IncidentFormat.clickable(
                "<dark_gray>${IncidentFormat.time(stats.lastSeen)} ${IncidentFormat.severity(incident.severity)} " +
                    "<white>${esc(incident.operation)} <gray>${esc(type)} <white>x${stats.count} <s_blue>[${incident.id}]",
                ClickEvent.runCommand("$root error ${incident.id}"),
                "<white>${esc(IncidentFormat.what(incident.error))}" +
                    "<newline><gray>${esc(IncidentFormat.where(incident))}" +
                    "<newline><s_blue>Click for details"
            )
        }

        if (incidents.size > LIST_SIZE) {
            components += "<gray>...and <white>${incidents.size - LIST_SIZE} <gray>older.".mm()
        }

        components += IncidentFormat.clickable(
            "<s_red>[clear all]",
            ClickEvent.suggestCommand("$root errors clear"),
            "<gray>Forget every recorded incident"
        )

        return components
    }

    fun incident(id: String, root: String): List<Component> {
        val incident = IncidentReporter.find(id)
            ?: return listOf(error("No incident with ref <white>${esc(id)}<s_red>. It may have been cleared, or pushed out by newer ones."))

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

        return lines.map { it.mm() } + IncidentFormat.clickable(
            "<s_blue>[print full trace to console]",
            ClickEvent.runCommand("$root error ${incident.id} trace"),
            "<gray>Prints every line of the stack trace to the console"
        )
    }

    fun printTrace(id: String, senderIsConsole: Boolean): Component? {
        val incident = IncidentReporter.find(id) ?: return error("No incident with ref <white>${esc(id)}<s_red>.")

        Logger.error("Full trace for incident ${incident.id} (${incident.operation}):")
        IncidentFormat.fullTrace(incident.error).forEach { Logger.error(it) }

        return if (senderIsConsole) null else success("Printed the full trace for <white>${incident.id} <s_blue>to the console.")
    }

    fun clearIncidents(): Component {
        IncidentReporter.clear()
        return success("Cleared all incidents.")
    }

    fun incidentIds(): List<String> = IncidentReporter.recent().asReversed().map { it.id }

    private fun timingLine(name: String, stats: Timings.Stats): String =
        "  <s_blue>${esc(name)} <white>${Timing.format(stats.average)} <gray>avg, " +
            "<white>${Timing.format(stats.max)} <gray>max, <white>${stats.count}x"

    private fun platformName(): String = if (SporeCore.isInitialized) SporeCore.platform.name else "SporeAPI"

    private fun field(name: String, value: String) = "<gray>$name: <white>${esc(value)}"
}
