package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.CommentStatus
import com.uber.jetbrains.reviewplugin.model.ReviewComment
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import com.uber.jetbrains.reviewplugin.services.CommentService

/**
 * Business logic for the comment popup editor.
 *
 * Handles validation, comment construction, save/update/delete operations,
 * and context formatting. The actual Swing/IntelliJ popup rendering is
 * deferred to IDE integration — this class provides the pure logic.
 */
class CommentPopupEditor(
    private val commentService: CommentService,
    private val session: ReviewSession,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val selectedText: String,
    val existingComment: ReviewComment?
) {

    val isEditMode: Boolean get() = existingComment != null

    val initialCommentText: String get() = existingComment?.commentText ?: ""

    val contextPreview: String get() = truncateContext(selectedText)

    fun buildHeaderText(): String {
        return if (startLine == endLine) {
            "Comment on line $startLine"
        } else {
            "Comment on lines $startLine-$endLine"
        }
    }

    fun validateCommentText(text: String): String? {
        if (text.trim().isEmpty()) {
            return "Comment text cannot be empty"
        }
        return null
    }

    fun buildNewComment(commentText: String): ReviewComment {
        return ReviewComment(
            filePath = filePath,
            startLine = startLine,
            endLine = endLine,
            selectedText = selectedText,
            commentText = commentText.trim(),
            authorId = System.getProperty("user.name") ?: "unknown",
            status = CommentStatus.DRAFT
        )
    }

    fun saveComment(commentText: String): SaveResult {
        val error = validateCommentText(commentText)
        if (error != null) {
            return SaveResult.ValidationError(error)
        }
        val trimmed = commentText.trim()
        return if (existingComment != null) {
            commentService.updateComment(session, existingComment.id, trimmed)
            SaveResult.Updated
        } else {
            val comment = buildNewComment(trimmed)
            commentService.addComment(session, comment)
            SaveResult.Created(comment)
        }
    }

    fun deleteComment(): Boolean {
        if (existingComment == null) return false
        commentService.deleteComment(session, existingComment.id)
        return true
    }

    sealed class SaveResult {
        data class Created(val comment: ReviewComment) : SaveResult()
        data object Updated : SaveResult()
        data class ValidationError(val message: String) : SaveResult()
    }

    companion object {
        const val MAX_CONTEXT_LENGTH = 100
        const val MIN_POPUP_WIDTH = 350
        const val MIN_POPUP_HEIGHT = 200
        const val TEXT_AREA_ROWS = 4
        const val TEXT_AREA_COLUMNS = 40

        fun truncateContext(text: String, maxLength: Int = MAX_CONTEXT_LENGTH): String {
            if (text.length <= maxLength) return text
            return text.take(maxLength) + "..."
        }

        /**
         * Capture text content at a specific line from document text.
         *
         * @param documentText The full document text
         * @param lineCount Total number of lines in the document
         * @param line 1-based line number
         * @return Trimmed text at that line, or empty string if out of bounds
         */
        fun captureContextAtLine(documentText: String, lineCount: Int, line: Int): String {
            if (line < 1 || line > lineCount) return ""
            val lines = documentText.split("\n")
            val index = line - 1
            if (index >= lines.size) return ""
            return lines[index].trim()
        }

        /**
         * Capture selected context or current line context.
         *
         * @param documentText The full document text
         * @param hasSelection Whether there is an active text selection
         * @param selectionStartLine 1-based start line of selection
         * @param selectionEndLine 1-based end line of selection
         * @param selectedText The selected text (null if no selection)
         * @param caretLine 1-based line of the caret position
         * @return Pair of line range and context text
         */
        fun captureSelectionContext(
            documentText: String,
            hasSelection: Boolean,
            selectionStartLine: Int,
            selectionEndLine: Int,
            selectedText: String?,
            caretLine: Int
        ): Pair<IntRange, String> {
            if (hasSelection) {
                return (selectionStartLine..selectionEndLine) to (selectedText ?: "")
            }
            val lineCount = documentText.split("\n").size
            val context = captureContextAtLine(documentText, lineCount, caretLine)
            return (caretLine..caretLine) to context
        }
    }
}
