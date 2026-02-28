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
import kotlin.test.assertTrue

class InlayAnnotationProviderTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService
    private lateinit var commentService: CommentService
    private lateinit var provider: InlayAnnotationProvider

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("inlay-annotation-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
        commentService = CommentService(storageManager, reviewModeService)
        provider = InlayAnnotationProvider()
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- computeAnnotations ---

    @Test
    fun `computeAnnotations returns empty for session with no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val annotations = provider.computeAnnotations(session, "docs/README.md")
        assertTrue(annotations.isEmpty())
    }

    @Test
    fun `computeAnnotations returns annotation for draft comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 15, "text", "Fix this"))

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals(10, annotations[0].line)
        assertEquals("// Fix this", annotations[0].text)
        assertEquals(InlayAnnotationProvider.COLOR_DRAFT, annotations[0].color)
        assertEquals(CommentStatus.DRAFT, annotations[0].status)
    }

    @Test
    fun `computeAnnotations returns annotation for pending comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Check this")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.PENDING)

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals("// Check this", annotations[0].text)
        assertEquals(InlayAnnotationProvider.COLOR_PENDING, annotations[0].color)
        assertEquals(CommentStatus.PENDING, annotations[0].status)
    }

    @Test
    fun `computeAnnotations shows Claude response for resolved comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Why?")
        commentService.addComment(session, comment)
        // Simulate Claude responding: update the comment with response and resolved status
        val index = session.comments.indexOfFirst { it.id == comment.id }
        session.comments[index] = session.comments[index].copy(
            claudeResponse = "Because X depends on Y",
            status = CommentStatus.RESOLVED
        )

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals("Claude: Because X depends on Y", annotations[0].text)
        assertEquals(InlayAnnotationProvider.COLOR_RESOLVED, annotations[0].color)
        assertEquals(CommentStatus.RESOLVED, annotations[0].status)
    }

    @Test
    fun `computeAnnotations excludes skipped comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Comment")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.SKIPPED)

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertTrue(annotations.isEmpty())
    }

    @Test
    fun `computeAnnotations excludes rejected comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Comment")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.REJECTED)

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertTrue(annotations.isEmpty())
    }

    @Test
    fun `computeAnnotations filters by filePath`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t1", "c1"))
        commentService.addComment(session, makeComment("docs/OTHER.md", 20, 20, "t2", "c2"))

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals(10, annotations[0].line)
    }

    @Test
    fun `computeAnnotations returns empty for unknown file`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))

        val annotations = provider.computeAnnotations(session, "docs/UNKNOWN.md")

        assertTrue(annotations.isEmpty())
    }

    @Test
    fun `computeAnnotations groups by startLine and shows first comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t1", "First comment"))
        commentService.addComment(session, makeComment("docs/README.md", 10, 12, "t2", "Second comment"))

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals(10, annotations[0].line)
        assertTrue(annotations[0].text.contains("First comment"))
    }

    @Test
    fun `computeAnnotations appends plus N more for multiple comments on same line`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t1", "First"))
        commentService.addComment(session, makeComment("docs/README.md", 10, 12, "t2", "Second"))
        commentService.addComment(session, makeComment("docs/README.md", 10, 15, "t3", "Third"))

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertTrue(annotations[0].text.endsWith("(+2 more)"))
    }

    @Test
    fun `computeAnnotations does not append plus N more for single comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t1", "Only one"))

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertFalse(annotations[0].text.contains("+"))
    }

    @Test
    fun `computeAnnotations truncates long comment text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val longComment = "A".repeat(100)
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", longComment))

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertTrue(annotations[0].text.length <= InlayAnnotationProvider.MAX_LENGTH + 10) // +margin for suffix
        assertTrue(annotations[0].text.contains("..."))
    }

    @Test
    fun `computeAnnotations returns multiple annotations for different lines`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t1", "c1"))
        commentService.addComment(session, makeComment("docs/README.md", 20, 20, "t2", "c2"))
        commentService.addComment(session, makeComment("docs/README.md", 30, 30, "t3", "c3"))

        val annotations = provider.computeAnnotations(session, "docs/README.md")

        assertEquals(3, annotations.size)
        assertEquals(10, annotations[0].line)
        assertEquals(20, annotations[1].line)
        assertEquals(30, annotations[2].line)
    }

    // --- buildAnnotationText ---

    @Test
    fun `buildAnnotationText returns user comment for draft`() {
        val text = InlayAnnotationProvider.buildAnnotationText(CommentStatus.DRAFT, "Fix this", null)
        assertEquals("// Fix this", text)
    }

    @Test
    fun `buildAnnotationText returns user comment for pending`() {
        val text = InlayAnnotationProvider.buildAnnotationText(CommentStatus.PENDING, "Check", null)
        assertEquals("// Check", text)
    }

    @Test
    fun `buildAnnotationText returns Claude response for resolved`() {
        val text = InlayAnnotationProvider.buildAnnotationText(CommentStatus.RESOLVED, "Why?", "Because X")
        assertEquals("Claude: Because X", text)
    }

    @Test
    fun `buildAnnotationText falls back to user comment when resolved but no response`() {
        val text = InlayAnnotationProvider.buildAnnotationText(CommentStatus.RESOLVED, "Why?", null)
        assertEquals("// Why?", text)
    }

    @Test
    fun `buildAnnotationText falls back to user comment when resolved but blank response`() {
        val text = InlayAnnotationProvider.buildAnnotationText(CommentStatus.RESOLVED, "Why?", "  ")
        assertEquals("// Why?", text)
    }

    // --- truncate ---

    @Test
    fun `truncate returns short text unchanged`() {
        assertEquals("hello", InlayAnnotationProvider.truncate("hello", 60))
    }

    @Test
    fun `truncate truncates long text with ellipsis`() {
        val long = "A".repeat(100)
        val result = InlayAnnotationProvider.truncate(long, 60)
        assertEquals(60, result.length)
        assertTrue(result.endsWith("..."))
    }

    @Test
    fun `truncate collapses newlines to spaces`() {
        val text = "line1\nline2\nline3"
        val result = InlayAnnotationProvider.truncate(text, 60)
        assertEquals("line1 line2 line3", result)
        assertFalse(result.contains("\n"))
    }

    @Test
    fun `truncate collapses carriage returns to spaces`() {
        val text = "line1\r\nline2"
        val result = InlayAnnotationProvider.truncate(text, 60)
        assertFalse(result.contains("\r"))
        assertFalse(result.contains("\n"))
    }

    @Test
    fun `truncate at exact boundary does not add ellipsis`() {
        val text = "A".repeat(60)
        val result = InlayAnnotationProvider.truncate(text, 60)
        assertEquals(60, result.length)
        assertFalse(result.contains("..."))
    }

    // --- isDisplayableStatus ---

    @Test
    fun `isDisplayableStatus returns true for DRAFT`() {
        assertTrue(InlayAnnotationProvider.isDisplayableStatus(CommentStatus.DRAFT))
    }

    @Test
    fun `isDisplayableStatus returns true for PENDING`() {
        assertTrue(InlayAnnotationProvider.isDisplayableStatus(CommentStatus.PENDING))
    }

    @Test
    fun `isDisplayableStatus returns true for RESOLVED`() {
        assertTrue(InlayAnnotationProvider.isDisplayableStatus(CommentStatus.RESOLVED))
    }

    @Test
    fun `isDisplayableStatus returns false for SKIPPED`() {
        assertFalse(InlayAnnotationProvider.isDisplayableStatus(CommentStatus.SKIPPED))
    }

    @Test
    fun `isDisplayableStatus returns false for REJECTED`() {
        assertFalse(InlayAnnotationProvider.isDisplayableStatus(CommentStatus.REJECTED))
    }

    // --- getTextColorForStatus ---

    @Test
    fun `getTextColorForStatus returns dark olive for DRAFT`() {
        assertEquals(InlayAnnotationProvider.COLOR_DRAFT, InlayAnnotationProvider.getTextColorForStatus(CommentStatus.DRAFT))
    }

    @Test
    fun `getTextColorForStatus returns dark blue for PENDING`() {
        assertEquals(InlayAnnotationProvider.COLOR_PENDING, InlayAnnotationProvider.getTextColorForStatus(CommentStatus.PENDING))
    }

    @Test
    fun `getTextColorForStatus returns dark green for RESOLVED`() {
        assertEquals(InlayAnnotationProvider.COLOR_RESOLVED, InlayAnnotationProvider.getTextColorForStatus(CommentStatus.RESOLVED))
    }

    // --- Color constants ---

    @Test
    fun `color constants have correct values`() {
        assertEquals(Color(0x9E, 0x9D, 0x24), InlayAnnotationProvider.COLOR_DRAFT)
        assertEquals(Color(0x15, 0x65, 0xC0), InlayAnnotationProvider.COLOR_PENDING)
        assertEquals(Color(0x2E, 0x7D, 0x32), InlayAnnotationProvider.COLOR_RESOLVED)
    }

    // --- computeBlockAnnotations ---

    @Test
    fun `computeBlockAnnotations returns empty for session with no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val annotations = provider.computeBlockAnnotations(session, "docs/README.md")
        assertTrue(annotations.isEmpty())
    }

    @Test
    fun `computeBlockAnnotations returns one annotation per comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t1", "First"))
        commentService.addComment(session, makeComment("docs/README.md", 10, 12, "t2", "Second"))

        val annotations = provider.computeBlockAnnotations(session, "docs/README.md")

        assertEquals(2, annotations.size)
        assertEquals("First", annotations[0].commentText)
        assertEquals("Second", annotations[1].commentText)
    }

    @Test
    fun `computeBlockAnnotations returns full text without truncation`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val longComment = "A".repeat(200)
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", longComment))

        val annotations = provider.computeBlockAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals(longComment, annotations[0].commentText)
    }

    @Test
    fun `computeBlockAnnotations includes Claude response`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Why?")
        commentService.addComment(session, comment)
        val index = session.comments.indexOfFirst { it.id == comment.id }
        session.comments[index] = session.comments[index].copy(
            claudeResponse = "Because X depends on Y",
            status = CommentStatus.RESOLVED
        )

        val annotations = provider.computeBlockAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals("Why?", annotations[0].commentText)
        assertEquals("Because X depends on Y", annotations[0].response)
        assertEquals(CommentStatus.RESOLVED, annotations[0].status)
    }

    @Test
    fun `computeBlockAnnotations excludes skipped and rejected comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t1", "c1", CommentStatus.DRAFT))
        val skipped = makeComment("docs/README.md", 20, 20, "t2", "c2")
        commentService.addComment(session, skipped)
        commentService.setCommentStatus(session, skipped.id, CommentStatus.SKIPPED)
        val rejected = makeComment("docs/README.md", 30, 30, "t3", "c3")
        commentService.addComment(session, rejected)
        commentService.setCommentStatus(session, rejected.id, CommentStatus.REJECTED)

        val annotations = provider.computeBlockAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals("c1", annotations[0].commentText)
    }

    @Test
    fun `computeBlockAnnotations filters by filePath`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t1", "c1"))
        commentService.addComment(session, makeComment("docs/OTHER.md", 20, 20, "t2", "c2"))

        val annotations = provider.computeBlockAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals(10, annotations[0].line)
    }

    @Test
    fun `computeBlockAnnotations preserves null response for pending comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))

        val annotations = provider.computeBlockAnnotations(session, "docs/README.md")

        assertEquals(1, annotations.size)
        assertEquals(null, annotations[0].response)
        assertEquals(CommentStatus.DRAFT, annotations[0].status)
    }

    // --- BlockAnnotation data class ---

    @Test
    fun `BlockAnnotation preserves all properties`() {
        val annotation = InlayAnnotationProvider.BlockAnnotation(
            line = 5,
            commentText = "Fix this",
            response = "Done",
            status = CommentStatus.RESOLVED
        )
        assertEquals(5, annotation.line)
        assertEquals("Fix this", annotation.commentText)
        assertEquals("Done", annotation.response)
        assertEquals(CommentStatus.RESOLVED, annotation.status)
    }

    @Test
    fun `BlockAnnotation equality`() {
        val a1 = InlayAnnotationProvider.BlockAnnotation(1, "c", null, CommentStatus.DRAFT)
        val a2 = InlayAnnotationProvider.BlockAnnotation(1, "c", null, CommentStatus.DRAFT)
        assertEquals(a1, a2)
        assertEquals(a1.hashCode(), a2.hashCode())
    }

    // --- InlayAnnotation data class ---

    @Test
    fun `InlayAnnotation preserves all properties`() {
        val annotation = InlayAnnotationProvider.InlayAnnotation(
            line = 5,
            text = "// hello",
            color = InlayAnnotationProvider.COLOR_DRAFT,
            status = CommentStatus.DRAFT
        )
        assertEquals(5, annotation.line)
        assertEquals("// hello", annotation.text)
        assertEquals(InlayAnnotationProvider.COLOR_DRAFT, annotation.color)
        assertEquals(CommentStatus.DRAFT, annotation.status)
    }

    @Test
    fun `InlayAnnotation equality`() {
        val a1 = InlayAnnotationProvider.InlayAnnotation(1, "t", InlayAnnotationProvider.COLOR_DRAFT, CommentStatus.DRAFT)
        val a2 = InlayAnnotationProvider.InlayAnnotation(1, "t", InlayAnnotationProvider.COLOR_DRAFT, CommentStatus.DRAFT)
        assertEquals(a1, a2)
        assertEquals(a1.hashCode(), a2.hashCode())
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
