package me.clearedSpore.sporeAPI

import com.velocitypowered.api.proxy.ProxyServer
import me.clearedSpore.sporeAPI.debug.IncidentFormat
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.debug.model.Severity
import org.slf4j.Logger

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class VelocityPlatform(
    override val name: String,
    override val basePackage: String,
    private val logger: Logger
) : SporePlatform {

    override fun isMainThread(): Boolean = false

    override fun log(line: String) {
        val plain = SporeCore.stripLegacy(line)

        when {
            "(error)" in plain -> logger.error(plain)
            "(warn)" in plain -> logger.warn(plain)
            else -> logger.info(plain)
        }
    }

    companion object {

        @Volatile
        private var alertsInstalled = false

        internal fun installStaffAlerts(server: ProxyServer) {
            if (alertsInstalled) return
            alertsInstalled = true

            IncidentReporter.onIncident { incident ->
                val permission = IncidentReporter.notifyPermission ?: return@onIncident
                if (incident.severity == Severity.WARNING) return@onIncident

                val message = IncidentFormat.chatAlert(incident)
                server.allPlayers
                    .filter { it.hasPermission(permission) }
                    .forEach { it.sendMessage(message) }
            }
        }
    }
}
