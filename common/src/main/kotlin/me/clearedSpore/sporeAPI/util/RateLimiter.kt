package me.clearedSpore.sporeAPI.util

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class RateLimiter<K : Any>(val limit: Int, val per: Duration) {

    init {
        require(limit > 0) { "limit must be positive, got $limit" }
        require(per.isPositive()) { "per must be positive, got $per" }
    }

    private val refillPerMilli: Double = limit / per.inWholeMilliseconds.toDouble()

    private val buckets: Cache<K, Bucket> = Caffeine.newBuilder()
        .expireAfterAccess(java.time.Duration.ofMillis(per.inWholeMilliseconds))
        .build()

    private class Bucket(var tokens: Double, var updatedAt: Long)

    fun tryAcquire(key: K, permits: Int = 1): Boolean = withBucket(key) { bucket ->
        if (bucket.tokens >= permits) {
            bucket.tokens -= permits
            true
        } else {
            false
        }
    }

    fun remaining(key: K): Int = withBucket(key) { it.tokens.toInt() }

    fun retryAfter(key: K, permits: Int = 1): Duration = withBucket(key) { bucket ->
        val missing = permits - bucket.tokens
        if (missing <= 0) Duration.ZERO else kotlin.math.ceil(missing / refillPerMilli).toLong().milliseconds
    }

    suspend fun acquire(key: K, permits: Int = 1) {
        require(permits <= limit) { "Can never acquire $permits permits with a limit of $limit" }

        while (!tryAcquire(key, permits)) {
            kotlinx.coroutines.delay(retryAfter(key, permits).coerceAtLeast(1.milliseconds))
        }
    }

    fun reset(key: K) = buckets.invalidate(key)

    fun resetAll() = buckets.invalidateAll()

    private inline fun <T> withBucket(key: K, action: (Bucket) -> T): T {
        val now = System.currentTimeMillis()
        val bucket = buckets.get(key) { Bucket(limit.toDouble(), now) }

        return synchronized(bucket) {
            val elapsed = (now - bucket.updatedAt).coerceAtLeast(0)
            bucket.tokens = (bucket.tokens + elapsed * refillPerMilli).coerceAtMost(limit.toDouble())
            bucket.updatedAt = now
            action(bucket)
        }
    }
}
