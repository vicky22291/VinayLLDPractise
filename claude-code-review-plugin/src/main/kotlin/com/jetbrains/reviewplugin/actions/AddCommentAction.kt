package com.uber.jetbrains.reviewplugin.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
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

class AddCommentAction : AnAction("Add Review Comment") {

    companion object {
        fun resolveReviewFilePath(e: AnActionEvent): String? {
            val project = e.project ?: return null
            val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
            val basePath = project.basePath
            val reviewModeService = project.getService(ReviewModeService::class.java)

            // Try VIRTUAL_FILE from data context first
            val contextFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
            if (contextFile != null) {
                // LightVirtualFile in diff editors: use tagged user data for reliable path
                val tagged = contextFile.getUserData(ReviewModeService.REVIEW_FILE_PATH_KEY)
                if (tagged != null) return tagged
                val rawPath = if (basePath != null && contextFile.path.startsWith(basePath)) {
                    contextFile.path.removePrefix("$basePath/")
                } else {
                    contextFile.path
                }
                return reviewModeService.resolveDiffFilePath(rawPath) ?: rawPath
            }

            // Fallback: get file from document (diff editors with LightVirtualFile)
            val docFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
            val tagged = docFile.getUserData(ReviewModeService.REVIEW_FILE_PATH_KEY)
            if (tagged != null) return tagged
            val path = docFile.path
            val rawPath = if (basePath != null && path.startsWith(basePath)) {
                path.removePrefix("$basePath/")
            } else {
                path
            }
            return reviewModeService.resolveDiffFilePath(rawPath) ?: rawPath
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val project = e.project
        val filePath = resolveReviewFilePath(e)
        val visible = editor != null && project != null && filePath != null
        e.presentation.isVisible = visible
        e.presentation.isEnabled = if (visible) {
            val service = project!!.getService(ReviewModeService::class.java)
            service.isInReviewMode(filePath!!)
        } else {
            false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        CommentPopupTracker.dismissActive()

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val relativePath = resolveReviewFilePath(e) ?: return

        val reviewModeService = project.getService(ReviewModeService::class.java)
        val commentService = project.getService(CommentService::class.java)
        val session = reviewModeService.getActiveSession(relativePath) ?: return

        val document = editor.document
        val (lineRange, selectedText) = CommentPopupEditor.captureSelectionContext(
            documentText = document.text,
            hasSelection = editor.selectionModel.hasSelection(),
            selectionStartLine = document.getLineNumber(editor.selectionModel.selectionStart) + 1,
            selectionEndLine = document.getLineNumber(editor.selectionModel.selectionEnd) + 1,
            selectedText = editor.selectionModel.selectedText,
            caretLine = document.getLineNumber(editor.caretModel.offset) + 1
        )

        val popupEditor = CommentPopupEditor(
            commentService, session, relativePath,
            lineRange.first, lineRange.last, selectedText,
            null
        )

        val textArea = JBTextArea(4, 40)
        val saveButton = JButton("Save")
        val cancelButton = JButton("Cancel")

        val panel = JPanel(BorderLayout(0, 8)).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(JLabel(popupEditor.buildHeaderText()), BorderLayout.NORTH)
            add(JBScrollPane(textArea), BorderLayout.CENTER)
            val buttons = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
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
            .setTitle("Add Comment")
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
        cancelButton.addActionListener { popup.cancel() }

        popup.showInBestPositionFor(editor)
    }
}
