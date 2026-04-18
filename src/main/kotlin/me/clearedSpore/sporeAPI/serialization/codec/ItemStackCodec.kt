package me.clearedSpore.sporeAPI.serialization.codec

import me.clearedSpore.sporeAPI.serialization.SporeCodec
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.*
import java.util.*

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class ItemStackCodec : SporeCodec<ItemStack> {

    override fun encode(value: ItemStack): String {
        val out = ByteArrayOutputStream()
        BukkitObjectOutputStream(out).use { it.writeObject(value) }
        return Base64.getEncoder().encodeToString(out.toByteArray())
    }

    override fun decode(data: String): ItemStack? {
        return try {
            val input = ByteArrayInputStream(Base64.getDecoder().decode(data))
            BukkitObjectInputStream(input).use {
                it.readObject() as? ItemStack
            }
        } catch (e: Exception) {
            null
        }
    }
}