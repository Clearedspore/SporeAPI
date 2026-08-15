package me.clearedSpore.sporeAPI.util

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

class CooldownMap<T : Any>(duration: Long, unit: TimeUnit) {

    private val durationMillis = unit.toMillis(duration)

    private val cache: Cache<T, Long> = Caffeine.newBuilder()
        .expireAfterWrite(duration, unit)
        .build()

    fun isOnCooldown(key: T): Boolean {
        return cache.getIfPresent(key) != null
    }

    fun add(key: T) {
        cache.put(key, System.currentTimeMillis() + durationMillis)
    }

    fun getRemaining(key: T): Long {
        val expiry = cache.getIfPresent(key) ?: return 0
        return (expiry - System.currentTimeMillis()).coerceAtLeast(0)
    }

    fun remove(key: T) {
        cache.invalidate(key)
    }

    fun clear() {
        cache.invalidateAll()
    }

}