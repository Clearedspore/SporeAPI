package me.clearedSpore.sporeAPI.serialization.codec

import me.clearedSpore.sporeAPI.serialization.SporeCodec
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.*
import java.util.*

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.
class InventoryCodec : SporeCodec<Inventory> {

    override fun encode(value: Inventory): String {
        val out = ByteArrayOutputStream()

        BukkitObjectOutputStream(out).use {
            it.writeInt(value.size)
            it.writeUTF(value.type.name)
            value.contents.forEach { item -> it.writeObject(item) }
        }

        return Base64.getEncoder().encodeToString(out.toByteArray())
    }

    override fun decode(data: String): Inventory? {
        return try {
            val input = ByteArrayInputStream(Base64.getDecoder().decode(data))

            BukkitObjectInputStream(input).use {
                val size = it.readInt()
                val type = it.readUTF()

                val inv = Bukkit.createInventory(null, size, type)

                for (i in 0 until size) {
                    inv.setItem(i, it.readObject() as? ItemStack)
                }

                inv
            }
        } catch (e: Exception) {
            null
        }
    }
}