package com.uber.jetbrains.reviewplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/**
 * Factory for creating the Review Tool Window in the IDE sidebar.
 * Delegates all logic to [ReviewToolWindowPanel] and rendering to [ReviewToolWindowSwingPanel].
 */
class ReviewToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val storageManager = project.getService(
            com.uber.jetbrains.reviewplugin.services.StorageManager::class.java
        )
        val reviewModeService = project.getService(
            com.uber.jetbrains.reviewplugin.services.ReviewModeService::class.java
        )
        val commentService = project.getService(
            com.uber.jetbrains.reviewplugin.services.CommentService::class.java
        )
        val panel = ReviewToolWindowPanel(reviewModeService, commentService, storageManager)
        panel.attach()

        val swingPanel = ReviewToolWindowSwingPanel(project, panel)
        val content = toolWindow.contentManager.factory.createContent(swingPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
