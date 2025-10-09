package me.clearedSpore.sporeAPI.util

import java.util.*
import java.util.concurrent.ConcurrentHashMap

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object Cooldown {

    private val cooldownDurations = mutableMapOf<String, Long>()
    private val activeCooldowns = mutableMapOf<String, MutableMap<UUID, Long>>()


    fun createCooldown(id: String, durationSeconds: Long) {
        cooldownDurations[id] = durationSeconds * 1000
    }

    fun addCooldown(id: String, playerId: UUID) {
        val duration = cooldownDurations[id]
            ?: throw IllegalArgumentException("Cooldown ID not found: $id")

        activeCooldowns.computeIfAbsent(id) { mutableMapOf() }[playerId] =
            System.currentTimeMillis() + duration
    }

    fun isOnCooldown(id: String, playerId: UUID): Boolean =
        getTimeLeft(id, playerId) > 0

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
}
