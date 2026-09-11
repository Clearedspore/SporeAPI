package me.clearedSpore.sporeAPI.debug

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import me.clearedSpore.sporeAPI.debug.model.Incident
import me.clearedSpore.sporeAPI.debug.model.Severity
import kotlin.coroutines.cancellation.CancellationException

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.



fun <T> runDebug(
    operation: String,
    severity: Severity = Severity.ERROR,
    details: Map<String, String> = emptyMap(),
    onFailure: ((Incident) -> Unit)? = null,
    block: () -> T
): T? {
    return try {
        DebugContext.inside(operation, block)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        failed(operation, exception, severity, details, onFailure)
    }
}

suspend fun <T> runDebugSuspending(
    operation: String,
    severity: Severity = Severity.ERROR,
    details: Map<String, String> = emptyMap(),
    onFailure: ((Incident) -> Unit)? = null,
    block: suspend () -> T
): T? {
    return try {
        DebugContext.insideSuspending(operation, block)
    } catch (cancelled: CancellationException) {
        if (!currentCoroutineContext().isActive) throw cancelled
        failed(operation, cancelled, severity, details, onFailure)
    } catch (exception: Exception) {
        failed(operation, exception, severity, details, onFailure)
    }
}

private fun failed(
    operation: String,
    error: Throwable,
    severity: Severity,
    details: Map<String, String>,
    onFailure: ((Incident) -> Unit)?
): Nothing? {
    val incident = IncidentReporter.report(operation, error, severity, details)

    if (onFailure != null) {
        try {
            onFailure(incident)
        } catch (exception: Exception) {
            IncidentReporter.report("$operation.onFailure", exception, severity)
        }
    }

    return null
}
