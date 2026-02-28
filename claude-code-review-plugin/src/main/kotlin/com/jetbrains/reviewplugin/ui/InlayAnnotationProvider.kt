package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.CommentStatus
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import java.awt.Color

/**
 * Computes inline annotations for review comments.
 *
 * Pure logic class with no IntelliJ dependencies — all rendering is handled by
 * [ReviewInlayRenderer] / [ReviewBlockInlayRenderer] and wired through [ReviewEditorListener].
 *
 * Provides two annotation modes:
 * - [computeAnnotations]: after-line-end text (truncated, grouped by line)
 * - [computeBlockAnnotations]: block inlays below commented lines (full text, one per comment)
 *
 * Text colors (darker/muted for readability against highlight backgrounds):
 * - DRAFT:    Dark olive  (#9E9D24)
 * - PENDING:  Dark blue   (#1565C0)
 * - RESOLVED: Dark green  (#2E7D32)
 */
class InlayAnnotationProvider {

    data class InlayAnnotation(
        val line: Int,
        val text: String,
        val color: Color,
        val status: CommentStatus
    )

    data class BlockAnnotation(
        val line: Int,
        val commentText: String,
        val response: String?,
        val status: CommentStatus
    )

    /**
     * Compute annotations for a given file within a session.
     *
     * Groups comments by startLine, picks first per line, appends "(+N more)" if multiple.
     */
    fun computeAnnotations(session: ReviewSession, filePath: String): List<InlayAnnotation> {
        val displayableComments = session.comments
            .filter { it.filePath == filePath && isDisplayableStatus(it.status) }

        val grouped = displayableComments.groupBy { it.startLine }

        return grouped.map { (line, comments) ->
            val first = comments.first()
            val rawText = buildAnnotationText(first.status, first.commentText, first.claudeResponse)
            val suffix = if (comments.size > 1) " (+${comments.size - 1} more)" else ""
            val text = truncate(rawText, MAX_LENGTH) + suffix
            val color = getTextColorForStatus(first.status)

            InlayAnnotation(
                line = line,
                text = text,
                color = color,
                status = first.status
            )
        }
    }

    /**
     * Compute block annotations for a given file within a session.
     *
     * Returns one annotation per displayable comment (not grouped).
     * Each annotation carries the full comment text and response for block rendering.
     */
    fun computeBlockAnnotations(session: ReviewSession, filePath: String): List<BlockAnnotation> {
        return session.comments
            .filter { it.filePath == filePath && isDisplayableStatus(it.status) }
            .map { comment ->
                BlockAnnotation(
                    line = comment.startLine,
                    commentText = comment.commentText,
                    response = comment.claudeResponse,
                    status = comment.status
                )
            }
    }

    companion object {
        const val MAX_LENGTH = 60

        val COLOR_DRAFT = Color(0x9E, 0x9D, 0x24)     // Dark olive #9E9D24
        val COLOR_PENDING = Color(0x15, 0x65, 0xC0)    // Dark blue #1565C0
        val COLOR_RESOLVED = Color(0x2E, 0x7D, 0x32)   // Dark green #2E7D32

        fun isDisplayableStatus(status: CommentStatus): Boolean {
            return when (status) {
                CommentStatus.DRAFT, CommentStatus.PENDING, CommentStatus.RESOLVED -> true
                CommentStatus.SKIPPED, CommentStatus.REJECTED -> false
            }
        }

        fun getTextColorForStatus(status: CommentStatus): Color {
            return when (status) {
                CommentStatus.DRAFT -> COLOR_DRAFT
                CommentStatus.PENDING -> COLOR_PENDING
                CommentStatus.RESOLVED -> COLOR_RESOLVED
                CommentStatus.SKIPPED -> COLOR_DRAFT
                CommentStatus.REJECTED -> COLOR_DRAFT
            }
        }

        fun buildAnnotationText(status: CommentStatus, commentText: String, claudeResponse: String?): String {
            return if (status == CommentStatus.RESOLVED && !claudeResponse.isNullOrBlank()) {
                "Claude: $claudeResponse"
            } else {
                "// $commentText"
            }
        }

        fun truncate(text: String, maxLength: Int): String {
            val collapsed = text.replace('\n', ' ').replace('\r', ' ')
            return if (collapsed.length <= maxLength) {
                collapsed
            } else {
                collapsed.substring(0, maxLength - 3) + "..."
            }
        }
    }
}
