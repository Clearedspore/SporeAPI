package me.clearedSpore.sporeAPI.menu

import me.clearedSpore.sporeAPI.menu.item.BuilderItem
import me.clearedSpore.sporeAPI.menu.item.Item
import me.clearedSpore.sporeAPI.task.Tasks
import me.clearedSpore.sporeAPI.util.CC.red
import me.clearedSpore.sporeAPI.util.ItemBuilder
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
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.



abstract class BasePaginatedMenu(
    protected val plugin: JavaPlugin,
    private val footer: Boolean = false
) : InventoryHolder, Listener {

    private lateinit var inventory: Inventory

    private val sourceItems = mutableListOf<Item>()
    private val filteredItems = mutableListOf<Item>()

    private val fixedItems = mutableMapOf<Int, MutableMap<Int, Item>>()
    private val slotMap = mutableMapOf<Int, Item>()

    private val clickHistory = mutableMapOf<Int, MutableMap<UUID, MutableList<Long>>>()

    protected var page = 1
    protected var searchQuery = ""

    var shouldReopen = false
    var autoRefreshOnClick = true
    var useInventory = false
    var cancelClicks = true
    var autoRefreshEnabled = true

    private var refreshTask: org.bukkit.scheduler.BukkitTask? = null

    private val SPAM_MAX = 3
    private val SPAM_WINDOW = 2000L

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun addItem(builder: ItemBuilder.() -> Unit, click: (Player, ClickType) -> Unit) {
        addItem(BuilderItem(builder, click))
    }

    abstract fun getMenuName(): String
    abstract fun getRows(): Int
    abstract fun createItems()
    abstract fun onInventoryClick(player: Player, click: ClickType, event: InventoryClickEvent)

    open fun onClose(player: Player) {}

    override fun getInventory(): Inventory = inventory

    fun open(player: Player) {
        inventory = Bukkit.createInventory(this, getRows() * 9, getMenuName())
        createItems()
        applySearch()
        render()
        player.openInventory(inventory)
    }

    fun addItem(item: Item) {
        sourceItems.add(item)
        filteredItems.add(item)
    }

    fun clearItems() {
        sourceItems.clear()
        filteredItems.clear()
        slotMap.clear()
        fixedItems.clear()
    }

    fun nextPage() {
        if (page * itemsPerPage() < filteredItems.size) {
            page++
            render()
        }
    }

    fun previousPage() {
        if (page > 1) {
            page--
            render()
        }
    }

    fun setMenuItem(x: Int, y: Int, item: Item, global: Boolean = false) {
        val slot = (y - 1) * 9 + (x - 1)

        fixedItems.computeIfAbsent(if (global) -1 else page) { mutableMapOf() }[slot] = item

        if (::inventory.isInitialized) {
            inventory.setItem(slot, item.createItem())
        }
    }

    private fun itemsPerPage(): Int {
        return if (footer) (getRows() - 2) * 7 else getRows() * 9
    }

    private fun render() {
        if (!::inventory.isInitialized) return

        inventory.clear()
        slotMap.clear()

        placeFixed()
        placeItems()
        placeNavigation()
        if (footer) placeFooter()
    }

    private fun placeFixed() {
        fixedItems[-1]?.forEach { (slot, item) ->
            inventory.setItem(slot, item.createItem())
        }

        fixedItems[page]?.forEach { (slot, item) ->
            inventory.setItem(slot, item.createItem())
        }
    }

    private fun placeItems() {
        val start = (page - 1) * itemsPerPage()
        val end = minOf(start + itemsPerPage(), filteredItems.size)

        var slotIndex = 0

        for (i in start until end) {
            val item = filteredItems[i]
            val slot = findNextFreeSlot(slotIndex++)

            if (slot != -1) {
                inventory.setItem(slot, item.createItem())
                slotMap[slot] = item
            }
        }
    }

    private fun findNextFreeSlot(start: Int): Int {
        for (i in start until inventory.size) {
            if (!isFixed(i) && inventory.getItem(i) == null) return i
        }
        return -1
    }

    private fun isFixed(slot: Int): Boolean {
        return fixedItems[page]?.containsKey(slot) == true ||
                fixedItems[-1]?.containsKey(slot) == true
    }

    private fun placeNavigation() {
        val row = if (footer) getRows() - 2 else getRows() - 1

        inventory.setItem(row * 9, createPrev())
        inventory.setItem(row * 9 + 8, createNext())
    }

    private fun placeFooter() {
        val pane = ItemStack(Material.GRAY_STAINED_GLASS_PANE)

        for (i in 0 until inventory.size) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, pane)
            }
        }
    }

    private fun createPrev(): ItemStack {
        return ItemStack(Material.RED_CARPET).apply {
            itemMeta = itemMeta?.apply { setDisplayName("Previous".red()) }
        }
    }

    private fun createNext(): ItemStack {
        return ItemStack(Material.LIME_CARPET).apply {
            itemMeta = itemMeta?.apply { setDisplayName("Next".red()) }
        }
    }

    private fun handleSpam(slot: Int, player: Player): Boolean {
        val map = clickHistory.computeIfAbsent(slot) { mutableMapOf() }
        val list = map.computeIfAbsent(player.uniqueId) { mutableListOf() }

        val now = System.currentTimeMillis()

        list.removeIf { it < now - SPAM_WINDOW }
        list.add(now)

        return list.size > SPAM_MAX
    }

    fun applySearch() {
        val query = searchQuery.lowercase().trim()

        filteredItems.clear()

        if (query.isEmpty()) {
            filteredItems.addAll(sourceItems)
            page = 1
            return
        }

        filteredItems.addAll(
            sourceItems.filter { item ->
                val stack = item.createItem()
                val name = stack.itemMeta?.displayName ?: return@filter false

                name.lowercase().contains(query)
            }
        )

        page = 1
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.inventory.holder != this) return

        val slot = event.rawSlot

        if (slot >= inventory.size) {
            if (!useInventory) event.isCancelled = true
            return
        }

        val item = slotMap[slot] ?: return

        if (handleSpam(slot, player)) {
            player.sendMessage("You're clicking too fast!".red())
            event.isCancelled = true
            return
        }

        shouldReopen = false

        onInventoryClick(player, event.click, event)

        item.onClickEvent(player, event.click)

        if (cancelClicks) event.isCancelled = true

        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1f)

        if (autoRefreshOnClick) render()
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        if (event.inventory.holder != this) return

        if (shouldReopen) {
            Tasks.run {
                open(player)
            }
        }

        onClose(player)
        refreshTask?.cancel()
    }
}