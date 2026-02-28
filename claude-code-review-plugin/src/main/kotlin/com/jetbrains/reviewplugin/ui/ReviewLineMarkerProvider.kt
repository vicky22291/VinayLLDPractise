package com.uber.jetbrains.reviewplugin.ui

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.uber.jetbrains.reviewplugin.actions.EditCommentAction
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import javax.swing.Icon

/**
 * LineMarkerProvider that shows gutter icons for review comments.
 * Delegates icon selection logic to [ReviewGutterIconProvider].
 */
class ReviewLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Only process leaf elements to avoid duplicate markers
        if (element.firstChild != null) return null

        val project = element.project
        val psiFile = element.containingFile ?: return null
        val file = psiFile.virtualFile ?: return null
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return null

        // Only create one marker per line: skip if this element isn't at the line's start offset
        val elementLine = document.getLineNumber(element.textRange.startOffset)
        val lineStartOffset = document.getLineStartOffset(elementLine)
        if (element.textRange.startOffset != lineStartOffset) return null

        val reviewModeService = project.getService(ReviewModeService::class.java)
        val commentService = project.getService(CommentService::class.java)

        val filePath = file.path
        val projectBasePath = project.basePath ?: return null
        val relativePath = if (filePath.startsWith(projectBasePath)) {
            filePath.removePrefix(projectBasePath).removePrefix("/")
        } else {
            filePath
        }

        if (!reviewModeService.isInReviewMode(relativePath)) return null

        val session = reviewModeService.getActiveSession(relativePath) ?: return null
        val line = elementLine + 1 // 1-based

        val provider = ReviewGutterIconProvider(reviewModeService, commentService)
        val info = provider.getIconInfoForLine(session, relativePath, line)

        val iconPath = ReviewGutterIconProvider.ICON_RESOURCE_PATHS[info.iconType] ?: return null
        val icon: Icon = IconLoader.getIcon(iconPath, ReviewLineMarkerProvider::class.java)

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { info.tooltip },
            { _, _ ->
                // Dispatch to EditComment for existing comments, AddComment for new
                val hasExistingComments = info.comments.isNotEmpty()
                val actionId = if (hasExistingComments) "ReviewPlugin.EditComment" else "ReviewPlugin.AddComment"
                val action = ActionManager.getInstance().getAction(actionId)
                if (action != null) {
                    val editor = (FileEditorManager.getInstance(project)
                        .getSelectedEditor(file) as? TextEditor)?.editor
                    if (editor != null) {
                        editor.caretModel.moveToOffset(document.getLineStartOffset(elementLine))
                        editor.selectionModel.removeSelection()
                    }
                    val dataContextBuilder = SimpleDataContext.builder()
                        .add(CommonDataKeys.PROJECT, project)
                        .add(CommonDataKeys.EDITOR, editor)
                        .add(CommonDataKeys.VIRTUAL_FILE, file)
                    if (hasExistingComments) {
                        dataContextBuilder.add(EditCommentAction.COMMENT_ID_KEY, info.comments.first().id.toString())
                    }
                    val dataContext = dataContextBuilder.build()
                    @Suppress("DEPRECATION")
                    val event = AnActionEvent.createFromAnAction(
                        action, null, "ReviewGutter", dataContext
                    )
                    action.actionPerformed(event)
                }
            },
            GutterIconRenderer.Alignment.RIGHT,
            { info.tooltip }
        )
    }
}
