package me.clearedSpore.sporeAPI.scoreboard

import io.papermc.paper.scoreboard.numbers.NumberFormat
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines.launchAsync
import me.clearedSpore.sporeAPI.coroutine.withRunCtx
import me.clearedSpore.sporeAPI.event.on
import me.clearedSpore.sporeAPI.task.Tasks
import me.clearedSpore.sporeAPI.util.Logger
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SidebarManager {

    const val MAX_LINES = 15

    private const val OBJECTIVE_NAME = "spore_sidebar"

    private val ENTRIES: List<String> = (0 until MAX_LINES).map { "§${"0123456789abcde"[it]}" }

    private val sessions = ConcurrentHashMap<UUID, Session>()

    @Volatile
    private var task: BukkitTask? = null

    @Volatile
    private var listenerRegistered = false

    @Volatile
    var takeOverExisting: Boolean = false

    val activeCount: Int get() = sessions.size

    fun show(player: Player, sidebar: Sidebar) {
        val existing = sessions[player.uniqueId]

        if (existing != null) {
            existing.sidebar = sidebar
            existing.reset()
            existing.refreshNow()
            return
        }

        if (!canTakeOver(player)) {
            Logger.warn(
                "Not showing a sidebar to ${player.name} - another plugin already owns their " +
                    "scoreboard. Set SidebarManager.takeOverExisting = true to override."
            )
            return
        }

        val session = Session(player, sidebar)
        sessions[player.uniqueId] = session

        player.scoreboard = session.board
        session.refreshNow()

        ensureRunning()
    }

    fun hide(player: Player) {
        val session = sessions.remove(player.uniqueId) ?: return
        session.detach()
    }

    fun refresh(player: Player) {
        sessions[player.uniqueId]?.refreshNow()
    }

    fun isShowing(player: Player): Boolean = sessions.containsKey(player.uniqueId)

    fun shutdown() {
        task?.cancel()
        task = null

        sessions.values.forEach { it.detach() }
        sessions.clear()
    }

    private fun canTakeOver(player: Player): Boolean {
        if (takeOverExisting) return true
        val main = Bukkit.getScoreboardManager().mainScoreboard
        return player.scoreboard === main
    }

    private fun ensureRunning() {
        if (task == null) {
            task = Tasks.runTimer(1L, 1L) { tick() }
        }

        if (!listenerRegistered) {
            listenerRegistered = true
            on<PlayerQuitEvent> { sessions.remove(it.player.uniqueId) }
        }
    }

    private fun tick() {
        if (sessions.isEmpty()) return

        sessions.values.forEach { session ->
            if (!session.player.isOnline) {
                sessions.remove(session.player.uniqueId)
                return@forEach
            }

            session.tick()
        }
    }

    private class Session(val player: Player, var sidebar: Sidebar) {

        val board: Scoreboard = Bukkit.getScoreboardManager().newScoreboard

        val objective: Objective = board
            .registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, Component.empty())
            .apply {
                displaySlot = DisplaySlot.SIDEBAR
                numberFormat(NumberFormat.blank())
            }

        var ticksUntilUpdate: Long = sidebar.updateIntervalTicks.coerceAtLeast(1L)

        private var ticksUntilFetch: Long = 0L
        private val fetching = AtomicBoolean(false)

        @Volatile
        private var snapshot: Any? = null

        private var lastTitle: Component? = null
        private var lastLines: List<Component> = emptyList()
        private var hidden = false

        fun reset() {
            lastTitle = null
            lastLines = emptyList()
            snapshot = null
        }

        fun tick() {
            val sidebar = sidebar

            if (sidebar is AsyncSidebar<*> && --ticksUntilFetch <= 0) {
                ticksUntilFetch = sidebar.fetchIntervalTicks.coerceAtLeast(1L)
                requestFetch(sidebar)
            }

            if (--ticksUntilUpdate > 0) return

            ticksUntilUpdate = sidebar.updateIntervalTicks.coerceAtLeast(1L)
            update()
        }

        fun refreshNow() {
            val sidebar = sidebar

            if (sidebar is AsyncSidebar<*>) {
                ticksUntilFetch = sidebar.fetchIntervalTicks.coerceAtLeast(1L)
                requestFetch(sidebar)
            }

            update()
        }

        private fun requestFetch(sidebar: AsyncSidebar<*>) {
            if (!fetching.compareAndSet(false, true)) return

            launchAsync {
                try {
                    val data = sidebar.fetchSnapshot(player)

                    withRunCtx {
                        if (sessions[player.uniqueId] === this@Session && this@Session.sidebar === sidebar) {
                            snapshot = data
                            update()
                        }
                    }
                } catch (ex: Throwable) {
                    Logger.warn("Sidebar fetch failed for ${player.name}: $ex")
                } finally {
                    fetching.set(false)
                }
            }
        }

        fun update() {
            if (!sidebar.shouldShow(player)) {
                if (!hidden) {
                    hidden = true
                    objective.displaySlot = null
                }
                return
            }

            if (hidden) {
                hidden = false
                objective.displaySlot = DisplaySlot.SIDEBAR
            }

            val data = snapshot
            if (sidebar is AsyncSidebar<*> && data == null) return

            val title = sidebar.titleFor(player, data)
            if (title != lastTitle) {
                objective.displayName(title)
                lastTitle = title
            }

            val lines = sidebar.linesFor(player, data).take(MAX_LINES)
            if (lines == lastLines) return

            for (index in lines.size until lastLines.size) {
                board.resetScores(ENTRIES[index])
            }

            lines.forEachIndexed { index, line ->
                val entry = ENTRIES[index]
                val team = board.getTeam(teamName(index))
                    ?: board.registerNewTeam(teamName(index)).apply { addEntry(entry) }

                if (index >= lastLines.size || lastLines[index] != line) {
                    team.prefix(line)
                }

                if (index >= lastLines.size) {
                    objective.getScore(entry).score = MAX_LINES - index
                }
            }

            lastLines = lines
        }

        fun detach() {
            if (!player.isOnline) return
            if (player.scoreboard !== board) return

            player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        }

        private fun teamName(index: Int) = "spore_$index"
    }
}
