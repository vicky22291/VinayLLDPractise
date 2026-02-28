package com.uber.jetbrains.reviewplugin.services

import com.uber.jetbrains.reviewplugin.listeners.ReviewModeListener
import com.uber.jetbrains.reviewplugin.model.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommentServiceTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService
    private lateinit var commentService: CommentService
    private lateinit var tracker: TestListener

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("comment-service-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
        commentService = CommentService(storageManager, reviewModeService)
        tracker = TestListener()
        reviewModeService.addListener(tracker)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- addComment ---

    @Test
    fun `addComment adds to session and notifies`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "text", "How does this work?")

        commentService.addComment(session, comment)

        assertEquals(1, session.comments.size)
        assertEquals(comment, session.comments[0])
        assertEquals(1, tracker.commentsChanged.size)
    }

    @Test
    fun `addComment saves drafts`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "text", "comment")

        commentService.addComment(session, comment)

        val loaded = storageManager.loadDrafts()
        assertEquals(1, loaded.size)
        assertEquals(1, loaded[0].comments.size)
    }

    @Test
    fun `addComment multiple comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")

        commentService.addComment(session, makeComment("file.md", 1, 5, "t1", "c1"))
        commentService.addComment(session, makeComment("file.md", 10, 15, "t2", "c2"))
        commentService.addComment(session, makeComment("file.md", 20, 25, "t3", "c3"))

        assertEquals(3, session.comments.size)
        assertEquals(3, tracker.commentsChanged.size)
    }

    // --- updateComment ---

    @Test
    fun `updateComment changes comment text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "text", "original")
        commentService.addComment(session, comment)

        commentService.updateComment(session, comment.id, "updated text")

        assertEquals("updated text", session.comments[0].commentText)
        assertEquals(2, tracker.commentsChanged.size)
    }

    @Test
    fun `updateComment preserves other fields`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 42, 45, "selected", "original")
        commentService.addComment(session, comment)

        commentService.updateComment(session, comment.id, "updated")

        val updated = session.comments[0]
        assertEquals(comment.id, updated.id)
        assertEquals("file.md", updated.filePath)
        assertEquals(42, updated.startLine)
        assertEquals(45, updated.endLine)
        assertEquals("selected", updated.selectedText)
        assertEquals("vinay.yerra", updated.authorId)
    }

    @Test
    fun `updateComment with unknown id does nothing`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "text", "original")
        commentService.addComment(session, comment)
        tracker.commentsChanged.clear()

        commentService.updateComment(session, UUID.randomUUID(), "new text")

        assertEquals("original", session.comments[0].commentText)
        assertEquals(0, tracker.commentsChanged.size)
    }

    @Test
    fun `updateComment saves drafts`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "text", "original")
        commentService.addComment(session, comment)

        commentService.updateComment(session, comment.id, "updated")

        val loaded = storageManager.loadDrafts()
        assertEquals("updated", loaded[0].comments[0].commentText)
    }

    // --- deleteComment ---

    @Test
    fun `deleteComment removes from session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "text", "comment")
        commentService.addComment(session, comment)

        commentService.deleteComment(session, comment.id)

        assertEquals(0, session.comments.size)
        assertEquals(2, tracker.commentsChanged.size) // add + delete
    }

    @Test
    fun `deleteComment saves drafts`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "text", "comment")
        commentService.addComment(session, comment)

        commentService.deleteComment(session, comment.id)

        val loaded = storageManager.loadDrafts()
        assertEquals(0, loaded[0].comments.size)
    }

    @Test
    fun `deleteComment with unknown id still notifies and saves`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "text", "comment")
        commentService.addComment(session, comment)
        tracker.commentsChanged.clear()

        commentService.deleteComment(session, UUID.randomUUID())

        // removeComment is a no-op for unknown ID, but notification still fires
        assertEquals(1, session.comments.size)
        assertEquals(1, tracker.commentsChanged.size)
    }

    // --- getCommentsForFile ---

    @Test
    fun `getCommentsForFile filters by file path`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file1.md", 1, 5, "t1", "c1"))
        commentService.addComment(session, makeComment("file2.md", 10, 15, "t2", "c2"))
        commentService.addComment(session, makeComment("file1.md", 20, 25, "t3", "c3"))

        val result = commentService.getCommentsForFile(session, "file1.md")

        assertEquals(2, result.size)
        assertTrue(result.all { it.filePath == "file1.md" })
    }

    @Test
    fun `getCommentsForFile returns empty for unknown file`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file1.md", 1, 5, "t1", "c1"))

        val result = commentService.getCommentsForFile(session, "unknown.md")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCommentsForFile returns empty for session with no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")

        val result = commentService.getCommentsForFile(session, "file.md")

        assertTrue(result.isEmpty())
    }

    // --- getCommentsForLine ---

    @Test
    fun `getCommentsForLine finds comment on exact line`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 10, 10, "t", "single line"))

        val result = commentService.getCommentsForLine(session, "file.md", 10)

        assertEquals(1, result.size)
        assertEquals("single line", result[0].commentText)
    }

    @Test
    fun `getCommentsForLine finds comment within range`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 10, 15, "t", "range"))

        assertEquals(1, commentService.getCommentsForLine(session, "file.md", 10).size)
        assertEquals(1, commentService.getCommentsForLine(session, "file.md", 12).size)
        assertEquals(1, commentService.getCommentsForLine(session, "file.md", 15).size)
    }

    @Test
    fun `getCommentsForLine excludes out-of-range lines`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 10, 15, "t", "range"))

        assertTrue(commentService.getCommentsForLine(session, "file.md", 9).isEmpty())
        assertTrue(commentService.getCommentsForLine(session, "file.md", 16).isEmpty())
    }

    @Test
    fun `getCommentsForLine filters by file path`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file1.md", 10, 15, "t", "c"))

        assertTrue(commentService.getCommentsForLine(session, "file2.md", 12).isEmpty())
    }

    @Test
    fun `getCommentsForLine returns multiple overlapping comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 10, 20, "t1", "wide"))
        commentService.addComment(session, makeComment("file.md", 14, 16, "t2", "narrow"))

        val result = commentService.getCommentsForLine(session, "file.md", 15)
        assertEquals(2, result.size)
    }

    // --- applyResponses ---

    @Test
    fun `applyResponses updates comments with claude responses`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 1, 5, "t1", "q1"))
        commentService.addComment(session, makeComment("file.md", 10, 15, "t2", "q2"))

        val reviewFile = ReviewFile(
            sessionId = session.id.toString(),
            type = "MARKDOWN",
            metadata = ReviewMetadata(author = "vinay.yerra", publishedAt = "2026-02-12T15:30:00Z"),
            comments = listOf(
                ReviewFileComment(index = 1, filePath = "file.md", startLine = 1, endLine = 5,
                    selectedText = "t1", userComment = "q1", status = "pending",
                    claudeResponse = "Answer 1"),
                ReviewFileComment(index = 2, filePath = "file.md", startLine = 10, endLine = 15,
                    selectedText = "t2", userComment = "q2", status = "pending",
                    claudeResponse = "Answer 2")
            )
        )

        commentService.applyResponses(session, reviewFile)

        assertEquals("Answer 1", session.comments[0].claudeResponse)
        assertEquals(CommentStatus.RESOLVED, session.comments[0].status)
        assertNotNull(session.comments[0].resolvedAt)

        assertEquals("Answer 2", session.comments[1].claudeResponse)
        assertEquals(CommentStatus.RESOLVED, session.comments[1].status)
        assertNotNull(session.comments[1].resolvedAt)
    }

    @Test
    fun `applyResponses notifies responses loaded`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 1, 5, "t", "q"))

        val reviewFile = ReviewFile(
            sessionId = session.id.toString(),
            type = "MARKDOWN",
            metadata = ReviewMetadata(author = "vinay.yerra", publishedAt = "2026-02-12T15:30:00Z"),
            comments = listOf(
                ReviewFileComment(index = 1, filePath = "file.md", startLine = 1, endLine = 5,
                    selectedText = "t", userComment = "q", status = "pending",
                    claudeResponse = "Answer")
            )
        )

        commentService.applyResponses(session, reviewFile)

        assertEquals(1, tracker.responsesLoaded.size)
    }

    @Test
    fun `applyResponses skips comments without response`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 1, 5, "t", "q"))

        val reviewFile = ReviewFile(
            sessionId = session.id.toString(),
            type = "MARKDOWN",
            metadata = ReviewMetadata(author = "vinay.yerra", publishedAt = "2026-02-12T15:30:00Z"),
            comments = listOf(
                ReviewFileComment(index = 1, filePath = "file.md", startLine = 1, endLine = 5,
                    selectedText = "t", userComment = "q", status = "pending",
                    claudeResponse = null)
            )
        )

        commentService.applyResponses(session, reviewFile)

        assertNull(session.comments[0].claudeResponse)
        assertEquals(CommentStatus.DRAFT, session.comments[0].status)
    }

    @Test
    fun `applyResponses ignores out-of-bounds index`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 1, 5, "t", "q"))

        val reviewFile = ReviewFile(
            sessionId = session.id.toString(),
            type = "MARKDOWN",
            metadata = ReviewMetadata(author = "vinay.yerra", publishedAt = "2026-02-12T15:30:00Z"),
            comments = listOf(
                ReviewFileComment(index = 5, filePath = "file.md", startLine = 1, endLine = 5,
                    selectedText = "t", userComment = "q", status = "pending",
                    claudeResponse = "Answer")
            )
        )

        commentService.applyResponses(session, reviewFile)

        // Original comment unchanged
        assertNull(session.comments[0].claudeResponse)
    }

    @Test
    fun `applyResponses ignores zero index`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 1, 5, "t", "q"))

        val reviewFile = ReviewFile(
            sessionId = session.id.toString(),
            type = "MARKDOWN",
            metadata = ReviewMetadata(author = "vinay.yerra", publishedAt = "2026-02-12T15:30:00Z"),
            comments = listOf(
                ReviewFileComment(index = 0, filePath = "file.md", startLine = 1, endLine = 5,
                    selectedText = "t", userComment = "q", status = "pending",
                    claudeResponse = "Answer")
            )
        )

        commentService.applyResponses(session, reviewFile)

        // index 0 maps to array index -1, which should be skipped
        assertNull(session.comments[0].claudeResponse)
    }

    @Test
    fun `applyResponses saves drafts`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("file.md", 1, 5, "t", "q"))

        val reviewFile = ReviewFile(
            sessionId = session.id.toString(),
            type = "MARKDOWN",
            metadata = ReviewMetadata(author = "vinay.yerra", publishedAt = "2026-02-12T15:30:00Z"),
            comments = listOf(
                ReviewFileComment(index = 1, filePath = "file.md", startLine = 1, endLine = 5,
                    selectedText = "t", userComment = "q", status = "pending",
                    claudeResponse = "Answer")
            )
        )

        commentService.applyResponses(session, reviewFile)

        val loaded = storageManager.loadDrafts()
        assertEquals("Answer", loaded[0].comments[0].claudeResponse)
    }

    // --- setCommentStatus ---

    @Test
    fun `setCommentStatus updates status`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "t", "q")
        commentService.addComment(session, comment)
        tracker.commentsChanged.clear()

        commentService.setCommentStatus(session, comment.id, CommentStatus.PENDING)

        assertEquals(CommentStatus.PENDING, session.comments[0].status)
        assertEquals(1, tracker.commentsChanged.size)
    }

    @Test
    fun `setCommentStatus with unknown id does nothing`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "t", "q")
        commentService.addComment(session, comment)
        tracker.commentsChanged.clear()

        commentService.setCommentStatus(session, UUID.randomUUID(), CommentStatus.RESOLVED)

        assertEquals(CommentStatus.DRAFT, session.comments[0].status)
        assertEquals(0, tracker.commentsChanged.size)
    }

    @Test
    fun `setCommentStatus saves drafts`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "t", "q")
        commentService.addComment(session, comment)

        commentService.setCommentStatus(session, comment.id, CommentStatus.SKIPPED)

        val loaded = storageManager.loadDrafts()
        assertEquals(CommentStatus.SKIPPED, loaded[0].comments[0].status)
    }

    @Test
    fun `setCommentStatus can set to rejected`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "t", "q")
        commentService.addComment(session, comment)

        commentService.setCommentStatus(session, comment.id, CommentStatus.REJECTED)

        assertEquals(CommentStatus.REJECTED, session.comments[0].status)
    }

    // --- addReply ---

    @Test
    fun `addReply sets draftReply on comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "t", "q")
        commentService.addComment(session, comment)
        tracker.commentsChanged.clear()

        commentService.addReply(session, comment.id, "my reply text")

        assertEquals("my reply text", session.comments[0].draftReply)
        assertEquals(1, tracker.commentsChanged.size)
    }

    @Test
    fun `addReply saves drafts`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "t", "q")
        commentService.addComment(session, comment)

        commentService.addReply(session, comment.id, "reply")

        val loaded = storageManager.loadDrafts()
        assertEquals("reply", loaded[0].comments[0].draftReply)
    }

    @Test
    fun `addReply with unknown id does nothing`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "t", "q")
        commentService.addComment(session, comment)
        tracker.commentsChanged.clear()

        commentService.addReply(session, UUID.randomUUID(), "reply")

        assertNull(session.comments[0].draftReply)
        assertEquals(0, tracker.commentsChanged.size)
    }

    @Test
    fun `addReply overwrites previous draftReply`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("file.md", 1, 5, "t", "q")
        commentService.addComment(session, comment)

        commentService.addReply(session, comment.id, "first reply")
        commentService.addReply(session, comment.id, "second reply")

        assertEquals("second reply", session.comments[0].draftReply)
    }

    // --- Helper ---

    private fun makeComment(
        filePath: String,
        startLine: Int,
        endLine: Int,
        selectedText: String,
        commentText: String,
        changeType: ChangeType? = null
    ): ReviewComment {
        return ReviewComment(
            filePath = filePath,
            startLine = startLine,
            endLine = endLine,
            selectedText = selectedText,
            commentText = commentText,
            authorId = "vinay.yerra",
            createdAt = Instant.parse("2026-02-12T15:28:12Z"),
            changeType = changeType
        )
    }

    private class TestListener : ReviewModeListener {
        val entered = mutableListOf<ReviewSession>()
        val exited = mutableListOf<ReviewSession>()
        val commentsChanged = mutableListOf<ReviewSession>()
        val responsesLoaded = mutableListOf<ReviewSession>()

        override fun onReviewModeEntered(session: ReviewSession) { entered.add(session) }
        override fun onReviewModeExited(session: ReviewSession) { exited.add(session) }
        override fun onCommentsChanged(session: ReviewSession) { commentsChanged.add(session) }
        override fun onResponsesLoaded(session: ReviewSession) { responsesLoaded.add(session) }
    }
}
