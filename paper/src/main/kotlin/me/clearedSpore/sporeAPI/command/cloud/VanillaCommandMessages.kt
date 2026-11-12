package me.clearedSpore.sporeAPI.command.cloud

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import java.util.Locale

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

internal class VanillaCommandMessages {

    private var store: TranslationStore<Component>? = null

    fun install(spec: CloudCustomization, render: (template: String, prefixed: Boolean) -> Component) {
        val vanilla = spec.vanilla
        val overrides = linkedMapOf<String, Component>()

        vanilla.unknownCommand?.let { overrides["command.unknown.command"] = render(it, true) }
        vanilla.incorrectArgument?.let { overrides["command.unknown.argument"] = render(it, true) }
        vanilla.hereMarker?.let { overrides["command.context.here"] = render(it, false) }

        if (overrides.isEmpty()) return

        val created = TranslationStore.component(Key.key("sporeapi", "vanilla_commands"))
        created.defaultLocale(Locale.US)
        created.registerAll(Locale.US, overrides)

        GlobalTranslator.translator().addSource(created)
        store = created
    }

    fun uninstall() {
        store?.let { GlobalTranslator.translator().removeSource(it) }
        store = null
    }
}
