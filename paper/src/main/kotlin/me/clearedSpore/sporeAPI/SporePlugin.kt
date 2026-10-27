package me.clearedSpore.sporeAPI

import me.clearedSpore.sporeAPI.annotation.RegisterCommand
import me.clearedSpore.sporeAPI.annotation.RegisterListener
import me.clearedSpore.sporeAPI.annotation.SubscribeEvents
import me.clearedSpore.sporeAPI.bossbar.BossBarManager
import me.clearedSpore.sporeAPI.command.CloudCommandRegistrar
import me.clearedSpore.sporeAPI.command.CommandRegistrar
import me.clearedSpore.sporeAPI.command.cloud.SporeCloudCommandManager
import me.clearedSpore.sporeAPI.coroutine.BukkitMainDispatcher
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.debug.LifecyclePhase
import me.clearedSpore.sporeAPI.debug.SporeDebug
import me.clearedSpore.sporeAPI.dialog.SporeDialogs
import me.clearedSpore.sporeAPI.event.SporeListeners
import me.clearedSpore.sporeAPI.menu.invui.SporeMenuDefaults
import me.clearedSpore.sporeAPI.scan.SporeScanner
import me.clearedSpore.sporeAPI.scan.forPlugin
import me.clearedSpore.sporeAPI.scan.methodsAnnotatedWith
import me.clearedSpore.sporeAPI.scoreboard.SidebarManager
import me.clearedSpore.sporeAPI.serialization.SporeSerialization
import me.clearedSpore.sporeAPI.tablist.TablistManager
import me.clearedSpore.sporeAPI.task.SporeScheduler
import me.clearedSpore.sporeAPI.task.Tasks
import me.clearedSpore.sporeAPI.util.ActionBar
import me.clearedSpore.sporeAPI.util.CC.accentDark
import me.clearedSpore.sporeAPI.util.FoliaUtil
import me.clearedSpore.sporeAPI.util.ItemBuilder
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.invui.InvUI

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
open class SporePlugin : JavaPlugin() {

    open val prefix: String = "SporeAPI &f» ".accentDark()

    val cloudCommandManager by lazy {
        SporeCloudCommandManager(this)
    }

    open val scanPackage: String = this.javaClass.`package`.name

    private val reflections by lazy { SporeScanner.forPlugin(this) }

    private val modules = mutableListOf<SporeModule>()

    private val commandRegistrars = mutableListOf<CommandRegistrar>()
    private val cloudRegistrar = CloudCommandRegistrar { cloudCommandManager }

    private val pendingCommands = mutableListOf<Any>()
    private var commandsReady = false

    protected fun registerCommand(command: Any) {
        if (commandsReady) {
            registerCommandNow(command, "plugin.registerCommand")
        } else {
            pendingCommands += command
        }
    }

    protected fun addCommandRegistrar(registrar: CommandRegistrar) {
        commandRegistrars += registrar
    }

    protected fun registerModule(module: SporeModule) {
        module.attach(this)
        modules += module
        Logger.info("Registered module ${module.javaClass.simpleName}")
    }

    protected open fun setupCloud(manager: SporeCloudCommandManager) {}

    final override fun onLoad() {
        SporeCore.init(PaperPlatform(this))
        SporeCoroutines.useMainDispatcher(BukkitMainDispatcher(this))
        PaperPlatform.installStaffAlerts()
        Logger.initialize(description.name)

        Logger.info("Loading plugin...")

        modules.forEach {
            Logger.info("Loading module ${it.javaClass.simpleName}...")
            it.onLoad()
        }

        onPluginLoad()
    }

    final override fun onEnable() {
        Logger.info("Detected server platform: ${if (FoliaUtil.isFolia) "Folia" else "Paper/Spigot"}")

        SporeApi.init(this)
        InvUI.getInstance().setPlugin(this)
        SporeMenuDefaults.register()
        Tasks.onInitialize(this)
        ActionBar.start()
        BossBarManager.init(this)
        SporeScheduler.init(this)
        SporeSerialization.init()
        ItemBuilder.init(this)
        setupCloud(cloudCommandManager)

        Logger.info("Scanning for commands...")
        scanAndRegisterCommands()

        commandsReady = true
        pendingCommands.forEach { registerCommandNow(it, "plugin.registerCommand") }
        pendingCommands.clear()

        Logger.info("Scanning for listeners...")
        scanAndRegisterListeners()

        Logger.info("Scanning for event subscribers...")
        scanAndRegisterEventSubscribers()

        Logger.info("Registering module commands...")

        var moduleCount = 0

        modules.forEach { module ->
            val name = module.javaClass.simpleName

            (module.getCommands() + module.getCloudCommands()).forEach { command ->
                if (registerCommandNow(
                        command,
                        "module.registerCommand",
                        mapOf("module" to name)
                    ) != null
                ) moduleCount++
            }

            module.getListeners().forEach {
                SporeListeners.register(it)
            }

            try {
                Logger.info("Enabling module $name...")
                module.onEnable()
                Logger.info("Module $name enabled.")
            } catch (e: Exception) {
                IncidentReporter.report("module.enable", e, details = mapOf("module" to name))
            }
        }

        Logger.info("Registered $moduleCount module command(s)")

        Logger.info("Enabling ${modules.size} module(s)...")


        onPluginEnable()

        SporeDebug.phase = LifecyclePhase.RUNNING
        Logger.info("Plugin enabled.")
    }

