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

    fun send() {
        if (webhookURL.isEmpty() || webhookURL == "webhook_url") {
            throw IllegalArgumentException("Invalid webhook URL: $webhookURL")
        }

        val payload = buildPayload()
        sendPayload(payload)
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

    private fun sendPayload(payload: String) {
        try {
            val url = URL(webhookURL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(payload.toByteArray(StandardCharsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode != 204) {
                Logger.error("Failed to send webhook. Response code: $responseCode")
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

        fun setFooter(footer: String): Embed {
            this.footer = footer
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
            footer?.let { builder.append("\"footer\":{\"text\":\"${it.replace("\"", "\\\"")}\"},") }

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
