package me.clearedSpore.sporeAPI.repository

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object SporeMongo {

    @Volatile
    private var databaseRef: MongoDatabase? = null

    val isInitialized: Boolean get() = databaseRef != null

    val database: MongoDatabase
        get() = databaseRef ?: error("SporeMongo.init(database) has not been called")

    fun init(database: MongoDatabase) {
        databaseRef = database
    }

    fun collection(name: String): MongoCollection<Document> = database.getCollection(name)
}
