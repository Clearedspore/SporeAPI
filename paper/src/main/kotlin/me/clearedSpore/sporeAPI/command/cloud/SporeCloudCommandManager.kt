package me.clearedSpore.sporeAPI.command.cloud

import io.papermc.paper.command.brigadier.CommandSourceStack
import me.clearedSpore.sporeAPI.SporePlugin
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.util.Logger
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.injection.ParameterInjector
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.paper.PaperCommandManager

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class SporeCloudCommandManager(
    plugin: SporePlugin
) {

    private val cloud: PaperCommandManager<CommandSourceStack> = PaperCommandManager.builder()
        .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
        .buildOnEnable(plugin)

    val annotationParser: AnnotationParser<CommandSourceStack> = AnnotationParser(
        cloud,
        CommandSourceStack::class.java
    )

    private val customization = CloudCustomization()

    private val customizer = CloudCustomizer(
        cloud,
        annotationParser,
        { it.sender },
        { IncidentReporter.report("command.cloud", it) }
    )

    private var applied = false

    val manager: PaperCommandManager<CommandSourceStack>
        get() = cloud

    init {
        cloud.brigadierManager().setNativeNumberSuggestions(true)

        cloud.parameterInjectorRegistry().registerInjector(
            CommandSender::class.java,
            ParameterInjector { context, _ -> context.sender().sender }
        )

        cloud.parameterInjectorRegistry().registerInjector(
            Player::class.java,
            ParameterInjector { context, _ -> context.sender().sender as? Player }
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
    fun finalizeSetup() {
        if (applied) return
        applied = true
        customizer.apply(customization)
    }

    fun register(instance: Any) {
        finalizeSetup()
        annotationParser.parse(instance)
    }
}
