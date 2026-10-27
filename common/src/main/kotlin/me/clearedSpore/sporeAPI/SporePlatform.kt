package me.clearedSpore.sporeAPI

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


interface SporePlatform {

    val name: String

    val basePackage: String

    fun isMainThread(): Boolean = false

    fun log(line: String)
}

open class StandalonePlatform(
    override val name: String,
    override val basePackage: String
) : SporePlatform {

    constructor(name: String, mainClass: Class<*>) : this(name, mainClass.packageName)

    override fun log(line: String) = println(SporeCore.stripLegacy(line))
}

object SporeCore {

    private val LEGACY_CODE = Regex("§[0-9a-fk-orx]", RegexOption.IGNORE_CASE)

    @Volatile
    private var platformRef: SporePlatform? = null

    val platform: SporePlatform
        get() = platformRef ?: error("SporeCore is not initialised yet - call SporeCore.init(platform) or extend SporePlugin")

    val isInitialized: Boolean get() = platformRef != null

    fun init(platform: SporePlatform) {
        platformRef = platform
    }

    fun isMainThread(): Boolean = runCatching { platformRef?.isMainThread() ?: false }.getOrDefault(false)

    fun log(line: String) {
        val platform = platformRef

        if (platform == null) {
            println(stripLegacy(line))
        } else {
            platform.log(line)
        }
    }

    fun stripLegacy(line: String): String = line.replace(LEGACY_CODE, "")
}
