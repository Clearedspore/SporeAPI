package me.clearedSpore.sporeAPI.menu.util.inventory

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

abstract class UtilInventoryMenu(
    protected val viewer: Player,
    protected val target: Player?,
    protected val editable: Boolean,
    protected val autoRefresh: Boolean,
    protected val utilItems: MutableList<UtilItem>?,
    protected val plugin: JavaPlugin
) : Listener {
    
    protected lateinit var inventory: Inventory

    protected var refreshTask: BukkitTask? = null

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    protected abstract val previewContents: Array<ItemStack?>
    protected abstract val previewArmor: Array<ItemStack?>
    protected abstract val previewOffhand: ItemStack?

    protected abstract fun onInventoryUpdate(
        contents: Array<ItemStack?>,
        armor: Array<ItemStack?>,
        offhand: ItemStack?
    )

    fun open() {
        inventory = Bukkit.createInventory(null, 54, this.title!!)
        redraw()
        viewer.openInventory(inventory)

        if (autoRefresh) {
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { this.redraw() }, 1L, 10L)
        }
    }

    protected abstract val title: String?

    protected fun redraw() {
        val contents = previewContents
        val armor = previewArmor
        val offhand = previewOffhand

        for (i in 0 until 27) inventory.setItem(i, contents[i])
        for (i in 27..35) inventory.setItem(i, contents[i])

        inventory.setItem(36, armor[0])
        inventory.setItem(37, armor[1])
        inventory.setItem(38, armor[2])
        inventory.setItem(39, armor[3])
        inventory.setItem(41, offhand)

        for (i in 45..53) inventory.setItem(i, null)

        utilItems?.forEach { item ->
            inventory.setItem(item.slot(), item.item())
        }
    }

    protected fun isInventorySlot(slot: Int): Boolean {
        return slot <= 41 && slot != 40
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.getInventory() == null || event.getInventory() != inventory) return

        val slot = event.getRawSlot()

        if (slot >= 45) {
            if (utilItems != null) {
                for (item in utilItems) {
                    if (item.slot() == slot) {
                        event.isCancelled = true
                        item.onClick(viewer, event)
                        return
                    }
                }
            }
            event.isCancelled = true
            return
        }

        if (!editable || !isInventorySlot(slot)) {
            event.isCancelled = true
            return
        }

        Bukkit.getScheduler().runTaskLater(plugin, Runnable { this.syncBack() }, 1L)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory == null || event.inventory != inventory) return

        for (slot in event.rawSlots) {
            if (!isInventorySlot(slot)) {
                event.isCancelled = true
                return
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, Runnable { this.syncBack() }, 1L)
    }

    protected fun syncBack() {
        val contents = arrayOfNulls<ItemStack>(36)
        val armor = arrayOfNulls<ItemStack>(4)

        for (i in 0..26) contents[i] = inventory.getItem(i)
        for (i in 27..35) contents[i] = inventory.getItem(i)

        armor[0] = inventory.getItem(36)
        armor[1] = inventory.getItem(37)
        armor[2] = inventory.getItem(38)
        armor[3] = inventory.getItem(39)

        val offhand = inventory.getItem(41)

        onInventoryUpdate(contents, armor, offhand)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.getInventory() != inventory) return

        if (refreshTask != null) refreshTask!!.cancel()
        HandlerList.unregisterAll(this)
    }
}
