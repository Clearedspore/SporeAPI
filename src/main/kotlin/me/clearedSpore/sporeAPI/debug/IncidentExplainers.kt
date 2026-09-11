package me.clearedSpore.sporeAPI.debug

import me.clearedSpore.sporeAPI.debug.model.Explanation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.



object IncidentExplainers {

    private val explainers = ConcurrentHashMap<String, CopyOnWriteArrayList<(Throwable) -> Explanation?>>()

    init {
        registerDefaults()
    }

    fun register(className: String, explain: (Throwable) -> Explanation?) {
        explainers.computeIfAbsent(className) { CopyOnWriteArrayList() }.add(0, explain)
    }

    fun register(className: String, summary: String, hint: String? = null) {
        val explanation = Explanation(summary, hint)
        register(className) { explanation }
    }

    inline fun <reified T : Throwable> register(noinline explain: (T) -> Explanation?) {
        register(T::class.java.name) { explain(it as T) }
    }

    fun explain(error: Throwable): Explanation? {
        val candidates = mutableListOf<Candidate>()

        TraceAnalyzer.causeChain(error).asReversed().forEachIndexed { distance, throwable ->
            var type: Class<*>? = throwable.javaClass
            var depth = 0

            while (type != null && type != Any::class.java) {
                explainers[type.name]?.let { candidates += Candidate(throwable, it, depth, distance) }
                type = type.superclass
                depth++
            }
        }

        candidates.sortWith(compareBy({ it.depth }, { it.distance }))

        for (candidate in candidates) {
            for (explain in candidate.explainers) {
                val explanation = try {
                    explain(candidate.throwable)
                } catch (_: Exception) {
                    null
                }

                if (explanation != null) return explanation
            }
        }

        return null
    }

    private class Candidate(
        val throwable: Throwable,
        val explainers: List<(Throwable) -> Explanation?>,
        val depth: Int,
        val distance: Int
    )

