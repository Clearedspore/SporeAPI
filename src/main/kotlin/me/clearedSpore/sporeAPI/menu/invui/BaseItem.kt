package me.clearedSpore.sporeAPI.menu.invui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper

abstract class BaseItem : AbstractItem() {

    open var autoUpdate: Boolean = true

    abstract fun item(player: Player): ItemStack

    abstract fun click(player: Player, clickType: ClickType)

    override fun getItemProvider(p0: Player): ItemProvider {
        return ItemWrapper(item(p0))
    }

    override fun handleClick(p0: ClickType, p1: Player, p2: Click) {
        click(p1, p0)
        if (autoUpdate) {
            notifyWindows()
        }
    }

}