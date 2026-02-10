package me.clearedSpore.sporeAPI.menu.util.inventory

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

abstract class AbstractInventoryMenu(
    viewer: Player,
    editable: Boolean = false,
    autoRefresh: Boolean = false,
    utilItems: MutableList<UtilItem>? = null,
    plugin: JavaPlugin
) : UtilInventoryMenu(viewer, editable, autoRefresh, utilItems, plugin) {

    abstract val inventoryContents: Array<ItemStack?>
    abstract val armorContents: Array<ItemStack?>
    abstract val offhandItem: ItemStack?

    override val previewContents: Array<ItemStack?> get() = inventoryContents
    override val previewArmor: Array<ItemStack?> get() = armorContents
    override val previewOffhand: ItemStack? get() = offhandItem
}
