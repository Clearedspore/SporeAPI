package me.clearedSpore.sporeAPI.util

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.*
import java.util.Base64
import kotlin.ranges.until
import kotlin.text.isEmpty

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object ItemSerializer {

    fun itemStackArrayToBase64(items: Array<ItemStack?>): String {
        val outputStream = ByteArrayOutputStream()
        val dataOutput = BukkitObjectOutputStream(outputStream)
        dataOutput.writeInt(items.size)
        for (item in items) {
            dataOutput.writeObject(item)
        }
        dataOutput.close()
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    fun itemStackArrayFromBase64(data: String): Array<ItemStack?> {
        val inputStream = ByteArrayInputStream(Base64.getDecoder().decode(data))
        val dataInput = BukkitObjectInputStream(inputStream)
        val size = dataInput.readInt()
        val items = arrayOfNulls<ItemStack>(size)
        for (i in 0 until size) {
            items[i] = dataInput.readObject() as? ItemStack
        }
        dataInput.close()
        return items
    }

    fun itemStackToBase64(item: ItemStack?): String {
        if (item == null) return ""
        val outputStream = ByteArrayOutputStream()
        val dataOutput = BukkitObjectOutputStream(outputStream)
        dataOutput.writeObject(item)
        dataOutput.close()
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    fun itemStackFromBase64(data: String): ItemStack? {
        if (data.isEmpty()) return null
        val inputStream = ByteArrayInputStream(Base64.getDecoder().decode(data))
        val dataInput = BukkitObjectInputStream(inputStream)
        val item = dataInput.readObject() as? ItemStack
        dataInput.close()
        return item
    }
}