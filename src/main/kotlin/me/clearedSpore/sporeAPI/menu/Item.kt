package me.clearedSpore.sporeAPI.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

abstract class Item {
    abstract fun createItem(): ItemStack
    abstract fun onClickEvent(clicker: Player, clickType: ClickType)

    open fun spamCooldown(): Boolean = true
    open fun cancelClick(): Boolean = true

    fun refresh(): ItemStack = createItem()
}
