package me.clearedSpore.sporeAPI.acf

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.PaperCommandManager

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object ACFExtension {

    inline fun <reified T : Enum<T>> PaperCommandManager.registerEnum() {

        commandContexts.registerContext(T::class.java) { context ->
            val input = context.popFirstArg()

            try {
                enumValueOf<T>(input.uppercase())
            } catch (e: IllegalArgumentException) {
                throw InvalidCommandArgument("Value not found: $input", false)
            }
        }
    }

    fun <T : Any> PaperCommandManager.registerRegistryType(
        clazz: Class<T>,
        resolver: (String) -> T?
    ) {

        commandContexts.registerContext(clazz) { context ->
            val input = context.popFirstArg()
            resolver(input) ?: throw InvalidCommandArgument("Value not found: $input", false)
        }
    }

}