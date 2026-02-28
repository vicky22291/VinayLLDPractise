package com.uber.jetbrains.reviewplugin.listeners

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewFileManager
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import java.nio.file.Path

class ReviewFileWatcher(private val project: Project) : BulkFileListener {

    override fun after(events: List<VFileEvent>) {
        for (event in events) {
            if (event !is VFileContentChangeEvent) continue
            val file = event.file
            if (!isReviewFile(file)) continue
            if (ReviewFileManager.isInternalWrite(file.path)) continue

            val reviewModeService = project.getService(ReviewModeService::class.java)
            val session = findSessionForFile(reviewModeService.getAllActiveSessions(), file.path)
                ?: continue

            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("ReviewPlugin")
                    .createNotification(
                        "Claude responded. Click to reload responses.",
                        NotificationType.INFORMATION
                    )
                    .addAction(NotificationAction.createSimple("Reload Responses") {
                        reloadResponses(session)
                    })
                    .notify(project)
            }
        }
    }

    private fun reloadResponses(session: ReviewSession) {
        val reviewFilePath = session.reviewFilePath ?: return
        val reviewFile = ReviewFileManager.load(Path.of(reviewFilePath))
        val commentService = project.getService(CommentService::class.java)
        commentService.applyResponses(session, reviewFile)
        com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart()
    }

    companion object {
        fun isReviewFile(file: VirtualFile): Boolean {
            return file.path.contains("/.review/")
                && file.name.endsWith(".review.json")
                && !file.path.contains("/.drafts/")
                && !file.path.contains("/archives/")
        }

        fun isReviewFilePath(path: String): Boolean {
            return path.contains("/.review/")
                && path.endsWith(".review.json")
                && !path.contains("/.drafts/")
                && !path.contains("/archives/")
        }

        fun findSessionForFile(sessions: List<ReviewSession>, filePath: String): ReviewSession? {
            return sessions.find { session ->
                session.reviewFilePath != null && filePath.endsWith(session.getReviewFileName())
            }
        }
    }
}
