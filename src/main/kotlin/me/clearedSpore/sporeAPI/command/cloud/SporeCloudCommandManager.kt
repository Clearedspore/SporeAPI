package me.clearedSpore.sporeAPI.command.cloud

import io.papermc.paper.command.brigadier.CommandSourceStack
import me.clearedSpore.sporeAPI.SporePlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
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

    fun register(instance: Any) {
        annotationParser.parse(instance)
    }
}