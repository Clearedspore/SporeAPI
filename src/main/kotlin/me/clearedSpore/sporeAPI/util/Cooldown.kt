package me.clearedSpore.sporeAPI.util

import java.util.*
import java.util.concurrent.ConcurrentHashMap

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object Cooldown {

    private val cooldownDurations = ConcurrentHashMap<String, Long>()
    private val activeCooldowns = ConcurrentHashMap<String, ConcurrentHashMap<UUID, Long>>()

    fun createCooldown(id: String, durationSeconds: Long) {
        cooldownDurations[id] = durationSeconds * 1000
    }

    fun addCooldown(id: String, playerId: UUID) {
        val duration = cooldownDurations[id]
            ?: throw IllegalArgumentException("Cooldown ID not found: $id")

        activeCooldowns.computeIfAbsent(id) { ConcurrentHashMap() }[playerId] =
            System.currentTimeMillis() + duration
    }

    fun isOnCooldown(id: String, playerId: UUID): Boolean =
        getTimeLeft(id, playerId) > 0

    fun updateCooldownDuration(id: String, newDurationSeconds: Long) {
        if (!cooldownDurations.containsKey(id)) {
            throw IllegalArgumentException("Cooldown ID not found: $id")
        }
        cooldownDurations[id] = newDurationSeconds * 1000
    }

    fun getTimeLeft(id: String, playerId: UUID): Long =
        activeCooldowns[id]?.get(playerId)?.let {
            (it - System.currentTimeMillis()).coerceAtLeast(0)
        } ?: 0

    fun removeCooldown(id: String, playerId: UUID) {
        activeCooldowns[id]?.remove(playerId)

        if (activeCooldowns[id]?.isEmpty() == true) {
            activeCooldowns.remove(id)
        }
    }

    fun cleanup() {
        val now = System.currentTimeMillis()

        activeCooldowns.forEach { (id, map) ->
            map.entries.removeIf { it.value <= now }

            if (map.isEmpty()) {
                activeCooldowns.remove(id)
            }
        }
    }
}