package com.zapshark.applydiff

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.components.service
import com.intellij.ide.BrowserUtil

class ShowWhatsNewOnUpdate : ProjectActivity {
    override suspend fun execute(project: Project) {
        val pluginId = PluginId.getId("com.zapshark.applydiff")
        val descriptor = PluginManagerCore.getPlugin(pluginId) ?: return
        val current = descriptor.version

        val state = service<WhatsNewState>()
        val last = state.lastShownVersion

        if (last == null || last != current) {
            // Show a one-time notification after update
            NotificationGroupManager.getInstance()
                .getNotificationGroup("ApplyDiff Updates")
                .createNotification(
                    "ApplyDiff updated to $current",
                    "Click to see what's new.",
                    NotificationType.INFORMATION
                )
                .addAction(
                    com.intellij.notification.NotificationAction.createSimple("Open What’s New") {
                        // Point this at your Marketplace "What's New", GitHub releases, or CHANGELOG
                        BrowserUtil.browse("https://github.com/your-org/your-repo/blob/main/CHANGELOG.md")
                    }
                )
                .notify(project)

            state.lastShownVersion = current
        }
    }
}
