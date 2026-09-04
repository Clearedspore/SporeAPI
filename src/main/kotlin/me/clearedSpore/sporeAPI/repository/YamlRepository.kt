package me.clearedSpore.sporeAPI.repository

import me.clearedSpore.sporeAPI.debug.blockingIo
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class YamlRepository<V : Any>(
    private val folder: File
) : Repository<String, V> {

    init {
        folder.mkdirs()
    }

    abstract fun idOf(value: V): String

    abstract fun write(value: V, config: YamlConfiguration)

    abstract fun read(id: String, config: YamlConfiguration): V?

    protected fun fileFor(id: String): File = File(folder, "$id.yml")

    override fun findBlocking(id: String): V? = blockingIo("${folder.name}/$id.yml") {
        val file = fileFor(id)
        if (!file.isFile) return@blockingIo null

        runCatching { read(id, YamlConfiguration.loadConfiguration(file)) }
            .onFailure { Logger.error("Failed to load ${file.name}: ${it.message}") }
            .getOrNull()
    }

    override fun findAllBlocking(): List<V> = blockingIo("${folder.name} findAll") {
        val files = folder.listFiles { file -> file.isFile && file.extension.equals("yml", true) }
            ?: return@blockingIo emptyList()

        files.sortedBy { it.name }.mapNotNull { file ->
            runCatching { read(file.nameWithoutExtension, YamlConfiguration.loadConfiguration(file)) }
                .onFailure { Logger.error("Failed to load ${file.name}: ${it.message}") }
                .getOrNull()
        }
    }

    override fun existsBlocking(id: String): Boolean = fileFor(id).isFile

    override fun saveBlocking(value: V) {
        val id = idOf(value)

        blockingIo("${folder.name}/$id.yml save") {
            runCatching {
                val config = YamlConfiguration()
                write(value, config)
                config.save(fileFor(id))
            }.onFailure { Logger.error("Failed to save $id.yml: ${it.message}") }
        }
    }

    override fun deleteBlocking(id: String) {
        blockingIo("${folder.name}/$id.yml delete") {
            runCatching { fileFor(id).delete() }
                .onFailure { Logger.error("Failed to delete $id.yml: ${it.message}") }
        }
    }
}
