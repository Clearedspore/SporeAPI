package me.clearedSpore.sporeAPI.menu.invui

import me.clearedSpore.sporeAPI.util.CC.green
import me.clearedSpore.sporeAPI.util.CC.red
import me.clearedSpore.sporeAPI.util.ItemBuilder
import org.bukkit.Material
import org.bukkit.Sound
import xyz.xenondevs.invui.gui.Markers
import xyz.xenondevs.invui.gui.Structure
import xyz.xenondevs.invui.item.BoundItem
import java.util.function.Supplier

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeMenuDefaults {


    val BACK_ITEM = ItemBuilder(Material.RED_CARPET)
        .setName("Previous Page".red())
        .hideAll()
        .build()

    val NEXT_ITEM = ItemBuilder(Material.LIME_CARPET)
        .setName("Next Page".green())
        .hideAll()
        .build()

    val FILLER_ITEM = ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
        .setName("test".green())
        .hideAll()
        .build()

    fun register() {
        Structure.addGlobalIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
        Structure.addGlobalIngredient('#', FILLER_ITEM)

        Structure.addGlobalIngredient(
            '<',
            Supplier {
                BoundItem.pagedBuilder()
                    .setItemProvider(BACK_ITEM)
                    .addClickHandler { _, gui, player ->
                        player.player.playSound(
                            player.player(),
                            Sound.ITEM_BOOK_PAGE_TURN,
                            1.0f,
                            1.0f
                        )
                        gui.page--
                    }
                    .build()
            }
        )

        Structure.addGlobalIngredient(
            '>',
            Supplier {
                BoundItem.pagedBuilder()
                    .setItemProvider(NEXT_ITEM)
                    .addClickHandler { _, gui, player ->
                        player.player.playSound(
                            player.player(),
                            Sound.ITEM_BOOK_PAGE_TURN,
                            1.0f,
                            1.0f
                        )
                        gui.page++
                    }
                    .build()
            }
        )
    }
}