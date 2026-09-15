package me.clearedSpore.sporeAPI.dialog

import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines.launch
import me.clearedSpore.sporeAPI.debug.runDebugSuspending
import org.bukkit.entity.Player

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


fun noticeDialog(
    title: String,
    text: String,
    button: String = "Back",
    width: Int = 300,
    back: (() -> SporeDialog)? = null
): SporeDialog = dialog("notice") {
    title(title)
    message(text, width = width)
    okButton(button) { click -> if (back != null) click.open(back()) else click.close() }
}

fun openLoaded(
    viewer: Player,
    title: String,
    text: String,
    operation: String,
    back: (() -> SporeDialog)? = null,
    width: Int = 300,
    load: suspend () -> SporeDialog
) {
    val loading = dialog("$operation.loading") {
        title(title)
        message(text, width = width)
        okButton("<gray>Cancel") { click -> if (back != null) click.open(back()) else click.close() }
    }

    viewer.openDialog(loading)

    launch {
        var ref: String? = null

        val next = runDebugSuspending(
            operation,
            details = mapOf("player" to viewer.name),
            onFailure = { ref = it.id }
        ) { load() } ?: noticeDialog(title, "<s_red>Something went wrong loading this (ref $ref).", width = width, back = back)

        if (!viewer.isOnline || SporeDialogs.current(viewer) !== loading) return@launch

        viewer.openDialog(next)
    }
}
