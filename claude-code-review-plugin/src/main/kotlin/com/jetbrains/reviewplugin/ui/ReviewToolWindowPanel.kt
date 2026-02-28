package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.listeners.ReviewModeListener
import com.uber.jetbrains.reviewplugin.model.*
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewFileManager
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.services.StorageManager
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

/**
 * Business logic for the Review Tool Window side panel.
 *
 * Manages panel state, computes display data for comment rows, and handles
 * user actions (publish, jump, reply, complete, reject). The actual Swing/IntelliJ
 * rendering is handled separately — this class provides the pure, testable logic.
 */
class ReviewToolWindowPanel(
    private val reviewModeService: ReviewModeService,
    private val commentService: CommentService,
    private val storageManager: StorageManager
) : ReviewModeListener {

    enum class PanelState {
        NO_REVIEW,
        DRAFTS,
        RESPONSES
    }

    enum class SortOrder { LINE_NUMBER, STATUS, CREATED_AT }

    data class DraftCommentRow(
        val commentId: UUID,
        val lineRange: String,
        val previewText: String,
        val filePath: String,
        val startLine: Int
    )

    data class ResolvedCommentRow(
        val index: Int,
        val lineRange: String,
        val userText: String,
        val claudeResponse: String?,
        val statusLabel: String,
        val filePath: String,
        val startLine: Int,
        val commentId: UUID
    )

    data class ChangedFileRow(
        val filePath: String,
        val commentCount: Int
    )

    data class JumpTarget(
        val filePath: String,
        val line: Int
    )

    data class PublishResult(
        val reviewFilePath: String,
        val cliCommand: String
    )

    private var currentSession: ReviewSession? = null

    fun attach() {
        reviewModeService.addListener(this)
        // Initialize from any already-active session (tool window may open after review started)
        currentSession = reviewModeService.getAllActiveSessions().firstOrNull()
    }

    fun detach() {
        reviewModeService.removeListener(this)
    }

    fun getCurrentSession(): ReviewSession? = currentSession

    fun getCurrentState(): PanelState {
        val session = currentSession ?: return PanelState.NO_REVIEW
        val hasResponses = session.comments.any { it.claudeResponse != null }
        return if (hasResponses) PanelState.RESPONSES else PanelState.DRAFTS
    }

    fun getSessionDisplayName(): String? = currentSession?.getDisplayName()

    fun isPublishButtonVisible(): Boolean {
        val session = currentSession ?: return false
        return session.comments.any { it.status == CommentStatus.DRAFT }
    }

    fun isCompleteEnabled(): Boolean {
        val session = currentSession ?: return false
        return session.status == ReviewSessionStatus.ACTIVE ||
            session.status == ReviewSessionStatus.PUBLISHED
    }

    fun isRejectEnabled(): Boolean = isCompleteEnabled()

    fun getCommentCount(): Int = currentSession?.comments?.size ?: 0

    fun getDraftCount(): Int = currentSession?.getDraftComments()?.size ?: 0

    fun isDiffReview(): Boolean = currentSession is GitDiffReviewSession

    fun getChangedFileRows(): List<ChangedFileRow> {
        val session = currentSession as? GitDiffReviewSession ?: return emptyList()
        val commentsByFile = session.comments.groupBy { it.filePath }
        return session.changedFiles.map { filePath ->
            ChangedFileRow(filePath, commentsByFile[filePath]?.size ?: 0)
        }
    }

    fun getDraftCommentRows(sortOrder: SortOrder = SortOrder.LINE_NUMBER): List<DraftCommentRow> {
        val session = currentSession ?: return emptyList()
        val rows = session.comments.map { comment ->
            DraftCommentRow(
                commentId = comment.id,
                lineRange = formatLineRange(comment.startLine, comment.endLine),
                previewText = truncatePreview(comment.commentText),
                filePath = comment.filePath,
                startLine = comment.startLine
            )
        }
        return when (sortOrder) {
            SortOrder.LINE_NUMBER -> rows.sortedBy { it.startLine }
            SortOrder.STATUS -> rows
            SortOrder.CREATED_AT -> rows
        }
    }

    fun getResolvedCommentRows(): List<ResolvedCommentRow> {
        val session = currentSession ?: return emptyList()
        return session.comments.mapIndexed { index, comment ->
            ResolvedCommentRow(
                index = index + 1,
                lineRange = formatLineRange(comment.startLine, comment.endLine),
                userText = comment.commentText,
                claudeResponse = comment.claudeResponse,
                statusLabel = formatStatusLabel(comment.status),
                filePath = comment.filePath,
                startLine = comment.startLine,
                commentId = comment.id
            )
        }
    }

    fun publishReview(): PublishResult? {
        val session = currentSession ?: return null
        val outputDir = storageManager.getReviewDirectory()
        val reviewFilePath = ReviewFileManager.publish(session, outputDir)

        session.reviewFilePath = reviewFilePath.toString()
        session.publishedAt = Instant.now()
        session.status = ReviewSessionStatus.PUBLISHED
        session.comments.forEach { it.status = CommentStatus.PENDING }

        storageManager.saveDrafts(session)

        val cliCommand = ReviewFileManager.generateCliCommand(reviewFilePath.toString())
        return PublishResult(
            reviewFilePath = reviewFilePath.toString(),
            cliCommand = cliCommand
        )
    }

    fun getJumpTarget(commentId: UUID): JumpTarget? {
        val session = currentSession ?: return null
        val comment = session.getComment(commentId) ?: return null
        return JumpTarget(filePath = comment.filePath, line = comment.startLine)
    }

    fun reloadResponses(): Boolean {
        val session = currentSession ?: return false
        val reviewFilePath = session.reviewFilePath ?: return false
        val reviewFile = ReviewFileManager.load(Path.of(reviewFilePath))
        commentService.applyResponses(session, reviewFile)
        return true
    }

    fun addReply(commentIndex: Int, replyText: String): Boolean {
        val session = currentSession ?: return false
        val reviewFilePath = session.reviewFilePath ?: return false
        if (replyText.isBlank()) return false

        val reply = Reply(
            author = System.getProperty("user.name") ?: "unknown",
            timestamp = Instant.now().toString(),
            text = replyText.trim()
        )
        ReviewFileManager.appendReply(Path.of(reviewFilePath), commentIndex, reply)

        if (commentIndex in 1..session.comments.size) {
            session.comments[commentIndex - 1] =
                session.comments[commentIndex - 1].copy(status = CommentStatus.PENDING)
        }
        return true
    }

    fun deleteComment(commentId: UUID): Boolean {
        val session = currentSession ?: return false
        session.getComment(commentId) ?: return false
        commentService.deleteComment(session, commentId)
        return true
    }

    fun editComment(commentId: UUID, newText: String): Boolean {
        val session = currentSession ?: return false
        if (newText.isBlank()) return false
        session.getComment(commentId) ?: return false
        commentService.updateComment(session, commentId, newText.trim())
        return true
    }

    fun completeReview(): Boolean {
        val session = currentSession ?: return false
        if (!isCompleteEnabled()) return false
        reviewModeService.completeReview(session)
        return true
    }

    fun rejectReview(): Boolean {
        val session = currentSession ?: return false
        if (!isRejectEnabled()) return false
        reviewModeService.rejectReview(session)
        return true
    }

    // --- ReviewModeListener ---

    override fun onReviewModeEntered(session: ReviewSession) {
        currentSession = session
    }

    override fun onReviewModeExited(session: ReviewSession) {
        if (currentSession?.id == session.id) {
            currentSession = null
        }
    }

    override fun onCommentsChanged(session: ReviewSession) {
        if (currentSession?.id == session.id) {
            currentSession = session
        }
    }

    override fun onResponsesLoaded(session: ReviewSession) {
        if (currentSession?.id == session.id) {
            currentSession = session
        }
    }

    companion object {
        const val PREVIEW_MAX_LENGTH = 80
        const val EMPTY_STATE_TITLE = "Claude Code Review"
        const val EMPTY_STATE_MESSAGE = "No active review session."
        const val EMPTY_STATE_HINT =
            "Right-click a .md file \u2192 \"Review this Markdown\"\nor VCS \u2192 \"Review the Diff\" to start."

        fun formatLineRange(startLine: Int, endLine: Int): String {
            return if (startLine == endLine) "Line $startLine" else "Line $startLine-$endLine"
        }

        fun truncatePreview(text: String, maxLength: Int = PREVIEW_MAX_LENGTH): String {
            if (text.length <= maxLength) return text
            return text.take(maxLength) + "..."
        }

        fun formatStatusLabel(status: CommentStatus): String {
            return status.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
