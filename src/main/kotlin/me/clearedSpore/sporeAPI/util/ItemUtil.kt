package me.clearedSpore.sporeAPI.util

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionData
import org.bukkit.potion.PotionType

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object ItemUtil {

    fun getAllObtainableItems(): List<ItemStack> {
        val items = mutableListOf<ItemStack>()

        Material.values().forEach { mat ->
            if (!mat.isItem) return@forEach
            val name = mat.name
            if (listOf("AIR", "BEDROCK", "BARRIER", "SPAWN_EGG", "END_PORTAL_FRAME", "COMMAND")
                    .any { it in name }) return@forEach
            items.add(ItemStack(mat))
        }

        val potionContainers = listOf(
            Material.POTION, Material.SPLASH_POTION,
            Material.LINGERING_POTION, Material.TIPPED_ARROW
        )

        PotionType.values().forEach { type ->
            potionContainers.forEach { container ->
                try {
                    items.add(createPotion(container, type))
                    if (type.isExtendable) items.add(createPotion(container, type, extended = true))
                    if (type.isUpgradeable) items.add(createPotion(container, type, upgraded = true))
                } catch (_: IllegalArgumentException) {}
            }
        }

        Enchantment.values().forEach { enchant ->
            for (level in 1..enchant.maxLevel) {
                val book = ItemStack(Material.ENCHANTED_BOOK)
                val meta = book.itemMeta as? EnchantmentStorageMeta ?: continue
                meta.addStoredEnchant(enchant, level, true)
                book.itemMeta = meta
                items.add(book)
            }
        }

        return items
    }

    private fun createPotion(container: Material, type: PotionType, extended: Boolean = false, upgraded: Boolean = false): ItemStack {
        val potion = ItemStack(container)
        val meta = potion.itemMeta as? PotionMeta
        meta?.setBasePotionData(PotionData(type, extended, upgraded))
        potion.itemMeta = meta
        return potion
    }

    fun formatMaterialName(material: Material): String =
        material.name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

    fun addLore(item: ItemStack, vararg loreLines: String): ItemStack {
        val meta = item.itemMeta ?: return item
        val lore = meta.lore?.toMutableList() ?: mutableListOf()
        loreLines.forEach { lore.add(ChatColor.translateAlternateColorCodes('&', it)) }
        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    fun setDisplayName(item: ItemStack, name: String): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name))
        item.itemMeta = meta
        return item
    }

    fun isSimilarIgnoreAmount(a: ItemStack?, b: ItemStack?): Boolean {
        if (a == null || b == null) return false
        return a.clone().apply { amount = 1 }.isSimilar(b.clone().apply { amount = 1 })
    }

    fun giveOrDrop(player: Player, item: ItemStack) {
        if (player.inventory.firstEmpty() != -1) {
            player.inventory.addItem(item)
        } else {
            player.world.dropItemNaturally(player.location, item)
        }
    }

    fun createEnchantedBook(enchantment: Enchantment, level: Int): ItemStack {
        val book = ItemStack(Material.ENCHANTED_BOOK)
        val meta = book.itemMeta as? EnchantmentStorageMeta
        meta?.addStoredEnchant(enchantment, level, true)
        book.itemMeta = meta
        return book
    }
}
