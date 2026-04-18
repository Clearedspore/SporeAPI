package me.clearedSpore.sporeAPI.serialization

import me.clearedSpore.sporeAPI.serialization.codec.InventoryCodec
import me.clearedSpore.sporeAPI.serialization.codec.ItemStackCodec
import me.clearedSpore.sporeAPI.serialization.codec.LocationCodec

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object SporeSerialization {

    fun init() {
        SporeCodecRegistry.register(org.bukkit.Location::class.java, LocationCodec())
        SporeCodecRegistry.register(org.bukkit.inventory.ItemStack::class.java, ItemStackCodec())
        SporeCodecRegistry.register(org.bukkit.inventory.Inventory::class.java, InventoryCodec())
    }
}