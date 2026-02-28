package com.uber.jetbrains.reviewplugin.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.uber.jetbrains.reviewplugin.model.ReviewSessionStatus
import com.uber.jetbrains.reviewplugin.services.ReviewModeService

class RejectReviewAction : AnAction("Reject Review") {

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = if (project != null) {
            project.getService(ReviewModeService::class.java)
                .getAllActiveSessions()
                .any { it.status == ReviewSessionStatus.ACTIVE || it.status == ReviewSessionStatus.PUBLISHED }
        } else {
            false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val reviewModeService = project.getService(ReviewModeService::class.java)

        val session = reviewModeService.getAllActiveSessions().firstOrNull() ?: return

        val commentCount = session.comments.size
        val confirmed = Messages.showYesNoDialog(
            project,
            "Reject this review? $commentCount comment(s) will be archived.",
            "Reject Review",
            Messages.getWarningIcon()
        )
        if (confirmed != Messages.YES) return

        reviewModeService.rejectReview(session)

        DaemonCodeAnalyzer.getInstance(project).restart()

        NotificationGroupManager.getInstance()
            .getNotificationGroup("ReviewPlugin")
            .createNotification(
                "Review rejected and archived.",
                NotificationType.INFORMATION
            )
            .notify(project)
    }
}
