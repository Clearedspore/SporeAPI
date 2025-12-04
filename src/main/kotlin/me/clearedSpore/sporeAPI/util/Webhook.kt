package me.clearedSpore.sporeAPI.util

import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class Webhook(private val webhookURL: String) {

    private var content: String? = null
    private var username: String? = null
    private var avatarUrl: String? = null
    private val embeds: MutableList<Embed> = mutableListOf()

    fun setMessage(message: String): Webhook {
        content = message
        return this
    }

    fun setUsername(username: String): Webhook {
        this.username = username
        return this
    }

    fun setProfileURL(url: String): Webhook {
        avatarUrl = url
        return this
    }

    fun addEmbed(embed: Embed): Webhook {
        embeds.add(embed)
        return this
    }

    fun clearEmbeds(): Webhook {
        embeds.clear()
        return this
    }

    fun send(): String? {
        if (webhookURL.isEmpty() || webhookURL == "webhook_url") {
            throw IllegalArgumentException("Invalid webhook URL: $webhookURL")
        }

        val payload = buildPayload()
        return sendPayload(payload)
    }

    private fun buildPayload(): String {
        val json = StringBuilder("{")
        content?.let { json.append("\"content\":\"${escape(it)}\",") }
        username?.let { json.append("\"username\":\"${escape(it)}\",") }
        avatarUrl?.let { json.append("\"avatar_url\":\"${escape(it)}\",") }
        if (embeds.isNotEmpty()) {
            val embedJson = embeds.joinToString(",", "[", "]") { it.toJson() }
            json.append("\"embeds\":$embedJson,")
        }
        if (json.last() == ',') json.setLength(json.length - 1)
        json.append("}")
        return json.toString()
    }

    private fun sendPayload(payload: String): String? {
        return try {
            val url = URL("$webhookURL?wait=true")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }

            val responseCode = connection.responseCode
            val responseText = connection.inputStream.bufferedReader().readText()

            if (responseCode in 200..299) {
                val regex = """"id":"(\d+)"""".toRegex()
                val messageId = regex.find(responseText)?.groups?.get(1)?.value
                Logger.info("Webhook sent successfully, messageId=$messageId")
                messageId
            } else {
                Logger.error("Webhook failed: HTTP $responseCode, response=$responseText")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun editMessage(messageId: String, newEmbed: Embed) {
        try {
            val url = URL("$webhookURL/messages/$messageId?wait=true")
            val connection = url.openConnection() as HttpURLConnection

            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true


            val payload = buildString {
                append("{")
                append("\"embeds\":[")
                append(newEmbed.toJson())
                append("]}")
            }

            connection.outputStream.use { it.write(payload.toByteArray(StandardCharsets.UTF_8)) }

            val code = connection.responseCode

            val responseText = if (code in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: "No response body"
            }

            if (code !in 200..299) {
                Logger.error("Failed to edit webhook message: HTTP $code, response=$responseText")
            } else {
                Logger.info("Webhook message $messageId edited successfully")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    private fun escape(text: String) = text.replace("\"", "\\\"")

    class Embed {
        private var title: String? = null
        private var description: String? = null
        private var color: Int? = null
        private var footer: String? = null
        private var footerIcon: String? = null
        private var thumbnail: String? = null
        private var image: String? = null
        private var author: String? = null
        private var authorIcon: String? = null
        private val fields: MutableList<Field> = mutableListOf()

        fun setTitle(title: String) = apply { this.title = title }
        fun setDescription(description: String) = apply { this.description = description }
        fun setColor(color: Int) = apply { this.color = color }
        fun setFooter(text: String, iconUrl: String? = null) = apply {
            footer = text; footerIcon = iconUrl
        }
        fun setThumbnail(url: String) = apply { thumbnail = url }
        fun setImage(url: String) = apply { image = url }
        fun setAuthor(name: String, iconUrl: String? = null) = apply {
            author = name; authorIcon = iconUrl
        }
        fun addField(name: String, value: String, inline: Boolean = false) = apply {
            fields.add(Field(name, value, inline))
        }

        fun toJson(): String {
            val builder = StringBuilder("{")
            title?.let { builder.append("\"title\":\"${it.replace("\"", "\\\"")}\",") }
            description?.let { builder.append("\"description\":\"${it.replace("\"", "\\\"")}\",") }
            color?.let { builder.append("\"color\":$it,") }
            if (author != null) {
                builder.append("\"author\":{\"name\":\"${author!!.replace("\"", "\\\"")}\"")
                authorIcon?.let { builder.append(",\"icon_url\":\"${it.replace("\"", "\\\"")}\"") }
                builder.append("},")
            }
            thumbnail?.let { builder.append("\"thumbnail\":{\"url\":\"${it.replace("\"", "\\\"")}\"},") }
            image?.let { builder.append("\"image\":{\"url\":\"${it.replace("\"", "\\\"")}\"},") }
            if (footer != null) {
                builder.append("\"footer\":{\"text\":\"${footer!!.replace("\"", "\\\"")}\"")
                footerIcon?.let { builder.append(",\"icon_url\":\"${it.replace("\"", "\\\"")}\"") }
                builder.append("},")
            }
            if (fields.isNotEmpty()) {
                builder.append("\"fields\":[${fields.joinToString(",") { it.toJson() }}],")
            }
            if (builder.last() == ',') builder.setLength(builder.length - 1)
            builder.append("}")
            return builder.toString()
        }

        class Field(private val name: String, private val value: String, private val inline: Boolean) {
            fun toJson(): String {
                return """{"name":"${name.replace("\"","\\\"")}","value":"${value.replace("\"","\\\"")}","inline":$inline}"""
            }
        }
    }
}
