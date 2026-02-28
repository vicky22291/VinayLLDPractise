package com.uber.jetbrains.reviewplugin.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.uber.jetbrains.reviewplugin.model.CommentStatus
import com.uber.jetbrains.reviewplugin.model.ReviewSessionStatus
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.services.StorageManager

class ReplyToReviewAction : AnAction("Reply to Review") {

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = if (project != null) {
            val service = project.getService(ReviewModeService::class.java)
            service.getAllActiveSessions().any { session ->
                session.status == ReviewSessionStatus.PUBLISHED &&
                    session.comments.any { it.status == CommentStatus.RESOLVED }
            }
        } else {
            false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val reviewModeService = project.getService(ReviewModeService::class.java)
        val storageManager = project.getService(StorageManager::class.java)

        val session = reviewModeService.getAllActiveSessions().firstOrNull { session ->
            session.status == ReviewSessionStatus.PUBLISHED &&
                session.comments.any { it.status == CommentStatus.RESOLVED }
        } ?: return

        session.status = ReviewSessionStatus.ACTIVE
        storageManager.saveDrafts(session)

        NotificationGroupManager.getInstance()
            .getNotificationGroup("ReviewPlugin")
            .createNotification(
                "Reply mode active. Add replies to resolved comments, then Publish.",
                NotificationType.INFORMATION
            )
            .notify(project)
    }
}
