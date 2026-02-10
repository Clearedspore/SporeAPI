package me.clearedSpore.sporeAPI.menu.util.inventory

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

abstract class UtilItem {

    abstract fun item(): ItemStack
    abstract fun slot(): Int
    abstract fun onClick(player: Player, event: InventoryClickEvent)
}
