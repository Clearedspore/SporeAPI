package me.clearedSpore.sporeAPI.repository

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import me.clearedSpore.sporeAPI.debug.blockingIo
import me.clearedSpore.sporeAPI.debug.runDebug
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
        runDebug("$collectionName.find", details = mapOf("id" to id)) {
            collection.find(Filters.eq(idField, id)).first()?.let(::fromDocument)
        }
    }

    override fun findAllBlocking(): List<V> = blockingIo("$collectionName.findAll") {
        runDebug("$collectionName.findAll") {
            collection.find().mapNotNull(::fromDocument)
        } ?: emptyList()
    }

    override fun existsBlocking(id: String): Boolean = blockingIo("$collectionName.exists") {
        runDebug("$collectionName.exists", details = mapOf("id" to id)) {
            collection.countDocuments(Filters.eq(idField, id)) > 0
        } ?: false
    }

    override fun saveBlocking(value: V) {
        val id = idOf(value)

        blockingIo("$collectionName.save") {
            runDebug("$collectionName.save", details = mapOf("id" to id)) {
                collection.replaceOne(
                    Filters.eq(idField, id),
                    toDocument(value),
                    ReplaceOptions().upsert(true)
                )
            }
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
            runDebug("$collectionName.saveAll", details = mapOf("count" to models.size.toString())) {
                collection.bulkWrite(models, BulkWriteOptions().ordered(false))
            }
        }
    }

    override fun deleteBlocking(id: String) {
        blockingIo("$collectionName.delete") {
            runDebug("$collectionName.delete", details = mapOf("id" to id)) {
                collection.deleteOne(Filters.eq(idField, id))
            }
        }
    }
}
