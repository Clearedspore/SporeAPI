package me.clearedSpore.sporeAPI.registry

import java.util.Collections

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object RegistryIndex {

    private val registries = Collections.synchronizedList(mutableListOf<Registry<*>>())

    internal fun track(registry: Registry<*>) {
        registries += registry
    }

    fun all(): List<Registry<*>> = synchronized(registries) { registries.toList() }
}
