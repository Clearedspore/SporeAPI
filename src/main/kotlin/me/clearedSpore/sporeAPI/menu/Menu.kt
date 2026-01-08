package me.clearedSpore.sporeAPI.menu

import me.clearedSpore.sporeAPI.util.CC.red
import me.clearedSpore.sporeAPI.util.Task
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

abstract class Menu(protected val plugin: JavaPlugin) : InventoryHolder, Listener {

    private lateinit var inventory: Inventory

    protected val itemMap = mutableMapOf<Int, Item>()

    protected var autoRefreshOnClick: Boolean = true
    private var autoRefreshTask: BukkitRunnable? = null
    private var autoRefreshEnabled = true

    private val SPAM_MAX_CLICKS = 3
    private val SPAM_TIME_WINDOW_MS = 2000L

    var shouldReopen = false

    private val slotClickTimestamps: MutableMap<Int, MutableMap<UUID, MutableList<Long>>> = mutableMapOf()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    open fun fillEmptySlots(): Boolean = false
    open fun useInventory(): Boolean = false
    open fun cancelClicks(): Boolean = true
    open fun clickSound(): Sound = Sound.UI_BUTTON_CLICK

    private fun fillEmptySlotsWithGlass() {
        val grayPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(" ")
                addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            }
        }

        for (slot in 0 until inventory.size) {
            val current = inventory.getItem(slot)
            if (current == null || current.type == Material.AIR) {
                inventory.setItem(slot, grayPane)
            }
        }
    }

    fun startAutoRefresh() {
        stopAutoRefresh()

        if (!autoRefreshEnabled) return

        autoRefreshTask = object : BukkitRunnable() {
            override fun run() {
                if (!::inventory.isInitialized) return

                if (inventory.viewers.isNotEmpty()) {
                    inventory.viewers.filterIsInstance<Player>().forEach { player ->
                        refreshMenu(player)
                    }
                } else {
                    cancel()
                }
            }
        }

        autoRefreshTask?.runTaskTimer(plugin, 20L, 20L)
    }

    fun setAutoRefresh(enabled: Boolean) {
        autoRefreshEnabled = enabled
        if (!enabled) stopAutoRefresh()
    }

    fun stopAutoRefresh() {
        autoRefreshTask?.cancel()
        autoRefreshTask = null
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.topInventory.holder != this) return
        if (event.view.title != getMenuName()) return

        val slot = event.rawSlot
        val topSize = event.view.topInventory.size

        if (slot >= topSize && !useInventory()) {
            event.isCancelled = true
            return
        }

        val item = itemMap[slot]
        if (item != null) {

            if (item.spamCooldown()) {
                val playerClicks: MutableList<Long> = slotClickTimestamps
                    .computeIfAbsent(slot) { mutableMapOf() }
                    .computeIfAbsent(player.uniqueId) { mutableListOf<Long>() }

                val now = System.currentTimeMillis()
                playerClicks.removeIf { it < now - SPAM_TIME_WINDOW_MS }
                playerClicks.add(now)

                if (playerClicks.size > SPAM_MAX_CLICKS) {
                    player.sendMessage("You're clicking too fast! Please wait a moment.".red())
                    event.isCancelled = true
                    return
                }
            }

            event.isCancelled = item.cancelClick()

            try {
                shouldReopen = false
                item.onClickEvent(player, event.click)
                val updated = item.createItem()
                inventory.setItem(slot, updated)
                player.playSound(player.location, clickSound(), 0.5f, 1.0f)

                if (autoRefreshOnClick) refreshMenu(player)

            } catch (e: Exception) {
                player.sendMessage("An error occurred while handling your click.".red())
                e.printStackTrace()
            }

        } else if (slot in 0 until topSize) {
            event.isCancelled = cancelClicks()
        }
    }

    fun clearItems() {
        itemMap.clear()
        if (::inventory.isInitialized) inventory.clear()
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory.holder != this) return
        if (event.player !is Player) return

        val player = event.player as Player

        if (shouldReopen) {
            Task.run {
                this.open(player)
            }
        }

        onClose(player)
        stopAutoRefresh()
    }


    open fun onClose(player: Player) {}

    fun refreshMenu(player: Player? = null) {
        clearItems()
        setMenuItems()
        if (fillEmptySlots()) fillEmptySlotsWithGlass()
        if (player != null) {
            player.updateInventory()
        } else {
            inventory.viewers.filterIsInstance<Player>().forEach { it.updateInventory() }
        }
    }

    abstract fun getMenuName(): String
    abstract fun getRows(): Int
    abstract fun setMenuItems()

    fun open(player: Player) {
        inventory = Bukkit.createInventory(this, getRows() * 9, getMenuName())
        setMenuItems()
        if (fillEmptySlots()) fillEmptySlotsWithGlass()
        player.openInventory(inventory)
        startAutoRefresh()
    }

    fun setMenuItem(x: Int, y: Int, item: Item) {
        val slot = (y - 1) * 9 + (x - 1)
        inventory.setItem(slot, item.createItem())
        itemMap[slot] = item
    }

    fun reloadItems() {
        clearItems()
        setMenuItems()
    }

    fun updateMenuItem(x: Int, y: Int, item: Item) {
        setMenuItem(x, y, item)
    }

    override fun getInventory(): Inventory = inventory
}
