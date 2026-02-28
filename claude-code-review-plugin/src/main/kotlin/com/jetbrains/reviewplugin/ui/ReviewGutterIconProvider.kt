package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.CommentStatus
import com.uber.jetbrains.reviewplugin.model.ReviewComment
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import java.awt.Color

/**
 * Provides gutter icon information for review mode.
 *
 * This class encapsulates the icon selection logic for the gutter icon provider.
 * It determines which icon and tooltip to show for each line based on the
 * comment status. The actual LineMarkerProvider integration (PsiElement handling,
 * first-leaf detection) is separate — this class provides the pure logic.
 */
class ReviewGutterIconProvider(
    private val reviewModeService: ReviewModeService,
    private val commentService: CommentService
) {

    /**
     * The type of gutter icon to display.
     */
    enum class GutterIconType {
        ADD_COMMENT,
        COMMENT_DRAFT,
        COMMENT_PENDING,
        COMMENT_RESOLVED
    }

    /**
     * Describes what to display in the gutter for a given line.
     */
    data class GutterIconInfo(
        val iconType: GutterIconType,
        val tooltip: String,
        val comments: List<ReviewComment> = emptyList()
    )

    /**
     * Fast-path check: is this file in review mode?
     */
    fun isFileInReviewMode(filePath: String): Boolean {
        return reviewModeService.isInReviewMode(filePath)
    }

    /**
     * Get the active review session for the given file.
     */
    fun getActiveSession(filePath: String): ReviewSession? {
        return reviewModeService.getActiveSession(filePath)
    }

    /**
     * Determine the gutter icon info for a specific line in a file.
     *
     * @param session The active review session
     * @param filePath The file path being displayed
     * @param line The 1-based line number
     * @return GutterIconInfo describing what to show, or null if not in review mode
     */
    fun getIconInfoForLine(session: ReviewSession, filePath: String, line: Int): GutterIconInfo {
        val comments = commentService.getCommentsForLine(session, filePath, line)

        if (comments.isNotEmpty()) {
            val iconType = selectIconForComments(comments)
            val tooltip = buildTooltip(comments)
            return GutterIconInfo(iconType, tooltip, comments)
        }

        return GutterIconInfo(GutterIconType.ADD_COMMENT, "Add comment")
    }

    companion object {
        /**
         * Priority order for icon selection when multiple comments exist on a line.
         * DRAFT > PENDING > RESOLVED (highest priority first).
         */
        private val STATUS_PRIORITY = mapOf(
            CommentStatus.DRAFT to 3,
            CommentStatus.PENDING to 2,
            CommentStatus.RESOLVED to 1,
            CommentStatus.SKIPPED to 0,
            CommentStatus.REJECTED to 0
        )

        /**
         * Map icon types to their SVG resource paths.
         */
        val ICON_RESOURCE_PATHS = mapOf(
            GutterIconType.ADD_COMMENT to "/icons/addComment.svg",
            GutterIconType.COMMENT_DRAFT to "/icons/commentExists.svg",
            GutterIconType.COMMENT_PENDING to "/icons/commentPending.svg",
            GutterIconType.COMMENT_RESOLVED to "/icons/commentResolved.svg"
        )

        /**
         * Map comment statuses to highlight background colors.
         */
        val HIGHLIGHT_COLORS = mapOf(
            CommentStatus.DRAFT to Color(0xFF, 0xF9, 0xC4),      // Light yellow #FFF9C4
            CommentStatus.PENDING to Color(0xBB, 0xDE, 0xFB),    // Light blue #BBDEFB
            CommentStatus.RESOLVED to Color(0xC8, 0xE6, 0xC9)    // Light green #C8E6C9
        )

        /**
         * Select the icon type for a set of comments on the same line.
         * Uses the highest-priority status: DRAFT > PENDING > RESOLVED.
         */
        fun selectIconForComments(comments: List<ReviewComment>): GutterIconType {
            if (comments.isEmpty()) return GutterIconType.ADD_COMMENT

            val highestStatus = comments.maxByOrNull { STATUS_PRIORITY[it.status] ?: 0 }?.status
                ?: return GutterIconType.ADD_COMMENT

            return when (highestStatus) {
                CommentStatus.DRAFT -> GutterIconType.COMMENT_DRAFT
                CommentStatus.PENDING -> GutterIconType.COMMENT_PENDING
                CommentStatus.RESOLVED -> GutterIconType.COMMENT_RESOLVED
                CommentStatus.SKIPPED -> GutterIconType.COMMENT_RESOLVED
                CommentStatus.REJECTED -> GutterIconType.COMMENT_RESOLVED
            }
        }

        /**
         * Build a tooltip string for comments on a line.
         */
        fun buildTooltip(comments: List<ReviewComment>): String {
            if (comments.isEmpty()) return "Add comment"

            if (comments.size == 1) {
                val c = comments[0]
                val statusLabel = c.status.name.lowercase().replaceFirstChar { it.uppercase() }
                val preview = c.commentText.take(60).let {
                    if (c.commentText.length > 60) "$it..." else it
                }
                return "$statusLabel: $preview"
            }

            return "${comments.size} comments (${comments.count { it.status == CommentStatus.RESOLVED }} resolved)"
        }

        /**
         * Get the highlight color for a comment status.
         * Returns null for statuses that shouldn't be highlighted.
         */
        fun getHighlightColor(status: CommentStatus): Color? {
            return HIGHLIGHT_COLORS[status]
        }
    }
}
