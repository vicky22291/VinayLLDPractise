package com.uber.jetbrains.reviewplugin.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.uber.jetbrains.reviewplugin.model.CommentStatus
import com.uber.jetbrains.reviewplugin.model.ReviewSessionStatus
import com.uber.jetbrains.reviewplugin.services.ReviewFileManager
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.services.StorageManager
import java.awt.datatransfer.StringSelection
import java.nio.file.Path
import java.time.Instant

class PublishReviewAction : AnAction("Publish Review") {

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
            return
        }
        val service = project.getService(ReviewModeService::class.java)
        val activeSessions = service.getAllActiveSessions()
        e.presentation.isVisible = activeSessions.isNotEmpty()
        e.presentation.isEnabled = activeSessions.any {
            it.getDraftComments().isNotEmpty() || it.getDraftReplyComments().isNotEmpty()
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val reviewModeService = project.getService(ReviewModeService::class.java)
        val storageManager = project.getService(StorageManager::class.java)

        val session = reviewModeService.getAllActiveSessions().firstOrNull() ?: return

        val isReplyRound = session.reviewFilePath != null && session.getDraftReplyComments().isNotEmpty()

        if (isReplyRound) {
            val reviewFilePath = Path.of(session.reviewFilePath!!)
            ReviewFileManager.publishReplies(session, reviewFilePath)

            session.getDraftReplyComments().forEach { comment ->
                val index = session.comments.indexOf(comment)
                session.comments[index] = comment.copy(draftReply = null, status = CommentStatus.PENDING)
            }
            session.status = ReviewSessionStatus.PUBLISHED
            storageManager.deleteDrafts(session.id)

            val command = ReviewFileManager.generateCliCommand(reviewFilePath.toString())
            CopyPasteManager.getInstance().setContents(StringSelection(command))

            NotificationGroupManager.getInstance()
                .getNotificationGroup("ReviewPlugin")
                .createNotification(
                    "Replies published. Command copied to clipboard.",
                    NotificationType.INFORMATION
                )
                .notify(project)
        } else {
            if (session.getDraftComments().isEmpty()) return

            storageManager.ensureReviewDirectory()
            val outputDir = storageManager.getReviewDirectory()
            val reviewFilePath = ReviewFileManager.publish(session, outputDir)

            session.reviewFilePath = reviewFilePath.toString()
            session.publishedAt = Instant.now()
            session.status = ReviewSessionStatus.PUBLISHED
            session.comments.forEach { it.status = CommentStatus.PENDING }
            storageManager.deleteDrafts(session.id)

            val command = ReviewFileManager.generateCliCommand(reviewFilePath.toString())
            CopyPasteManager.getInstance().setContents(StringSelection(command))

            NotificationGroupManager.getInstance()
                .getNotificationGroup("ReviewPlugin")
                .createNotification(
                    "Review published. Command copied to clipboard.",
                    NotificationType.INFORMATION
                )
                .notify(project)
        }
    }
}