    private fun registerDefaults() {
        register(
            "java.lang.NullPointerException",
            "Something was null (missing) where the code expected a value.",
            "Look at the line under 'Where'. It's often an offline player, a missing config key or an unloaded world."
        )
        register(
            "kotlin.UninitializedPropertyAccessException",
            "A lateinit property was read before it was given a value.",
            "Make sure it's set (usually in onEnable) before anything uses it."
        )
        register(
            "java.lang.NumberFormatException",
            "Some text couldn't be read as a number.",
            "Check config values and command arguments that are supposed to be numbers."
        )
        register(
            "java.lang.ClassCastException",
            "A value had a different type than the code expected.",
            "Check 'as' casts, and data loaded from configs or databases."
        )
        register(
            "java.lang.IndexOutOfBoundsException",
            "The code asked for a position in a list, array or text that doesn't exist.",
            "Check that it isn't empty or shorter than expected before reading from it."
        )
        register(
            "java.util.NoSuchElementException",
            "The code expected an element that wasn't there, like first() on an empty list.",
            "Use firstOrNull() or getOrNull() and handle the empty case."
        )
        register(
            "java.util.ConcurrentModificationException",
            "A collection was changed while it was being looped over.",
            "Loop over a copy (toList()), or remove through the iterator. Two threads sharing a collection cause this too."
        )
        register(
            "java.lang.UnsupportedOperationException",
            "The code tried to change something that can't be changed, usually a read-only list or map.",
            "Copy it first (toMutableList(), toMutableMap()) and change the copy."
        )
        register(
            "java.lang.ArithmeticException",
            "A calculation failed, usually a division by zero.",
            "Check the numbers going into it, especially anything that can be 0."
        )
        register(
            "java.lang.IllegalArgumentException",
            "A method was given a value it doesn't accept.",
            "The message names the value often a bad enum or material name, or a number out of range."
        )
        register("java.lang.IllegalStateException") { error ->
            val message = error.message.orEmpty().lowercase()

            when {
                "async" in message || "synchronously" in message -> Explanation(
                    "Bukkit was used from an async thread, which it doesn't allow.",
                    "Switch to the main thread first wrap the call in withRunCtx { } or schedule it with runTask."
                )
                "zip file closed" in message -> Explanation(
                    "The plugin's jar was replaced or closed while the server was running.",
                    "Don't swap jars or use /reload on a running server. Restart it instead."
                )
                else -> null
            }
        }
        register(
            MainThreadIoException::class.java.name,
            "A blocking call (database or file) ran on the main server thread, which freezes the server while it waits.",
            "Use the suspend repository methods (find, save, ...) or move the call into withAsyncCtx { }."
        )
        register(
            "java.lang.StackOverflowError",
            "Code kept calling itself without stopping (endless recursion).",
            "Look for a function that calls itself, or two functions that call each other."
        )
        register(
            "java.lang.OutOfMemoryError",
            "The server ran out of memory.",
            "Give it more RAM, or look for something that keeps growing. A cache, list or map that is never cleared."
        )
        register(
            "java.lang.NoClassDefFoundError",
            "A class the code needs is missing at runtime.",
            "A required plugin isn't installed, or a library wasn't shaded into the jar."
        )
        register(
            "java.lang.ClassNotFoundException",
            "A class the code needs couldn't be found.",
            "A required plugin isn't installed, or a library wasn't shaded into the jar."
        )
        register(
            "java.lang.NoSuchMethodError",
            "The code was built against a different version of a library or the server than the one running.",
            "Update the plugin or its dependency so the versions match the server."
        )
        register(
            "java.lang.NoSuchFieldError",
            "The code was built against a different version of a library or the server than the one running.",
            "Update the plugin or its dependency so the versions match the server."
        )
        register(
            "java.util.concurrent.TimeoutException",
            "Something took too long and was stopped.",
            "Whatever it was waiting on a database, a website, another thread didn't answer in time."
        )
        register(
            "kotlinx.coroutines.TimeoutCancellationException",
            "A withTimeout block ran out of time.",
            "Whatever it was waiting on didn't answer in time. Raise the timeout or find out why it's slow."
        )

        register(
            "java.io.IOException",
            "Reading or writing data a file or a network connection failed.",
            "The message usually names the file or connection."
        )
        register(
            "java.io.FileNotFoundException",
            "A file couldn't be found or opened.",
            "Check that the path exists and the server is allowed to read it."
        )
        register(
            "java.nio.file.NoSuchFileException",
            "A file or folder doesn't exist.",
            "Check the path in the message."
        )
        register(
            "java.nio.file.AccessDeniedException",
            "The server isn't allowed to read or write this file.",
            "Check the file's permissions, and that no other program has it locked."
        )
        register(
            "java.net.ConnectException",
            "Couldn't connect to another server.",
            "Check the address and port, and that the other server is running and reachable."
        )
        register(
            "java.net.SocketTimeoutException",
            "A connection to another server took too long to answer.",
            "The other server may be down or overloaded, or a firewall is blocking it."
        )
        register(
            "java.net.UnknownHostException",
            "A server address couldn't be found.",
            "Check the host name for typos."
        )


        register(
            "com.mongodb.MongoException",
            "MongoDB returned an error.",
            "The message is MongoDB's own reason."
        )
        register(
            "com.mongodb.MongoTimeoutException",
            "MongoDB couldn't be reached in time.",
            "Check the connection URI, that the database is running, and that this server is allowed to connect to it."
        )
        register(
            "com.mongodb.MongoSecurityException",
            "MongoDB rejected the login.",
            "Check the username, password and authSource in the connection URI."
        )
        register(
            "com.mongodb.MongoSocketException",
            "The connection to MongoDB failed or was cut off.",
            "Check that the database is running and reachable from this server."
        )
        register(
            "com.mongodb.MongoConfigurationException",
            "The MongoDB connection URI is invalid.",
            "It should look like mongodb://user:password@host:27017/database."
        )
        register("com.mongodb.MongoWriteException") { error ->
            if ("E11000" !in error.message.orEmpty()) return@register null

            Explanation(
                "MongoDB already has a document with this unique key.",
                "Check your unique indexes, or replace/upsert instead of inserting."
            )
        }
        register(
            "org.bson.codecs.configuration.CodecConfigurationException",
            "MongoDB doesn't know how to store one of the values in this document.",
            "Convert it to a basic type (text, number, list or Document) in toDocument()."
        )


        register(
            "org.bukkit.configuration.InvalidConfigurationException",
            "A YAML file isn't valid YAML.",
            "Check the indentation (spaces, not tabs) and quotes. A YAML validator shows the exact line."
        )
        register(
            "org.bukkit.plugin.IllegalPluginAccessException",
            "Something used the plugin after it was disabled, like scheduling a task during shutdown.",
            "Check plugin.isEnabled before scheduling, and stop repeating tasks in onDisable."
        )
        register(
            "com.google.gson.JsonParseException",
            "Some saved JSON data couldn't be read.",
            "The data may be corrupted, or saved in an older format."
        )
    }
}
