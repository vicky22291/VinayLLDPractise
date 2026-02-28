package com.uber.jetbrains.reviewplugin.listeners

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.services.StorageManager

class ReviewFileWatcherStartup : ProjectActivity {

    override suspend fun execute(project: Project) {
        // 1. Register file watcher via message bus
        project.messageBus.connect()
            .subscribe(
                VirtualFileManager.VFS_CHANGES,
                ReviewFileWatcher(project)
            )

        // 2. Restore suspended sessions from drafts
        val storageManager = project.getService(StorageManager::class.java)
        val reviewModeService = project.getService(ReviewModeService::class.java)
        val restoredSessions = storageManager.loadDrafts()

        for (session in restoredSessions) {
            reviewModeService.restoreSuspendedSession(session)
        }

        if (restoredSessions.isNotEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("ReviewPlugin")
                .createNotification(
                    "${restoredSessions.size} review session(s) restored from previous session.",
                    NotificationType.INFORMATION
                )
                .notify(project)
        }
    }
}
