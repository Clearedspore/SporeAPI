package me.clearedSpore.sporeAPI.repository

import me.clearedSpore.sporeAPI.coroutine.withAsyncCtx

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


interface Repository<K : Any, V : Any> {

    fun findBlocking(id: K): V?

    fun findAllBlocking(): List<V>

    fun existsBlocking(id: K): Boolean = findBlocking(id) != null

    fun saveBlocking(value: V)

    fun saveAllBlocking(values: Iterable<V>) = values.forEach(::saveBlocking)

    fun deleteBlocking(id: K)

    suspend fun find(id: K): V? = withAsyncCtx { findBlocking(id) }

    suspend fun findAll(): List<V> = withAsyncCtx { findAllBlocking() }

    suspend fun exists(id: K): Boolean = withAsyncCtx { existsBlocking(id) }

    suspend fun save(value: V) = withAsyncCtx { saveBlocking(value) }

    suspend fun saveAll(values: Iterable<V>) = withAsyncCtx { saveAllBlocking(values) }

    suspend fun delete(id: K) = withAsyncCtx { deleteBlocking(id) }
}
