package me.clearedSpore.sporeAPI.util

import me.clearedSpore.sporeAPI.task.SporeScheduledTask
import me.clearedSpore.sporeAPI.task.Tasks
import me.clearedSpore.sporeAPI.util.CC.translate
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object ActionBar {

    private const val SHARED_MAP_KEY = "sporeapi-actionbar-entries"
    private const val SHARED_OWNER_KEY = "sporeapi-actionbar-owner"

    private const val TEXT = 0
    private const val EXPIRES_AT = 1

    private const val SEPARATOR = " §7| "
    private const val DEFAULT_DURATION = 2000L

    private val id = Any()

    @Suppress("UNCHECKED_CAST")
    private val map: ConcurrentHashMap<UUID, ConcurrentHashMap<String, Array<Any>>> =
        synchronized(System.getProperties()) {
            System.getProperties().getOrPut(SHARED_MAP_KEY) {
                ConcurrentHashMap<UUID, ConcurrentHashMap<String, Array<Any>>>()
            } as ConcurrentHashMap<UUID, ConcurrentHashMap<String, Array<Any>>>
        }

    @Suppress("UNCHECKED_CAST")
    private val owner: AtomicReference<Any?> =
        synchronized(System.getProperties()) {
            System.getProperties().getOrPut(SHARED_OWNER_KEY) { AtomicReference<Any?>(null) } as AtomicReference<Any?>
        }

    private var task: SporeScheduledTask? = null

    fun start() {
        if (task != null) return

        task = Tasks.runTimer(0, 1) {
            tick()
        }
    }

    fun stop() {
        task?.cancel()
        task = null
        owner.compareAndSet(id, null)
    }

    fun put(player: Player, key: String, text: String, durationMillis: Long = DEFAULT_DURATION) {
        val colored = text.translate()
        val expire = if (durationMillis == 0L) Long.MAX_VALUE else System.currentTimeMillis() + durationMillis
        map.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }[key] = arrayOf(colored, expire)
    }

    fun Player.actionBar(key: String, text: String, durationMillis: Long = DEFAULT_DURATION) {
        put(this, key, text, durationMillis)
    }

    fun remove(player: Player, key: String) {
        map[player.uniqueId]?.remove(key)
        dropIfEmpty(player.uniqueId)
    }

    fun send(player: Player) {
        val playerMap = map[player.uniqueId] ?: return
        val now = System.currentTimeMillis()

        playerMap.entries.removeIf { (it.value[EXPIRES_AT] as Long) <= now }

        if (playerMap.isEmpty()) {
            dropIfEmpty(player.uniqueId)
            return
        }

        player.sendActionBar(playerMap.values.joinToString(SEPARATOR) { it[TEXT] as String })
    }


    fun tick() {
        if (owner.get() != id && !owner.compareAndSet(null, id)) return

        for (player in Bukkit.getOnlinePlayers()) Tasks.runEntity(player, { send(player) })
    }

    private fun dropIfEmpty(uuid: UUID) {
        map.computeIfPresent(uuid) { _, entries -> if (entries.isEmpty()) null else entries }
    }
}
