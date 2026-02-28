package com.uber.jetbrains.reviewplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class ReviewStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = WIDGET_ID
    override fun getDisplayName(): String = "Review Mode"
    override fun createWidget(project: Project): StatusBarWidget =
        ReviewStatusBarWidget(project)

    companion object {
        const val WIDGET_ID = "ReviewModeStatus"
    }
}
