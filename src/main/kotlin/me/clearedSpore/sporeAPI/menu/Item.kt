package me.clearedSpore.sporeAPI.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

abstract class Item {
    private var cachedItem: ItemStack? = null

    abstract fun createItem(): ItemStack
    abstract fun onClickEvent(clicker: Player, clickType: ClickType)

    open fun cancelClick(): Boolean = true

    fun updateItem(): ItemStack {
        if (cachedItem == null) {
            cachedItem = createItem()
            return cachedItem!!
        }

        val updatedItem = createItem()
        if (cachedItem!!.type != updatedItem.type) {
            cachedItem = updatedItem
            return cachedItem!!
        }

        val cachedMeta = cachedItem!!.itemMeta
        val updatedMeta = updatedItem.itemMeta

        if (cachedMeta != null && updatedMeta != null) {
            if (updatedMeta.hasDisplayName()) {
                cachedMeta.setDisplayName(updatedMeta.displayName)
            } else if (cachedMeta.hasDisplayName()) {
                cachedMeta.setDisplayName(null)
            }

            if (updatedMeta.hasLore()) {
                cachedMeta.lore = updatedMeta.lore
            } else if (cachedMeta.hasLore()) {
                cachedMeta.lore = null
            }

            if (updatedMeta.hasCustomModelData()) {
                cachedMeta.setCustomModelData(updatedMeta.customModelData)
            } else if (cachedMeta.hasCustomModelData()) {
                cachedMeta.setCustomModelData(null)
            }

            cachedMeta.enchants.keys.forEach { cachedMeta.removeEnchant(it) }
            updatedMeta.enchants.forEach { (enchant, level) ->
                cachedMeta.addEnchant(enchant, level, true)
            }

            cachedMeta.addItemFlags(*updatedMeta.itemFlags.toTypedArray())
            cachedItem!!.itemMeta = cachedMeta
        } else if (updatedMeta != null) {
            cachedItem!!.itemMeta = updatedMeta
        }

        cachedItem!!.amount = updatedItem.amount
        return cachedItem!!
    }

    fun forceRefresh(): ItemStack {
        cachedItem = null
        return updateItem()
    }

    fun getCachedItem(): ItemStack? = cachedItem
    open fun needsRefresh(): Boolean = false
}
