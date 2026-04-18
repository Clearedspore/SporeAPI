package me.clearedSpore.sporeAPI.dsl

import me.clearedSpore.sporeAPI.Extension.uuid
import me.clearedSpore.sporeAPI.util.Cooldown
import org.bukkit.command.CommandSender

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object DSL {

    fun CommandSender.cooldown(
        id: String,
        action: (remainingSeconds: Long) -> Unit
    ): Boolean {
        val uuid = this.uuid

        val remaining = Cooldown.getTimeLeft(id, uuid)

        if (remaining > 0) {
            action(remaining / 1000)
            return false
        }

        return true
    }

    fun CommandSender.withCooldown(id: String, seconds: Long): Boolean {
        val uuid = this.uuid

        if (Cooldown.isOnCooldown(id, uuid)) {
            return false
        }

        Cooldown.createCooldown(id, seconds)
        Cooldown.addCooldown(id, uuid)
        return true
    }
}