package me.clearedSpore.sporeAPI.exception

import me.clearedSpore.sporeAPI.util.Logger

class LoggedException(
    private val userMessage: String,
    internalMessage: String = userMessage,
    private val level: Level = Level.ERROR,
    private val channel: Channel = Channel.GENERAL,
    private val developerOnly: Boolean = false,
    cause: Throwable? = null
) : RuntimeException(internalMessage, cause) {

    enum class Level {
        INFO, WARN, ERROR
    }

    enum class Channel {
        GENERAL, DATABASE
    }

    fun log() {
        val name = this::class.simpleName ?: "Exception"

        val devTag = if (developerOnly) "§d[DEV] " else ""
        val dbTag = if (channel == Channel.DATABASE) "§b[Database] " else ""

        val notice =
            if (developerOnly && !Logger.developerNotice.isNullOrBlank())
                " §7(${Logger.developerNotice})"
            else ""

        val formatted =
            "${Logger.prefix}§f$devTag$dbTag$name: §7$message$notice"

        when (channel) {
            Channel.GENERAL -> when (level) {
                Level.INFO -> Logger.info(formatted)
                Level.WARN -> Logger.warn(formatted)
                Level.ERROR -> Logger.error(formatted)
            }

            Channel.DATABASE -> when (level) {
                Level.INFO -> Logger.infoDB(formatted)
                Level.WARN -> Logger.warnDB(formatted)
                Level.ERROR -> Logger.errorDB(formatted)
            }
        }

        cause?.let {
            Logger.error("${Logger.prefix}§7Caused by §f${it::class.simpleName}: §7${it.message}")
        }
    }

    fun getPublicMessage(): String {
        return if (developerOnly && !Logger.developerNotice.isNullOrBlank())
            "$userMessage\n§7${Logger.developerNotice}"
        else
            userMessage
    }
}
