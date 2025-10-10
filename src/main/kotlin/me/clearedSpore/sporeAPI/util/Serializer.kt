package me.clearedSpore.sporeAPI.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.bukkit.Bukkit
import org.bukkit.Location

object Serializer {
    private val gson = Gson()

    fun toJson(obj: Any?): String {
        if (obj == null) return "null"

        return when (obj) {
            is Location -> serializeLocation(obj)
            else -> gson.toJson(obj)
        }
    }

    fun fromJson(json: String?, type: Class<*>): Any? {
        if (json == null || json == "null") return null

        return when {
            type == Location::class.java -> deserializeLocation(json)
            else -> try {
                gson.fromJson(json, type)
            } catch (e: JsonSyntaxException) {
                null
            }
        }
    }

    fun auto(value: Any?): String {
        return toJson(value)
    }


    fun <T : Any> autoDeserialize(json: String?, type: Class<T>): T? {
        return fromJson(json, type) as? T
    }

    private fun serializeLocation(loc: Location): String {
        return gson.toJson(
            mapOf(
                "world" to loc.world?.name,
                "x" to loc.x,
                "y" to loc.y,
                "z" to loc.z,
                "yaw" to loc.yaw,
                "pitch" to loc.pitch
            )
        )
    }

    private fun deserializeLocation(json: String): Location? {
        return try {
            val map = gson.fromJson(json, Map::class.java)
            val worldName = map["world"] as? String ?: return null
            val world = Bukkit.getWorld(worldName) ?: return null
            Location(
                world,
                (map["x"] as Number).toDouble(),
                (map["y"] as Number).toDouble(),
                (map["z"] as Number).toDouble(),
                (map["yaw"] as Number).toFloat(),
                (map["pitch"] as Number).toFloat()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
