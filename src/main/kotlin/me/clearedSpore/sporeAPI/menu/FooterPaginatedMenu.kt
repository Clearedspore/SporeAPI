package me.clearedSpore.sporeAPI.menu


import me.clearedSpore.sporeAPI.util.CC.blue
import me.clearedSpore.sporeAPI.util.CC.white
import me.clearedSpore.sporeAPI.util.ChatInput
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin

/**
 * OUTDATED!!!
 * Use the BasePaginated menu class!
 */

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

abstract class FooterPaginatedMenu(protected val plugin: JavaPlugin) : InventoryHolder, Listener {

    private lateinit var inventory: Inventory
    private val items = mutableListOf<ItemStack>()
    private val paginatedItemMap = mutableMapOf<Int, Item>()
    private var page = 0
    private val fixedItems = mutableMapOf<Int, MutableMap<Int, Item>>()

    protected var searchQuery = ""
    protected val originalItems = mutableListOf<ItemStack>()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    abstract fun getMenuName(): String
    abstract fun getRows(): Int
    abstract fun createItems()

    fun getCurrentPage(): Int = page

    fun open(player: Player) {
        inventory = Bukkit.createInventory(this, getRows() * 9, getMenuName())
        createItems()
        setMenuItems()
        player.openInventory(inventory)
    }

    fun setMenuItems() {
        inventory.clear()
        paginatedItemMap.clear()

        placeFixedItems()
        placeGlassPanes()

        val bottomRowStart = (getRows() - 1) * 9
        if (!isFixedItemSlot(bottomRowStart)) inventory.setItem(bottomRowStart, createPreviousPageItem())
        if (!isFixedItemSlot(bottomRowStart + 8)) inventory.setItem(bottomRowStart + 8, createNextPageItem())

        val start = page * getItemsPerPage()
        val end = minOf(start + getItemsPerPage(), items.size)

        var slotIndex = 0
        for (i in start until end) {
            val row = (slotIndex / 7) + 1
            val col = (slotIndex % 7) + 1
            val slot = row * 9 + col

            if (!isFixedItemSlot(slot) && inventory.getItem(slot) == null) {
                val stack = items[i]
                inventory.setItem(slot, stack)
                paginatedItemMap[slot] = object : Item() {
                    override fun createItem(): ItemStack = stack
                    override fun onClickEvent(clicker: Player, clickType: ClickType) {}
                }
            }
            slotIndex++
        }
    }

    private fun placeFixedItems() {
        fixedItems[-1]?.forEach { (slot, item) -> inventory.setItem(slot, item.createItem()) }
        fixedItems[page]?.forEach { (slot, item) -> inventory.setItem(slot, item.createItem()) }
    }

    private fun placeGlassPanes() {
        if (getRows() < 3) return
        val grayPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply { setDisplayName(" ") }
        }

        for (i in 0 until 9) placeGlassPaneIfNotFixed(i, grayPane)
        val bottomRowStart = (getRows() - 1) * 9
        for (i in bottomRowStart + 1 until bottomRowStart + 8) placeGlassPaneIfNotFixed(i, grayPane)
        for (row in 1 until getRows() - 1) {
            placeGlassPaneIfNotFixed(row * 9, grayPane)
            placeGlassPaneIfNotFixed(row * 9 + 8, grayPane)
        }
    }

    private fun placeGlassPaneIfNotFixed(slot: Int, pane: ItemStack) {
        if (!isFixedItemSlot(slot)) inventory.setItem(slot, pane)
    }

    private fun isFixedItemSlot(slot: Int): Boolean =
        fixedItems[page]?.containsKey(slot) == true || fixedItems[-1]?.containsKey(slot) == true

    fun setMenuItem(x: Int, y: Int, pageNumber: Int, item: Item) {
        val slot = (y - 1) * 9 + (x - 1)
        fixedItems.computeIfAbsent(pageNumber) { mutableMapOf() }[slot] = item
        if (::inventory.isInitialized && (pageNumber == -1 || pageNumber == page)) {
            inventory.setItem(slot, item.createItem())
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

    fun getItemsPerPage(): Int = (getRows() - 2) * 7

    fun createPreviousPageItem(): ItemStack = ItemStack(Material.RED_CARPET).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName("Previous page".blue())
            lore = mutableListOf("Click to go to the previous page".white(), "Current page: $page".white())
        }
        if (page >= 1) amount = page
    }

    fun createNextPageItem(): ItemStack = ItemStack(Material.LIME_CARPET).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName("Next page".blue())
            lore = mutableListOf("Click to go to the next page".white(), "Current page: $page".white())
        }
        if (page >= 1) amount = page
    }

    fun addItem(item: ItemStack) {
        originalItems.add(item)
        if (searchQuery.isEmpty()) items.add(item) else applySearch()
    }

    fun addItem(item: Item) {
        val stack = item.createItem()
        addItem(stack)
    }

    fun clearItems() {
        items.clear()
        if (::inventory.isInitialized) inventory.clear()
    }

    fun reloadItems() {
        inventory.clear()
        items.clear()
        fixedItems.clear()
        page = 0
        searchQuery = ""
        originalItems.clear()
        createItems()
        setMenuItems()
    }

    fun setGlobalMenuItem(x: Int, y: Int, item: Item) = setMenuItem(x, y, -1, item)

    fun addSearchItem(x: Int, y: Int, chatInput: ChatInput) {
        setGlobalMenuItem(x, y, object : Item() {
            override fun createItem(): ItemStack = ItemStack(Material.OAK_SIGN).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName("Search".blue())
                    lore = mutableListOf("Click to search items".white()).apply {
                        if (searchQuery.isNotEmpty()) add("Current: $searchQuery".white())
                    }
                }
            }

            override fun onClickEvent(clicker: Player, clickType: ClickType) {
                chatInput.awaitChatInput(clicker) { input ->
                    searchQuery = input?.trim()?.lowercase() ?: ""
                    page = 0
                    applySearch()
                    setMenuItems()
                    if (::inventory.isInitialized) clicker.openInventory(inventory)
                }
            }
        })
    }

    private fun applySearch() {
        items.clear()
        if (searchQuery.isEmpty()) {
            items.addAll(originalItems)
            return
        }

        val query = searchQuery.lowercase()
        originalItems.forEach { item ->
            val name = if (item.hasItemMeta() && item.itemMeta?.hasDisplayName() == true)
                ChatColor.stripColor(item.itemMeta!!.displayName!!)!!.lowercase()
            else
                item.type.name.lowercase().replace("_", " ")

            if (name.contains(query)) items.add(item)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) return
        val player = event.whoClicked as Player
        if (!::inventory.isInitialized || event.view.topInventory.holder != this) return
        event.isCancelled = true

        val slot = event.rawSlot
        if (slot < 0 || slot >= event.view.topInventory.size) return
        val clickedItem = event.currentItem ?: return
        if (clickedItem.type == Material.AIR || clickedItem.type.name.contains("GLASS_PANE")) return

        fixedItems[page]?.get(slot)?.onClickEvent(player, event.click)
        fixedItems[-1]?.get(slot)?.onClickEvent(player, event.click)

        val bottomRowStart = (getRows() - 1) * 9
        when (slot) {
            bottomRowStart -> previousPage()
            bottomRowStart + 8 -> nextPage()
        }

        paginatedItemMap[slot]?.onClickEvent(player, event.click)
        onInventoryClickEvent(player, event.click, event)
    }

    protected abstract fun onInventoryClickEvent(clicker: Player, clickType: ClickType, event: InventoryClickEvent)
    override fun getInventory(): Inventory = inventory
}
