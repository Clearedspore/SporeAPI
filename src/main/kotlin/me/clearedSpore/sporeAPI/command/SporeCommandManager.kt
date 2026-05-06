package me.clearedSpore.sporeAPI.command

import me.clearedSpore.sporeAPI.SporePlugin
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class SporeCommandManager(
    plugin: SporePlugin
) {

    val manager: LegacyPaperCommandManager<CommandSender> =
        LegacyPaperCommandManager.createNative(
            plugin,
            ExecutionCoordinator.simpleCoordinator()
        )

    private val annotationParser = AnnotationParser(manager, CommandSender::class.java)

    fun registerCommand(command: Any) {
        annotationParser.parse(command)
    }

}
