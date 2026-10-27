package me.clearedSpore.sporeAPI

import co.aikar.commands.BaseCommand
import co.aikar.commands.Locales
import co.aikar.commands.MessageKeys
import co.aikar.commands.PaperCommandManager
import me.clearedSpore.sporeAPI.command.CommandRegistrar
import me.clearedSpore.sporeAPI.command.SporeCommandManager
import me.clearedSpore.sporeAPI.util.CC.accent
import me.clearedSpore.sporeAPI.util.CC.error
import me.clearedSpore.sporeAPI.util.CC.translate
import me.clearedSpore.sporeAPI.util.CC.white

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


open class SporeAcfPlugin : SporePlugin() {

    val commandManager by lazy {
        SporeCommandManager(this).also {
            setupACF(it)
        }
    }

    init {
        addCommandRegistrar(AcfCommandRegistrar { commandManager })
    }

    protected open fun setupACF(manager: PaperCommandManager) {
        setupLocales(manager)
    }

    private fun setupLocales(manager: PaperCommandManager) {
        val locales = manager.locales

        locales.addMessage(Locales.ENGLISH, MessageKeys.HELP_HEADER, "$prefix &fAvailable Commands:")
        locales.addMessage(Locales.ENGLISH, MessageKeys.HELP_FORMAT, "/{command}".accent() + " {parameters}".error() + " - {description}".white())
        locales.addMessage(Locales.ENGLISH, MessageKeys.HELP_PAGE_INFORMATION, "Page &f{page} ".accent() + "out of &f{totalpages}".accent() + " pages".accent())
        locales.addMessage(Locales.ENGLISH, MessageKeys.HELP_NO_RESULTS, "No results were found!".error())
        locales.addMessage(Locales.ENGLISH, MessageKeys.HELP_SEARCH_HEADER, "Results for &f{search}".accent())

        locales.addMessage(Locales.ENGLISH, MessageKeys.HELP_DETAILED_HEADER, prefix + " Command Help for &e/{command}".white())
        locales.addMessage(Locales.ENGLISH, MessageKeys.HELP_DETAILED_COMMAND_FORMAT, "Usage: &f/{command}".accent() + " {parameters}".error())
        locales.addMessage(Locales.ENGLISH, MessageKeys.HELP_DETAILED_PARAMETER_FORMAT, "&7- &f{parameter} &7- &f{description}".translate())

        locales.addMessage(Locales.ENGLISH, MessageKeys.INVALID_SYNTAX, prefix + " Use &e{command} ".error() + "{syntax}".error())
        locales.addMessage(Locales.ENGLISH, MessageKeys.PERMISSION_DENIED, prefix + "You don't have permission to use this command!".error())
        locales.addMessage(Locales.ENGLISH, MessageKeys.UNKNOWN_COMMAND, prefix + "That command does not exist!".error())
    }
}

class AcfCommandRegistrar(private val manager: () -> PaperCommandManager) : CommandRegistrar {

    override val name: String = "ACF"

    override fun supports(command: Any): Boolean = command is BaseCommand

    override fun register(command: Any) {
        manager().registerCommand(command as BaseCommand)
    }
}
