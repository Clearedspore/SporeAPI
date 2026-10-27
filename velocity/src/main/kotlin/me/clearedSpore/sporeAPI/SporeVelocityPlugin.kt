package me.clearedSpore.sporeAPI

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import me.clearedSpore.sporeAPI.annotation.RegisterCommand
import me.clearedSpore.sporeAPI.annotation.RegisterListener
import me.clearedSpore.sporeAPI.command.cloud.SporeVelocityCommandManager
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.debug.LifecyclePhase
import me.clearedSpore.sporeAPI.debug.SporeDebug
import me.clearedSpore.sporeAPI.scan.SporeScanner
import java.nio.file.Path
import org.slf4j.Logger as Slf4jLogger
import me.clearedSpore.sporeAPI.util.Logger

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


abstract class SporeVelocityPlugin(
    val server: ProxyServer,
    val logger: Slf4jLogger,
    val dataDirectory: Path
) {

    open val pluginName: String
        get() = javaClass.getAnnotation(Plugin::class.java)?.name?.takeIf { it.isNotBlank() } ?: javaClass.simpleName

    val commandManager by lazy { SporeVelocityCommandManager(this) }

    private val pendingCommands = mutableListOf<Any>()
    private var commandsReady = false

    protected open fun setupCloud(manager: SporeVelocityCommandManager) {}

    protected fun registerCommand(command: Any) {
        if (commandsReady) {
            registerCommandNow(command)
        } else {
            pendingCommands += command
        }
    }

    protected fun registerListener(listener: Any) {
        server.eventManager.register(this, listener)
    }

    @Subscribe
    fun onSporeProxyInitialize(event: ProxyInitializeEvent) {
        SporeCore.init(VelocityPlatform(pluginName, javaClass.packageName, logger))
        SporeVelocity.init(this)
        VelocityPlatform.installStaffAlerts(server)
        Logger.initialize(pluginName)

        setupCloud(commandManager)
        scanAndRegisterCommands()
        commandsReady = true
        pendingCommands.forEach(::registerCommandNow)
        pendingCommands.clear()

        scanAndRegisterListeners()

        try {
            onPluginEnable()
        } catch (exception: Exception) {
            IncidentReporter.report("plugin.enable", exception)
        }

        SporeDebug.phase = LifecyclePhase.RUNNING
        Logger.info("Plugin enabled.")
    }

    @Subscribe
    fun onSporeProxyShutdown(event: ProxyShutdownEvent) {
        SporeDebug.phase = LifecyclePhase.STOPPING

        try {
            onPluginDisable()
        } catch (exception: Exception) {
            IncidentReporter.report("plugin.disable", exception)
        }

        SporeCoroutines.shutdown()
        Logger.info("Plugin disabled.")
    }

    open fun onPluginEnable() {}
    open fun onPluginDisable() {}

    private fun scanAndRegisterCommands() {
        var count = 0

        SporeScanner.annotatedWith(javaClass, RegisterCommand::class.java).forEach { clazz ->
            try {
                if (registerCommandNow(SporeScanner.instantiate(clazz))) count++
            } catch (exception: Exception) {
                IncidentReporter.report("command.register", exception, details = mapOf("class" to clazz.name))
            }
        }

        Logger.info("Auto-registered $count command(s)")
    }

    private fun registerCommandNow(command: Any): Boolean = try {
        commandManager.register(command)
        true
    } catch (exception: Exception) {
        IncidentReporter.report("command.register", exception, details = mapOf("command" to command.javaClass.name))
        false
    }

    private fun scanAndRegisterListeners() {
        var count = 0

        SporeScanner.annotatedWith(javaClass, RegisterListener::class.java).forEach { clazz ->
            try {
                registerListener(SporeScanner.instantiate(clazz))
                count++
            } catch (exception: Exception) {
                IncidentReporter.report("listener.register", exception, details = mapOf("class" to clazz.name))
            }
        }

        Logger.info("Auto-registered $count listener(s)")
    }
}

object SporeVelocity {

    @Volatile
    private var pluginRef: SporeVelocityPlugin? = null

    val plugin: SporeVelocityPlugin
        get() = pluginRef ?: error("SporeVelocity is not initialised yet - is your plugin extending SporeVelocityPlugin?")

    val server: ProxyServer get() = plugin.server

    val isInitialized: Boolean get() = pluginRef != null

    internal fun init(plugin: SporeVelocityPlugin) {
        pluginRef = plugin
    }
}
