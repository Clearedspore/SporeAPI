package me.clearedSpore.sporeAPI.data

import me.clearedSpore.sporeAPI.SporeApi
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeKeys {

    private val cache = HashMap<String, NamespacedKey>()

    @Synchronized
    fun key(name: String): NamespacedKey =
        cache.getOrPut(name) { NamespacedKey(SporeApi.plugin, name.lowercase()) }
}


private fun <P : Any, C : Any> ItemStack.write(key: String, type: PersistentDataType<P, C>, value: C): ItemStack {
    val meta = itemMeta ?: return this
    meta.persistentDataContainer.set(SporeKeys.key(key), type, value)
    itemMeta = meta
    return this
}

private fun <P : Any, C : Any> ItemStack.read(key: String, type: PersistentDataType<P, C>): C? =
    itemMeta?.persistentDataContainer?.get(SporeKeys.key(key), type)

fun ItemStack.setData(key: String, value: String): ItemStack = write(key, PersistentDataType.STRING, value)

fun ItemStack.setData(key: String, value: Int): ItemStack = write(key, PersistentDataType.INTEGER, value)

fun ItemStack.setData(key: String, value: Long): ItemStack = write(key, PersistentDataType.LONG, value)

fun ItemStack.setData(key: String, value: Double): ItemStack = write(key, PersistentDataType.DOUBLE, value)

fun ItemStack.setData(key: String, value: Boolean): ItemStack = write(key, PersistentDataType.BOOLEAN, value)

fun ItemStack.setData(key: String, value: ByteArray): ItemStack = write(key, PersistentDataType.BYTE_ARRAY, value)

fun ItemStack.getString(key: String): String? = read(key, PersistentDataType.STRING)

fun ItemStack.getInt(key: String): Int? = read(key, PersistentDataType.INTEGER)

fun ItemStack.getLong(key: String): Long? = read(key, PersistentDataType.LONG)

fun ItemStack.getDouble(key: String): Double? = read(key, PersistentDataType.DOUBLE)

fun ItemStack.getBoolean(key: String): Boolean? = read(key, PersistentDataType.BOOLEAN)

fun ItemStack.getBytes(key: String): ByteArray? = read(key, PersistentDataType.BYTE_ARRAY)

fun ItemStack.hasData(key: String): Boolean =
    itemMeta?.persistentDataContainer?.has(SporeKeys.key(key)) == true

fun ItemStack.removeData(key: String): ItemStack {
    val meta = itemMeta ?: return this
    meta.persistentDataContainer.remove(SporeKeys.key(key))
    itemMeta = meta
    return this
}

fun PersistentDataHolder.setData(key: String, value: String) =
    persistentDataContainer.set(SporeKeys.key(key), PersistentDataType.STRING, value)

fun PersistentDataHolder.setData(key: String, value: Int) =
    persistentDataContainer.set(SporeKeys.key(key), PersistentDataType.INTEGER, value)

fun PersistentDataHolder.setData(key: String, value: Long) =
    persistentDataContainer.set(SporeKeys.key(key), PersistentDataType.LONG, value)

fun PersistentDataHolder.setData(key: String, value: Double) =
    persistentDataContainer.set(SporeKeys.key(key), PersistentDataType.DOUBLE, value)

fun PersistentDataHolder.setData(key: String, value: Boolean) =
    persistentDataContainer.set(SporeKeys.key(key), PersistentDataType.BOOLEAN, value)

fun PersistentDataHolder.setData(key: String, value: ByteArray) =
    persistentDataContainer.set(SporeKeys.key(key), PersistentDataType.BYTE_ARRAY, value)

fun PersistentDataHolder.getString(key: String): String? =
    persistentDataContainer.get(SporeKeys.key(key), PersistentDataType.STRING)

fun PersistentDataHolder.getInt(key: String): Int? =
    persistentDataContainer.get(SporeKeys.key(key), PersistentDataType.INTEGER)

fun PersistentDataHolder.getLong(key: String): Long? =
    persistentDataContainer.get(SporeKeys.key(key), PersistentDataType.LONG)

fun PersistentDataHolder.getDouble(key: String): Double? =
    persistentDataContainer.get(SporeKeys.key(key), PersistentDataType.DOUBLE)

fun PersistentDataHolder.getBoolean(key: String): Boolean? =
    persistentDataContainer.get(SporeKeys.key(key), PersistentDataType.BOOLEAN)

fun PersistentDataHolder.getBytes(key: String): ByteArray? =
    persistentDataContainer.get(SporeKeys.key(key), PersistentDataType.BYTE_ARRAY)

fun PersistentDataHolder.hasData(key: String): Boolean =
    persistentDataContainer.has(SporeKeys.key(key))

fun PersistentDataHolder.removeData(key: String) =
    persistentDataContainer.remove(SporeKeys.key(key))
