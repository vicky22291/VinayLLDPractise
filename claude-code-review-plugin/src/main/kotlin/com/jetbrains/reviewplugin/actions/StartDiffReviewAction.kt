package com.uber.jetbrains.reviewplugin.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import com.uber.jetbrains.reviewplugin.model.GitDiffReviewSession
import com.uber.jetbrains.reviewplugin.model.ReviewSessionStatus
import com.uber.jetbrains.reviewplugin.services.GitDiffService
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.ui.BranchSelectionDialog

class StartDiffReviewAction : AnAction("Review the Diff") {

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isVisible = false
            return
        }
        val gitDiffService = GitDiffService(project)
        val hasRepo = gitDiffService.hasGitRepository()
        e.presentation.isVisible = hasRepo
        e.presentation.isEnabled = hasRepo && !hasDiffReviewActive(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val gitDiffService = GitDiffService(project)

        val dialog = BranchSelectionDialog(project, gitDiffService)
        if (!dialog.showAndGet()) return

        val baseBranch = dialog.getBaseBranch()
        val compareBranch = dialog.getCompareBranch()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading diff...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Finding changed files..."
                val changedFiles = gitDiffService.getChangedFiles(baseBranch, compareBranch)

                if (changedFiles.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(project, "No changes between branches.", "Review the Diff")
                    }
                    return
                }

                indicator.text = "Loading file contents..."
                val contents = gitDiffService.loadDiffContents(baseBranch, compareBranch, changedFiles)

                ApplicationManager.getApplication().invokeLater {
                    val reviewModeService = project.getService(ReviewModeService::class.java)
                    reviewModeService.enterDiffReview(baseBranch, compareBranch, changedFiles)

                    gitDiffService.showDiffView(baseBranch, compareBranch, contents)

                    DaemonCodeAnalyzer.getInstance(project).restart()
                    ToolWindowManager.getInstance(project)
                        .getToolWindow("Claude Code Review")
                        ?.show()
                }
            }
        })
    }

    private fun hasDiffReviewActive(project: com.intellij.openapi.project.Project): Boolean {
        return project.getService(ReviewModeService::class.java)
            .getAllActiveSessions()
            .any { it is GitDiffReviewSession && it.status == ReviewSessionStatus.ACTIVE }
    }
}
