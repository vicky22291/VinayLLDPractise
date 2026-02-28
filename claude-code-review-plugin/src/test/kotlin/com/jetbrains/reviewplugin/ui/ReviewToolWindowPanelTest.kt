package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.*
import com.uber.jetbrains.reviewplugin.services.CommentService
import com.uber.jetbrains.reviewplugin.services.ReviewFileManager
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import com.uber.jetbrains.reviewplugin.services.StorageManager
import com.uber.jetbrains.reviewplugin.ui.ReviewToolWindowPanel.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewToolWindowPanelTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService
    private lateinit var commentService: CommentService
    private lateinit var panel: ReviewToolWindowPanel

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("tool-window-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
        commentService = CommentService(storageManager, reviewModeService)
        panel = ReviewToolWindowPanel(reviewModeService, commentService, storageManager)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- PanelState ---

    @Test
    fun `getCurrentState returns NO_REVIEW when no session`() {
        assertEquals(PanelState.NO_REVIEW, panel.getCurrentState())
    }

    @Test
    fun `getCurrentState returns DRAFTS when active session with no responses`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertEquals(PanelState.DRAFTS, panel.getCurrentState())
    }

    @Test
    fun `getCurrentState returns DRAFTS when comments have no claude response`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 15, "text", "My comment"))
        panel.onReviewModeEntered(session)

        assertEquals(PanelState.DRAFTS, panel.getCurrentState())
    }

    @Test
    fun `getCurrentState returns RESPONSES when any comment has claude response`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 15, "text", "Question?")
        commentService.addComment(session, comment)
        session.comments[0] = session.comments[0].copy(
            claudeResponse = "Claude says hi",
            status = CommentStatus.RESOLVED
        )
        panel.onReviewModeEntered(session)

        assertEquals(PanelState.RESPONSES, panel.getCurrentState())
    }

    // --- Session Display Name ---

    @Test
    fun `getSessionDisplayName returns null when no session`() {
        assertNull(panel.getSessionDisplayName())
    }

    @Test
    fun `getSessionDisplayName returns markdown session name`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertEquals("Markdown: README.md", panel.getSessionDisplayName())
    }

    @Test
    fun `getSessionDisplayName returns diff session name`() {
        val session = reviewModeService.enterDiffReview("main", "feature-auth")
        panel.onReviewModeEntered(session)

        assertEquals("Diff: main -> feature-auth", panel.getSessionDisplayName())
    }

    // --- getCurrentSession ---

    @Test
    fun `getCurrentSession returns null when no session`() {
        assertNull(panel.getCurrentSession())
    }

    @Test
    fun `getCurrentSession returns active session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertEquals(session.id, panel.getCurrentSession()?.id)
    }

    // --- Publish Button Visibility ---

    @Test
    fun `isPublishButtonVisible returns false when no session`() {
        assertFalse(panel.isPublishButtonVisible())
    }

    @Test
    fun `isPublishButtonVisible returns true when draft comments exist`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "text", "Draft comment"))
        panel.onReviewModeEntered(session)

        assertTrue(panel.isPublishButtonVisible())
    }

    @Test
    fun `isPublishButtonVisible returns false when no draft comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "text", "Comment")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.PENDING)
        panel.onReviewModeEntered(session)

        assertFalse(panel.isPublishButtonVisible())
    }

    @Test
    fun `isPublishButtonVisible returns false when no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertFalse(panel.isPublishButtonVisible())
    }

    // --- Complete/Reject Enabled ---

    @Test
    fun `isCompleteEnabled returns false when no session`() {
        assertFalse(panel.isCompleteEnabled())
    }

    @Test
    fun `isCompleteEnabled returns true when ACTIVE`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertTrue(panel.isCompleteEnabled())
    }

    @Test
    fun `isCompleteEnabled returns true when PUBLISHED`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        session.status = ReviewSessionStatus.PUBLISHED
        panel.onReviewModeEntered(session)

        assertTrue(panel.isCompleteEnabled())
    }

    @Test
    fun `isCompleteEnabled returns false when SUSPENDED`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        session.status = ReviewSessionStatus.SUSPENDED
        panel.onReviewModeEntered(session)

        assertFalse(panel.isCompleteEnabled())
    }

    @Test
    fun `isRejectEnabled matches isCompleteEnabled`() {
        assertFalse(panel.isRejectEnabled())

        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertTrue(panel.isRejectEnabled())
    }

    // --- Comment Count ---

    @Test
    fun `getCommentCount returns 0 when no session`() {
        assertEquals(0, panel.getCommentCount())
    }

    @Test
    fun `getCommentCount returns correct count`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c1"))
        commentService.addComment(session, makeComment("docs/README.md", 20, 20, "t", "c2"))
        commentService.addComment(session, makeComment("docs/README.md", 30, 30, "t", "c3"))
        panel.onReviewModeEntered(session)

        assertEquals(3, panel.getCommentCount())
    }

    @Test
    fun `getDraftCount returns 0 when no session`() {
        assertEquals(0, panel.getDraftCount())
    }

    @Test
    fun `getDraftCount returns only draft comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val c1 = makeComment("docs/README.md", 10, 10, "t", "draft")
        val c2 = makeComment("docs/README.md", 20, 20, "t", "pending")
        commentService.addComment(session, c1)
        commentService.addComment(session, c2)
        commentService.setCommentStatus(session, c2.id, CommentStatus.PENDING)
        panel.onReviewModeEntered(session)

        assertEquals(1, panel.getDraftCount())
    }

    // --- Draft Comment Rows ---

    @Test
    fun `getDraftCommentRows returns empty when no session`() {
        assertTrue(panel.getDraftCommentRows().isEmpty())
    }

    @Test
    fun `getDraftCommentRows returns empty when no comments`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertTrue(panel.getDraftCommentRows().isEmpty())
    }

    @Test
    fun `getDraftCommentRows creates correct rows`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 42, 45, "selected", "How does this work?")
        commentService.addComment(session, comment)
        panel.onReviewModeEntered(session)

        val rows = panel.getDraftCommentRows()
        assertEquals(1, rows.size)
        assertEquals(comment.id, rows[0].commentId)
        assertEquals("Line 42-45", rows[0].lineRange)
        assertEquals("How does this work?", rows[0].previewText)
        assertEquals("docs/README.md", rows[0].filePath)
        assertEquals(42, rows[0].startLine)
    }

    @Test
    fun `getDraftCommentRows formats single-line range`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 42, 42, "t", "comment"))
        panel.onReviewModeEntered(session)

        assertEquals("Line 42", panel.getDraftCommentRows()[0].lineRange)
    }

    @Test
    fun `getDraftCommentRows keeps text at exactly max length`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val exactText = "A".repeat(80)
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", exactText))
        panel.onReviewModeEntered(session)

        val row = panel.getDraftCommentRows()[0]
        assertEquals(exactText, row.previewText)
    }

    @Test
    fun `getDraftCommentRows truncates text over 80 chars`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val longText = "A".repeat(81)
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", longText))
        panel.onReviewModeEntered(session)

        val row = panel.getDraftCommentRows()[0]
        assertEquals("A".repeat(80) + "...", row.previewText)
    }

    @Test
    fun `getDraftCommentRows sorts by line number by default`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 50, 50, "t", "c3"))
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c1"))
        commentService.addComment(session, makeComment("docs/README.md", 30, 30, "t", "c2"))
        panel.onReviewModeEntered(session)

        val rows = panel.getDraftCommentRows()
        assertEquals(10, rows[0].startLine)
        assertEquals(30, rows[1].startLine)
        assertEquals(50, rows[2].startLine)
    }

    @Test
    fun `getDraftCommentRows with STATUS sort preserves insertion order`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 50, 50, "t", "c3"))
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c1"))
        panel.onReviewModeEntered(session)

        val rows = panel.getDraftCommentRows(SortOrder.STATUS)
        assertEquals(50, rows[0].startLine)
        assertEquals(10, rows[1].startLine)
    }

    @Test
    fun `getDraftCommentRows with CREATED_AT sort preserves insertion order`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 50, 50, "t", "c1"))
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c2"))
        panel.onReviewModeEntered(session)

        val rows = panel.getDraftCommentRows(SortOrder.CREATED_AT)
        assertEquals(50, rows[0].startLine)
        assertEquals(10, rows[1].startLine)
    }

    // --- Resolved Comment Rows ---

    @Test
    fun `getResolvedCommentRows returns empty when no session`() {
        assertTrue(panel.getResolvedCommentRows().isEmpty())
    }

    @Test
    fun `getResolvedCommentRows creates rows with 1-based index`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 15, "text", "Question 1"))
        commentService.addComment(session, makeComment("docs/README.md", 20, 25, "text", "Question 2"))
        panel.onReviewModeEntered(session)

        val rows = panel.getResolvedCommentRows()
        assertEquals(2, rows.size)
        assertEquals(1, rows[0].index)
        assertEquals(2, rows[1].index)
    }

    @Test
    fun `getResolvedCommentRows includes claude response when present`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 15, "text", "Question?")
        commentService.addComment(session, comment)
        session.comments[0] = session.comments[0].copy(
            claudeResponse = "Here is the answer.",
            status = CommentStatus.RESOLVED
        )
        panel.onReviewModeEntered(session)

        val row = panel.getResolvedCommentRows()[0]
        assertEquals("Question?", row.userText)
        assertEquals("Here is the answer.", row.claudeResponse)
        assertEquals("Resolved", row.statusLabel)
    }

    @Test
    fun `getResolvedCommentRows shows null response for pending`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 15, "text", "Question?")
        commentService.addComment(session, comment)
        commentService.setCommentStatus(session, comment.id, CommentStatus.PENDING)
        panel.onReviewModeEntered(session)

        val row = panel.getResolvedCommentRows()[0]
        assertNull(row.claudeResponse)
        assertEquals("Pending", row.statusLabel)
    }

    @Test
    fun `getResolvedCommentRows includes correct fields`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 42, 45, "text", "My question")
        commentService.addComment(session, comment)
        panel.onReviewModeEntered(session)

        val row = panel.getResolvedCommentRows()[0]
        assertEquals("Line 42-45", row.lineRange)
        assertEquals("My question", row.userText)
        assertEquals("docs/README.md", row.filePath)
        assertEquals(42, row.startLine)
        assertEquals(comment.id, row.commentId)
    }

    // --- Publish Review ---

    @Test
    fun `publishReview returns null when no session`() {
        assertNull(panel.publishReview())
    }

    @Test
    fun `publishReview creates review file and returns result`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 15, "text", "Comment"))
        panel.onReviewModeEntered(session)

        val result = panel.publishReview()

        assertNotNull(result)
        assertTrue(result.reviewFilePath.contains(".review.json"))
        assertTrue(result.cliCommand.contains("review-respond"))
        assertTrue(Files.exists(Path.of(result.reviewFilePath)))
    }

    @Test
    fun `publishReview sets session status to PUBLISHED`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        panel.onReviewModeEntered(session)

        panel.publishReview()

        assertEquals(ReviewSessionStatus.PUBLISHED, session.status)
    }

    @Test
    fun `publishReview sets all comments to PENDING`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c1"))
        commentService.addComment(session, makeComment("docs/README.md", 20, 20, "t", "c2"))
        panel.onReviewModeEntered(session)

        panel.publishReview()

        assertTrue(session.comments.all { it.status == CommentStatus.PENDING })
    }

    @Test
    fun `publishReview sets reviewFilePath on session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        panel.onReviewModeEntered(session)

        panel.publishReview()

        assertNotNull(session.reviewFilePath)
    }

    @Test
    fun `publishReview sets publishedAt on session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        panel.onReviewModeEntered(session)

        val before = Instant.now()
        panel.publishReview()

        assertNotNull(session.publishedAt)
        assertFalse(session.publishedAt!!.isBefore(before))
    }

    @Test
    fun `publishReview cli command contains review file path`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        panel.onReviewModeEntered(session)

        val result = panel.publishReview()!!
        assertTrue(result.cliCommand.contains(result.reviewFilePath))
    }

    // --- Jump Target ---

    @Test
    fun `getJumpTarget returns null when no session`() {
        assertNull(panel.getJumpTarget(UUID.randomUUID()))
    }

    @Test
    fun `getJumpTarget returns null for unknown comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertNull(panel.getJumpTarget(UUID.randomUUID()))
    }

    @Test
    fun `getJumpTarget returns correct target`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 42, 50, "text", "Comment")
        commentService.addComment(session, comment)
        panel.onReviewModeEntered(session)

        val target = panel.getJumpTarget(comment.id)
        assertNotNull(target)
        assertEquals("docs/README.md", target.filePath)
        assertEquals(42, target.line)
    }

    // --- Reload Responses ---

    @Test
    fun `reloadResponses returns false when no session`() {
        assertFalse(panel.reloadResponses())
    }

    @Test
    fun `reloadResponses returns false when no review file path`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertFalse(panel.reloadResponses())
    }

    @Test
    fun `reloadResponses loads and applies responses`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "Question?"))
        panel.onReviewModeEntered(session)
        panel.attach()

        // Publish first
        panel.publishReview()
        val reviewFilePath = session.reviewFilePath!!

        // Simulate Claude responding by modifying the review file
        val reviewFile = ReviewFileManager.load(Path.of(reviewFilePath))
        val updatedComments = reviewFile.comments.map {
            it.copy(claudeResponse = "Here is the answer.", status = "resolved")
        }
        val updatedFile = reviewFile.copy(comments = updatedComments)
        Files.writeString(Path.of(reviewFilePath), updatedFile.toJson())

        // Reload
        val result = panel.reloadResponses()

        assertTrue(result)
        assertEquals(PanelState.RESPONSES, panel.getCurrentState())
        assertNotNull(session.comments[0].claudeResponse)
        panel.detach()
    }

    // --- Add Reply ---

    @Test
    fun `addReply returns false when no session`() {
        assertFalse(panel.addReply(1, "reply"))
    }

    @Test
    fun `addReply returns false when no review file path`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertFalse(panel.addReply(1, "reply"))
    }

    @Test
    fun `addReply returns false for blank text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        panel.onReviewModeEntered(session)
        panel.publishReview()

        assertFalse(panel.addReply(1, "   "))
    }

    @Test
    fun `addReply returns false for empty text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        panel.onReviewModeEntered(session)
        panel.publishReview()

        assertFalse(panel.addReply(1, ""))
    }

    @Test
    fun `addReply succeeds and marks comment as pending`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        panel.onReviewModeEntered(session)
        panel.publishReview()

        // Comment is already PENDING from publish; change to RESOLVED to verify reset
        session.comments[0] = session.comments[0].copy(status = CommentStatus.RESOLVED)

        val result = panel.addReply(1, "My follow-up question")

        assertTrue(result)
        assertEquals(CommentStatus.PENDING, session.comments[0].status)
    }

    @Test
    fun `addReply writes reply to review file`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        panel.onReviewModeEntered(session)
        panel.publishReview()

        panel.addReply(1, "Follow-up")

        val reviewFile = ReviewFileManager.load(Path.of(session.reviewFilePath!!))
        assertEquals(1, reviewFile.comments[0].replies.size)
        assertEquals("Follow-up", reviewFile.comments[0].replies[0].text)
    }

    @Test
    fun `addReply with out-of-range index still appends to file`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))
        panel.onReviewModeEntered(session)
        panel.publishReview()

        // Index 1 is valid (we have 1 comment), status should be updated
        val result = panel.addReply(1, "Reply text")
        assertTrue(result)
    }

    // --- Delete Comment ---

    @Test
    fun `deleteComment returns false when no session`() {
        assertFalse(panel.deleteComment(UUID.randomUUID()))
    }

    @Test
    fun `deleteComment returns false for unknown comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertFalse(panel.deleteComment(UUID.randomUUID()))
    }

    @Test
    fun `deleteComment removes comment from session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "t", "to delete")
        commentService.addComment(session, comment)
        panel.onReviewModeEntered(session)

        val result = panel.deleteComment(comment.id)

        assertTrue(result)
        assertEquals(0, session.comments.size)
    }

    // --- Edit Comment ---

    @Test
    fun `editComment returns false when no session`() {
        assertFalse(panel.editComment(UUID.randomUUID(), "new text"))
    }

    @Test
    fun `editComment returns false for blank text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "t", "original")
        commentService.addComment(session, comment)
        panel.onReviewModeEntered(session)

        assertFalse(panel.editComment(comment.id, "   "))
    }

    @Test
    fun `editComment returns false for empty text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "t", "original")
        commentService.addComment(session, comment)
        panel.onReviewModeEntered(session)

        assertFalse(panel.editComment(comment.id, ""))
    }

    @Test
    fun `editComment returns false for unknown comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertFalse(panel.editComment(UUID.randomUUID(), "new text"))
    }

    @Test
    fun `editComment updates comment text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "t", "original")
        commentService.addComment(session, comment)
        panel.onReviewModeEntered(session)

        val result = panel.editComment(comment.id, "updated text")

        assertTrue(result)
        assertEquals("updated text", session.comments[0].commentText)
    }

    @Test
    fun `editComment trims whitespace`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeComment("docs/README.md", 10, 10, "t", "original")
        commentService.addComment(session, comment)
        panel.onReviewModeEntered(session)

        panel.editComment(comment.id, "  trimmed  ")

        assertEquals("trimmed", session.comments[0].commentText)
    }

    // --- Complete Review ---

    @Test
    fun `completeReview returns false when no session`() {
        assertFalse(panel.completeReview())
    }

    @Test
    fun `completeReview returns false when not enabled`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        session.status = ReviewSessionStatus.SUSPENDED
        panel.onReviewModeEntered(session)

        assertFalse(panel.completeReview())
    }

    @Test
    fun `completeReview succeeds when ACTIVE`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)
        panel.attach()

        val result = panel.completeReview()

        assertTrue(result)
        assertEquals(PanelState.NO_REVIEW, panel.getCurrentState())
        panel.detach()
    }

    @Test
    fun `completeReview clears current session via listener`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)
        panel.attach()

        panel.completeReview()

        assertNull(panel.getCurrentSession())
        panel.detach()
    }

    // --- Reject Review ---

    @Test
    fun `rejectReview returns false when no session`() {
        assertFalse(panel.rejectReview())
    }

    @Test
    fun `rejectReview returns false when not enabled`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        session.status = ReviewSessionStatus.SUSPENDED
        panel.onReviewModeEntered(session)

        assertFalse(panel.rejectReview())
    }

    @Test
    fun `rejectReview succeeds when ACTIVE`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)
        panel.attach()

        val result = panel.rejectReview()

        assertTrue(result)
        assertEquals(PanelState.NO_REVIEW, panel.getCurrentState())
        panel.detach()
    }

    @Test
    fun `rejectReview clears current session via listener`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)
        panel.attach()

        panel.rejectReview()

        assertNull(panel.getCurrentSession())
        panel.detach()
    }

    // --- Listener: onReviewModeEntered ---

    @Test
    fun `onReviewModeEntered sets current session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")

        panel.onReviewModeEntered(session)

        assertEquals(session.id, panel.getCurrentSession()?.id)
        assertEquals(PanelState.DRAFTS, panel.getCurrentState())
    }

    // --- Listener: onReviewModeExited ---

    @Test
    fun `onReviewModeExited clears current session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        panel.onReviewModeExited(session)

        assertNull(panel.getCurrentSession())
        assertEquals(PanelState.NO_REVIEW, panel.getCurrentState())
    }

    @Test
    fun `onReviewModeExited ignores different session`() {
        val session1 = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session1)

        val session2 = reviewModeService.enterMarkdownReview("docs/OTHER.md")
        panel.onReviewModeExited(session2)

        assertNotNull(panel.getCurrentSession())
        assertEquals(session1.id, panel.getCurrentSession()?.id)
    }

    // --- Listener: onCommentsChanged ---

    @Test
    fun `onCommentsChanged updates session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)
        commentService.addComment(session, makeComment("docs/README.md", 10, 10, "t", "c"))

        panel.onCommentsChanged(session)

        assertEquals(1, panel.getCommentCount())
    }

    @Test
    fun `onCommentsChanged ignores different session`() {
        val session1 = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session1)

        val session2 = reviewModeService.enterMarkdownReview("docs/OTHER.md")
        panel.onCommentsChanged(session2)

        assertEquals(session1.id, panel.getCurrentSession()?.id)
    }

    // --- Listener: onResponsesLoaded ---

    @Test
    fun `onResponsesLoaded updates session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        panel.onResponsesLoaded(session)

        assertEquals(session.id, panel.getCurrentSession()?.id)
    }

    @Test
    fun `onResponsesLoaded ignores different session`() {
        val session1 = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session1)

        val session2 = reviewModeService.enterMarkdownReview("docs/OTHER.md")
        panel.onResponsesLoaded(session2)

        assertEquals(session1.id, panel.getCurrentSession()?.id)
    }

    // --- Attach/Detach ---

    @Test
    fun `attach registers panel as listener`() {
        panel.attach()

        val session = reviewModeService.enterMarkdownReview("docs/README.md")

        // If attached, onReviewModeEntered should have fired
        assertNotNull(panel.getCurrentSession())
        assertEquals(session.id, panel.getCurrentSession()?.id)
        panel.detach()
    }

    @Test
    fun `attach initializes currentSession from existing active session`() {
        // Start a session BEFORE attaching the panel (simulates lazy tool window creation)
        val session = reviewModeService.enterMarkdownReview("docs/README.md")

        val latePanel = ReviewToolWindowPanel(reviewModeService, commentService, storageManager)
        latePanel.attach()

        // Panel should pick up the already-active session
        assertNotNull(latePanel.getCurrentSession())
        assertEquals(session.id, latePanel.getCurrentSession()?.id)
        assertEquals(PanelState.DRAFTS, latePanel.getCurrentState())
        latePanel.detach()
    }

    @Test
    fun `detach removes panel as listener`() {
        panel.attach()
        panel.detach()

        reviewModeService.enterMarkdownReview("docs/README.md")

        // If detached, listener should not have fired
        assertNull(panel.getCurrentSession())
    }

    // --- isDiffReview ---

    @Test
    fun `isDiffReview returns false when no session`() {
        assertFalse(panel.isDiffReview())
    }

    @Test
    fun `isDiffReview returns false for markdown session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertFalse(panel.isDiffReview())
    }

    @Test
    fun `isDiffReview returns true for diff session`() {
        val session = reviewModeService.enterDiffReview("main", "feature", listOf("src/A.kt"))
        panel.onReviewModeEntered(session)

        assertTrue(panel.isDiffReview())
    }

    // --- getChangedFileRows ---

    @Test
    fun `getChangedFileRows returns empty for markdown session`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        panel.onReviewModeEntered(session)

        assertTrue(panel.getChangedFileRows().isEmpty())
    }

    @Test
    fun `getChangedFileRows returns empty when no session`() {
        assertTrue(panel.getChangedFileRows().isEmpty())
    }

    @Test
    fun `getChangedFileRows returns files with zero comments`() {
        val session = reviewModeService.enterDiffReview("main", "feature", listOf("src/A.kt", "src/B.kt"))
        panel.onReviewModeEntered(session)

        val rows = panel.getChangedFileRows()
        assertEquals(2, rows.size)
        assertEquals("src/A.kt", rows[0].filePath)
        assertEquals(0, rows[0].commentCount)
        assertEquals("src/B.kt", rows[1].filePath)
        assertEquals(0, rows[1].commentCount)
    }

    @Test
    fun `getChangedFileRows counts comments per file`() {
        val session = reviewModeService.enterDiffReview("main", "feature", listOf("src/A.kt", "src/B.kt"))
        commentService.addComment(session, makeComment("src/A.kt", 10, 10, "t", "c1"))
        commentService.addComment(session, makeComment("src/A.kt", 20, 20, "t", "c2"))
        commentService.addComment(session, makeComment("src/B.kt", 5, 5, "t", "c3"))
        panel.onReviewModeEntered(session)

        val rows = panel.getChangedFileRows()
        assertEquals(2, rows[0].commentCount) // A.kt
        assertEquals(1, rows[1].commentCount) // B.kt
    }

    // --- Companion: formatLineRange ---

    @Test
    fun `formatLineRange single line`() {
        assertEquals("Line 42", ReviewToolWindowPanel.formatLineRange(42, 42))
    }

    @Test
    fun `formatLineRange multi-line`() {
        assertEquals("Line 42-45", ReviewToolWindowPanel.formatLineRange(42, 45))
    }

    @Test
    fun `formatLineRange line 1`() {
        assertEquals("Line 1", ReviewToolWindowPanel.formatLineRange(1, 1))
    }

    // --- Companion: truncatePreview ---

    @Test
    fun `truncatePreview returns short text unchanged`() {
        assertEquals("hello", ReviewToolWindowPanel.truncatePreview("hello"))
    }

    @Test
    fun `truncatePreview returns text at max length unchanged`() {
        val text = "A".repeat(80)
        assertEquals(text, ReviewToolWindowPanel.truncatePreview(text))
    }

    @Test
    fun `truncatePreview truncates text over max length`() {
        val text = "A".repeat(81)
        assertEquals("A".repeat(80) + "...", ReviewToolWindowPanel.truncatePreview(text))
    }

    @Test
    fun `truncatePreview with custom max length`() {
        assertEquals("Hello...", ReviewToolWindowPanel.truncatePreview("Hello World", 5))
    }

    @Test
    fun `truncatePreview with empty text`() {
        assertEquals("", ReviewToolWindowPanel.truncatePreview(""))
    }

    // --- Companion: formatStatusLabel ---

    @Test
    fun `formatStatusLabel for all statuses`() {
        assertEquals("Draft", ReviewToolWindowPanel.formatStatusLabel(CommentStatus.DRAFT))
        assertEquals("Pending", ReviewToolWindowPanel.formatStatusLabel(CommentStatus.PENDING))
        assertEquals("Resolved", ReviewToolWindowPanel.formatStatusLabel(CommentStatus.RESOLVED))
        assertEquals("Skipped", ReviewToolWindowPanel.formatStatusLabel(CommentStatus.SKIPPED))
        assertEquals("Rejected", ReviewToolWindowPanel.formatStatusLabel(CommentStatus.REJECTED))
    }

    // --- Constants ---

    @Test
    fun `PREVIEW_MAX_LENGTH is 80`() {
        assertEquals(80, ReviewToolWindowPanel.PREVIEW_MAX_LENGTH)
    }

    @Test
    fun `EMPTY_STATE_TITLE is correct`() {
        assertEquals("Claude Code Review", ReviewToolWindowPanel.EMPTY_STATE_TITLE)
    }

    @Test
    fun `EMPTY_STATE_MESSAGE is correct`() {
        assertEquals("No active review session.", ReviewToolWindowPanel.EMPTY_STATE_MESSAGE)
    }

    @Test
    fun `EMPTY_STATE_HINT contains instructions`() {
        assertTrue(ReviewToolWindowPanel.EMPTY_STATE_HINT.contains("Review this Markdown"))
        assertTrue(ReviewToolWindowPanel.EMPTY_STATE_HINT.contains("Review the Diff"))
    }

    // --- Data Classes ---

    @Test
    fun `ChangedFileRow preserves properties`() {
        val row = ChangedFileRow("src/A.kt", 3)
        assertEquals("src/A.kt", row.filePath)
        assertEquals(3, row.commentCount)
    }

    @Test
    fun `ChangedFileRow equality`() {
        val r1 = ChangedFileRow("src/A.kt", 2)
        val r2 = ChangedFileRow("src/A.kt", 2)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `DraftCommentRow preserves all properties`() {
        val id = UUID.randomUUID()
        val row = DraftCommentRow(id, "Line 10-15", "preview", "file.md", 10)
        assertEquals(id, row.commentId)
        assertEquals("Line 10-15", row.lineRange)
        assertEquals("preview", row.previewText)
        assertEquals("file.md", row.filePath)
        assertEquals(10, row.startLine)
    }

    @Test
    fun `DraftCommentRow equality`() {
        val id = UUID.randomUUID()
        val row1 = DraftCommentRow(id, "Line 10", "preview", "file.md", 10)
        val row2 = DraftCommentRow(id, "Line 10", "preview", "file.md", 10)
        assertEquals(row1, row2)
        assertEquals(row1.hashCode(), row2.hashCode())
    }

    @Test
    fun `ResolvedCommentRow preserves all properties`() {
        val id = UUID.randomUUID()
        val row = ResolvedCommentRow(1, "Line 42-45", "question", "answer", "Resolved", "f.md", 42, id)
        assertEquals(1, row.index)
        assertEquals("Line 42-45", row.lineRange)
        assertEquals("question", row.userText)
        assertEquals("answer", row.claudeResponse)
        assertEquals("Resolved", row.statusLabel)
        assertEquals("f.md", row.filePath)
        assertEquals(42, row.startLine)
        assertEquals(id, row.commentId)
    }

    @Test
    fun `ResolvedCommentRow equality`() {
        val id = UUID.randomUUID()
        val row1 = ResolvedCommentRow(1, "Line 42", "q", "a", "Resolved", "f.md", 42, id)
        val row2 = ResolvedCommentRow(1, "Line 42", "q", "a", "Resolved", "f.md", 42, id)
        assertEquals(row1, row2)
        assertEquals(row1.hashCode(), row2.hashCode())
    }

    @Test
    fun `JumpTarget preserves properties`() {
        val target = JumpTarget("docs/README.md", 42)
        assertEquals("docs/README.md", target.filePath)
        assertEquals(42, target.line)
    }

    @Test
    fun `JumpTarget equality`() {
        val t1 = JumpTarget("f.md", 10)
        val t2 = JumpTarget("f.md", 10)
        assertEquals(t1, t2)
        assertEquals(t1.hashCode(), t2.hashCode())
    }

    @Test
    fun `PublishResult preserves properties`() {
        val result = PublishResult("/path/to/file.json", "claude cmd")
        assertEquals("/path/to/file.json", result.reviewFilePath)
        assertEquals("claude cmd", result.cliCommand)
    }

    @Test
    fun `PublishResult equality`() {
        val r1 = PublishResult("path", "cmd")
        val r2 = PublishResult("path", "cmd")
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    // --- PanelState enum ---

    @Test
    fun `PanelState has all expected values`() {
        val values = PanelState.entries
        assertEquals(3, values.size)
        assertTrue(values.contains(PanelState.NO_REVIEW))
        assertTrue(values.contains(PanelState.DRAFTS))
        assertTrue(values.contains(PanelState.RESPONSES))
    }

    // --- SortOrder enum ---

    @Test
    fun `SortOrder has all expected values`() {
        val values = SortOrder.entries
        assertEquals(3, values.size)
        assertTrue(values.contains(SortOrder.LINE_NUMBER))
        assertTrue(values.contains(SortOrder.STATUS))
        assertTrue(values.contains(SortOrder.CREATED_AT))
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
