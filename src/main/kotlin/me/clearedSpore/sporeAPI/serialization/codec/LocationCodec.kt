package me.clearedSpore.sporeAPI.serialization.codec

import me.clearedSpore.sporeAPI.serialization.SporeCodec
import org.bukkit.Bukkit
import org.bukkit.Location

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class LocationCodec : SporeCodec<Location> {

    override fun encode(value: Location): String {
        return """
            {
                "world":"${value.world?.name}",
                "x":${value.x},
                "y":${value.y},
                "z":${value.z},
                "yaw":${value.yaw},
                "pitch":${value.pitch}
            }
        """.trimIndent()
    }

    override fun decode(data: String): Location? {
        return try {
            val json = com.google.gson.JsonParser.parseString(data).asJsonObject
            val world = Bukkit.getWorld(json["world"].asString) ?: return null

            Location(
                world,
                json["x"].asDouble,
                json["y"].asDouble,
                json["z"].asDouble,
                json["yaw"].asFloat,
                json["pitch"].asFloat
            )
        } catch (e: Exception) {
            null
        }
    }
}