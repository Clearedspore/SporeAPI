package me.clearedSpore.sporeAPI.repository

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import me.clearedSpore.sporeAPI.debug.blockingIo
import me.clearedSpore.sporeAPI.util.Logger
import org.bson.Document

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class MongoRepository<V : Any>(
    private val collectionName: String,
    private val idField: String = "_id"
) : Repository<String, V> {

    protected val collection: MongoCollection<Document>
        get() = SporeMongo.collection(collectionName)

    abstract fun idOf(value: V): String

    abstract fun toDocument(value: V): Document

    abstract fun fromDocument(document: Document): V?

    override fun findBlocking(id: String): V? = blockingIo("$collectionName.find") {
        runCatching { collection.find(Filters.eq(idField, id)).first()?.let(::fromDocument) }
            .onFailure { Logger.error("Failed to load $collectionName '$id': ${it.message}") }
            .getOrNull()
    }

    override fun findAllBlocking(): List<V> = blockingIo("$collectionName.findAll") {
        runCatching { collection.find().mapNotNull(::fromDocument) }
            .onFailure { Logger.error("Failed to load $collectionName: ${it.message}") }
            .getOrDefault(emptyList())
    }

    override fun existsBlocking(id: String): Boolean = blockingIo("$collectionName.exists") {
        runCatching { collection.countDocuments(Filters.eq(idField, id)) > 0 }
            .onFailure { Logger.error("Failed to check $collectionName '$id': ${it.message}") }
            .getOrDefault(false)
    }

    override fun saveBlocking(value: V) {
        val id = idOf(value)

        blockingIo("$collectionName.save") {
            runCatching {
                collection.replaceOne(
                    Filters.eq(idField, id),
                    toDocument(value),
                    ReplaceOptions().upsert(true)
                )
            }.onFailure { Logger.error("Failed to save $collectionName '$id': ${it.message}") }
        }
    }

    /**
     * One round trip for the whole batch instead of one per value.
     *
     * Matters most on the shutdown path, where saving every loaded value is unavoidably blocking:
     * a hundred players is one bulk write rather than a hundred sequential ones.
     */
    override fun saveAllBlocking(values: Iterable<V>) {
        val models = values.map { value ->
            ReplaceOneModel(
                Filters.eq(idField, idOf(value)),
                toDocument(value),
                ReplaceOptions().upsert(true)
            )
        }

        if (models.isEmpty()) return

        blockingIo("$collectionName.saveAll") {
            runCatching { collection.bulkWrite(models, BulkWriteOptions().ordered(false)) }
                .onFailure { Logger.error("Failed to save ${models.size} $collectionName: ${it.message}") }
        }
    }

    override fun deleteBlocking(id: String) {
        blockingIo("$collectionName.delete") {
            runCatching { collection.deleteOne(Filters.eq(idField, id)) }
                .onFailure { Logger.error("Failed to delete $collectionName '$id': ${it.message}") }
        }
    }
}
