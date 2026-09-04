package me.clearedSpore.sporeAPI.registry

import me.clearedSpore.sporeAPI.scan.SporeScanner
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KClass

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class Registry<T : Any>(
    private val type: Class<T>,
    private val annotation: Class<out Annotation>?
) {

    constructor(type: KClass<T>, annotation: KClass<out Annotation>? = null) :
        this(type.java, annotation?.java)

    private val entries = LinkedHashMap<String, T>()

    @Volatile
    private var snapshot: Map<String, T> = emptyMap()

    init {
        @Suppress("LeakingThis")
        RegistryIndex.track(this)
    }

    abstract fun idOf(value: T): String

    protected open fun onRegister(value: T) {}

    val name: String get() = javaClass.simpleName

    val size: Int get() = snapshot.size

    @Synchronized
    fun register(value: T) {
        val id = idOf(value)
        val existing = entries[id]

        if (existing != null) {
            throw DuplicateRegistrationException(
                "$name already has '$id' registered by ${existing.javaClass.name} - " +
                    "refused ${value.javaClass.name}"
            )
        }

        entries[id] = value
        snapshot = LinkedHashMap(entries)
        onRegister(value)
    }

    fun registerAll(values: Iterable<T>) = values.forEach(::register)

    operator fun get(id: String): T? = snapshot[id]

    fun require(id: String): T =
        snapshot[id] ?: throw NoSuchElementException("$name has nothing registered under '$id'")

    fun contains(id: String): Boolean = snapshot.containsKey(id)

    fun all(): Collection<T> = snapshot.values

    fun ids(): Set<String> = snapshot.keys

    @Synchronized
    fun unregister(id: String): T? {
        val removed = entries.remove(id) ?: return null
        snapshot = LinkedHashMap(entries)
        return removed
    }

    @Synchronized
    fun clear() {
        entries.clear()
        snapshot = emptyMap()
    }

    fun scan(plugin: JavaPlugin): Int {
        val annotation = this.annotation
            ?: error("$name was built without an annotation, so it cannot scan - register manually")

        var count = 0

        SporeScanner.annotatedWith(plugin, annotation).forEach { clazz ->
            if (!type.isAssignableFrom(clazz)) {
                Logger.warn(
                    "${clazz.simpleName} is annotated with @${annotation.simpleName} " +
                        "but is not a ${type.simpleName}"
                )
                return@forEach
            }

            try {
                @Suppress("UNCHECKED_CAST")
                register(SporeScanner.instantiate(clazz) as T)
                count++
            } catch (exception: DuplicateRegistrationException) {
                throw exception
            } catch (exception: Exception) {
                Logger.error("Failed to register ${clazz.simpleName}: ${exception.message}")
                exception.printStackTrace()
            }
        }

        Logger.info("$name registered $count ${type.simpleName}(s)")
        return count
    }
}
