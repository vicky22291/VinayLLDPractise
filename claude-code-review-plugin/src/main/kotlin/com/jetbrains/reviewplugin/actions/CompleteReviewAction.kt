package com.uber.jetbrains.reviewplugin.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.uber.jetbrains.reviewplugin.model.ReviewSessionStatus
import com.uber.jetbrains.reviewplugin.services.ReviewModeService

class CompleteReviewAction : AnAction("Complete Review") {

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

        val pendingCount = session.getPendingComments().size
        if (pendingCount > 0) {
            val confirmed = Messages.showYesNoDialog(
                project,
                "$pendingCount comment(s) are still pending. Complete anyway?",
                "Complete Review",
                Messages.getQuestionIcon()
            )
            if (confirmed != Messages.YES) return
        }

        reviewModeService.completeReview(session)

        DaemonCodeAnalyzer.getInstance(project).restart()

        NotificationGroupManager.getInstance()
            .getNotificationGroup("ReviewPlugin")
            .createNotification(
                "Review completed and archived.",
                NotificationType.INFORMATION
            )
            .notify(project)
    }
}
