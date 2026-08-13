package me.clearedSpore.sporeAPI.menu.invui

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.window.Window

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


fun Gui.open(player: Player, title: String) {
    Window.builder()
        .setUpperGui(this)
        .setTitle(title)
        .open(player)
}

fun Gui.open(player: Player, title: Component) {
    Window.builder()
        .setUpperGui(this)
        .setTitle(title)
        .open(player)
}

fun Gui.openSplit(player: Player, title: String, lower: Gui) {
    Window.builder()
        .setUpperGui(this)
        .setLowerGui(lower)
        .setTitle(title)
        .open(player)
}

fun Gui.openSplit(player: Player, title: Component, lower: Gui) {
    Window.builder()
        .setUpperGui(this)
        .setLowerGui(lower)
        .setTitle(title)
        .open(player)
}