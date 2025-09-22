package me.clearedSpore.sporeAPI.util

import java.security.SecureRandom


object IdUtil {
    private val base62Chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val random = SecureRandom()

    fun generateId(length: Int = 16): String {
        val sb = StringBuilder()
        repeat(length) {
            val index = random.nextInt(base62Chars.length)
            sb.append(base62Chars[index])
        }
        return sb.toString()
    }
}