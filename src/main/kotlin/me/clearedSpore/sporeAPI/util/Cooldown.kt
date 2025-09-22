package me.clearedSpore.sporeAPI.util

import java.util.*
import java.util.concurrent.ConcurrentHashMap

object Cooldown {

    private val cooldownDurations = mutableMapOf<String, Long>()
    private val activeCooldowns = mutableMapOf<String, MutableMap<UUID, Long>>()

    fun createCooldown(id: String, duration: Long) {
        cooldownDurations[id] = duration
    }

    fun addCooldown(id: String, playerId: UUID) {
        val duration = cooldownDurations[id] ?: throw IllegalArgumentException("Cooldown ID not found: $id")
        activeCooldowns.computeIfAbsent(id) { mutableMapOf() }[playerId] = System.currentTimeMillis() + duration
    }

    fun isOnCooldown(id: String, playerId: UUID): Boolean = getTimeLeft(id, playerId) > 0

    fun getTimeLeft(id: String, playerId: UUID): Long =
        activeCooldowns[id]?.get(playerId)?.let { (it - System.currentTimeMillis()).coerceAtLeast(0) } ?: 0

    fun removeCooldown(id: String, playerId: UUID) {
        activeCooldowns[id]?.remove(playerId)
        if (activeCooldowns[id]?.isEmpty() == true) activeCooldowns.remove(id)
    }
}
