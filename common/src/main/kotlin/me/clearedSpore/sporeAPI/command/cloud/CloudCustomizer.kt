package me.clearedSpore.sporeAPI.command.cloud

import me.clearedSpore.sporeAPI.util.CC.mm
import me.clearedSpore.sporeAPI.util.Logger
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.incendo.cloud.CommandManager
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.string.PropertyReplacingStringProcessor
import org.incendo.cloud.caption.CaptionProvider
import org.incendo.cloud.exception.ArgumentParseException
import org.incendo.cloud.exception.CommandExecutionException
import org.incendo.cloud.exception.InjectionException
import org.incendo.cloud.exception.InvalidCommandSenderException
import org.incendo.cloud.exception.InvalidSyntaxException
import org.incendo.cloud.exception.NoPermissionException
import org.incendo.cloud.exception.NoSuchCommandException
import org.incendo.cloud.exception.parsing.ParserException
import org.incendo.cloud.minecraft.extras.AudienceProvider
import org.incendo.cloud.minecraft.extras.MinecraftHelp
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.

class CloudCustomizer<C : Any>(
    private val manager: CommandManager<C>,
    private val parser: AnnotationParser<C>,
    private val audience: (C) -> Audience,
    private val reportInternal: (Throwable) -> Unit
) {

    fun apply(spec: CloudCustomization) {
        val theme = tagResolver(spec.theme)

        applyCoreSettings(spec)
        val help = if (spec.help.enabled) buildHelp(spec) else null

        fun send(sender: C, template: String, vararg extra: TagResolver) {
            audience(sender).sendMessage((spec.theme.prefix + template).mm(theme, *extra))
        }

        registerExceptionHandlers(spec, help, ::send)

        spec.handlers.forEach { custom ->
            manager.exceptionController().registerHandler(custom.type) { ctx ->
                custom.handler(audience(ctx.context().sender()), ctx.exception())
            }
        }
    }

    private fun applyCoreSettings(spec: CloudCustomization) {
        spec.overrideExistingCommands?.let { manager.settings().set(ManagerSetting.OVERRIDE_EXISTING_COMMANDS, it) }

        spec.captions.entries.forEach { (caption, text) ->
            manager.captionRegistry().registerProvider(CaptionProvider.constantProvider(caption, text))
        }

        if (spec.placeholders.isNotEmpty()) {
            val values = spec.placeholders.toMap()
            parser.stringProcessor(PropertyReplacingStringProcessor { values[it] })
        }
    }

    private fun registerExceptionHandlers(
        spec: CloudCustomization,
        help: MinecraftHelp<C>?,
        send: (C, String, Array<out TagResolver>) -> Unit
    ) {
        val messages = spec.messages
        val controller = manager.exceptionController()

        controller.registerHandler(InjectionException::class.java) { ctx ->
            send(ctx.context().sender(), messages.playerOnly, arrayOf(Placeholder.unparsed("sender_type", "player")))
        }

        controller.registerHandler(CommandExecutionException::class.java) { ctx ->
            val failure = ctx.exception().cause ?: ctx.exception()
            val injected = unresolvedParameterType(failure)

            if (injected != null) {
                send(ctx.context().sender(), messages.playerOnly, arrayOf(Placeholder.unparsed("sender_type", injected)))
            } else {
                reportInternal(failure)
                send(ctx.context().sender(), messages.internalError, emptyArray())
            }
        }

        controller.registerHandler(InvalidCommandSenderException::class.java) { ctx ->
            val required = ctx.exception().requiredSenderTypes()
                .joinToString(", ") { (it as? Class<*>)?.simpleName ?: it.typeName }
            send(ctx.context().sender(), messages.playerOnly, arrayOf(Placeholder.unparsed("sender_type", required)))
        }

        messages.noPermission?.let { template ->
            controller.registerHandler(NoPermissionException::class.java) { ctx ->
                val permission = ctx.exception().missingPermission().permissionString()
                send(ctx.context().sender(), template, arrayOf(Placeholder.unparsed("permission", permission)))
            }
        }

        messages.unknownCommand?.let { template ->
            controller.registerHandler(NoSuchCommandException::class.java) { ctx ->
                val typed = ctx.exception().suppliedCommand()
                send(ctx.context().sender(), template, arrayOf(Placeholder.unparsed("command", typed)))
            }
        }

        messages.argumentError?.let { template ->
            controller.registerHandler(ArgumentParseException::class.java) { ctx ->
                val cause = ctx.exception().cause
                val text = (cause as? ParserException)?.formatCaption(manager.captionFormatter())
                    ?: cause?.message
                    ?: ctx.exception().message.orEmpty()
                send(ctx.context().sender(), template, arrayOf(Placeholder.unparsed("cause", text)))
            }
        }

        val helpOnSyntax = help != null && spec.help.showOnInvalidSyntax
        if (messages.invalidSyntax != null || helpOnSyntax) {
            controller.registerHandler(InvalidSyntaxException::class.java) { ctx ->
                val sender = ctx.context().sender()
                messages.invalidSyntax?.let {
                    val syntax = "/" + ctx.exception().correctSyntax()
                    send(sender, it, arrayOf(Placeholder.unparsed("syntax", syntax)))
                }
                if (helpOnSyntax) {
                    help?.queryCommands(ctx.exception().currentChain().joinToString(" ") { it.name() }, sender)
                }
            }
        }
    }

    private fun buildHelp(spec: CloudCustomization): MinecraftHelp<C>? {
        val settings = spec.help
        val root = settings.root
        if (root == null) {
            Logger.warn("Cloud help is enabled but no root command was set (help { root = \"...\" }); skipping it.")
            return null
        }

        val theme = spec.theme
        val help = MinecraftHelp.builder<C>()
            .commandManager(manager)
            .audienceProvider(AudienceProvider { audience(it) })
            .commandPrefix("/$root ${settings.label}")
            .colors(
                MinecraftHelp.helpColors(
                    color(theme.primary), color(theme.accent), color(theme.accent), color(theme.muted), color(theme.primary)
                )
            )
            .maxResultsPerPage(settings.perPage)
            .messages(settings.messages)
            .build()

        var builder = manager.commandBuilder(root)
            .literal(settings.label)
            .optional("query", StringParser.greedyStringParser())
        settings.permission?.let { builder = builder.permission(it) }

        manager.command(builder.handler { ctx -> help.queryCommands(ctx.getOrDefault("query", ""), ctx.sender()) })

        return help
    }

    private fun tagResolver(theme: CloudTheme): TagResolver = TagResolver.builder()
        .tag("primary", Tag.styling(color(theme.primary)))
        .tag("accent", Tag.styling(color(theme.accent)))
        .tag("error", Tag.styling(color(theme.error)))
        .tag("muted", Tag.styling(color(theme.muted)))
        .build()

    private fun unresolvedParameterType(failure: Throwable): String? {
        val marker = "Could not create value for parameter"
        val message = generateSequence(failure) { it.cause }
            .filterIsInstance<IllegalArgumentException>()
            .mapNotNull { it.message }
            .firstOrNull { it.startsWith(marker) }
            ?: return null

        return Regex("of type '([^']+)'").find(message)?.groupValues?.get(1)?.substringAfterLast('.')?.lowercase() ?: "player"
    }

    fun render(spec: CloudCustomization, template: String, prefixed: Boolean = true, vararg extra: TagResolver): Component =
        ((if (prefixed) spec.theme.prefix else "") + template).mm(tagResolver(spec.theme), *extra)

    private fun color(hex: String): TextColor =
        TextColor.fromHexString(hex) ?: error("Invalid colour '$hex' in cloud theme, expected #RRGGBB")
}
