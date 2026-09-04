package me.clearedSpore.sporeAPI.debug

import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.Bukkit
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


enum class MainThreadIoPolicy {
    IGNORE,

    WARN,

    THROW
}

class MainThreadIoException(operation: String) :
    IllegalStateException("Blocking call '$operation' was made on the server thread")

object SporeDebug {

    private const val MAX_WARNINGS = 20
    private const val WARN_INTERVAL_MILLIS = 30_000L

    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private const val TRACE_SKIP = 4
    private const val TRACE_DEPTH = 6

    @Volatile
    var mainThreadIoPolicy: MainThreadIoPolicy = MainThreadIoPolicy.WARN

    private val warnings = ArrayDeque<MainThreadIo>()
    private val lastWarned = ConcurrentHashMap<String, Long>()

    data class MainThreadIo(
        val operation: String,
        val time: String,
        val count: Int,
        val trace: List<String>
    )

    private val counts = ConcurrentHashMap<String, Int>()

    fun warnings(): List<MainThreadIo> = synchronized(warnings) { warnings.toList() }

    fun counts(): Map<String, Int> = counts.toMap()

    fun clearWarnings() {
        synchronized(warnings) { warnings.clear() }
        counts.clear()
        lastWarned.clear()
    }

    @PublishedApi
    internal fun check(operation: String) {
        val policy = mainThreadIoPolicy
        if (policy == MainThreadIoPolicy.IGNORE) return
        if (!Bukkit.isPrimaryThread()) return

        record(operation, policy)
    }

    private fun record(operation: String, policy: MainThreadIoPolicy) {
        val count = counts.merge(operation, 1, Int::plus) ?: 1

        val trace = Thread.currentThread().stackTrace
            .drop(TRACE_SKIP)
            .take(TRACE_DEPTH)
            .map { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }

        val warning = MainThreadIo(operation, LocalTime.now().format(TIME_FORMAT), count, trace)

        synchronized(warnings) {
            warnings.addLast(warning)
            while (warnings.size > MAX_WARNINGS) warnings.removeFirst()
        }

        if (policy == MainThreadIoPolicy.THROW) throw MainThreadIoException(operation)

        if (!shouldLog(operation)) return

        Logger.warn("Blocking '$operation' ran on the main thread (x$count):")
        trace.forEach { Logger.warn("    at $it") }
    }

    private fun shouldLog(operation: String): Boolean {
        val now = System.currentTimeMillis()
        val previous = lastWarned[operation]

        if (previous != null && now - previous < WARN_INTERVAL_MILLIS) return false

        lastWarned[operation] = now
        return true
    }
}

inline fun <T> blockingIo(operation: String, block: () -> T): T {
    SporeDebug.check(operation)
    return block()
}
