package me.clearedSpore.sporeAPI.menu.util.inventory

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

abstract class AbstractPlayerInventoryMenu(
    viewer: Player,
    open val target: Player,
    editable: Boolean = false,
    autoRefresh: Boolean = false,
    utilItems: MutableList<UtilItem>? = null,
    plugin: JavaPlugin
) : UtilInventoryMenu(viewer, editable, autoRefresh, utilItems, plugin) {

    override val previewContents: Array<ItemStack?> get() = target.inventory.contents
    override val previewArmor: Array<ItemStack?> get() = target.inventory.armorContents
    override val previewOffhand: ItemStack? get() = target.inventory.itemInOffHand

    override fun onInventoryUpdate(contents: Array<ItemStack?>, armor: Array<ItemStack?>, offhand: ItemStack?) {
        target.inventory.contents = contents
        target.inventory.armorContents = armor
        target.inventory.setItemInOffHand(offhand)
        target.updateInventory()
    }
}
