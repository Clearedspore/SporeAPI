package me.clearedSpore.sporeAPI.serialization

import java.util.concurrent.ConcurrentHashMap
// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object SporeCodecRegistry {

    private val codecs = mutableMapOf<Class<*>, SporeCodec<Any>>()

    fun <T : Any> register(type: Class<T>, codec: SporeCodec<T>) {
        codecs[type] = codec as SporeCodec<Any>
    }

    fun get(type: Class<*>): SporeCodec<Any>? {
        return codecs[type]
    }
}