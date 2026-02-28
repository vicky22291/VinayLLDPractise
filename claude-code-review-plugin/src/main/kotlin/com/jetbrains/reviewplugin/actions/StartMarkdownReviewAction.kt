package com.uber.jetbrains.reviewplugin.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.uber.jetbrains.reviewplugin.services.ReviewModeService

class StartMarkdownReviewAction : AnAction("Review this Markdown") {

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = e.project
        val visible = file != null && file.extension == "md"
        e.presentation.isVisible = visible
        e.presentation.isEnabled = if (visible && project != null) {
            val service = project.getService(ReviewModeService::class.java)
            !service.isInReviewMode(relativePath(project, file!!))
        } else {
            false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val reviewModeService = project.getService(ReviewModeService::class.java)
        reviewModeService.enterMarkdownReview(relativePath(project, file))

        DaemonCodeAnalyzer.getInstance(project).restart()

        ToolWindowManager.getInstance(project)
            .getToolWindow("Claude Code Review")
            ?.show()
    }

    companion object {
        fun relativePath(project: Project, file: VirtualFile): String {
            val basePath = project.basePath ?: return file.path
            return file.path.removePrefix("$basePath/")
        }
    }
}
