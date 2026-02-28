package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.*
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.services.StorageManager
import com.uber.jetbrains.reviewplugin.ui.ReviewGutterIconProvider.GutterIconType
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewGutterIconProviderTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService
    private lateinit var commentService: CommentService
    private lateinit var provider: ReviewGutterIconProvider

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("gutter-icon-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
        commentService = CommentService(storageManager, reviewModeService)
        provider = ReviewGutterIconProvider(reviewModeService, commentService)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- isFileInReviewMode ---

    @Test
    fun `isFileInReviewMode returns false when no session`() {
        assertFalse(provider.isFileInReviewMode("docs/README.md"))
    }

    @Test
    fun `isFileInReviewMode returns true for active markdown session`() {
        reviewModeService.enterMarkdownReview("docs/README.md")
        assertTrue(provider.isFileInReviewMode("docs/README.md"))
    }

    @Test
    fun `isFileInReviewMode returns false for different file`() {
        reviewModeService.enterMarkdownReview("docs/README.md")
        assertFalse(provider.isFileInReviewMode("docs/OTHER.md"))
    }

    @Test
    fun `isFileInReviewMode returns true for file in diff session changed files`() {
        reviewModeService.enterDiffReview("main", "feature-auth", listOf("src/Main.kt", "src/Auth.kt"))
        assertTrue(provider.isFileInReviewMode("src/Main.kt"))
        assertTrue(provider.isFileInReviewMode("src/Auth.kt"))
    }

    @Test
    fun `isFileInReviewMode returns false for file not in diff changed files`() {
        reviewModeService.enterDiffReview("main", "feature-auth", listOf("src/Main.kt"))
        assertFalse(provider.isFileInReviewMode("src/Other.kt"))
    }

    // --- getActiveSession ---

    @Test
    fun `getActiveSession returns null when no session`() {
        assertNull(provider.getActiveSession("docs/README.md"))
    }

    @Test
    fun `getActiveSession returns session for matching file`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val result = provider.getActiveSession("docs/README.md")
        assertNotNull(result)
        assertEquals(session.id, result.id)
    }

    @Test
    fun `getActiveSession returns null for non-matching file`() {
        reviewModeService.enterMarkdownReview("docs/README.md")
        assertNull(provider.getActiveSession("docs/OTHER.md"))
    }

    // --- getIconInfoForLine ---

    @Test
    fun `getIconInfoForLine returns ADD_COMMENT for line with no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val info = provider.getIconInfoForLine(session, "docs/README.md", 10)

        assertEquals(GutterIconType.ADD_COMMENT, info.iconType)
        assertEquals("Add comment", info.tooltip)
        assertTrue(info.comments.isEmpty())
    }

    @Test
    fun `getIconInfoForLine returns COMMENT_DRAFT for draft comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "My comment")
        commentService.addComment(session, comment)

        val info = provider.getIconInfoForLine(session, "docs/README.md", 10)

        assertEquals(GutterIconType.COMMENT_DRAFT, info.iconType)
        assertEquals(1, info.comments.size)
        assertTrue(info.tooltip.contains("Draft"))
    }

    @Test
    fun `getIconInfoForLine returns COMMENT_PENDING for pending comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "My comment")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.PENDING)

        val info = provider.getIconInfoForLine(session, "docs/README.md", 10)

        assertEquals(GutterIconType.COMMENT_PENDING, info.iconType)
        assertTrue(info.tooltip.contains("Pending"))
    }

    @Test
    fun `getIconInfoForLine returns COMMENT_RESOLVED for resolved comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "My comment")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.RESOLVED)

        val info = provider.getIconInfoForLine(session, "docs/README.md", 10)

        assertEquals(GutterIconType.COMMENT_RESOLVED, info.iconType)
        assertTrue(info.tooltip.contains("Resolved"))
    }

    @Test
    fun `getIconInfoForLine returns ADD_COMMENT for line outside comment range`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 15, "text", "Comment")
        commentService.addComment(session, comment)

        val info = provider.getIconInfoForLine(session, "docs/README.md", 5)

        assertEquals(GutterIconType.ADD_COMMENT, info.iconType)
    }

    @Test
    fun `getIconInfoForLine returns comment for line within multi-line comment range`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 15, "text", "Comment")
        commentService.addComment(session, comment)

        val info = provider.getIconInfoForLine(session, "docs/README.md", 12)

        assertEquals(GutterIconType.COMMENT_DRAFT, info.iconType)
        assertEquals(1, info.comments.size)
    }

    @Test
    fun `getIconInfoForLine with multiple comments shows highest priority (DRAFT over PENDING)`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c1 = makeComment("docs/README.md", 10, 15, "t1", "Comment 1")
        val c2 = makeComment("docs/README.md", 12, 14, "t2", "Comment 2")
        commentService.addComment(session, c1)
        commentService.addComment(session, c2)
        commentService.setCommentStatus(session, c2.id, CommentStatus.PENDING)

        // Line 13 overlaps both comments: c1 is DRAFT, c2 is PENDING
        val info = provider.getIconInfoForLine(session, "docs/README.md", 13)

        assertEquals(GutterIconType.COMMENT_DRAFT, info.iconType)
        assertEquals(2, info.comments.size)
    }

    @Test
    fun `getIconInfoForLine with multiple comments shows highest priority (PENDING over RESOLVED)`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c1 = makeComment("docs/README.md", 10, 15, "t1", "Comment 1")
        val c2 = makeComment("docs/README.md", 12, 14, "t2", "Comment 2")
        commentService.addComment(session, c1)
        commentService.addComment(session, c2)
        commentService.setCommentStatus(session, c1.id, CommentStatus.RESOLVED)
        commentService.setCommentStatus(session, c2.id, CommentStatus.PENDING)

        val info = provider.getIconInfoForLine(session, "docs/README.md", 13)

        assertEquals(GutterIconType.COMMENT_PENDING, info.iconType)
    }

    @Test
    fun `getIconInfoForLine returns ADD_COMMENT for different file`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/OTHER.md", 10, 10, "text", "Comment")
        commentService.addComment(session, comment)

        val info = provider.getIconInfoForLine(session, "docs/README.md", 10)

        assertEquals(GutterIconType.ADD_COMMENT, info.iconType)
    }

    // --- selectIconForComments (companion) ---

    @Test
    fun `selectIconForComments returns ADD_COMMENT for empty list`() {
        assertEquals(GutterIconType.ADD_COMMENT, ReviewGutterIconProvider.selectIconForComments(emptyList()))
    }

    @Test
    fun `selectIconForComments returns COMMENT_DRAFT for single draft`() {
        val comments = listOf(makeComment("f", 1, 1, "t", "c", CommentStatus.DRAFT))
        assertEquals(GutterIconType.COMMENT_DRAFT, ReviewGutterIconProvider.selectIconForComments(comments))
    }

    @Test
    fun `selectIconForComments returns COMMENT_PENDING for single pending`() {
        val comments = listOf(makeComment("f", 1, 1, "t", "c", CommentStatus.PENDING))
        assertEquals(GutterIconType.COMMENT_PENDING, ReviewGutterIconProvider.selectIconForComments(comments))
    }

    @Test
    fun `selectIconForComments returns COMMENT_RESOLVED for single resolved`() {
        val comments = listOf(makeComment("f", 1, 1, "t", "c", CommentStatus.RESOLVED))
        assertEquals(GutterIconType.COMMENT_RESOLVED, ReviewGutterIconProvider.selectIconForComments(comments))
    }

    @Test
    fun `selectIconForComments returns COMMENT_RESOLVED for skipped status`() {
        val comments = listOf(makeComment("f", 1, 1, "t", "c", CommentStatus.SKIPPED))
        assertEquals(GutterIconType.COMMENT_RESOLVED, ReviewGutterIconProvider.selectIconForComments(comments))
    }

    @Test
    fun `selectIconForComments returns COMMENT_RESOLVED for rejected status`() {
        val comments = listOf(makeComment("f", 1, 1, "t", "c", CommentStatus.REJECTED))
        assertEquals(GutterIconType.COMMENT_RESOLVED, ReviewGutterIconProvider.selectIconForComments(comments))
    }

    @Test
    fun `selectIconForComments picks DRAFT over PENDING and RESOLVED`() {
        val comments = listOf(
            makeComment("f", 1, 1, "t", "c", CommentStatus.RESOLVED),
            makeComment("f", 1, 1, "t", "c", CommentStatus.PENDING),
            makeComment("f", 1, 1, "t", "c", CommentStatus.DRAFT)
        )
        assertEquals(GutterIconType.COMMENT_DRAFT, ReviewGutterIconProvider.selectIconForComments(comments))
    }

    @Test
    fun `selectIconForComments picks PENDING over RESOLVED`() {
        val comments = listOf(
            makeComment("f", 1, 1, "t", "c", CommentStatus.RESOLVED),
            makeComment("f", 1, 1, "t", "c", CommentStatus.PENDING)
        )
        assertEquals(GutterIconType.COMMENT_PENDING, ReviewGutterIconProvider.selectIconForComments(comments))
    }

    // --- buildTooltip (companion) ---

    @Test
    fun `buildTooltip returns 'Add comment' for empty list`() {
        assertEquals("Add comment", ReviewGutterIconProvider.buildTooltip(emptyList()))
    }

    @Test
    fun `buildTooltip shows status and text preview for single comment`() {
        val comments = listOf(makeComment("f", 1, 1, "t", "How does this work?", CommentStatus.DRAFT))
        val tooltip = ReviewGutterIconProvider.buildTooltip(comments)
        assertEquals("Draft: How does this work?", tooltip)
    }

    @Test
    fun `buildTooltip truncates long comment text at 60 chars`() {
        val longText = "A".repeat(80)
        val comments = listOf(makeComment("f", 1, 1, "t", longText, CommentStatus.PENDING))
        val tooltip = ReviewGutterIconProvider.buildTooltip(comments)
        assertTrue(tooltip.contains("Pending: "))
        assertTrue(tooltip.endsWith("..."))
        assertTrue(tooltip.length < 80)
    }

    @Test
    fun `buildTooltip does not truncate text at exactly 60 chars`() {
        val exactText = "A".repeat(60)
        val comments = listOf(makeComment("f", 1, 1, "t", exactText, CommentStatus.DRAFT))
        val tooltip = ReviewGutterIconProvider.buildTooltip(comments)
        assertFalse(tooltip.endsWith("..."))
    }

    @Test
    fun `buildTooltip shows count for multiple comments`() {
        val comments = listOf(
            makeComment("f", 1, 1, "t", "c1", CommentStatus.DRAFT),
            makeComment("f", 1, 1, "t", "c2", CommentStatus.RESOLVED),
            makeComment("f", 1, 1, "t", "c3", CommentStatus.RESOLVED)
        )
        val tooltip = ReviewGutterIconProvider.buildTooltip(comments)
        assertEquals("3 comments (2 resolved)", tooltip)
    }

    @Test
    fun `buildTooltip shows zero resolved when none resolved`() {
        val comments = listOf(
            makeComment("f", 1, 1, "t", "c1", CommentStatus.DRAFT),
            makeComment("f", 1, 1, "t", "c2", CommentStatus.PENDING)
        )
        val tooltip = ReviewGutterIconProvider.buildTooltip(comments)
        assertEquals("2 comments (0 resolved)", tooltip)
    }

    // --- getHighlightColor (companion) ---

    @Test
    fun `getHighlightColor returns yellow for DRAFT`() {
        val color = ReviewGutterIconProvider.getHighlightColor(CommentStatus.DRAFT)
        assertNotNull(color)
        assertEquals(Color(0xFF, 0xF9, 0xC4), color)
    }

    @Test
    fun `getHighlightColor returns blue for PENDING`() {
        val color = ReviewGutterIconProvider.getHighlightColor(CommentStatus.PENDING)
        assertNotNull(color)
        assertEquals(Color(0xBB, 0xDE, 0xFB), color)
    }

    @Test
    fun `getHighlightColor returns green for RESOLVED`() {
        val color = ReviewGutterIconProvider.getHighlightColor(CommentStatus.RESOLVED)
        assertNotNull(color)
        assertEquals(Color(0xC8, 0xE6, 0xC9), color)
    }

    @Test
    fun `getHighlightColor returns null for SKIPPED`() {
        assertNull(ReviewGutterIconProvider.getHighlightColor(CommentStatus.SKIPPED))
    }

    @Test
    fun `getHighlightColor returns null for REJECTED`() {
        assertNull(ReviewGutterIconProvider.getHighlightColor(CommentStatus.REJECTED))
    }

    // --- ICON_RESOURCE_PATHS ---

    @Test
    fun `ICON_RESOURCE_PATHS has entries for all icon types`() {
        for (type in GutterIconType.entries) {
            assertTrue(ReviewGutterIconProvider.ICON_RESOURCE_PATHS.containsKey(type),
                "Missing resource path for $type")
        }
    }

    @Test
    fun `ICON_RESOURCE_PATHS values point to svg files`() {
        for ((_, path) in ReviewGutterIconProvider.ICON_RESOURCE_PATHS) {
            assertTrue(path.endsWith(".svg"), "Resource path should be .svg: $path")
            assertTrue(path.startsWith("/icons/"), "Resource path should start with /icons/: $path")
        }
    }

    // --- GutterIconType enum ---

    @Test
    fun `GutterIconType has all expected values`() {
        val types = GutterIconType.entries
        assertEquals(4, types.size)
        assertTrue(types.contains(GutterIconType.ADD_COMMENT))
        assertTrue(types.contains(GutterIconType.COMMENT_DRAFT))
        assertTrue(types.contains(GutterIconType.COMMENT_PENDING))
        assertTrue(types.contains(GutterIconType.COMMENT_RESOLVED))
    }

    // --- Helper ---

    private fun makeComment(
        filePath: String,
        startLine: Int,
        endLine: Int,
        selectedText: String,
        commentText: String,
        status: CommentStatus = CommentStatus.DRAFT
    ): ReviewComment {
        return ReviewComment(
            filePath = filePath,
            startLine = startLine,
            endLine = endLine,
            selectedText = selectedText,
            commentText = commentText,
            authorId = "vinay.yerra",
            createdAt = Instant.parse("2026-02-12T15:28:12Z"),
            status = status
        )
    }
}
