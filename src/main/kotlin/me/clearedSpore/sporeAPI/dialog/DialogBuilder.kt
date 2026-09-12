package me.clearedSpore.sporeAPI.dialog

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import me.clearedSpore.sporeAPI.util.CC.mm
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.roundToInt

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


enum class AfterClick(internal val paper: DialogBase.DialogAfterAction) {
    CLOSE(DialogBase.DialogAfterAction.CLOSE),

    KEEP_OPEN(DialogBase.DialogAfterAction.NONE),

    WAIT_FOR_RESPONSE(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
}

enum class DialogCloseReason {
    BUTTON,

    EXIT,

    CLOSED,

    REPLACED,

    QUIT
}

class DialogBuilder internal constructor() {

    var closeWithEscape: Boolean = true
    var afterClick: AfterClick = AfterClick.CLOSE
    var columns: Int = 2

    private var title: Component = Component.empty()
    private var externalTitle: Component? = null

    private val body = mutableListOf<DialogBody>()
    private val fields = mutableListOf<DialogField<*>>()
    private val buttons = mutableListOf<DialogButton>()

    private var confirmation: Pair<DialogButton, DialogButton>? = null
    private var ok: DialogButton? = null
    private var exit: DialogButton? = null
    private var exitSet = false

    internal val closeHandlers = mutableListOf<(DialogCloseReason) -> Unit>()

    fun title(text: String) {
        title = text.mm()
    }

    fun title(text: Component) {
        title = text
    }

    fun externalTitle(text: String) {
        externalTitle = text.mm()
    }

    fun message(text: String, width: Int = 300) = message(text.mm(), width)

    fun message(text: Component, width: Int = 300) {
        body += DialogBody.plainMessage(text, width)
    }


    fun item(
        item: ItemStack,
        description: String? = null,
        decorations: Boolean = true,
        tooltip: Boolean = true,
        width: Int = 16,
        height: Int = 16
    ) {
        val builder = DialogBody.item(item)
            .showDecorations(decorations)
            .showTooltip(tooltip)
            .width(width)
            .height(height)

        description?.let { builder.description(DialogBody.plainMessage(it.mm())) }
        body += builder.build()
    }

    fun textField(label: String, key: String? = null, configure: TextField.() -> Unit = {}): TextField =
        add(TextField(keyFor(key), label).apply(configure))

    fun checkbox(label: String, key: String? = null, configure: Checkbox.() -> Unit = {}): Checkbox =
        add(Checkbox(keyFor(key), label).apply(configure))

    fun slider(
        label: String,
        range: ClosedFloatingPointRange<Float>,
        key: String? = null,
        configure: Slider<Float>.() -> Unit = {}
    ): Slider<Float> =
        add(Slider(keyFor(key), label, range.start, range.endInclusive) { it }.apply(configure))

    fun slider(label: String, range: IntRange, key: String? = null, configure: Slider<Int>.() -> Unit = {}): Slider<Int> =
        add(
            Slider(keyFor(key), label, range.first.toFloat(), range.last.toFloat()) { it.roundToInt() }
                .apply { step = 1 }
                .apply(configure)
        )

    fun choice(label: String, key: String? = null, configure: Choice.() -> Unit): Choice =
        add(Choice(keyFor(key), label).apply(configure))

    fun button(label: String, tooltip: String? = null, width: Int = 150, onClick: (DialogClick) -> Unit) {
        buttons += DialogButton(label.mm(), tooltip?.mm(), width, handler = onClick)
    }

    fun commandButton(label: String, command: String, tooltip: String? = null, width: Int = 150) =
        button(label, tooltip, width) { it.player.performCommand(command.removePrefix("/")) }

    fun linkButton(label: String, url: String, tooltip: String? = null, width: Int = 150) {
        buttons += DialogButton(label.mm(), tooltip?.mm(), width, link = ClickEvent.openUrl(url))
    }

