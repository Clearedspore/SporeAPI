package me.clearedSpore.sporeAPI.command

import co.aikar.commands.PaperCommandManager
import me.clearedSpore.sporeAPI.SporePlugin

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class SporeCommandManager(
    plugin: SporePlugin
) : PaperCommandManager(plugin) {


    init {
        enableUnstableAPI("help")
    }


}