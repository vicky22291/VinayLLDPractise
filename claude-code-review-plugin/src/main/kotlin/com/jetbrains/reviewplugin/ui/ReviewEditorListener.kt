package com.uber.jetbrains.reviewplugin.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.IconLoader
import com.uber.jetbrains.reviewplugin.actions.EditCommentAction
import com.uber.jetbrains.reviewplugin.listeners.ReviewModeListener
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import javax.swing.Icon

/**
 * Applies line background highlights and inline comment annotations to editors for files under review.
 * Uses [LineHighlighter] for background colors and [InlayAnnotationProvider] for after-line-end text.
 */
class ReviewEditorListener : EditorFactoryListener, ReviewModeListener {

    private val lineHighlighter = LineHighlighter()
    private val inlayAnnotationProvider = InlayAnnotationProvider()
    private val trackedEditors = mutableSetOf<Editor>()
    private val registeredProjects = mutableSetOf<com.intellij.openapi.project.Project>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        val reviewModeService = project.getService(ReviewModeService::class.java)

        // Always register as listener so we receive onReviewModeEntered for late-restored sessions
        if (registeredProjects.add(project)) {
            reviewModeService.addListener(this)
        }

        val filePath = getRelativePath(editor, project.basePath) ?: return
        if (!reviewModeService.isInReviewMode(filePath)) return

