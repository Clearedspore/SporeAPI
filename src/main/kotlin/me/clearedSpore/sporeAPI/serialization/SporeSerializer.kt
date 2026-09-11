package me.clearedSpore.sporeAPI.serialization

import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.debug.runDebug

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object SporeSerializer {

    fun serialize(obj: Any?): String {
        if (obj == null) return "null"

        val codec = SporeCodecRegistry.get(obj::class.java)

        if (codec != null) {
            @Suppress("UNCHECKED_CAST")
            return (codec as SporeCodec<Any>).encode(obj)
        }

        return GsonHolder.gson.toJson(obj)
    }

    fun <T : Any> deserialize(data: String?, type: Class<T>): T? {
        if (data == null || data == "null") return null

        val codec = SporeCodecRegistry.get(type)

        if (codec != null) {
            @Suppress("UNCHECKED_CAST")
            return (codec as SporeCodec<T>).decode(data)
        }

        return runDebug("serialization.decode", Severity.WARNING, mapOf("type" to type.simpleName)) {
            GsonHolder.gson.fromJson(data, type)
        }
    }
}
