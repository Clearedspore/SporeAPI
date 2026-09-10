package me.clearedSpore.sporeAPI.debug

object TraceAnalyzer {

    private const val MAX_CAUSE_DEPTH = 20

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

    private fun firstPluginFrame(error: Throwable, pluginPackage: String): StackTraceElement? =
        error.stackTrace.firstOrNull { it.className.startsWith(pluginPackage) }
}