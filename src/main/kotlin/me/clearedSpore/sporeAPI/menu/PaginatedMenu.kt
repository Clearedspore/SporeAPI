package me.clearedSpore.sporeAPI.menu

import me.clearedSpore.sporeAPI.util.CC.blue
import me.clearedSpore.sporeAPI.util.CC.white
import me.clearedSpore.sporeAPI.util.ChatInput
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

/**
 * OUTDATED!!!
 * Use the BasePaginated menu class!
 */


// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

abstract class PaginatedMenu(protected val plugin: JavaPlugin) : InventoryHolder, Listener {

    private lateinit var inventory: Inventory
    private val items = mutableListOf<ItemStack>()
    private var page = 0
    private val fixedItems = mutableMapOf<Int, MutableMap<Int, Item>>()

    protected var searchQuery: String = ""
    protected val originalItems = mutableListOf<ItemStack>()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun getCurrentPage(): Int = page

    fun open(player: Player) {
        inventory = Bukkit.createInventory(this, getRows() * 9, getMenuName())
        createItems()
        setMenuItems()
        player.openInventory(inventory!!)
    }

    abstract fun getMenuName(): String
    abstract fun getRows(): Int
    abstract fun createItems()

    fun setMenuItems() {
        inventory?.clear()
        inventory?.setItem(0, createPreviousPageItem())
        inventory?.setItem(8, createNextPageItem())

        fixedItems[-1]?.forEach { (slot, item) ->
            inventory?.setItem(slot, item.createItem())
        }

        fixedItems[page]?.forEach { (slot, item) ->
            inventory?.setItem(slot, item.createItem())
        }

        val start = page * getItemsPerPage()
        val end = minOf(start + getItemsPerPage(), items.size)

        for (i in start until end) {
            val row = (i - start) / 9 + 1
            val col = (i - start) % 9
            val slot = row * 9 + col

            val isFixedSlot = (fixedItems[page]?.containsKey(slot) == true) || (fixedItems[-1]?.containsKey(slot) == true)
            if (!isFixedSlot) inventory?.setItem(slot, items[i])
        }
    }

    fun setMenuItem(x: Int, y: Int, pageNumber: Int, item: Item) {
        val slot = (y - 1) * 9 + (x - 1)
        fixedItems.computeIfAbsent(pageNumber) { mutableMapOf() }
        fixedItems[pageNumber]?.put(slot, item)

        if (inventory != null && page == pageNumber) {
            inventory?.setItem(slot, item.createItem())
        }
    }

    @Deprecated("Use setMenuItem with page number")
    fun setMenuItem(x: Int, y: Int, item: Item) = setMenuItem(x, y, page, item)

    fun nextPage() {
        if ((page + 1) * getItemsPerPage() < items.size) {
            page++
            setMenuItems()
        }
    }

    fun previousPage() {
        if (page > 0) {
            page--
            setMenuItems()
        }
    }

    fun getItemsPerPage(): Int = getRows() * 9 - 9

    fun createPreviousPageItem(): ItemStack {
        val item = ItemStack(Material.RED_CARPET)
        val meta = item.itemMeta
        meta?.let {
            it.setDisplayName("Previous page".blue())
            val lore = mutableListOf("Click to go to the previous page".blue(), "Current page: $page".blue())
            if (page >= 1) item.amount = page
            it.lore = lore
            item.itemMeta = it
        }
        return item
    }

    fun createNextPageItem(): ItemStack {
        val item = ItemStack(Material.LIME_CARPET)
        val meta = item.itemMeta
        meta?.let {
            it.setDisplayName("Next page".blue())
            val lore = mutableListOf("Click to go to the next page".blue(), "Current page: $page".blue())
            if (page >= 1) item.amount = page
            it.lore = lore
            item.itemMeta = it
        }
        return item
    }

    fun addItem(item: ItemStack) {
        originalItems.add(item)
        if (searchQuery.isEmpty()) items.add(item)
    }

    fun addItem(item: Item) {
        val stack = item.createItem()
        originalItems.add(stack)
        if (searchQuery.isEmpty()) items.add(stack)
    }

    fun clearItems() {
        items.clear()
        inventory?.clear()
    }

    fun reloadItems() {
        inventory?.clear()
        items.clear()
        fixedItems.clear()
        page = 0
        if (searchQuery.isEmpty()) items.addAll(originalItems) else applySearch()
        setMenuItems()
    }

    fun setGlobalMenuItem(x: Int, y: Int, item: Item) = setMenuItem(x, y, -1, item)

    fun addSearchItem(x: Int, y: Int, chatInput: ChatInput) {
        setGlobalMenuItem(x, y, object : Item() {
            override fun createItem(): ItemStack {
                val item = ItemStack(Material.OAK_SIGN)
                val meta = item.itemMeta
                meta?.let {
                    it.setDisplayName("Search".blue())
                    val lore = mutableListOf("Click to search items".white())
                    if (searchQuery.isNotEmpty()) lore.add("Current: $searchQuery".white())
                    it.lore = lore
                    item.itemMeta = it
                }
                return item
            }

            override fun onClickEvent(clicker: Player, clickType: ClickType) {
                clicker.closeInventory()
                chatInput.awaitChatInput(clicker) { input ->
                    searchQuery = input?.trim()?.lowercase() ?: ""
                    if (searchQuery.isEmpty()) {
                        items.clear()
                        items.addAll(originalItems)
                    } else {
                        applySearch()
                    }
                    page = 0
                    setMenuItems()
                    clicker.openInventory(inventory!!)
                }
            }
        })
    }

    private fun applySearch() {
        items.clear()
        val query = searchQuery.lowercase()

        for (item in originalItems) {
            val name = if (item.hasItemMeta() && item.itemMeta!!.hasDisplayName()) {
                ChatColor.stripColor(item.itemMeta!!.displayName ?: "")!!.lowercase()
            } else {
                item.type.toString().lowercase().replace("_", " ")
            }
            if (name.contains(query)) items.add(item)
        }
    }

    protected abstract fun onInventoryClickEvent(clicker: Player, clickType: ClickType, event: InventoryClickEvent)

    override fun getInventory(): Inventory = inventory!!
}
