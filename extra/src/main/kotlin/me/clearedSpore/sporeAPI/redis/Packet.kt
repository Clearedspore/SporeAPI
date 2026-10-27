package me.clearedSpore.sporeAPI.redis

import com.google.gson.JsonElement
import kotlinx.coroutines.Job

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class Packet {

    fun send() = SporeRedis.send(this)

    fun sendAsync(): Job = SporeRedis.sendAsync(this)

    open fun onReceive() {}
}

data class RedisSettings(
    val address: String = "localhost:6379",
    val username: String = "",
    val password: String = "",
    val channel: String = "spore",
    val timeoutMillis: Int = 2_000,
    val receiveOwnPackets: Boolean = true
) {

    val host: String get() = address.substringBefore(":")

    val port: Int get() = address.substringAfter(":", "6379").toInt()
}

internal class PacketEnvelope(
    val type: String,
    val data: JsonElement,
    val origin: String? = null
)
