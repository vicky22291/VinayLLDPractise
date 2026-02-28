package com.uber.jetbrains.reviewplugin.listeners

import com.uber.jetbrains.reviewplugin.model.*
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewFileManager
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.services.StorageManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewFileWatcherTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService
    private lateinit var commentService: CommentService

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("file-watcher-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
        commentService = CommentService(storageManager, reviewModeService)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- isReviewFilePath ---

    @Test
    fun `isReviewFilePath matches review files in review directory`() {
        assertTrue(ReviewFileWatcher.isReviewFilePath("/project/.review/test.review.json"))
    }

    @Test
    fun `isReviewFilePath rejects files outside review directory`() {
        assertFalse(ReviewFileWatcher.isReviewFilePath("/project/test.review.json"))
    }

    @Test
    fun `isReviewFilePath rejects non-review-json files`() {
        assertFalse(ReviewFileWatcher.isReviewFilePath("/project/.review/test.json"))
    }

    @Test
    fun `isReviewFilePath rejects draft files`() {
        assertFalse(ReviewFileWatcher.isReviewFilePath("/project/.review/.drafts/session-123.review.json"))
    }

    @Test
    fun `isReviewFilePath rejects archive files`() {
        assertFalse(ReviewFileWatcher.isReviewFilePath("/project/.review/archives/test-abc12.review.json"))
    }

    @Test
    fun `isReviewFilePath matches nested project path`() {
        assertTrue(ReviewFileWatcher.isReviewFilePath("/users/dev/project/.review/docs--README.review.json"))
    }

    // --- findSessionForFile ---

    @Test
    fun `findSessionForFile matches by review file name`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        session.reviewFilePath = "/project/.review/docs--README.review.json"

        val found = ReviewFileWatcher.findSessionForFile(
            listOf(session),
            "/project/.review/docs--README.review.json"
        )
        assertNotNull(found)
        assertEquals(session.id, found.id)
    }

    @Test
    fun `findSessionForFile returns null when no match`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        session.reviewFilePath = "/project/.review/docs--README.review.json"

        val found = ReviewFileWatcher.findSessionForFile(
            listOf(session),
            "/project/.review/other-file.review.json"
        )
        assertNull(found)
    }

    @Test
    fun `findSessionForFile returns null when session has no review file path`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")

        val found = ReviewFileWatcher.findSessionForFile(
            listOf(session),
            "/project/.review/docs--README.review.json"
        )
        assertNull(found)
    }

    @Test
    fun `findSessionForFile returns null for empty sessions list`() {
        val found = ReviewFileWatcher.findSessionForFile(
            emptyList(),
            "/project/.review/test.review.json"
        )
        assertNull(found)
    }

    @Test
    fun `findSessionForFile matches by file name suffix`() {
        val session = reviewModeService.enterMarkdownReview("docs/ARCH.md")
        session.reviewFilePath = "/old/path/.review/docs--ARCH.review.json"

        // Different base path, same file name
        val found = ReviewFileWatcher.findSessionForFile(
            listOf(session),
            "/new/path/.review/docs--ARCH.review.json"
        )
        assertNotNull(found)
    }

    // --- isInternalWrite ---

    @Test
    fun `publish marks file as internal write`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "ctx", "question"))

        storageManager.ensureReviewDirectory()
        val outputDir = storageManager.getReviewDirectory()
        val reviewFilePath = ReviewFileManager.publish(session, outputDir)

        assertTrue(ReviewFileManager.isInternalWrite(reviewFilePath.toAbsolutePath().toString()))
    }

    @Test
    fun `non-published file is not internal write`() {
        assertFalse(ReviewFileManager.isInternalWrite("/some/random/path.review.json"))
    }

    // --- Helper ---

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
