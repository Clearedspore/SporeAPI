package me.clearedSpore.sporeAPI.debug

import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.debug.model.Incident
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object IncidentReporter {

    private const val MAX_INCIDENTS = 50
    private const val LOG_INTERVAL_MILLIS = 30_000L

    @Volatile
    var consoleTraceDepth: Int = 8

    @Volatile
    var notifyPermission: String? = null

    @Volatile
    internal var detailCommand: String? = null

    private val incidents = ArrayDeque<Incident>()
    private val counts = ConcurrentHashMap<String, Int>()
    private val firstSeen = ConcurrentHashMap<String, Long>()
    private val lastSeen = ConcurrentHashMap<String, Long>()
    private val lastLogged = ConcurrentHashMap<String, Long>()
    private val latest = ConcurrentHashMap<String, Incident>()
    private val handlers = CopyOnWriteArrayList<(Incident) -> Unit>()

    data class Stats(val count: Int, val firstSeen: Long, val lastSeen: Long)

    fun report(
        operation: String,
        error: Throwable,
        severity: Severity = Severity.ERROR,
        details: Map<String, String> = emptyMap()
    ): Incident {
        try {
            val location = pluginPackage()?.let { TraceAnalyzer.findLocation(error, it) }
            val fingerprint = TraceAnalyzer.fingerprint(operation, error, location)
            val now = System.currentTimeMillis()

            val count = counts.merge(fingerprint, 1, Int::plus) ?: 1
            firstSeen.putIfAbsent(fingerprint, now)
            lastSeen[fingerprint] = now

            if (!shouldLog(fingerprint)) {
                return latest[fingerprint] ?: create(operation, error, severity, details, now, location, fingerprint)
            }

            val previous = latest[fingerprint]
            val created = create(operation, error, severity, details, now, location, fingerprint)
            val incident = if (previous != null) created.copy(id = previous.id) else created

            synchronized(incidents) {
                incidents.removeAll { it.fingerprint == fingerprint }
                incidents.addLast(incident)
                while (incidents.size > MAX_INCIDENTS) forget(incidents.removeFirst())
            }
            latest[fingerprint] = incident

            log(incident, count)
            notifyStaff(incident)

            handlers.forEach { handler ->
                try {
                    handler(incident)
                } catch (exception: Exception) {
                    Logger.warn("An incident handler threw: $exception")
                }
            }

            return incident
        } catch (failure: Exception) {
            System.err.println("IncidentReporter failed while reporting '$operation'")
            failure.printStackTrace()
            error.printStackTrace()
            return Incident(
                operation = operation,
                error = error,
                severity = severity,
                details = details,
                thread = Thread.currentThread().name,
                mainThread = false,
                phase = SporeDebug.phase
            )
        }
    }

    fun recent(): List<Incident> = synchronized(incidents) { incidents.toList() }

    fun find(id: String): Incident? = synchronized(incidents) {
        incidents.firstOrNull { it.id == id }
            ?: incidents.filter { it.id.equals(id, ignoreCase = true) }.singleOrNull()
    }

    fun stats(incident: Incident): Stats {
        val fingerprint = incident.fingerprint ?: return Stats(1, incident.time, incident.time)

        return Stats(
            count = counts[fingerprint] ?: 1,
            firstSeen = firstSeen[fingerprint] ?: incident.time,
            lastSeen = lastSeen[fingerprint] ?: incident.time
        )
    }

    fun onIncident(handler: (Incident) -> Unit) {
        handlers += handler
    }

    fun clear() = synchronized(incidents) {
        incidents.clear()
        counts.clear()
        firstSeen.clear()
        lastSeen.clear()
        lastLogged.clear()
        latest.clear()
    }

    internal fun pluginPackage(): String? =
        if (SporeApi.isInitialized) SporeApi.plugin.javaClass.packageName else null

    private fun create(
        operation: String,
        error: Throwable,
        severity: Severity,
        details: Map<String, String>,
        time: Long,
        location: String?,
        fingerprint: String
    ) = Incident(
        operation = operation,
        error = error,
        severity = severity,
        details = details,
        time = time,
        mainThread = isMainThread(),
        thread = Thread.currentThread().name,
        phase = SporeDebug.phase,
        breadcrumbs = DebugContext.current(),
        location = location,
        fingerprint = fingerprint,
        explanation = IncidentExplainers.explain(error)
    )

    private fun log(incident: Incident, count: Int) {
        val lines = IncidentFormat.consoleLines(incident, count)

        if (incident.severity == Severity.WARNING) {
            lines.forEach { Logger.warn(it) }
        } else {
            lines.forEach { Logger.error(it) }
        }
    }

    private fun notifyStaff(incident: Incident) {
        val permission = notifyPermission ?: return
        if (incident.severity == Severity.WARNING || !SporeApi.isInitialized) return

        val message = IncidentFormat.chatAlert(incident)
        val send = Runnable {
            Bukkit.getOnlinePlayers()
                .filter { it.hasPermission(permission) }
                .forEach { it.sendMessage(message) }
        }

        val plugin = SporeApi.plugin
        when {
            isMainThread() -> send.run()
            plugin.isEnabled -> Bukkit.getScheduler().runTask(plugin, send)
        }
    }

    private fun forget(incident: Incident) {
        val fingerprint = incident.fingerprint ?: return

        counts.remove(fingerprint)
        firstSeen.remove(fingerprint)
        lastSeen.remove(fingerprint)
        lastLogged.remove(fingerprint)
        latest.remove(fingerprint)
    }

    private fun shouldLog(fingerprint: String): Boolean {
        val now = System.currentTimeMillis()
        val previous = lastLogged[fingerprint]

        if (previous != null && now - previous < LOG_INTERVAL_MILLIS) return false

        lastLogged[fingerprint] = now
        return true
    }

    private fun isMainThread(): Boolean = runCatching { Bukkit.isPrimaryThread() }.getOrDefault(false)
}
