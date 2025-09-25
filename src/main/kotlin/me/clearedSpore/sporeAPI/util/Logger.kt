package me.clearedSpore.sporeAPI.util

import me.clearedSpore.sporeAPI.util.CC.blue
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

object Logger {

    private var pluginName: String = "Plugin"

    fun initialize(name: String) {
        pluginName = name
    }

    fun log(sender: CommandSender, permission: String, message: String) {
        val prefix = "\uD83D\uDEE0 $pluginName » ".blue()
        Bukkit.getOnlinePlayers().filter { it.hasPermission(permission) }.forEach {
            it.sendMessage("$prefix&f${sender.name} has $message".blue())
        }
    }

    fun info(message: String) = Bukkit.getConsoleSender().sendMessage("§9[$pluginName] (info) §f$message")
    fun warn(message: String) = Bukkit.getConsoleSender().sendMessage("§9[$pluginName] §6(warn) §f$message")
    fun error(message: String) = Bukkit.getConsoleSender().sendMessage("§9[$pluginName] §c(error) §f$message")
    fun infoDB(message: String) = Bukkit.getConsoleSender().sendMessage("§9[$pluginName Database] (info) §f$message")
    fun warnDB(message: String) = Bukkit.getConsoleSender().sendMessage("§9[$pluginName Database] §6(warn) §f$message")
    fun errorDB(message: String) = Bukkit.getConsoleSender().sendMessage("§9[$pluginName Database] §c(error) §f$message")

    fun log(webhookURL: String?, message: String) = sendMessage(webhookURL, message)
    fun log(title: String, description: String, color: String, webhookURL: String?) = sendEmbed(title, description, color, webhookURL)

    private fun sendMessage(webhookURL: String?, message: String) {
        sendPayload("{\"content\":\"$message\"}", webhookURL)
    }

    private fun sendEmbed(title: String, description: String, color: String, webhookURL: String?) {
        val jsonPayload = """{"embeds":[{"title":"$title","description":"$description","color":$color}]}"""
        sendPayload(jsonPayload, webhookURL)
    }

    private fun sendPayload(jsonPayload: String, webhookURL: String?) {
        if (webhookURL.isNullOrEmpty() || webhookURL == "webhook_url") {
            error("Invalid webhook URL: $webhookURL")
            return
        }

        info("Sending payload to webhook URL: $webhookURL")
        info("Payload: $jsonPayload")

        try {
            val url = URL(webhookURL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode != 204) error("Failed to send message to Discord. Response code: $responseCode")
            else info("Message sent successfully.")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
