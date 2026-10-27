package me.clearedSpore.sporeAPI

import me.clearedSpore.sporeAPI.debug.IncidentFormat
import me.clearedSpore.sporeAPI.debug.IncidentReporter
import me.clearedSpore.sporeAPI.debug.model.Severity
import me.clearedSpore.sporeAPI.task.Tasks
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

// Copyright (c) 2025-2026 ClearedSpore
// Licensed under the MIT License. See LICENSE file in the project root for details.


class PaperPlatform(private val plugin: JavaPlugin) : SporePlatform {

    override val name: String get() = plugin.name

    override val basePackage: String = plugin.javaClass.packageName

    override fun isMainThread(): Boolean = Bukkit.isPrimaryThread()

    override fun log(line: String) = Bukkit.getConsoleSender().sendMessage(line)

    companion object {

        @Volatile
        private var alertsInstalled = false

        internal fun installStaffAlerts() {
            if (alertsInstalled) return
            alertsInstalled = true

            IncidentReporter.onIncident { incident ->
                val permission = IncidentReporter.notifyPermission ?: return@onIncident
                if (incident.severity == Severity.WARNING || !SporeApi.isInitialized) return@onIncident

                val message = IncidentFormat.chatAlert(incident)
                val send = Runnable {
                    Bukkit.getOnlinePlayers()
                        .filter { it.hasPermission(permission) }
                        .forEach { it.sendMessage(message) }
                }

                when {
                    Bukkit.isPrimaryThread() -> send.run()
                    SporeApi.plugin.isEnabled -> Tasks.run(send)
                }
            }
        }
    }
}
