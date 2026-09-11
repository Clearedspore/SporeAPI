package me.clearedSpore.sporeAPI.debug

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object TraceAnalyzer {

    private const val MAX_CAUSE_DEPTH = 20

    private val apiPackage: String = TraceAnalyzer::class.java.packageName.substringBeforeLast('.')

    fun rootCause(error: Throwable): Throwable {
        var current = error
        var depth = 0

        while (depth < MAX_CAUSE_DEPTH) {
            val next = current.cause ?: break
            current = next
            depth++
        }

        return current
    }

    fun causeChain(error: Throwable): List<Throwable> {
        val chain = mutableListOf(error)
        var current = error

        while (chain.size <= MAX_CAUSE_DEPTH) {
            val next = current.cause ?: break
            if (chain.any { it === next }) break
            chain += next
            current = next
        }

        return chain
    }

    fun fingerprint(operation: String, error: Throwable, location: String?): String {
        val root = rootCause(error)
        return "$operation:${root.javaClass.name}@${location ?: "unknown"}"
    }

    fun findLocation(error: Throwable, pluginPackage: String): String? {
        val frame = firstPluginFrame(rootCause(error), pluginPackage)
            ?: firstPluginFrame(error, pluginPackage)
            ?: return null

        val className = frame.className.substringAfterLast('.')
        return "${frame.fileName}:${frame.lineNumber} ($className.${frame.methodName})"
    }

    fun isPluginFrame(frame: StackTraceElement, pluginPackage: String): Boolean =
        frame.className.startsWith("$pluginPackage.") && !frame.className.startsWith("$apiPackage.")

    private fun firstPluginFrame(error: Throwable, pluginPackage: String): StackTraceElement? =
        error.stackTrace.firstOrNull { isPluginFrame(it, pluginPackage) }
}
