package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.ReviewComment
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import com.uber.jetbrains.reviewplugin.services.CommentService

/**
 * Business logic for the reply popup editor.
 *
 * Handles validation, reply text formatting, and save operations.
 * The actual Swing/IntelliJ popup rendering is deferred to IDE integration —
 * this class provides the pure logic.
 */
class ReplyPopupEditor(
    private val commentService: CommentService,
    private val session: ReviewSession,
    val comment: ReviewComment
) {

    val initialReplyText: String get() = comment.draftReply ?: ""

    fun buildHeaderText(): String {
        val response = comment.claudeResponse ?: return "No response yet"
        val truncated = if (response.length > MAX_RESPONSE_PREVIEW_LENGTH) {
            response.take(MAX_RESPONSE_PREVIEW_LENGTH) + "..."
        } else {
            response
        }
        return "Claude's response:\n$truncated"
    }

    fun validateReplyText(text: String): String? {
        if (text.trim().isEmpty()) {
            return "Reply text cannot be empty"
        }
        return null
    }

    fun saveReply(replyText: String): SaveResult {
        val error = validateReplyText(replyText)
        if (error != null) {
            return SaveResult.ValidationError(error)
        }
        commentService.addReply(session, comment.id, replyText.trim())
        return SaveResult.Saved
    }

    sealed class SaveResult {
        data object Saved : SaveResult()
        data class ValidationError(val message: String) : SaveResult()
    }

    companion object {
        const val MAX_RESPONSE_PREVIEW_LENGTH = 200
    }
}
