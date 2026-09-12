package me.clearedSpore.sporeAPI.dialog

import io.papermc.paper.dialog.DialogResponseView
import org.bukkit.entity.Player

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class SporeDialog {

    open val name: String
        get() = javaClass.simpleName.ifEmpty { "dialog" }

    abstract fun DialogBuilder.build(player: Player)

    open fun onClose(player: Player, reason: DialogCloseReason) {}

    fun open(player: Player) = SporeDialogs.open(player, this)

    fun line(label: String, value: String) = "<s_blue>$label: <white>$value"
}

class DialogClick internal constructor(
    val player: Player,
    val dialog: SporeDialog,
    val response: DialogResponseView?
) {

    operator fun <T> get(field: DialogField<T>): T = field.read(response)

    fun close() = SporeDialogs.close(player)

    fun reopen() = SporeDialogs.reopen(player)

    fun open(other: SporeDialog) = SporeDialogs.open(player, other)
}

fun dialog(name: String = "dialog", content: DialogBuilder.(Player) -> Unit): SporeDialog {
    val dialogName = name

    return object : SporeDialog() {
        override val name: String = dialogName

        override fun DialogBuilder.build(player: Player) = content(player)
    }
}

fun Player.openDialog(dialog: SporeDialog) = SporeDialogs.open(this, dialog)

fun Player.openDialog(name: String = "dialog", content: DialogBuilder.(Player) -> Unit) =
    SporeDialogs.open(this, dialog(name, content))
