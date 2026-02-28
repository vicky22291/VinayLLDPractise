package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.CommentStatus
import com.uber.jetbrains.reviewplugin.model.ReviewComment
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import java.awt.Color

/**
 * Computes line highlights for commented lines in a review session.
 *
 * This class computes the highlight data (line ranges and colors) needed
 * to visually mark commented lines. It does NOT interact with IntelliJ's
 * MarkupModel or Editor directly — that integration is left to the
 * platform-dependent wiring code. This keeps the logic testable.
 *
 * Highlight colors by status:
 * - DRAFT:    Light yellow (#FFF9C4)
 * - PENDING:  Light blue   (#BBDEFB)
 * - RESOLVED: Light green  (#C8E6C9)
 */
class LineHighlighter {

    /**
     * Represents a single line range highlight.
     */
    data class HighlightInfo(
        val filePath: String,
        val startLine: Int,
        val endLine: Int,
        val color: Color,
        val status: CommentStatus
    )

    /**
     * Compute all highlights for a given file within a session.
     *
     * @param session The review session
     * @param filePath The file to compute highlights for
     * @return List of highlight infos for all commented lines in the file
     */
    fun computeHighlights(session: ReviewSession, filePath: String): List<HighlightInfo> {
        return session.comments
            .filter { it.filePath == filePath }
            .mapNotNull { comment ->
                val color = getColorForStatus(comment.status) ?: return@mapNotNull null
                HighlightInfo(
                    filePath = comment.filePath,
                    startLine = comment.startLine,
                    endLine = comment.endLine,
                    color = color,
                    status = comment.status
                )
            }
    }

    /**
     * Compute all highlights for all files in a session.
     *
     * @param session The review session
     * @return Map of file path to list of highlights for that file
     */
    fun computeAllHighlights(session: ReviewSession): Map<String, List<HighlightInfo>> {
        return session.comments
            .mapNotNull { comment ->
                val color = getColorForStatus(comment.status) ?: return@mapNotNull null
                HighlightInfo(
                    filePath = comment.filePath,
                    startLine = comment.startLine,
                    endLine = comment.endLine,
                    color = color,
                    status = comment.status
                )
            }
            .groupBy { it.filePath }
    }

    /**
     * Check if any highlights exist for a file.
     */
    fun hasHighlights(session: ReviewSession, filePath: String): Boolean {
        return session.comments.any {
            it.filePath == filePath && getColorForStatus(it.status) != null
        }
    }

    companion object {
        val COLOR_DRAFT = Color(0xFF, 0xF9, 0xC4)     // Light yellow #FFF9C4
        val COLOR_PENDING = Color(0xBB, 0xDE, 0xFB)   // Light blue #BBDEFB
        val COLOR_RESOLVED = Color(0xC8, 0xE6, 0xC9)  // Light green #C8E6C9

        /**
         * Get the highlight color for a comment status.
         * Returns null for statuses that should not be highlighted (SKIPPED, REJECTED).
         */
        fun getColorForStatus(status: CommentStatus): Color? {
            return when (status) {
                CommentStatus.DRAFT -> COLOR_DRAFT
                CommentStatus.PENDING -> COLOR_PENDING
                CommentStatus.RESOLVED -> COLOR_RESOLVED
                CommentStatus.SKIPPED -> null
                CommentStatus.REJECTED -> null
            }
        }
    }
}
