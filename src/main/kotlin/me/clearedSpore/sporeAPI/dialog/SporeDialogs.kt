package me.clearedSpore.sporeAPI.dialog

import io.papermc.paper.connection.PlayerGameConnection
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.dialog.DialogResponseView
import io.papermc.paper.event.player.PlayerCustomClickEvent
import io.papermc.paper.registry.data.dialog.action.DialogAction
import me.clearedSpore.sporeAPI.SporeApi
import me.clearedSpore.sporeAPI.debug.runDebug
import me.clearedSpore.sporeAPI.event.on
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeDialogs {

    private const val PREFIX = "dialog/"

    private val OWN_INVENTORY = setOf(InventoryType.CRAFTING, InventoryType.CREATIVE)

    private val sessions = ConcurrentHashMap<UUID, Session>()
    private val tokens = AtomicLong()

    private var listening = false

    private val namespace: String by lazy {
        SporeApi.plugin.name.lowercase().replace(Regex("[^a-z0-9_.-]"), "_")
    }

    private class Session(val dialog: SporeDialog, var builder: DialogBuilder, var token: Long) {
        var ended = false
    }

    fun open(player: Player, dialog: SporeDialog) {
        if (!Bukkit.isPrimaryThread()) return runOnMain { open(player, dialog) }
        listen()

        val token = tokens.incrementAndGet()
        val (builder, paper) = create(player, dialog, token) ?: return

        val previous = sessions.put(player.uniqueId, Session(dialog, builder, token))
        show(player, paper)
        previous?.let { finish(player, it, DialogCloseReason.REPLACED) }
    }

    fun reopen(player: Player) {
        if (!Bukkit.isPrimaryThread()) return runOnMain { reopen(player) }

        val session = sessions[player.uniqueId] ?: return
        val token = tokens.incrementAndGet()
        val (builder, paper) = create(player, session.dialog, token) ?: return

        session.builder = builder
        session.token = token
        show(player, paper)
    }

    fun close(player: Player) {
        if (!Bukkit.isPrimaryThread()) return runOnMain { close(player) }

        player.closeDialog()
        sessions.remove(player.uniqueId)?.let { finish(player, it, DialogCloseReason.CLOSED) }
    }

    fun isOpen(player: Player): Boolean = sessions.containsKey(player.uniqueId)

    fun current(player: Player): SporeDialog? = sessions[player.uniqueId]?.dialog

    fun shutdown() {
        sessions.keys.toList().forEach { id -> Bukkit.getPlayer(id)?.let { close(it) } }
        sessions.clear()
    }

    private fun create(player: Player, dialog: SporeDialog, token: Long): Pair<DialogBuilder, Dialog>? =
        runDebug("dialog.${dialog.name}.open", details = mapOf("player" to player.name)) {
            val builder = DialogBuilder()
            with(dialog) { builder.build(player) }

            builder to builder.toDialog { id -> DialogAction.customClick(Key.key(namespace, "$PREFIX$token/$id"), null) }
        }

    private fun show(player: Player, dialog: Dialog) {
        if (player.openInventory.type !in OWN_INVENTORY) player.closeInventory()
        player.showDialog(dialog)
    }

    private fun listen() {
        if (listening) return
        listening = true

        on<PlayerCustomClickEvent> { event -> onClick(event) }
        on<PlayerQuitEvent> { event ->
            sessions.remove(event.player.uniqueId)?.let { finish(event.player, it, DialogCloseReason.QUIT) }
        }
    }

    private fun onClick(event: PlayerCustomClickEvent) {
        val key = event.identifier
        if (key.namespace() != namespace || !key.value().startsWith(PREFIX)) return

        val player = (event.commonConnection as? PlayerGameConnection)?.player ?: return
        val token = key.value().removePrefix(PREFIX).substringBefore('/').toLongOrNull() ?: return
        val id = key.value().substringAfterLast('/')
        val response = event.dialogResponseView

        runOnMain { click(player, token, id, response) }
    }

    private fun click(player: Player, token: Long, id: String, response: DialogResponseView?) {
        val session = sessions[player.uniqueId]?.takeIf { it.token == token } ?: return
        val builder = session.builder
        val button = builder.buttonFor(id) ?: return
        val exiting = id == DialogBuilder.EXIT || id == DialogBuilder.NO

        button.handler?.let { handler ->
            runDebug(
                "dialog.${session.dialog.name}.click",
                details = mapOf("player" to player.name, "button" to plain(button.label))
            ) {
                handler(DialogClick(player, session.dialog, response))
            }
        }

        if (sessions[player.uniqueId] !== session || session.token != token) return

        when {
            exiting || builder.afterClick == AfterClick.CLOSE -> {
                if (builder.afterClick != AfterClick.CLOSE) player.closeDialog()
                sessions.remove(player.uniqueId, session)
                finish(player, session, if (exiting) DialogCloseReason.EXIT else DialogCloseReason.BUTTON)
            }
            builder.afterClick == AfterClick.WAIT_FOR_RESPONSE -> {
                player.closeDialog()
                sessions.remove(player.uniqueId, session)
                finish(player, session, DialogCloseReason.BUTTON)
            }
        }
    }

    private fun finish(player: Player, session: Session, reason: DialogCloseReason) {
        if (session.ended) return
        session.ended = true

        val operation = "dialog.${session.dialog.name}.close"
        val details = mapOf("player" to player.name, "reason" to reason.name)

        runDebug(operation, details = details) { session.dialog.onClose(player, reason) }
        session.builder.closeHandlers.forEach { handler -> runDebug(operation, details = details) { handler(reason) } }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) return block()

        val plugin = SporeApi.plugin
        if (plugin.isEnabled) Bukkit.getScheduler().runTask(plugin, Runnable { block() })
    }

    private fun plain(component: Component): String = PlainTextComponentSerializer.plainText().serialize(component)
}
