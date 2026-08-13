package me.clearedSpore.sporeAPI

import co.aikar.commands.Locales
import co.aikar.commands.MessageKeys
import co.aikar.commands.PaperCommandManager
import me.clearedSpore.sporeAPI.annotation.RegisterCommand
import me.clearedSpore.sporeAPI.annotation.RegisterListener
import me.clearedSpore.sporeAPI.bossbar.BossBarManager
import me.clearedSpore.sporeAPI.command.SporeCommand
import me.clearedSpore.sporeAPI.command.SporeCommandManager
import me.clearedSpore.sporeAPI.command.cloud.SporeCloudCommandManager
import me.clearedSpore.sporeAPI.menu.invui.SporeMenuDefaults
import me.clearedSpore.sporeAPI.serialization.SporeSerialization
import me.clearedSpore.sporeAPI.task.SporeScheduler
import me.clearedSpore.sporeAPI.task.TaskBuilder
import me.clearedSpore.sporeAPI.util.CC.accent
import me.clearedSpore.sporeAPI.util.CC.accentDark
import me.clearedSpore.sporeAPI.util.CC.error
import me.clearedSpore.sporeAPI.util.CC.translate
import me.clearedSpore.sporeAPI.util.CC.white
import me.clearedSpore.sporeAPI.util.Logger
import me.clearedSpore.sporeAPI.task.Tasks
import me.clearedSpore.sporeAPI.util.ActionBar
import me.clearedSpore.sporeAPI.util.Cooldown
import me.clearedSpore.sporeAPI.util.ItemBuilder
import org.bukkit.plugin.java.JavaPlugin
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.FilterBuilder

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


open class SporePlugin : JavaPlugin() {

    open val prefix: String = "SporeAPI &f» ".accentDark()

    val commandManager by lazy {
        SporeCommandManager(this).also {
            setupACF(it)
        }
    }

    val cloudCommandManager by lazy {
        SporeCloudCommandManager(this)
    }

    open val scanPackage: String = this.javaClass.`package`.name

    private val reflections by lazy {
        val jarUrl = this.file.toURI().toURL()

        Reflections(
            ConfigurationBuilder()
                .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
                .setUrls(jarUrl)
                .addClassLoaders(this.javaClass.classLoader)
                .filterInputsBy(FilterBuilder().includePackage(scanPackage))
        )
    }

    private val modules = mutableListOf<SporeModule>()
    private val pluginCommands = mutableListOf<SporeCommand>()

    protected fun registerCommand(command: SporeCommand) {
        pluginCommands += command
    }

