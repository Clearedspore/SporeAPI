package me.clearedSpore.sporeAPI.command

import me.clearedSpore.sporeAPI.command.cloud.SporeCloudCommandManager

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


interface CommandRegistrar {

    val name: String

    fun supports(command: Any): Boolean

    fun register(command: Any)
}

class CloudCommandRegistrar(private val manager: () -> SporeCloudCommandManager) : CommandRegistrar {

    override val name: String = "Cloud"

    override fun supports(command: Any): Boolean {
        val clazz = command.javaClass

        if (clazz.annotations.any(::isCloudAnnotation)) return true

        return clazz.declaredMethods.any { method -> method.annotations.any(::isCloudAnnotation) }
    }

    override fun register(command: Any) {
        manager().register(command)
    }

    private fun isCloudAnnotation(annotation: Annotation): Boolean =
        (annotation as java.lang.annotation.Annotation).annotationType().`package`.name.startsWith("org.incendo.cloud.annotations")
}
