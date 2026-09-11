package me.clearedSpore.sporeAPI.debug

import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.debug.model.Incident
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.util.CC.mm
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


internal object IncidentFormat {

    private const val MAX_MESSAGE_LENGTH = 220

    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun what(error: Throwable): String {
        val root = TraceAnalyzer.rootCause(error)
        val message = root.message?.oneLine()?.takeIf { it.isNotBlank() } ?: return root.javaClass.simpleName

        return "${root.javaClass.simpleName}: ${message.shorten()}"
    }

    fun where(incident: Incident): String = when {
        incident.location != null -> incident.location
        IncidentReporter.pluginPackage() == null -> "unknown. SporeAPI wasn't initialised yet"
        else -> "not in your plugin's code. See the trace"
    }

    fun thread(incident: Incident): String {
        val kind = if (incident.mainThread) "main thread" else "async"
        val phase = when (incident.phase) {
            LifecyclePhase.STARTING -> "during startup"
            LifecyclePhase.RUNNING -> "while running"
            LifecyclePhase.STOPPING -> "during shutdown"
        }

        return "${incident.thread} ($kind), $phase"
    }

    fun details(incident: Incident): String =
        incident.details.entries.joinToString(", ") { (key, value) -> "$key=$value" }

    fun breadcrumbs(incident: Incident): String = incident.breadcrumbs.joinToString(" > ")

    fun time(millis: Long): String =
        LocalTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(TIME_FORMAT)

    fun ago(millis: Long): String {
        val seconds = ((System.currentTimeMillis() - millis) / 1000).coerceAtLeast(0)

        return when {
            seconds < 60 -> "${seconds}s ago"
            seconds < 3_600 -> "${seconds / 60}m ago"
            seconds < 86_400 -> "${seconds / 3_600}h ago"
            else -> "${seconds / 86_400}d ago"
        }
    }

    fun frame(frame: StackTraceElement): String =
        "${frame.className.substringAfterLast('.')}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})"

    fun consoleLines(incident: Incident, count: Int): List<String> {
        val lines = mutableListOf<String>()
        val seen = if (count > 1) " §7- seen §f${count}x" else ""

        lines += "§f${incident.operation} failed §7[ref §f${incident.id}§7]$seen"
        lines += field("What", what(incident.error))

        incident.explanation?.let { explanation ->
            lines += field("Why", explanation.summary)
            explanation.hint?.let { lines += field("Hint", it) }
        }

        lines += field("Where", where(incident))
        if (incident.breadcrumbs.isNotEmpty()) lines += field("Inside", breadcrumbs(incident))
        if (incident.details.isNotEmpty()) lines += field("Details", details(incident))
        lines += field("Thread", thread(incident))
        IncidentReporter.detailCommand?.let { lines += field("More", "$it ${incident.id}") }

        if (incident.severity != Severity.WARNING) {
            lines += consoleTrace(incident.error, IncidentReporter.consoleTraceDepth)
        }

        return lines
    }

    fun fullTrace(error: Throwable): List<String> =
        error.stackTraceToString().lines()
            .filter { it.isNotBlank() }
            .map { "§7${it.replace("\t", "    ")}" }

    private fun consoleTrace(error: Throwable, depth: Int): List<String> {
        if (depth <= 0) return emptyList()

        val pluginPackage = IncidentReporter.pluginPackage()
        val lines = mutableListOf("  §7Trace:")

        TraceAnalyzer.causeChain(error).forEachIndexed { index, throwable ->
            val prefix = if (index == 0) "" else "Caused by: "
            val message = throwable.message?.oneLine()?.takeIf { it.isNotBlank() }?.let { ": ${it.shorten()}" }.orEmpty()
            lines += "    §7$prefix${throwable.javaClass.name}$message"

            val frames = throwable.stackTrace
            frames.take(depth).forEach { frame ->
                val colour = if (pluginPackage != null && TraceAnalyzer.isPluginFrame(frame, pluginPackage)) "§f" else "§8"
                lines += "      ${colour}at $frame"
            }

            if (frames.size > depth) lines += "      §8... ${frames.size - depth} more"
        }

        return lines
    }

    private fun field(name: String, value: String) = "  §7${"$name:".padEnd(9)}§f$value"

    fun esc(text: String): String = MiniMessage.miniMessage().escapeTags(text)

    fun severity(severity: Severity): String = when (severity) {
        Severity.WARNING -> "<gold>warning</gold>"
        Severity.ERROR -> "<s_red>error</s_red>"
        Severity.CRITICAL -> "<dark_red><bold>critical</bold></dark_red>"
    }

    fun clickable(text: String, click: ClickEvent<*>?, hover: String): Component {
        val component = text.mm().hoverEvent(hover.mm())
        return if (click == null) component else component.clickEvent(click)
    }

    fun chatAlert(incident: Incident): Component {
        val command = IncidentReporter.detailCommand?.let { "$it ${incident.id}" }
        val plugin = if (SporeApi.isInitialized) "<s_blue>${esc(SporeApi.plugin.name)} <dark_gray>» " else ""
        val type = TraceAnalyzer.rootCause(incident.error).javaClass.simpleName

        return clickable(
            "$plugin<s_red>⚠ <white>${esc(incident.operation)} <gray>failed: <white>${esc(type)} <s_blue>[${incident.id}]",
            command?.let { ClickEvent.runCommand(it) },
            "<white>${esc(what(incident.error))}" + if (command != null) "<newline><s_blue>Click for details" else ""
        )
    }

    private fun String.oneLine(): String = replace("\r", "").replace('\n', ' ')

    private fun String.shorten(): String =
        if (length <= MAX_MESSAGE_LENGTH) this else take(MAX_MESSAGE_LENGTH - 3) + "..."
}