    protected fun registerModule(module: SporeModule) {
        module.attach(this)
        modules += module
        Logger.info("Registered module ${module.javaClass.simpleName}")
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

    final override fun onLoad() {
        Logger.initialize(description.name)

        Logger.info("Loading plugin...")

        modules.forEach {
            Logger.info("Loading module ${it.javaClass.simpleName}...")
            it.onLoad()
        }

        onPluginLoad()
    }

    final override fun onEnable() {
        SporeMenuDefaults.register()
        Tasks.onInitialize(this)
        ActionBar.start()
        BossBarManager.init(this)
        SporeScheduler.init(this)
        SporeSerialization.init()
        ItemBuilder.init(this)
        commandManager

        Logger.info("Scanning for commands...")
        scanAndRegisterCommands()

        Logger.info("Scanning for listeners...")
        scanAndRegisterListeners()

        Logger.info("Registering module commands...")

        var moduleCount = 0

        modules.forEach { module ->
            module.getCommands().forEach { command ->
                try {
                    commandManager.registerCommand(command)
                    moduleCount++
                } catch (e: Exception) {
                    Logger.error("Failed to register module command ${command.javaClass.simpleName}")
                    e.printStackTrace()
                }
            }

            module.getCloudCommands().forEach { command ->
                try {
                    cloudCommandManager.register(command)
                    moduleCount++
                } catch (e: Exception) {
                    Logger.error("Failed to register module cloud command ${command.javaClass.simpleName}")
                    e.printStackTrace()
                }
            }

            module.getListeners().forEach {
                server.pluginManager.registerEvents(it, this)
            }

            val name = module.javaClass.simpleName
            try {
                Logger.info("Enabling module $name...")
                module.onEnable()
                Logger.info("Module $name enabled.")
            } catch (e: Exception) {
                Logger.error("Failed to enable module $name")
                e.printStackTrace()
            }
        }

        Logger.info("Registered $moduleCount module command(s)")

        Logger.info("Enabling ${modules.size} module(s)...")


        onPluginEnable()
        Logger.info("Plugin enabled.")
    }

    final override fun onDisable() {
        Logger.info("Shutting down scheduler...")
        SporeScheduler.shutdown()
        Logger.info("Cancelling all tasks...")
        ActionBar.stop()
        Tasks.cancelAll()

        Logger.info("Disabling modules...")

        modules.forEach {
            val name = it.javaClass.simpleName
            try {
                Logger.info("Disabling module $name...")
                it.onDisable()
                Logger.info("Module $name disabled.")
            } catch (e: Exception) {
                Logger.error("Failed to disable module $name")
                e.printStackTrace()
            }
        }

        onPluginDisable()
        Logger.info("Plugin disabled.")
    }

    private fun scanAndRegisterCommands() {
        val classes = reflections.getTypesAnnotatedWith(RegisterCommand::class.java)
        var acfCount = 0
        var cloudCount = 0
        Logger.info("Command scan package: $scanPackage")
        Logger.info("Annotated command classes found: ${classes.size}")

        classes.forEach { clazz ->
            try {
                val instance = kotlinObjectInstanceOrNull(clazz)
                    ?: clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()

                when {
                    SporeCommand::class.java.isAssignableFrom(clazz) -> {
                        commandManager.registerCommand(instance as SporeCommand)
                        acfCount++
                    }
                    isCloudCommand(clazz) -> {
                        cloudCommandManager.register(instance)
                        cloudCount++
                    }
                    else -> {
                        Logger.warn("Class ${clazz.simpleName} is annotated with @RegisterCommand but is neither a SporeCommand nor a cloud command")
                    }
                }

            } catch (e: Exception) {
                Logger.error("Failed to register command ${clazz.simpleName}")
                e.printStackTrace()
            }
        }

        Logger.info("Auto-registered $acfCount ACF command(s) and $cloudCount cloud command(s)")
    }

    private fun isCloudCommand(clazz: Class<*>): Boolean {
        val onClass = clazz.annotations.any {
            it.annotationClass.java.`package`.name.startsWith("org.incendo.cloud.annotations")
        }
        if (onClass) return true

        return clazz.declaredMethods.any { method ->
            method.annotations.any {
                it.annotationClass.java.`package`.name.startsWith("org.incendo.cloud.annotations")
            }
        }
    }

    private fun scanAndRegisterListeners() {
        val classes = reflections.getTypesAnnotatedWith(RegisterListener::class.java)
        var count = 0

        classes.forEach { clazz ->
            try {
                if (!org.bukkit.event.Listener::class.java.isAssignableFrom(clazz)) {
                    Logger.warn("Class ${clazz.simpleName} is annotated with @RegisterListener but does not implement Listener")
                    return@forEach
                }

                val instance = (kotlinObjectInstanceOrNull(clazz)
                    ?: clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()) as org.bukkit.event.Listener

                server.pluginManager.registerEvents(instance, this)
                count++

            } catch (e: Exception) {
                Logger.error("Failed to register listener ${clazz.simpleName}")
                e.printStackTrace()
            }
        }

        Logger.info("Auto-registered $count listener(s)")
    }

    private fun kotlinObjectInstanceOrNull(clazz: Class<*>): Any? {
        return try {
            val field = clazz.getDeclaredField("INSTANCE")
            if (java.lang.reflect.Modifier.isStatic(field.modifiers) && clazz.isAssignableFrom(field.type)) {
                field.isAccessible = true
                field.get(null)
            } else null
        } catch (e: NoSuchFieldException) {
            null
        }
    }

    open fun onPluginLoad() {}
    open fun onPluginEnable() {}
    open fun onPluginDisable() {}
}
