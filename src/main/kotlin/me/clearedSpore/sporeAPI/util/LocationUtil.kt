package me.clearedSpore.sporeAPI.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.json.JSONObject

object LocationUtil {

    fun toJson(location: Location): String {
        val json = JSONObject()
        json.put("world", location.world?.name)
        json.put("x", location.x)
        json.put("y", location.y)
        json.put("z", location.z)
        json.put("yaw", location.yaw)
        json.put("pitch", location.pitch)
        return json.toString()
    }

    fun fromJson(jsonString: String?): Location? {
        if (jsonString.isNullOrEmpty()) return null

        return try {
            val json = JSONObject(jsonString)
            val worldName = json.optString("world", null) ?: return null
            val world: World = Bukkit.getWorld(worldName) ?: return null

            val x = json.optDouble("x")
            val y = json.optDouble("y")
            val z = json.optDouble("z")
            val yaw = json.optDouble("yaw").toFloat()
            val pitch = json.optDouble("pitch").toFloat()

            Location(world, x, y, z, yaw, pitch)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