        val session = reviewModeService.getActiveSession(filePath) ?: return
        trackedEditors.add(editor)
        applyHighlights(editor, session, filePath)
        applyInlays(editor, session, filePath)
        applyGutterIcons(editor, session, filePath)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        trackedEditors.remove(event.editor)
    }

    override fun onCommentsChanged(session: ReviewSession) = refreshAllEditors(session)
    override fun onResponsesLoaded(session: ReviewSession) = refreshAllEditors(session)

    override fun onReviewModeEntered(session: ReviewSession) {
        // Scan ALL open editors — not just tracked ones — to handle the startup race
        // where editors were created before sessions were restored.
        val allEditors = com.intellij.openapi.editor.EditorFactory.getInstance().allEditors
        for (editor in allEditors) {
            val project = editor.project ?: continue
            val filePath = getRelativePath(editor, project.basePath) ?: continue
            if (!isFileInSession(session, filePath)) continue
            if (trackedEditors.add(editor)) {
                applyHighlights(editor, session, filePath)
                applyInlays(editor, session, filePath)
                applyGutterIcons(editor, session, filePath)
            }
        }
    }
    override fun onReviewModeExited(session: ReviewSession) {
        // Clear highlights from all tracked editors for this session
        val toRemove = mutableListOf<Editor>()
        for (editor in trackedEditors) {
            val project = editor.project ?: continue
            val filePath = getRelativePath(editor, project.basePath) ?: continue
            val isInSession = isFileInSession(session, filePath)
            if (isInSession) {
                clearInlays(editor)
                clearHighlights(editor)
                clearGutterIcons(editor)
                toRemove.add(editor)
            }
        }
        trackedEditors.removeAll(toRemove.toSet())
    }

    private fun refreshAllEditors(session: ReviewSession) {
        for (editor in trackedEditors.toList()) {
            val project = editor.project ?: continue
            val filePath = getRelativePath(editor, project.basePath) ?: continue
            if (isFileInSession(session, filePath)) {
                clearInlays(editor)
                clearHighlights(editor)
                clearGutterIcons(editor)
                applyHighlights(editor, session, filePath)
                applyInlays(editor, session, filePath)
                applyGutterIcons(editor, session, filePath)
            }
        }
    }

    private fun applyHighlights(editor: Editor, session: ReviewSession, filePath: String) {
        val highlights = lineHighlighter.computeHighlights(session, filePath)
        val markupModel = editor.markupModel

        for (highlight in highlights) {
            val startOffset = safeLineStartOffset(editor, highlight.startLine - 1)
            val endOffset = safeLineEndOffset(editor, highlight.endLine - 1)
            if (startOffset < 0 || endOffset < 0 || startOffset > endOffset) continue

            val textAttributes = TextAttributes()
            textAttributes.backgroundColor = highlight.color
            markupModel.addRangeHighlighter(
                startOffset, endOffset, HighlighterLayer.SELECTION - 1,
                textAttributes, HighlighterTargetArea.LINES_IN_RANGE
            )
        }
    }

    private fun clearHighlights(editor: Editor) {
        val markupModel = editor.markupModel
        for (highlighter in markupModel.allHighlighters) {
            if (highlighter.layer == HighlighterLayer.SELECTION - 1) {
                markupModel.removeHighlighter(highlighter)
            }
        }
    }

    private fun applyInlays(editor: Editor, session: ReviewSession, filePath: String) {
        val blockAnnotations = inlayAnnotationProvider.computeBlockAnnotations(session, filePath)
        for (annotation in blockAnnotations) {
            val lineEndOffset = safeLineEndOffset(editor, annotation.line - 1)
            if (lineEndOffset < 0) continue
            val bgColor = ReviewGutterIconProvider.getHighlightColor(annotation.status)
                ?: java.awt.Color.LIGHT_GRAY
            val textColor = InlayAnnotationProvider.getTextColorForStatus(annotation.status)
            val renderer = ReviewBlockInlayRenderer(
                annotation.commentText, annotation.response, bgColor, textColor
            )
            editor.inlayModel.addBlockElement(lineEndOffset, true, false, 0, renderer)
        }
    }

    private fun clearInlays(editor: Editor) {
        val blockInlays = editor.inlayModel.getBlockElementsInRange(0, editor.document.textLength)
        for (inlay in blockInlays) {
            if (inlay.renderer is ReviewBlockInlayRenderer) {
                inlay.dispose()
            }
        }
    }

    private fun applyGutterIcons(editor: Editor, session: ReviewSession, filePath: String) {
        val project = editor.project ?: return
        val reviewModeService = project.getService(ReviewModeService::class.java)
        val commentService = project.getService(CommentService::class.java)
        val provider = ReviewGutterIconProvider(reviewModeService, commentService)
        val markupModel = editor.markupModel
        val doc = editor.document

        for (line in 1..doc.lineCount) {
            val info = provider.getIconInfoForLine(session, filePath, line)
            val iconPath = ReviewGutterIconProvider.ICON_RESOURCE_PATHS[info.iconType] ?: continue
            val icon: Icon = IconLoader.getIcon(iconPath, ReviewLineMarkerProvider::class.java)

            val lineIndex = line - 1
            val startOffset = doc.getLineStartOffset(lineIndex)
            val endOffset = doc.getLineEndOffset(lineIndex)
            val highlighter = markupModel.addRangeHighlighter(
                startOffset, endOffset, HighlighterLayer.LAST,
                null, HighlighterTargetArea.LINES_IN_RANGE
            )
            val vFile = FileDocumentManager.getInstance().getFile(doc)
            highlighter.gutterIconRenderer = ReviewGutterIconRendererImpl(
                icon, info.tooltip, info.comments.isNotEmpty(),
                info.comments.firstOrNull()?.id?.toString(),
                lineIndex, editor, vFile
            )
        }
    }

    private fun clearGutterIcons(editor: Editor) {
        val markupModel = editor.markupModel
        for (highlighter in markupModel.allHighlighters) {
            if (highlighter.gutterIconRenderer is ReviewGutterIconRendererImpl) {
                markupModel.removeHighlighter(highlighter)
            }
        }
    }

    private class ReviewGutterIconRendererImpl(
        private val icon: Icon,
        private val tooltip: String,
        private val hasExistingComments: Boolean,
        private val commentId: String?,
        private val lineIndex: Int,
        private val editor: Editor,
        private val vFile: com.intellij.openapi.vfs.VirtualFile?
    ) : GutterIconRenderer() {

        override fun getIcon(): Icon = icon
        override fun getTooltipText(): String = tooltip
        override fun getAlignment(): Alignment = Alignment.RIGHT

        override fun getClickAction(): com.intellij.openapi.actionSystem.AnAction {
            return object : com.intellij.openapi.actionSystem.AnAction() {
                override fun actionPerformed(e: AnActionEvent) {
                    editor.caretModel.moveToOffset(editor.document.getLineStartOffset(lineIndex))
                    editor.selectionModel.removeSelection()

                    val actionId = if (hasExistingComments) "ReviewPlugin.EditComment" else "ReviewPlugin.AddComment"
                    val action = ActionManager.getInstance().getAction(actionId) ?: return
                    val dataContextBuilder = SimpleDataContext.builder()
                        .add(CommonDataKeys.PROJECT, editor.project)
                        .add(CommonDataKeys.EDITOR, editor)
                    if (vFile != null) {
                        dataContextBuilder.add(CommonDataKeys.VIRTUAL_FILE, vFile)
                    }
                    if (hasExistingComments && commentId != null) {
                        dataContextBuilder.add(EditCommentAction.COMMENT_ID_KEY, commentId)
                    }
                    @Suppress("DEPRECATION")
                    val event = AnActionEvent.createFromAnAction(
                        action, null, "ReviewGutter", dataContextBuilder.build()
                    )
                    action.actionPerformed(event)
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ReviewGutterIconRendererImpl) return false
            return lineIndex == other.lineIndex && tooltip == other.tooltip
        }

        override fun hashCode(): Int = 31 * lineIndex + tooltip.hashCode()
    }

    private fun getRelativePath(editor: Editor, basePath: String?): String? {
        val vFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
        // LightVirtualFile in diff editors: use tagged user data for reliable path
        val tagged = vFile.getUserData(ReviewModeService.REVIEW_FILE_PATH_KEY)
        if (tagged != null) return tagged
        val path = vFile.path
        val rawPath = if (basePath != null && path.startsWith(basePath)) {
            path.removePrefix(basePath).removePrefix("/")
        } else {
            path
        }
        // Fallback: try diff-aware resolution (handles "base/" prefix for left-side diff)
        val project = editor.project
        if (project != null) {
            val resolved = project.getService(ReviewModeService::class.java).resolveDiffFilePath(rawPath)
            if (resolved != null) return resolved
        }
        return rawPath
    }

    private fun isFileInSession(session: ReviewSession, filePath: String): Boolean {
        return when (session) {
            is com.uber.jetbrains.reviewplugin.model.MarkdownReviewSession ->
                session.sourceFilePath == filePath
            is com.uber.jetbrains.reviewplugin.model.GitDiffReviewSession ->
                filePath in session.changedFiles
        }
    }

    private fun safeLineStartOffset(editor: Editor, line: Int): Int {
        val doc = editor.document
        if (line < 0 || line >= doc.lineCount) return -1
        return doc.getLineStartOffset(line)
    }

    private fun safeLineEndOffset(editor: Editor, line: Int): Int {
        val doc = editor.document
        if (line < 0 || line >= doc.lineCount) return -1
        return doc.getLineEndOffset(line)
    }
}
