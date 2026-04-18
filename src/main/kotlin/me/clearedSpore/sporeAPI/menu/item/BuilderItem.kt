package me.clearedSpore.sporeAPI.menu.item

import me.clearedSpore.sporeAPI.util.ItemBuilder
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class BuilderItem(
    private val builder: ItemBuilder.() -> Unit,
    private val click: (Player, ClickType) -> Unit
) : Item() {

    private val base = ItemBuilder(org.bukkit.Material.STONE)

    override fun createItem(): ItemStack {
        return base.apply(builder).build()
    }

    override fun onClickEvent(clicker: Player, clickType: ClickType) {
        click(clicker, clickType)
    }
}