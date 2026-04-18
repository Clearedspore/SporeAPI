package me.clearedSpore.sporeAPI.menu

import me.clearedSpore.sporeAPI.menu.item.Item
import me.clearedSpore.sporeAPI.task.Tasks
import me.clearedSpore.sporeAPI.util.CC.red
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.



abstract class Menu(protected val plugin: JavaPlugin) : InventoryHolder, Listener {

    private lateinit var inventory: Inventory
    protected val itemMap = mutableMapOf<Int, Item>()

    var shouldReopen = false

    protected var autoRefreshOnClick = true
    private var autoRefreshTask: BukkitRunnable? = null
    private var autoRefreshEnabled = true

    private val SPAM_MAX = 3
    private val SPAM_WINDOW = 2000L

    private val clickLog = ConcurrentHashMap<Int, ConcurrentHashMap<UUID, MutableList<Long>>>()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    abstract fun getMenuName(): String
    abstract fun getRows(): Int
    abstract fun setMenuItems()

    open fun fillEmptySlots(): Boolean = false
    open fun useInventory(): Boolean = false
    open fun cancelClicks(): Boolean = true
    open fun clickSound(): Sound = Sound.UI_BUTTON_CLICK

    fun open(player: Player) {
        if (!::inventory.isInitialized) {
            inventory = Bukkit.createInventory(this, getRows() * 9, getMenuName())
        }

        inventory.clear()
        itemMap.clear()

        setMenuItems()

        if (fillEmptySlots()) fillGlass()

        player.openInventory(inventory)
        startAutoRefresh()
    }

    private fun fillGlass() {
        val pane = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(" ")
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }

        for (i in 0 until inventory.size) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, pane)
            }
        }
    }

    fun setMenuItem(x: Int, y: Int, item: Item) {
        val slot = (y - 1) * 9 + (x - 1)
        inventory.setItem(slot, item.createItem())
        itemMap[slot] = item
    }

    fun refresh(player: Player? = null) {
        inventory.clear()
        itemMap.clear()

        setMenuItems()
        if (fillEmptySlots()) fillGlass()

        player?.updateInventory()
            ?: inventory.viewers.filterIsInstance<Player>().forEach { it.updateInventory() }
    }

    fun startAutoRefresh() {
        stopAutoRefresh()

        if (!autoRefreshEnabled) return

        autoRefreshTask = object : BukkitRunnable() {
            override fun run() {
                if (!::inventory.isInitialized) return

                if (inventory.viewers.isEmpty()) {
                    cancel()
                    return
                }

                refresh()
            }
        }

        autoRefreshTask!!.runTaskTimer(plugin, 20L, 20L)
    }

    fun stopAutoRefresh() {
        autoRefreshTask?.cancel()
        autoRefreshTask = null
    }

    fun setAutoRefresh(enabled: Boolean) {
        autoRefreshEnabled = enabled
        if (!enabled) stopAutoRefresh()
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.inventory.holder != this) return

        val slot = event.rawSlot
        val topSize = event.view.topInventory.size

        if (slot >= topSize) {
            if (!useInventory()) event.isCancelled = true
            return
        }

        val item = itemMap[slot] ?: run {
            if (slot in 0 until topSize) event.isCancelled = cancelClicks()
            return
        }

        if (isSpam(slot, player.uniqueId)) {
            player.sendMessage("You're clicking too fast!".red())
            event.isCancelled = true
            return
        }

        shouldReopen = false

        item.onClickEvent(player, event.click)

        player.playSound(player.location, clickSound(), 0.5f, 1.0f)

        if (autoRefreshOnClick) refresh(player)
        event.isCancelled = item.cancelClick()
    }

    private fun isSpam(slot: Int, uuid: UUID): Boolean {
        val map = clickLog.computeIfAbsent(slot) { ConcurrentHashMap() }
        val list = map.computeIfAbsent(uuid) { mutableListOf() }

        val now = System.currentTimeMillis()
        list.removeIf { it < now - SPAM_WINDOW }
        list.add(now)

        return list.size > SPAM_MAX
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.inventory.holder != this) return
        if (event.player !is Player) return

        val player = event.player as Player

        if (shouldReopen) {
            Tasks.run { open(player) }
        }

        onClose(player)
        stopAutoRefresh()
    }

    open fun onClose(player: Player) {}

    override fun getInventory(): Inventory = inventory
}