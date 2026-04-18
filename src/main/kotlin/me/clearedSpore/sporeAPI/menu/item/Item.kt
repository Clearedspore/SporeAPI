package me.clearedSpore.sporeAPI.menu.item

import me.clearedSpore.sporeAPI.util.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class Item {

    abstract fun onClickEvent(clicker: Player, clickType: ClickType)

    open fun spamCooldown(): Boolean = true
    open fun cancelClick(): Boolean = true

    abstract fun createItem(): ItemStack

    open fun buildItem(): ItemStack {
        val built = createItem()
        return built
    }

    fun asBuilder(builder: ItemBuilder.() -> Unit): ItemStack {
        val b = ItemBuilder(Material.STONE)
        b.builder()
        return b.build()
    }
}