    final override fun onDisable() {
        SporeDebug.phase = LifecyclePhase.STOPPING

        SporeCoroutines.shutdown()

        SporeDialogs.shutdown()
        SidebarManager.shutdown()
        TablistManager.shutdown()
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
                IncidentReporter.report("module.disable", e, details = mapOf("module" to name))
            }
        }

        onPluginDisable()
        Logger.info("Plugin disabled.")
    }

    private fun scanAndRegisterCommands() {
        val classes = reflections.getTypesAnnotatedWith(RegisterCommand::class.java)
        val counts = linkedMapOf<String, Int>()
        Logger.info("Command scan package: $scanPackage")
        Logger.info("Annotated command classes found: ${classes.size}")

        classes.forEach { clazz ->
            try {
                val instance = SporeScanner.objectInstanceOrNull(clazz)
                    ?: clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()

                registerCommandNow(instance, "command.register")?.let { counts.merge(it, 1, Int::plus) }
            } catch (e: Exception) {
                IncidentReporter.report("command.register", e, details = mapOf("class" to clazz.name))
            }
        }

        val summary = counts.entries.joinToString(", ") { (framework, count) -> "$count $framework" }.ifEmpty { "0" }
        Logger.info("Auto-registered command(s): $summary")
    }

    private fun looksLikeAcf(clazz: Class<*>): Boolean =
        generateSequence(clazz.superclass) { it.superclass }.any { it.simpleName == "BaseCommand" }

    private fun registerCommandNow(
        command: Any,
        operation: String,
        details: Map<String, String> = emptyMap()
    ): String? {
        val registrar = (commandRegistrars + cloudRegistrar).firstOrNull { it.supports(command) }

        if (registrar == null) {
            val frameworks = (commandRegistrars + cloudRegistrar).joinToString(" or ") { it.name }
            Logger.warn(
                "${command.javaClass.simpleName} is not a command any registered framework understands ($frameworks)" +
                        if (looksLikeAcf(command.javaClass)) " - ACF commands need a main class extending SporeAcfPlugin (sporeapi-acf)" else ""
            )
            return null
        }

        return try {
            registrar.register(command)
            registrar.name
        } catch (e: Exception) {
            IncidentReporter.report(
                operation,
                e,
                details = details + mapOf("command" to command.javaClass.name, "framework" to registrar.name)
            )
            null
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

                val instance = (SporeScanner.objectInstanceOrNull(clazz)
                    ?: clazz.getDeclaredConstructor().apply { isAccessible = true }
                        .newInstance()) as org.bukkit.event.Listener

                SporeListeners.register(instance)
                count++

            } catch (e: Exception) {
                IncidentReporter.report("listener.register", e, details = mapOf("class" to clazz.name))
            }
        }

        Logger.info("Auto-registered $count listener(s)")
    }

    private fun scanAndRegisterEventSubscribers() {
        val methods = SporeScanner.methodsAnnotatedWith(this, SubscribeEvents::class.java)
        var count = 0

        methods.forEach { method ->
            try {
                if (method.parameterCount != 0) {
                    Logger.warn(
                        "Method ${method.declaringClass.simpleName}#${method.name} is annotated with " +
                                "@SubscribeEvents but takes parameters, so it cannot be auto-invoked"
                    )
                    return@forEach
                }

                val instance = SporeScanner.objectInstanceOrNull(method.declaringClass)
                    ?: method.declaringClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()

                method.isAccessible = true
                method.invoke(instance)
                count++

            } catch (e: Exception) {
                IncidentReporter.report(
                    "event.subscribe", e,
                    details = mapOf("class" to method.declaringClass.name, "method" to method.name)
                )
            }
        }

        Logger.info("Auto-invoked $count event subscriber method(s)")
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
