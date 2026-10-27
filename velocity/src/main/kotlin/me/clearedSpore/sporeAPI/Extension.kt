package me.clearedSpore.sporeAPI

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import me.clearedSpore.sporeAPI.util.CC.mm
import java.util.UUID

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


object Extension {

    private val CONSOLE_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun CommandSource.success(message: String) = sendMessage("<s_blue>✔ | $message".mm())
    fun CommandSource.error(message: String) = sendMessage("<s_red>✖ | $message".mm())

    val CommandSource.uuid: UUID
        get() = if (this is Player) uniqueId else CONSOLE_UUID
}
