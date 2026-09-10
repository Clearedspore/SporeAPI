package me.clearedSpore.sporeAPI.debug

import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.debug.model.Incident
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

object IncidentReporter {

    private const val MAX_INCIDENTS = 50
    private const val LOG_INTERVAL_MILLIS = 30_000L

    private val incidents = ArrayDeque<Incident>()
    private val counts = ConcurrentHashMap<String, Int>()
    private val lastLogged = ConcurrentHashMap<String, Long>()
    private val latest = ConcurrentHashMap<String, Incident>()


    fun report(
        operation: String,
        error: Throwable,
        severity: Severity = Severity.ERROR,
        details: Map<String, String> = emptyMap()
    ): Incident {
        try {

            val location = pluginPackage()?.let { TraceAnalyzer.findLocation(error, it) }
            val fingerprint = TraceAnalyzer.fingerprint(operation, error, location)

            val incident = Incident(
                operation = operation,
                error = error,
                severity = severity,
                details = details,
                thread = Thread.currentThread().name,
                mainThread = Bukkit.isPrimaryThread(),
                phase = SporeDebug.phase,
                location = location,
                fingerprint = fingerprint,
            )

            val count = counts.merge(fingerprint, 1, Int::plus) ?: 1

            if (!shouldLog(fingerprint)) return latest[fingerprint] ?: incident

            synchronized(incidents) {
                incidents.addLast(incident)
                while (incidents.size > MAX_INCIDENTS) incidents.removeFirst()
            }
            latest[fingerprint] = incident
            val root = TraceAnalyzer.rootCause(error)
            val message = "$operation failed: ${root.javaClass.simpleName} - ${root.message} " +
                    "at ${location ?: "unknown location"} [x$count, ref ${incident.id}]"

            if (severity == Severity.ERROR || severity == Severity.CRITICAL) {
                Logger.error(message)
            } else {
                Logger.warn(message)
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
    }

    fun clear() = synchronized(incidents) {
        incidents.clear()
        counts.clear()
        lastLogged.clear()
        latest.clear()
    }


    private fun shouldLog(operation: String): Boolean {
        val now = System.currentTimeMillis()
        val previous = lastLogged[operation]

        if (previous != null && now - previous < LOG_INTERVAL_MILLIS) return false

        lastLogged[operation] = now
        return true
    }

    private fun pluginPackage(): String? =
        if (SporeApi.isInitialized) SporeApi.plugin.javaClass.packageName else null
}