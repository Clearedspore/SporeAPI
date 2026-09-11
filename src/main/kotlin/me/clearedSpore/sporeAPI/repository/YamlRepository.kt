package me.clearedSpore.sporeAPI.repository

import me.clearedSpore.sporeAPI.debug.blockingIo
import me.clearedSpore.sporeAPI.debug.runDebug
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

        runDebug("${folder.name}.load", details = mapOf("file" to file.name)) {
            read(id, YamlConfiguration.loadConfiguration(file))
        }
    }

    override fun findAllBlocking(): List<V> = blockingIo("${folder.name} findAll") {
        val files = folder.listFiles { file -> file.isFile && file.extension.equals("yml", true) }
            ?: return@blockingIo emptyList()

        files.sortedBy { it.name }.mapNotNull { file ->
            runDebug("${folder.name}.load", details = mapOf("file" to file.name)) {
                read(file.nameWithoutExtension, YamlConfiguration.loadConfiguration(file))
            }
        }
    }

    override fun existsBlocking(id: String): Boolean = fileFor(id).isFile

    override fun saveBlocking(value: V) {
        val id = idOf(value)

        blockingIo("${folder.name}/$id.yml save") {
            runDebug("${folder.name}.save", details = mapOf("file" to "$id.yml")) {
                val config = YamlConfiguration()
                write(value, config)
                config.save(fileFor(id))
            }
        }
    }

    override fun deleteBlocking(id: String) {
        blockingIo("${folder.name}/$id.yml delete") {
            runDebug("${folder.name}.delete", details = mapOf("file" to "$id.yml")) {
                fileFor(id).delete()
            }
        }
    }
}
