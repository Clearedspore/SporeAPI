package me.clearedSpore.sporeAPI.coroutine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class Cached<V>(val value: V, val loadedAt: Long)

class SuspendCache<K : Any, V : Any>(ttl: Duration, maximumSize: Long = 1_000) {

    private val entries: Cache<K, Cached<V>> = Caffeine.newBuilder()
        .expireAfterWrite(ttl.toMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(maximumSize)
        .build()

    private val loading = ConcurrentHashMap<K, Deferred<Cached<V>>>()

    private val generation = AtomicLong()

    suspend fun get(key: K, load: suspend () -> V): Cached<V> {
        entries.getIfPresent(key)?.let { return it }

        val pending = loading.computeIfAbsent(key) {
            val startedIn = generation.get()

            SporeCoroutines.scope.async(SporeCoroutines.async) {
                val cached = Cached(load(), System.currentTimeMillis())

                if (generation.get() == startedIn) {
                    entries.put(key, cached)
                }

                cached
            }
        }

        return try {
            pending.await()
        } finally {
            loading.remove(key, pending)
        }
    }

    fun invalidate(key: K) {
        generation.incrementAndGet()
        loading.remove(key)
        entries.invalidate(key)
    }

    fun invalidateAll() {
        generation.incrementAndGet()
        loading.clear()
        entries.invalidateAll()
    }
}
