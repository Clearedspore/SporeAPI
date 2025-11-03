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
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

abstract class BasePaginatedMenu(
    protected val plugin: JavaPlugin,
    private val footer: Boolean = false
) : InventoryHolder, Listener {

    private lateinit var inventory: Inventory
    protected val originalItems = mutableListOf<Item>()
    protected val items = mutableListOf<ItemStack>()
    protected val fixedItems = mutableMapOf<Int, MutableMap<Int, Item>>()
    protected val paginatedItemMap = mutableMapOf<Int, Item>()
    protected var page = 0
    protected var searchQuery: String = ""
    protected var autoRefreshOnClick: Boolean = true
    private val itemToObjectMap = WeakHashMap<ItemStack, Item>()

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    open fun useInventory(): Boolean = false
    open fun cancelClicks(): Boolean = true
    open fun clickSound(): Sound = Sound.UI_BUTTON_CLICK
    open fun getStartRow(): Int = if (footer) 1 else 1

    abstract fun getMenuName(): String
    abstract fun getRows(): Int
    abstract fun createItems()
    protected abstract fun onInventoryClickEvent(clicker: Player, clickType: ClickType, event: InventoryClickEvent)

    private var autoRefreshTask: BukkitRunnable? = null
    private var autoRefreshEnabled = true

    fun open(player: Player) {
        inventory = Bukkit.createInventory(this, getRows() * 9, getMenuName())
        createItems()
        applySearch()
        setMenuItems()
        player.openInventory(inventory)
    }

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

    fun getItemsPerPage(): Int = if (footer) (getRows() - 2) * 7 else (getRows() - getStartRow()) * 9

    fun addItem(item: Item) {
        originalItems.add(item)
        val stack = item.createItem()
        items.add(stack)
        itemToObjectMap[stack] = item
    }

    fun startAutoRefresh() {
        stopAutoRefresh()
        if (!autoRefreshEnabled) return

        autoRefreshTask = object : BukkitRunnable() {
            override fun run() {
                if (!::inventory.isInitialized) return
                if (inventory.viewers.isNotEmpty()) {
                    inventory.viewers.filterIsInstance<Player>().forEach { refreshMenu(it) }
                } else cancel()
            }
        }
        autoRefreshTask?.runTaskTimer(plugin, 20L, 20L)
    }

    fun stopAutoRefresh() {
        autoRefreshTask?.cancel()
        autoRefreshTask = null
    }

    fun setAutoRefresh(enabled: Boolean) {
        autoRefreshEnabled = enabled
        if (!enabled) stopAutoRefresh()
    }

    fun setGlobalMenuItem(x: Int, y: Int, item: Item) = setMenuItem(x, y, -1, item)

    fun setMenuItem(x: Int, y: Int, pageNumber: Int, item: Item) {
        val slot = (y - 1) * 9 + (x - 1)
        fixedItems.computeIfAbsent(pageNumber) { mutableMapOf() }[slot] = item
        if (::inventory.isInitialized && (pageNumber == -1 || pageNumber == page)) {
            inventory.setItem(slot, item.createItem())
        }
    }

    @Deprecated("Use setMenuItem with page number")
    fun setMenuItem(x: Int, y: Int, item: Item) = setMenuItem(x, y, page, item)

    fun setMenuItems() {
        inventory.clear()
        paginatedItemMap.clear()
        placeFixedItems()
        if (footer) placeFooter()
        placePaginatedItems()
    }

    private fun placeFixedItems() {
        fixedItems[-1]?.forEach { (slot, item) -> inventory.setItem(slot, item.createItem()) }
        fixedItems[page]?.forEach { (slot, item) -> inventory.setItem(slot, item.createItem()) }
    }

    private fun placePaginatedItems() {
        val start = page * getItemsPerPage()
        val end = minOf(start + getItemsPerPage(), items.size)
        var slotIndex = 0
        val itemsPerRow = if (footer) 7 else 9

        for (i in start until end) {
            val row = (slotIndex / itemsPerRow) + getStartRow()
            val col = if (footer) (slotIndex % 7) + 1 else slotIndex % 9
            val slot = row * 9 + col

            if (!isFixedItemSlot(slot) && inventory.getItem(slot) == null) {
                val stack = items[i]
                inventory.setItem(slot, stack)
                val itemObj = itemToObjectMap[stack]
                paginatedItemMap[slot] = itemObj ?: object : Item() {
                    override fun createItem(): ItemStack = stack
                    override fun onClickEvent(clicker: Player, clickType: ClickType) {}
                }
            }
            slotIndex++
        }
    }

    private fun placeFooter() {
        if (getRows() < 3) return
        val bottomRowStart = (getRows() - 1) * 9
        val grayPane = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply { itemMeta = itemMeta?.apply { setDisplayName(" ") } }

        for (row in 1 until getRows() - 1) {
            placeGlassPaneIfNotFixed(row * 9, grayPane)
            placeGlassPaneIfNotFixed(row * 9 + 8, grayPane)
        }
        for (i in 0 until 9) placeGlassPaneIfNotFixed(i, grayPane)
        for (i in bottomRowStart + 1 until bottomRowStart + 8) placeGlassPaneIfNotFixed(i, grayPane)

        if (!isFixedItemSlot(bottomRowStart)) inventory.setItem(bottomRowStart, createPreviousPageItem())
        if (!isFixedItemSlot(bottomRowStart + 8)) inventory.setItem(bottomRowStart + 8, createNextPageItem())
    }

    private fun placeGlassPaneIfNotFixed(slot: Int, pane: ItemStack) {
        if (!isFixedItemSlot(slot)) inventory.setItem(slot, pane)
    }

    private fun isFixedItemSlot(slot: Int): Boolean =
        fixedItems[page]?.containsKey(slot) == true || fixedItems[-1]?.containsKey(slot) == true

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
                clicker.closeInventory()
                chatInput.awaitChatInput(clicker) { input ->
                    searchQuery = input?.trim()?.lowercase() ?: ""
                    page = 0
                    applySearch()
                    setMenuItems()
                    open(clicker)
                    if (::inventory.isInitialized) clicker.updateInventory()
                }
            }
        })
    }

    private fun applySearch() {
        items.clear()
        itemToObjectMap.clear()
        val query = searchQuery.lowercase().trim()
        val filtered = if (query.isEmpty()) originalItems else originalItems.filter { item ->
            val name = ChatColor.stripColor(item.createItem().itemMeta?.displayName ?: item.createItem().type.name)
            name!!.lowercase().contains(query)
        }

        filtered.forEach { item ->
            val stack = item.createItem()
            items.add(stack)
            itemToObjectMap[stack] = item
        }
    }

    fun refreshMenu(player: Player? = null) {
        if (!::inventory.isInitialized) return

        inventory.clear()
        paginatedItemMap.clear()
        itemToObjectMap.clear()
        items.clear()

        createItems()
        applySearch()
        setMenuItems()

        player?.updateInventory() ?: inventory.viewers.filterIsInstance<Player>().forEach { it.updateInventory() }
    }


    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.topInventory.holder != this) return

        val slot = event.rawSlot
        val topSize = event.view.topInventory.size
        if (slot >= topSize && !useInventory()) { event.isCancelled = true; return }

        val clickedItem = event.currentItem ?: return
        if (clickedItem.type == Material.AIR) return
        if (clickedItem.type.name.contains("GLASS_PANE") && clickedItem.itemMeta?.displayName == " ") { event.isCancelled = true; return }

        fixedItems[page]?.get(slot)?.onClickEvent(player, event.click)
            ?: fixedItems[-1]?.get(slot)?.onClickEvent(player, event.click)

        val bottomRowStart = (getRows() - 1) * 9
        if (slot == bottomRowStart) previousPage()
        if (slot == bottomRowStart + 8) nextPage()

        paginatedItemMap[slot]?.onClickEvent(player, event.click)
        onInventoryClickEvent(player, event.click, event)

        event.isCancelled = cancelClicks()
        player.playSound(player.location, clickSound(), 0.5f, 1.0f)
        if (autoRefreshOnClick) refreshMenu(player)
    }

    override fun getInventory(): Inventory = inventory
}
