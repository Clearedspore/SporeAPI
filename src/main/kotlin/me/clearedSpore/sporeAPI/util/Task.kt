package me.clearedSpore.sporeAPI.util

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.*
import java.util.function.Supplier

object Task {

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var plugin: JavaPlugin? = null
    private val repeatingTasks = ConcurrentHashMap<Any, ScheduledFuture<*>>()

    fun initialize(pluginInstance: JavaPlugin) { plugin = pluginInstance }

    fun runAsync(task: Runnable): CompletableFuture<Void> = CompletableFuture.runAsync(task)
    fun <T> supplyAsync(task: Supplier<T>): CompletableFuture<T> = CompletableFuture.supplyAsync(task)

    fun runOnNextTick(task: Runnable) = scheduler.schedule(task, 50, TimeUnit.MILLISECONDS)
    fun runDelayed(task: Runnable, delay: Long, unit: TimeUnit) = scheduler.schedule(task, delay, unit)
    fun runRepeated(task: Runnable, initialDelay: Long, period: Long, unit: TimeUnit) =
        scheduler.scheduleAtFixedRate(task, initialDelay, period, unit)

    fun runRepeated(key: Any, task: Runnable, initialDelay: Long, period: Long, unit: TimeUnit) {
        cancel(key)
        repeatingTasks[key] = scheduler.scheduleAtFixedRate(task, initialDelay, period, unit)
    }

    fun cancel(key: Any) { repeatingTasks.remove(key)?.cancel(false) }

    fun runWithFixedDelay(task: Runnable, initialDelay: Long, delay: Long, unit: TimeUnit) =
        scheduler.scheduleWithFixedDelay(task, initialDelay, delay, unit)

    fun runTask(task: Runnable) = plugin?.let { Bukkit.getScheduler().runTask(it, task) }
        ?: throw IllegalStateException("Task class not initialized with a plugin instance.")

    fun shutdown() = scheduler.shutdown()
}
