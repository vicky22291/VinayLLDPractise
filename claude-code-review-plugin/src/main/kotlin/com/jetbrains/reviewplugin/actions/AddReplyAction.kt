package com.uber.jetbrains.reviewplugin.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.uber.jetbrains.reviewplugin.model.CommentStatus
import com.uber.jetbrains.reviewplugin.model.ReviewSessionStatus
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.ui.ReplyPopupEditor
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class AddReplyAction : AnAction("Add Reply") {

    override fun update(e: AnActionEvent) {
        val commentId = e.getData(EditCommentAction.COMMENT_ID_KEY)
        val project = e.project
        if (commentId == null || project == null) {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
            return
        }
        val reviewModeService = project.getService(ReviewModeService::class.java)
        val session = reviewModeService.getAllActiveSessions().firstOrNull { s ->
            s.status == ReviewSessionStatus.ACTIVE && s.comments.any { it.id.toString() == commentId }
        }
        val comment = session?.comments?.find { it.id.toString() == commentId }
        e.presentation.isVisible = comment?.status == CommentStatus.RESOLVED
        e.presentation.isEnabled = comment?.status == CommentStatus.RESOLVED
    }

    override fun actionPerformed(e: AnActionEvent) {
        CommentPopupTracker.dismissActive()

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val commentId = e.getData(EditCommentAction.COMMENT_ID_KEY) ?: return

        val reviewModeService = project.getService(ReviewModeService::class.java)
        val commentService = project.getService(CommentService::class.java)
        val session = reviewModeService.getAllActiveSessions().firstOrNull { s ->
            s.comments.any { it.id.toString() == commentId }
        } ?: return
        val comment = session.comments.find { it.id.toString() == commentId } ?: return

        val replyEditor = ReplyPopupEditor(commentService, session, comment)

        val headerLabel = JLabel("<html><pre>${escapeHtml(replyEditor.buildHeaderText())}</pre></html>")
        val textArea = JBTextArea(4, 40)
        textArea.text = replyEditor.initialReplyText
        val saveButton = JButton("Save Reply")
        val cancelButton = JButton("Cancel")

        val panel = JPanel(BorderLayout(0, 8)).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(JBScrollPane(headerLabel), BorderLayout.NORTH)
            add(JBScrollPane(textArea), BorderLayout.CENTER)
            val buttons = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(Box.createHorizontalGlue())
                add(cancelButton)
                add(Box.createRigidArea(Dimension(8, 0)))
                add(saveButton)
            }
            add(buttons, BorderLayout.SOUTH)
            preferredSize = Dimension(400, 300)
        }

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, textArea)
            .setTitle("Reply to Comment")
            .setMovable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(false)
            .setCancelKeyEnabled(true)
            .createPopup()

        CommentPopupTracker.track(popup)

        saveButton.addActionListener {
            val result = replyEditor.saveReply(textArea.text)
            if (result is ReplyPopupEditor.SaveResult.Saved) {
                DaemonCodeAnalyzer.getInstance(project).restart()
                popup.cancel()
            }
        }
        cancelButton.addActionListener { popup.cancel() }

        popup.showInBestPositionFor(editor)
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
