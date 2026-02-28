package com.uber.jetbrains.reviewplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.uber.jetbrains.reviewplugin.listeners.ReviewModeListener
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import com.uber.jetbrains.reviewplugin.services.ReviewModeService

class ReviewStatusBarWidget(private val project: Project) :
    StatusBarWidget, StatusBarWidget.TextPresentation, ReviewModeListener {

    private var statusBar: StatusBar? = null

    override fun ID(): String = ReviewStatusBarWidgetFactory.WIDGET_ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String {
        val sessions = project.getService(ReviewModeService::class.java).getAllActiveSessions()
        return formatStatusText(sessions)
    }

    override fun getTooltipText(): String = "Claude Code Review status"

    override fun getAlignment(): Float = java.awt.Component.CENTER_ALIGNMENT

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        project.getService(ReviewModeService::class.java).addListener(this)
    }

    override fun dispose() {
        project.getService(ReviewModeService::class.java).removeListener(this)
        statusBar = null
    }

    override fun onReviewModeEntered(session: ReviewSession) = updateWidget()
    override fun onReviewModeExited(session: ReviewSession) = updateWidget()
    override fun onCommentsChanged(session: ReviewSession) = updateWidget()

    private fun updateWidget() {
        statusBar?.updateWidget(ID())
    }

    companion object {
        fun formatStatusText(sessions: List<ReviewSession>): String {
            if (sessions.isEmpty()) return ""
            val session = sessions.first()
            val draftCount = session.getDraftComments().size
            return "Review Mode: Active | $draftCount drafts"
        }
    }
}
