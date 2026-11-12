package me.clearedSpore.sporeAPI.command.cloud

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import me.clearedSpore.sporeAPI.SporeVelocityPlugin
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.util.Logger
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.injection.ParameterInjector
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.velocity.VelocityCommandManager

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class SporeVelocityCommandManager(plugin: SporeVelocityPlugin) {

    private val cloud: VelocityCommandManager<CommandSource> = VelocityCommandManager(
        plugin.server.pluginManager.ensurePluginContainer(plugin),
        plugin.server,
        ExecutionCoordinator.simpleCoordinator(),
        SenderMapper.identity()
    )

    val annotationParser: AnnotationParser<CommandSource> = AnnotationParser(
        cloud,
        CommandSource::class.java
    )

    private val customization = CloudCustomization()

    private val customizer = CloudCustomizer(
        cloud,
        annotationParser,
        { it },
        { IncidentReporter.report("command.cloud", it) }
    )

    private var applied = false

    val manager: VelocityCommandManager<CommandSource>
        get() {
            ensureApplied()
            return cloud
        }

    init {
        cloud.parameterInjectorRegistry().registerInjector(
            Player::class.java,
            ParameterInjector { context, _ -> context.sender() as? Player }
        )

        annotationParser.installCoroutineSupport(
            scope = SporeCoroutines.scope,
            onlyForSuspending = true
        )
    }

    @Synchronized
    fun customize(block: CloudCustomization.() -> Unit) {
        if (applied) {
            Logger.warn("customize() was called after commands were registered and was ignored; call it from setupCloud.")
            return
        }
        customization.block()
    }

    @Synchronized
    private fun ensureApplied() {
        if (applied) return
        applied = true
        customizer.apply(customization)
    }

    fun register(instance: Any) {
        ensureApplied()
        annotationParser.parse(instance)
    }
}
