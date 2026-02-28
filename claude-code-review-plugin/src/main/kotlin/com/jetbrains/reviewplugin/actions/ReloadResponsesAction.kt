package com.uber.jetbrains.reviewplugin.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.uber.jetbrains.reviewplugin.model.ReviewSessionStatus
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewFileManager
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import java.nio.file.Path

class ReloadResponsesAction : AnAction("Reload Responses") {

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
            return
        }
        val activeSessions = project.getService(ReviewModeService::class.java).getAllActiveSessions()
        e.presentation.isVisible = activeSessions.isNotEmpty()
        e.presentation.isEnabled = activeSessions.any {
            it.reviewFilePath != null && it.status == ReviewSessionStatus.PUBLISHED
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val reviewModeService = project.getService(ReviewModeService::class.java)
        val commentService = project.getService(CommentService::class.java)

        val sessions = reviewModeService.getAllActiveSessions()
            .filter { it.reviewFilePath != null }

        for (session in sessions) {
            val reviewFile = ReviewFileManager.load(Path.of(session.reviewFilePath!!))
            commentService.applyResponses(session, reviewFile)
        }

        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
