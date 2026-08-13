package me.clearedSpore.sporeAPI.command.cloud

import io.papermc.paper.command.brigadier.CommandSourceStack
import me.clearedSpore.sporeAPI.SporePlugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.exception.InjectionException
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.injection.ParameterInjector
import org.incendo.cloud.paper.PaperCommandManager

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class SporeCloudCommandManager(
    plugin: SporePlugin
) {

    val manager: PaperCommandManager<CommandSourceStack> = PaperCommandManager.builder()
        .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
        .buildOnEnable(plugin)

    val annotationParser: AnnotationParser<CommandSourceStack> = AnnotationParser(
        manager,
        CommandSourceStack::class.java
    )

    init {
        manager.parameterInjectorRegistry().registerInjector(
            CommandSender::class.java,
            ParameterInjector { context, _ -> context.sender().sender }
        )

        manager.parameterInjectorRegistry().registerInjector(
            Player::class.java,
            ParameterInjector { context, _ -> context.sender().sender as? Player }
        )

        manager.exceptionController().registerHandler(InjectionException::class.java) { ctx ->
            ctx.context().sender().sender.sendMessage("§cOnly players can run this command.")
        }
    }

    fun register(instance: Any) {
        annotationParser.parse(instance)
    }
}