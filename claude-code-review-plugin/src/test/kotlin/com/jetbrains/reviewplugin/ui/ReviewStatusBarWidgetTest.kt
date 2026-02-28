package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.*
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.services.StorageManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.assertEquals

class ReviewStatusBarWidgetTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService
    private lateinit var commentService: CommentService

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("status-bar-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
        commentService = CommentService(storageManager, reviewModeService)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `formatStatusText returns empty when no sessions`() {
        assertEquals("", ReviewStatusBarWidget.formatStatusText(emptyList()))
    }

    @Test
    fun `formatStatusText shows 0 drafts for session with no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        assertEquals(
            "Review Mode: Active | 0 drafts",
            ReviewStatusBarWidget.formatStatusText(listOf(session))
        )
    }

    @Test
    fun `formatStatusText shows correct draft count`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c1"))
        commentService.addComment(session, makeComment("docs/README.md", 20, 20, "t", "c2"))
        commentService.addComment(session, makeComment("docs/README.md", 30, 30, "t", "c3"))

        assertEquals(
            "Review Mode: Active | 3 drafts",
            ReviewStatusBarWidget.formatStatusText(listOf(session))
        )
    }

    @Test
    fun `formatStatusText excludes non-draft comments from count`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c1 = makeComment("docs/README.md", 10, 10, "t", "draft")
        val c2 = makeComment("docs/README.md", 20, 20, "t", "pending")
        commentService.addComment(session, c1)
        commentService.addComment(session, c2)
        commentService.setCommentStatus(session, c2.id, CommentStatus.PENDING)

        assertEquals(
            "Review Mode: Active | 1 drafts",
            ReviewStatusBarWidget.formatStatusText(listOf(session))
        )
    }

    @Test
    fun `formatStatusText uses first session when multiple exist`() {
        val session1 = reviewModeService.enterMarkdownReview("docs/A.md")
        commentService.addComment(session1, makeComment("docs/A.md", 10, 10, "t", "c1"))
        val session2 = reviewModeService.enterMarkdownReview("docs/B.md")
        commentService.addComment(session2, makeComment("docs/B.md", 10, 10, "t", "c2"))
        commentService.addComment(session2, makeComment("docs/B.md", 20, 20, "t", "c3"))

        val result = ReviewStatusBarWidget.formatStatusText(listOf(session1, session2))
        assertEquals("Review Mode: Active | 1 drafts", result)
    }

    private fun makeComment(
        filePath: String,
        startLine: Int,
        endLine: Int,
        selectedText: String,
        commentText: String
    ): ReviewComment {
        return ReviewComment(
            filePath = filePath,
            startLine = startLine,
            endLine = endLine,
            selectedText = selectedText,
            commentText = commentText,
            authorId = "vinay.yerra",
            createdAt = Instant.parse("2026-02-12T15:28:12Z"),
            status = CommentStatus.DRAFT
        )
    }
}
