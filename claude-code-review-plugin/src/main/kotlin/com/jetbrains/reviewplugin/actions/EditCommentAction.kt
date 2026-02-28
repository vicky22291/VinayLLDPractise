package com.uber.jetbrains.reviewplugin.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.ui.CommentPopupEditor
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class EditCommentAction : AnAction("Edit Review Comment") {

    companion object {
        val COMMENT_ID_KEY: DataKey<String> = DataKey.create("ReviewPlugin.CommentId")
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = e.getData(COMMENT_ID_KEY) != null
        e.presentation.isEnabled = e.getData(COMMENT_ID_KEY) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        CommentPopupTracker.dismissActive()

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val commentId = e.getData(COMMENT_ID_KEY) ?: return

        val reviewModeService = project.getService(ReviewModeService::class.java)
        val commentService = project.getService(CommentService::class.java)
        val relativePath = AddCommentAction.resolveReviewFilePath(e) ?: return
        val session = reviewModeService.getActiveSession(relativePath) ?: return

        val existingComment = session.comments.find { it.id.toString() == commentId } ?: return

        val popupEditor = CommentPopupEditor(
            commentService, session, relativePath,
            existingComment.startLine, existingComment.endLine, existingComment.selectedText,
            existingComment
        )

        val textArea = JBTextArea(4, 40)
        textArea.text = popupEditor.initialCommentText
        val saveButton = JButton("Save")
        val deleteButton = JButton("Delete")
        val cancelButton = JButton("Cancel")

        val panel = JPanel(BorderLayout(0, 8)).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(JLabel(popupEditor.buildHeaderText()), BorderLayout.NORTH)
            add(JBScrollPane(textArea), BorderLayout.CENTER)
            val buttons = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(deleteButton)
                add(Box.createHorizontalGlue())
                add(cancelButton)
                add(Box.createRigidArea(Dimension(8, 0)))
                add(saveButton)
            }
            add(buttons, BorderLayout.SOUTH)
            preferredSize = Dimension(350, 200)
        }

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, textArea)
            .setTitle("Edit Comment")
            .setMovable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(false)
            .setCancelKeyEnabled(true)
            .createPopup()

        CommentPopupTracker.track(popup)

        saveButton.addActionListener {
            val text = textArea.text
            if (text.isNotBlank()) {
                popupEditor.saveComment(text)
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
            popup.cancel()
        }
        deleteButton.addActionListener {
            popupEditor.deleteComment()
            DaemonCodeAnalyzer.getInstance(project).restart()
            popup.cancel()
        }
        cancelButton.addActionListener { popup.cancel() }

        popup.showInBestPositionFor(editor)
    }
}
