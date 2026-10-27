package me.clearedSpore.sporeAPI.debug

import me.clearedSpore.sporeAPI.util.CC.mm
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class Timing(val name: String) {

    data class Step(val name: String, val duration: Duration, val error: Throwable?) {
        val failed: Boolean get() = error != null
    }

    private val startedAt = System.nanoTime()
    private val recorded = CopyOnWriteArrayList<Step>()

    val steps: List<Step> get() = recorded.toList()

    val total: Duration get() = recorded.fold(Duration.ZERO) { sum, step -> sum + step.duration }

    val elapsed: Duration get() = (System.nanoTime() - startedAt).nanoseconds

    val failed: List<Step> get() = recorded.filter { it.failed }

    fun <T> step(name: String, block: () -> T): T? {
        val start = System.nanoTime()

        return try {
            block().also { record(name, start, null) }
        } catch (exception: Exception) {
            fail(name, start, exception)
        }
    }

    suspend fun <T> stepSuspend(name: String, block: suspend () -> T): T? {
        val start = System.nanoTime()

        return try {
            block().also { record(name, start, null) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            fail(name, start, exception)
        }
    }

    fun summary(): Component = summaryText().mm().hoverEvent(HoverEvent.showText(breakdownText().mm()))

    fun summaryText(): String {
        val failures = recorded.count { it.failed }
        val failedPart = if (failures > 0) " <s_red>($failures failed)" else ""

        return "<s_blue>${esc(name)} <gray>took <white>${format(total)}$failedPart"
    }

    fun breakdownText(): String {
        val steps = this.steps
        val slowest = steps.filterNot { it.failed }.maxByOrNull { it.duration }

        return buildString {
            append("<s_blue>").append(esc(name)).append("<newline>")

            steps.forEach { step ->
                append("<newline>")
                when {
                    step.failed -> append("<s_red>✗ ").append(esc(step.name)).append(" <gray>")
                        .append(format(step.duration)).append(" <dark_gray>(")
                        .append(esc(step.error!!.javaClass.simpleName)).append(")")

                    step === slowest && steps.size > 1 -> append("<gold>").append(esc(step.name)).append(" <white>")
                        .append(format(step.duration)).append(" <gray>slowest")

                    else -> append("<gray>").append(esc(step.name)).append(" <white>").append(format(step.duration))
                }
            }

            append("<newline><newline><gray>Total <white>").append(format(total))
        }
    }

    private fun record(name: String, start: Long, error: Throwable?) {
        recorded += Step(name, (System.nanoTime() - start).nanoseconds, error)
    }

    private fun fail(name: String, start: Long, exception: Exception): Nothing? {
        record(name, start, exception)
        IncidentReporter.report("timing.${this.name}.$name", exception)
        return null
    }

    private fun esc(text: String): String = MiniMessage.miniMessage().escapeTags(text)

    companion object {

        fun format(duration: Duration): String {
            val millis = duration.inWholeMicroseconds / 1000.0

            return when {
                millis < 10 -> "%.2fms".format(millis)
                millis < 1000 -> "${millis.toLong()}ms"
                else -> "%.2fs".format(millis / 1000)
            }
        }
    }
}

object Timings {

    data class Stats(val count: Long, val total: Duration, val max: Duration) {
        val average: Duration get() = if (count == 0L) Duration.ZERO else total / count.toDouble()
    }

    private val stats = ConcurrentHashMap<String, Stats>()

    inline fun <T> timed(name: String, block: () -> T): T {
        val start = System.nanoTime()

        try {
            return block()
        } finally {
            record(name, (System.nanoTime() - start).nanoseconds)
        }
    }

    fun record(name: String, duration: Duration) {
        stats.merge(name, Stats(1, duration, duration)) { old, new ->
            Stats(old.count + new.count, old.total + new.total, maxOf(old.max, new.max))
        }
    }

    fun stats(): Map<String, Stats> = stats.toMap()

    fun slowest(limit: Int = 5): List<Pair<String, Stats>> =
        stats.entries.sortedByDescending { it.value.average }.take(limit).map { it.key to it.value }

    fun clear() = stats.clear()
}
