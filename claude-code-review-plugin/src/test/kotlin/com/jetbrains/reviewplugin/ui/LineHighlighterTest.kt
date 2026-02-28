package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.*
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.services.StorageManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LineHighlighterTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService
    private lateinit var commentService: CommentService
    private lateinit var highlighter: LineHighlighter

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("line-highlighter-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
        commentService = CommentService(storageManager, reviewModeService)
        highlighter = LineHighlighter()
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- computeHighlights ---

    @Test
    fun `computeHighlights returns empty for session with no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")

        val highlights = highlighter.computeHighlights(session, "docs/README.md")

        assertTrue(highlights.isEmpty())
    }

    @Test
    fun `computeHighlights returns highlight for draft comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 15, "text", "Comment")
        commentService.addComment(session, comment)

        val highlights = highlighter.computeHighlights(session, "docs/README.md")

        assertEquals(1, highlights.size)
        assertEquals("docs/README.md", highlights[0].filePath)
        assertEquals(10, highlights[0].startLine)
        assertEquals(15, highlights[0].endLine)
        assertEquals(LineHighlighter.COLOR_DRAFT, highlights[0].color)
        assertEquals(CommentStatus.DRAFT, highlights[0].status)
    }

    @Test
    fun `computeHighlights returns blue for pending comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Comment")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.PENDING)

        val highlights = highlighter.computeHighlights(session, "docs/README.md")

        assertEquals(1, highlights.size)
        assertEquals(LineHighlighter.COLOR_PENDING, highlights[0].color)
        assertEquals(CommentStatus.PENDING, highlights[0].status)
    }

    @Test
    fun `computeHighlights returns green for resolved comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Comment")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.RESOLVED)

        val highlights = highlighter.computeHighlights(session, "docs/README.md")

        assertEquals(1, highlights.size)
        assertEquals(LineHighlighter.COLOR_RESOLVED, highlights[0].color)
        assertEquals(CommentStatus.RESOLVED, highlights[0].status)
    }

    @Test
    fun `computeHighlights excludes skipped comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Comment")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.SKIPPED)

        val highlights = highlighter.computeHighlights(session, "docs/README.md")

        assertTrue(highlights.isEmpty())
    }

    @Test
    fun `computeHighlights excludes rejected comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Comment")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.REJECTED)

        val highlights = highlighter.computeHighlights(session, "docs/README.md")

        assertTrue(highlights.isEmpty())
    }

    @Test
    fun `computeHighlights returns multiple highlights for multiple comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 15, "t1", "c1"))
        commentService.addComment(session, makeComment("docs/README.md", 20, 25, "t2", "c2"))
        commentService.addComment(session, makeComment("docs/README.md", 30, 35, "t3", "c3"))

        val highlights = highlighter.computeHighlights(session, "docs/README.md")

        assertEquals(3, highlights.size)
        assertEquals(10, highlights[0].startLine)
        assertEquals(20, highlights[1].startLine)
        assertEquals(30, highlights[2].startLine)
    }

    @Test
    fun `computeHighlights filters by file path`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t1", "c1"))
        commentService.addComment(session, makeComment("docs/OTHER.md", 20, 20, "t2", "c2"))

        val highlights = highlighter.computeHighlights(session, "docs/README.md")

        assertEquals(1, highlights.size)
        assertEquals("docs/README.md", highlights[0].filePath)
    }

    @Test
    fun `computeHighlights returns empty for unknown file`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))

        val highlights = highlighter.computeHighlights(session, "docs/UNKNOWN.md")

        assertTrue(highlights.isEmpty())
    }

    @Test
    fun `computeHighlights mixed statuses return correct colors`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c1 = makeComment("docs/README.md", 10, 10, "t1", "draft")
        val c2 = makeComment("docs/README.md", 20, 20, "t2", "pending")
        val c3 = makeComment("docs/README.md", 30, 30, "t3", "resolved")
        commentService.addComment(session, c1)
        commentService.addComment(session, c2)
        commentService.addComment(session, c3)
        commentService.setCommentStatus(session, c2.id, CommentStatus.PENDING)
        commentService.setCommentStatus(session, c3.id, CommentStatus.RESOLVED)

        val highlights = highlighter.computeHighlights(session, "docs/README.md")

        assertEquals(3, highlights.size)
        assertEquals(LineHighlighter.COLOR_DRAFT, highlights[0].color)
        assertEquals(LineHighlighter.COLOR_PENDING, highlights[1].color)
        assertEquals(LineHighlighter.COLOR_RESOLVED, highlights[2].color)
    }

    // --- computeAllHighlights ---

    @Test
    fun `computeAllHighlights returns empty for no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")

        val all = highlighter.computeAllHighlights(session)

        assertTrue(all.isEmpty())
    }

    @Test
    fun `computeAllHighlights groups by file`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file1.md", 10, 10, "t1", "c1"))
        commentService.addComment(session, makeComment("file1.md", 20, 20, "t2", "c2"))
        commentService.addComment(session, makeComment("file2.md", 5, 5, "t3", "c3"))

        val all = highlighter.computeAllHighlights(session)

        assertEquals(2, all.size)
        assertEquals(2, all["file1.md"]?.size)
        assertEquals(1, all["file2.md"]?.size)
    }

    @Test
    fun `computeAllHighlights excludes non-highlightable statuses`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c1 = makeComment("file.md", 10, 10, "t1", "draft")
        val c2 = makeComment("file.md", 20, 20, "t2", "skipped")
        commentService.addComment(session, c1)
        commentService.addComment(session, c2)
        commentService.setCommentStatus(session, c2.id, CommentStatus.SKIPPED)

        val all = highlighter.computeAllHighlights(session)

        assertEquals(1, all.size)
        assertEquals(1, all["file.md"]?.size)
        assertEquals(10, all["file.md"]?.get(0)?.startLine)
    }

    // --- hasHighlights ---

    @Test
    fun `hasHighlights returns false for no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        assertFalse(highlighter.hasHighlights(session, "docs/README.md"))
    }

    @Test
    fun `hasHighlights returns true for file with draft comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        assertTrue(highlighter.hasHighlights(session, "docs/README.md"))
    }

    @Test
    fun `hasHighlights returns false for different file`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        assertFalse(highlighter.hasHighlights(session, "docs/OTHER.md"))
    }

    @Test
    fun `hasHighlights returns true for pending comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c = makeComment("docs/README.md", 10, 10, "t", "c")
        commentService.addComment(session, c)
        commentService.setCommentStatus(session, c.id, CommentStatus.PENDING)
        assertTrue(highlighter.hasHighlights(session, "docs/README.md"))
    }

    @Test
    fun `hasHighlights returns true for resolved comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c = makeComment("docs/README.md", 10, 10, "t", "c")
        commentService.addComment(session, c)
        commentService.setCommentStatus(session, c.id, CommentStatus.RESOLVED)
        assertTrue(highlighter.hasHighlights(session, "docs/README.md"))
    }

    @Test
    fun `hasHighlights returns false when only skipped comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c = makeComment("docs/README.md", 10, 10, "t", "c")
        commentService.addComment(session, c)
        commentService.setCommentStatus(session, c.id, CommentStatus.SKIPPED)
        assertFalse(highlighter.hasHighlights(session, "docs/README.md"))
    }

    @Test
    fun `hasHighlights returns false when only rejected comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c = makeComment("docs/README.md", 10, 10, "t", "c")
        commentService.addComment(session, c)
        commentService.setCommentStatus(session, c.id, CommentStatus.REJECTED)
        assertFalse(highlighter.hasHighlights(session, "docs/README.md"))
    }

    // --- getColorForStatus (companion) ---

    @Test
    fun `getColorForStatus returns correct colors for all statuses`() {
        assertEquals(Color(0xFF, 0xF9, 0xC4), LineHighlighter.getColorForStatus(CommentStatus.DRAFT))
        assertEquals(Color(0xBB, 0xDE, 0xFB), LineHighlighter.getColorForStatus(CommentStatus.PENDING))
        assertEquals(Color(0xC8, 0xE6, 0xC9), LineHighlighter.getColorForStatus(CommentStatus.RESOLVED))
        assertNull(LineHighlighter.getColorForStatus(CommentStatus.SKIPPED))
        assertNull(LineHighlighter.getColorForStatus(CommentStatus.REJECTED))
    }

    // --- Color constants ---

    @Test
    fun `color constants have correct values`() {
        assertEquals(Color(0xFF, 0xF9, 0xC4), LineHighlighter.COLOR_DRAFT)
        assertEquals(Color(0xBB, 0xDE, 0xFB), LineHighlighter.COLOR_PENDING)
        assertEquals(Color(0xC8, 0xE6, 0xC9), LineHighlighter.COLOR_RESOLVED)
    }

    // --- HighlightInfo data class ---

    @Test
    fun `HighlightInfo preserves all properties`() {
        val info = LineHighlighter.HighlightInfo(
            filePath = "test.md",
            startLine = 5,
            endLine = 10,
            color = LineHighlighter.COLOR_DRAFT,
            status = CommentStatus.DRAFT
        )
        assertEquals("test.md", info.filePath)
        assertEquals(5, info.startLine)
        assertEquals(10, info.endLine)
        assertEquals(LineHighlighter.COLOR_DRAFT, info.color)
        assertEquals(CommentStatus.DRAFT, info.status)
    }

    @Test
    fun `HighlightInfo equality`() {
        val info1 = LineHighlighter.HighlightInfo("f", 1, 2, LineHighlighter.COLOR_DRAFT, CommentStatus.DRAFT)
        val info2 = LineHighlighter.HighlightInfo("f", 1, 2, LineHighlighter.COLOR_DRAFT, CommentStatus.DRAFT)
        assertEquals(info1, info2)
        assertEquals(info1.hashCode(), info2.hashCode())
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