    fun exitButton(label: String = "Close", tooltip: String? = null, width: Int = 150, onClick: (DialogClick) -> Unit = {}) {
        exit = DialogButton(label.mm(), tooltip?.mm(), width, handler = onClick)
        exitSet = true
    }

    fun noExitButton() {
        exit = null
        exitSet = true
    }

    fun confirm(yes: String = "Yes", no: String = "No", onNo: (DialogClick) -> Unit = {}, onYes: (DialogClick) -> Unit) {
        confirmation = DialogButton(yes.mm(), null, 150, handler = onYes) to DialogButton(no.mm(), null, 150, handler = onNo)
    }

    fun okButton(label: String = "OK", onClick: (DialogClick) -> Unit = {}) {
        ok = DialogButton(label.mm(), null, 150, handler = onClick)
    }

    fun onClose(handler: (DialogCloseReason) -> Unit) {
        closeHandlers += handler
    }

    internal fun buttonFor(id: String): DialogButton? = when (id) {
        EXIT -> resolvedExit()
        YES -> confirmation?.first
        NO -> confirmation?.second
        OK -> resolvedOk()
        else -> id.removePrefix("b").toIntOrNull()?.let { buttons.getOrNull(it) }
    }

    internal fun toDialog(action: (String) -> DialogAction): Dialog {
        val confirmation = confirmation
        check(confirmation == null || buttons.isEmpty()) { "A dialog can use confirm(...) or button(...), not both" }

        val base = DialogBase.builder(title)
            .canCloseWithEscape(closeWithEscape)
            .pause(false)
            .afterAction(afterClick.paper)
            .body(body)
            .inputs(fields.map { it.toInput() })

        externalTitle?.let { base.externalTitle(it) }

        val type = when {
            confirmation != null -> DialogType.confirmation(
                confirmation.first.toPaper(action(YES)),
                confirmation.second.toPaper(action(NO))
            )
            buttons.isNotEmpty() -> {
                val multi = DialogType.multiAction(buttons.mapIndexed { index, button -> button.toPaper(action("b$index")) })
                    .columns(columns)

                resolvedExit()?.let { multi.exitAction(it.toPaper(action(EXIT))) }
                multi.build()
            }
            else -> DialogType.notice(resolvedOk().toPaper(action(OK)))
        }

        val built = base.build()
        return Dialog.create { factory -> factory.empty().base(built).type(type) }
    }

    private fun <F : DialogField<*>> add(field: F): F {
        fields += field
        return field
    }

    private fun keyFor(key: String?): String {
        val resolved = key ?: "input_${fields.size}"

        require(KEY.matches(resolved)) { "Input key '$resolved' may only use letters, digits and _" }
        require(fields.none { it.key == resolved }) { "Two inputs use the key '$resolved'" }
        return resolved
    }

    private val defaultOk by lazy { DialogButton("OK".mm(), null, 150) }
    private val defaultExit by lazy { DialogButton("Close".mm(), null, 150) }

    private fun resolvedOk(): DialogButton = ok ?: defaultOk

    private fun resolvedExit(): DialogButton? = when {
        exitSet -> exit
        closeWithEscape -> defaultExit
        else -> null
    }

    internal companion object {
        const val EXIT = "exit"
        const val YES = "yes"
        const val NO = "no"
        const val OK = "ok"

        private val KEY = Regex("[a-zA-Z0-9_]+")
    }
}

internal class DialogButton(
    val label: Component,
    val tooltip: Component?,
    val width: Int,
    val link: ClickEvent<*>? = null,
    val handler: ((DialogClick) -> Unit)? = null
) {

    fun toPaper(action: DialogAction): ActionButton {
        val builder = ActionButton.builder(label)
            .width(width)
            .action(link?.let { DialogAction.staticAction(it) } ?: action)

        tooltip?.let { builder.tooltip(it) }
        return builder.build()
    }
}
