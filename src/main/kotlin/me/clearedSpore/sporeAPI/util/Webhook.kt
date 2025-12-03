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
    private var embeds: MutableList<Embed> = mutableListOf()

    fun setMessage(message: String): Webhook {
        this.content = message
        return this
    }

    fun setUsername(username: String): Webhook {
        this.username = username
        return this
    }

    fun setProfileURL(url: String): Webhook {
        this.avatarUrl = url
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
        val jsonBuilder = StringBuilder("{")
        content?.let { jsonBuilder.append("\"content\":\"${escape(it)}\",") }
        username?.let { jsonBuilder.append("\"username\":\"${escape(it)}\",") }
        avatarUrl?.let { jsonBuilder.append("\"avatar_url\":\"${escape(it)}\",") }

        if (embeds.isNotEmpty()) {
            val embedJson = embeds.joinToString(",", "[", "]") { it.toJson() }
            jsonBuilder.append("\"embeds\":$embedJson,")
        }

        if (jsonBuilder.last() == ',') jsonBuilder.setLength(jsonBuilder.length - 1)
        jsonBuilder.append("}")
        return jsonBuilder.toString()
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

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val regex = """"id":"(\d+)"""".toRegex()
                regex.find(response)?.groups?.get(1)?.value
            } else {
                Logger.error("Webhook failed: HTTP $responseCode")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun editMessage(messageId: String, newEmbed: Embed) {
        val url = URL("$webhookURL/messages/$messageId")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "PATCH"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val payload = """{"embeds":[${newEmbed.toJson()}]}"""

        connection.outputStream.use {
            it.write(payload.toByteArray(StandardCharsets.UTF_8))
        }

        val response = connection.responseCode
        if (response !in 200..299) {
            Logger.error("Failed to edit webhook message: HTTP $response")
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

        fun setTitle(title: String): Embed {
            this.title = title
            return this
        }

        fun setDescription(description: String): Embed {
            this.description = description
            return this
        }

        fun setColor(color: Int): Embed {
            this.color = color
            return this
        }

        fun setFooter(text: String, iconUrl: String? = null): Embed {
            this.footer = text
            this.footerIcon = iconUrl
            return this
        }

        fun setThumbnail(url: String): Embed {
            this.thumbnail = url
            return this
        }

        fun setImage(url: String): Embed {
            this.image = url
            return this
        }

        fun setAuthor(name: String, iconUrl: String? = null): Embed {
            this.author = name
            this.authorIcon = iconUrl
            return this
        }

        fun addField(name: String, value: String, inline: Boolean = false): Embed {
            fields.add(Field(name, value, inline))
            return this
        }

        fun toJson(): String {
            val builder = StringBuilder("{")
            title?.let { builder.append("\"title\":\"${it.replace("\"", "\\\"")}\",") }
            description?.let { builder.append("\"description\":\"${it.replace("\"", "\\\"")}\",") }
            color?.let { builder.append("\"color\":$it,") }

            if (author != null) {
                builder.append("\"author\":{")
                builder.append("\"name\":\"${author!!.replace("\"", "\\\"")}\"")
                if (authorIcon != null) builder.append(",\"icon_url\":\"${authorIcon!!.replace("\"", "\\\"")}\"")
                builder.append("},")
            }

            thumbnail?.let { builder.append("\"thumbnail\":{\"url\":\"${it.replace("\"", "\\\"")}\"},") }
            image?.let { builder.append("\"image\":{\"url\":\"${it.replace("\"", "\\\"")}\"},") }

            if (footer != null) {
                builder.append("\"footer\":{")
                builder.append("\"text\":\"${footer!!.replace("\"", "\\\"")}\"")
                if (footerIcon != null) builder.append(",\"icon_url\":\"${footerIcon!!.replace("\"", "\\\"")}\"")
                builder.append("},")
            }

            if (fields.isNotEmpty()) {
                builder.append("\"fields\":[")
                builder.append(fields.joinToString(",") { it.toJson() })
                builder.append("],")
            }

            if (builder.last() == ',') builder.setLength(builder.length - 1)
            builder.append("}")
            return builder.toString()
        }

        class Field(private val name: String, private val value: String, private val inline: Boolean) {
            fun toJson(): String {
                return "{\"name\":\"${name.replace("\"", "\\\"")}\"," +
                        "\"value\":\"${value.replace("\"", "\\\"")}\"," +
                        "\"inline\":$inline}"
            }
        }
    }
}
