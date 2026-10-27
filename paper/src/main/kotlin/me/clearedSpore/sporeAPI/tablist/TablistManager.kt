package me.clearedSpore.sporeAPI.tablist

import kotlinx.coroutines.CancellationException
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines.launchAsync
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.event.on
import me.clearedSpore.sporeAPI.task.SporeScheduledTask
import me.clearedSpore.sporeAPI.task.Tasks
import me.clearedSpore.sporeAPI.util.FoliaUtil
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object TablistManager {

    private val sessions = ConcurrentHashMap<UUID, Session>()
    private val spread = AtomicLong()

    @Volatile
    private var adapter: Tablist? = null

    @Volatile
    private var task: SporeScheduledTask? = null

    @Volatile
    private var listenersRegistered = false

    val activeCount: Int get() = sessions.size

    val current: Tablist? get() = adapter

    fun use(tablist: Tablist) {
        adapter = tablist
        ensureRunning()

        Tasks.run {
            Bukkit.getOnlinePlayers().forEach(::attach)
            sessions.values.forEach { it.refreshNow() }
        }
    }

    fun clear() {
        adapter = null
        sessions.values.forEach { it.detach() }
        sessions.clear()
    }

    fun refresh(player: Player) {
        sessions[player.uniqueId]?.refreshNow()
    }

    fun refreshAll() {
        sessions.values.forEach { it.refreshNow() }
    }

    fun shutdown() {
        task?.cancel()
        task = null
        clear()
    }

    private fun attach(player: Player) {
        if (adapter == null || sessions.containsKey(player.uniqueId)) return
        sessions[player.uniqueId] = Session(player)
    }

    private fun ensureRunning() {
        if (task == null) {
            task = Tasks.runTimer(1L, 1L) { tick() }
        }

        if (!listenersRegistered) {
            listenersRegistered = true
            on<PlayerJoinEvent> { event ->
                attach(event.player)
                sessions[event.player.uniqueId]?.refreshNow()
            }
            on<PlayerQuitEvent> { sessions.remove(it.player.uniqueId) }
        }
    }

    private fun tick() {
        val tablist = adapter ?: return
        if (sessions.isEmpty()) return

        sessions.values.forEach { session ->
            if (!session.player.isOnline) {
                sessions.remove(session.player.uniqueId)
                return@forEach
            }

            session.tick(tablist)
        }
    }

    private fun onPlayerThread(player: Player, block: () -> Unit) {
        if (FoliaUtil.isFolia) Tasks.runEntity(player, block) else block()
    }

    private class Session(val player: Player) {

        private val slot: Long = spread.getAndIncrement()
        private var ticksUntilUpdate: Long = -1L
        private var ticksUntilFetch: Long = 0L
        private val fetching = AtomicBoolean(false)

        @Volatile
        private var snapshot: Any? = null

        @Volatile
        private var snapshotOf: Tablist? = null

        private var lastHeader: Component? = null
        private var lastFooter: Component? = null
        private var lastName: Component? = null
        private var lastOrder: Int? = null
        private var hidden = false

        fun tick(tablist: Tablist) {
            if (tablist is AsyncTablist<*> && --ticksUntilFetch <= 0) {
                ticksUntilFetch = tablist.fetchIntervalTicks.coerceAtLeast(1L)
                requestFetch(tablist)
            }

            val interval = tablist.updateIntervalTicks.coerceAtLeast(1L)
            if (ticksUntilUpdate < 0) ticksUntilUpdate = slot % interval + 1
            if (--ticksUntilUpdate > 0) return

            ticksUntilUpdate = interval
            onPlayerThread(player) { render(tablist) }
        }

        fun refreshNow() {
            val tablist = adapter ?: return

            if (tablist is AsyncTablist<*>) {
                ticksUntilFetch = tablist.fetchIntervalTicks.coerceAtLeast(1L)
                requestFetch(tablist)
            }

            onPlayerThread(player) { render(tablist) }
        }

        private fun requestFetch(tablist: AsyncTablist<*>) {
            if (!fetching.compareAndSet(false, true)) return

            launchAsync {
                try {
                    val data = tablist.fetchSnapshot(player)
                    val first = snapshotOf !== tablist
                    snapshot = data
                    snapshotOf = tablist

                    if (first && player.isOnline) Tasks.runEntity(player, { render(tablist) })
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    IncidentReporter.report(
                        "tablist.fetch", exception, Severity.WARNING,
                        mapOf("player" to player.name, "tablist" to tablist.javaClass.simpleName)
                    )
                } finally {
                    fetching.set(false)
                }
            }
        }

        private fun render(tablist: Tablist) {
            if (!player.isOnline || adapter !== tablist) return

            try {
                if (!tablist.shouldShow(player)) {
                    if (!hidden) {
                        hidden = true
                        sendHeaderFooter(Component.empty(), Component.empty())
                    }
                } else {
                    hidden = false
                    renderHeaderFooter(tablist)
                }

                val name = tablist.name(player)
                if (name != lastName) {
                    player.playerListName(name)
                    lastName = name
                }

                val order = tablist.order(player)
                if (order != lastOrder) {
                    player.playerListOrder = order ?: 0
                    lastOrder = order
                }
            } catch (exception: Exception) {
                IncidentReporter.report(
                    "tablist.render", exception, Severity.WARNING,
                    mapOf("player" to player.name, "tablist" to tablist.javaClass.simpleName)
                )
            }
        }

        private fun renderHeaderFooter(tablist: Tablist) {
            val data = if (snapshotOf === tablist) snapshot else null
            if (tablist is AsyncTablist<*> && data == null) return

            sendHeaderFooter(tablist.headerFor(player, data), tablist.footerFor(player, data))
        }

        private fun sendHeaderFooter(header: Component, footer: Component) {
            if (header == lastHeader && footer == lastFooter) return

            player.sendPlayerListHeaderAndFooter(header, footer)
            lastHeader = header
            lastFooter = footer
        }

        fun detach() {
            if (!player.isOnline) return

            runCatching {
                onPlayerThread(player) {
                    player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty())
                    player.playerListName(null)
                    player.playerListOrder = 0
                }
            }
        }
    }
}
