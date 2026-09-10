package me.clearedSpore.sporeAPI.debug.model

import me.clearedSpore.sporeAPI.debug.LifecyclePhase
import me.clearedSpore.sporeAPI.util.IdUtil

data class Incident(
    val id: String = IdUtil.generateId(5),
    val operation: String,
    val error: Throwable,
    val severity: Severity,
    val details: Map<String, String> = emptyMap(),
    val time: Long = System.currentTimeMillis(),
    val mainThread: Boolean,
    val thread: String,
    val phase: LifecyclePhase,
    val breadcrumbs: List<String> = emptyList(),
    val location: String? = null,
    val fingerprint: String? = null
)
