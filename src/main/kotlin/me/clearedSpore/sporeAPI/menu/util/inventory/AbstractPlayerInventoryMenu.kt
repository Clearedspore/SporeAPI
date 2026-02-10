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

    private val menuContents: Array<ItemStack?> = Array(36) { target.inventory.getItem(it) }
    private val menuArmor: Array<ItemStack?> = Array(4) { target.inventory.armorContents[it] }
    private var menuOffhand: ItemStack? = target.inventory.itemInOffHand

    override val previewContents: Array<ItemStack?> get() = menuContents
    override val previewArmor: Array<ItemStack?> get() = menuArmor
    override val previewOffhand: ItemStack? get() = menuOffhand

    override fun redraw() {
        for (i in 0..8) inventory.setItem(i, menuContents[i] ?: placeholderItem)
        for (i in 9..17) inventory.setItem(i, menuContents[i] ?: placeholderItem)
        for (i in 18..26) inventory.setItem(i, menuContents[i] ?: placeholderItem)
        for (i in 27..35) inventory.setItem(i, menuContents[i] ?: placeholderItem)

        for (i in 36..39) inventory.setItem(i, menuArmor[i - 36] ?: getArmorPlaceholder(i))
        inventory.setItem(41, menuOffhand ?: getOffhandPlaceholder())
        for (i in 45..53) inventory.setItem(i, null)
        utilItems?.forEach { item ->
            inventory.setItem(item.slot(), item.item())
        }
    }

    override fun onInventoryUpdate(contents: Array<ItemStack?>, armor: Array<ItemStack?>, offhand: ItemStack?) {
        for (i in contents.indices) menuContents[i] = contents[i]
        for (i in armor.indices) menuArmor[i] = armor[i]
        menuOffhand = offhand
    }
}
