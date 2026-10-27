package me.clearedSpore.sporeAPI.command.cloud

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import me.clearedSpore.sporeAPI.SporeVelocityPlugin
import me.clearedSpore.sporeAPI.coroutine.SporeCoroutines
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.util.CC.mm
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.exception.CommandExecutionException
import org.incendo.cloud.exception.InjectionException
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.injection.ParameterInjector
import org.incendo.cloud.kotlin.coroutines.annotations.installCoroutineSupport
import org.incendo.cloud.velocity.VelocityCommandManager

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class SporeVelocityCommandManager(plugin: SporeVelocityPlugin) {

    val manager: VelocityCommandManager<CommandSource> = VelocityCommandManager(
        plugin.server.pluginManager.ensurePluginContainer(plugin),
        plugin.server,
        ExecutionCoordinator.simpleCoordinator(),
        SenderMapper.identity()
    )

    val annotationParser: AnnotationParser<CommandSource> = AnnotationParser(
        manager,
        CommandSource::class.java
    )

    init {
        manager.parameterInjectorRegistry().registerInjector(
            Player::class.java,
            ParameterInjector { context, _ -> context.sender() as? Player }
        )

        annotationParser.installCoroutineSupport(
            scope = SporeCoroutines.scope,
            onlyForSuspending = true
        )

        manager.exceptionController().registerHandler(InjectionException::class.java) { ctx ->
            ctx.context().sender().sendMessage("<s_red>Only players can run this command.".mm())
        }

        manager.exceptionController().registerHandler(CommandExecutionException::class.java) { ctx ->
            IncidentReporter.report(
                "command.cloud",
                ctx.exception().cause ?: ctx.exception()
            )
            ctx.context().sender().sendMessage("<s_red>An internal error occurred while running that command.".mm())
        }
    }

    fun register(instance: Any) {
        annotationParser.parse(instance)
    }
}
