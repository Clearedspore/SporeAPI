package me.clearedSpore.sporeAPI.dialog

import io.papermc.paper.dialog.DialogResponseView
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import me.clearedSpore.sporeAPI.util.CC.mm

// Copyright (c) 2025 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


sealed class DialogField<T>(val key: String, val label: String) {

    internal abstract fun read(response: DialogResponseView?): T

    internal abstract fun toInput(): DialogInput
}

class TextField internal constructor(key: String, label: String) : DialogField<String>(key, label) {

    var initial: String = ""
    var maxLength: Int = 32
    var width: Int = 200
    var showLabel: Boolean = true

    private var multiline: TextDialogInput.MultilineOptions? = null

    fun multiline(maxLines: Int? = null, height: Int? = null) {
        multiline = TextDialogInput.MultilineOptions.create(maxLines, height)
    }

    override fun read(response: DialogResponseView?): String = response?.getText(key) ?: initial

    override fun toInput(): DialogInput {
        require(initial.length <= maxLength) { "Text field '$label' starts longer than its maxLength ($maxLength)" }

        val builder = DialogInput.text(key, label.mm())
            .initial(initial)
            .maxLength(maxLength)
            .width(width)
            .labelVisible(showLabel)

        multiline?.let { builder.multiline(it) }
        return builder.build()
    }
}

class Checkbox internal constructor(key: String, label: String) : DialogField<Boolean>(key, label) {

    var initial: Boolean = false

    override fun read(response: DialogResponseView?): Boolean = response?.getBoolean(key) ?: initial

    override fun toInput(): DialogInput = DialogInput.bool(key, label.mm()).initial(initial).build()
}

class Slider<T : Number> internal constructor(
    key: String,
    label: String,
    private val start: Float,
    private val end: Float,
    private val convert: (Float) -> T
) : DialogField<T>(key, label) {

    var initial: Number? = null
    var step: Number? = null
    var width: Int = 200

    var format: String? = null

    override fun read(response: DialogResponseView?): T =
        convert(response?.getFloat(key) ?: initial?.toFloat() ?: start)

    override fun toInput(): DialogInput {
        val builder = DialogInput.numberRange(key, label.mm(), start, end).width(width)

        initial?.let { builder.initial(it.toFloat()) }
        step?.let { builder.step(it.toFloat()) }
        format?.let { builder.labelFormat(it) }
        return builder.build()
    }
}

class Choice internal constructor(key: String, label: String) : DialogField<String>(key, label) {

    var width: Int = 200
    var showLabel: Boolean = true

    private val options = mutableListOf<SingleOptionDialogInput.OptionEntry>()

    fun option(id: String, display: String = id, selected: Boolean = false) {
        options += SingleOptionDialogInput.OptionEntry.create(id, display.mm(), selected)
    }

    override fun read(response: DialogResponseView?): String =
        response?.getText(key) ?: (options.firstOrNull { it.initial() } ?: options.firstOrNull())?.id().orEmpty()

    override fun toInput(): DialogInput {
        require(options.isNotEmpty()) { "Choice '$label' needs at least one option" }

        return DialogInput.singleOption(key, label.mm(), options)
            .width(width)
            .labelVisible(showLabel)
            .build()
    }
}
