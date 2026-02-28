package com.uber.jetbrains.reviewplugin.ui

import com.uber.jetbrains.reviewplugin.model.CommentStatus
import com.uber.jetbrains.reviewplugin.model.ReviewComment
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplyPopupEditorTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService
    private lateinit var commentService: CommentService

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("reply-popup-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
        commentService = CommentService(storageManager, reviewModeService)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- initialReplyText ---

    @Test
    fun `initialReplyText is empty when no draftReply`() {
        val comment = makeResolvedComment(claudeResponse = "response")
        val editor = createEditor(comment)
        assertEquals("", editor.initialReplyText)
    }

    @Test
    fun `initialReplyText returns existing draftReply`() {
        val comment = makeResolvedComment(claudeResponse = "response", draftReply = "existing reply")
        val editor = createEditor(comment)
        assertEquals("existing reply", editor.initialReplyText)
    }

    // --- buildHeaderText ---

    @Test
    fun `buildHeaderText shows claude response`() {
        val comment = makeResolvedComment(claudeResponse = "This is my detailed response")
        val editor = createEditor(comment)
        assertEquals("Claude's response:\nThis is my detailed response", editor.buildHeaderText())
    }

    @Test
    fun `buildHeaderText truncates long response`() {
        val longResponse = "A".repeat(250)
        val comment = makeResolvedComment(claudeResponse = longResponse)
        val editor = createEditor(comment)
        val header = editor.buildHeaderText()
        assertTrue(header.contains("A".repeat(200)))
        assertTrue(header.endsWith("..."))
    }

    @Test
    fun `buildHeaderText shows no response message when null`() {
        val comment = makeResolvedComment(claudeResponse = null)
        val editor = createEditor(comment)
        assertEquals("No response yet", editor.buildHeaderText())
    }

    @Test
    fun `buildHeaderText does not truncate short response`() {
        val comment = makeResolvedComment(claudeResponse = "Short")
        val editor = createEditor(comment)
        assertEquals("Claude's response:\nShort", editor.buildHeaderText())
    }

    @Test
    fun `buildHeaderText does not truncate response at max length`() {
        val exactLength = "A".repeat(200)
        val comment = makeResolvedComment(claudeResponse = exactLength)
        val editor = createEditor(comment)
        assertEquals("Claude's response:\n$exactLength", editor.buildHeaderText())
    }

    // --- validateReplyText ---

    @Test
    fun `validateReplyText returns error for empty text`() {
        val comment = makeResolvedComment(claudeResponse = "response")
        val editor = createEditor(comment)
        assertNotNull(editor.validateReplyText(""))
    }

    @Test
    fun `validateReplyText returns error for whitespace-only text`() {
        val comment = makeResolvedComment(claudeResponse = "response")
        val editor = createEditor(comment)
        assertNotNull(editor.validateReplyText("   \t\n  "))
    }

    @Test
    fun `validateReplyText returns null for valid text`() {
        val comment = makeResolvedComment(claudeResponse = "response")
        val editor = createEditor(comment)
        assertNull(editor.validateReplyText("This is a valid reply"))
    }

    @Test
    fun `validateReplyText returns null for text with surrounding whitespace`() {
        val comment = makeResolvedComment(claudeResponse = "response")
        val editor = createEditor(comment)
        assertNull(editor.validateReplyText("  valid reply  "))
    }

    // --- saveReply ---

    @Test
    fun `saveReply sets draftReply on comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeResolvedComment(claudeResponse = "response")
        commentService.addComment(session, comment)

        val editor = ReplyPopupEditor(commentService, session, session.comments[0])

        val result = editor.saveReply("my reply")

        assertTrue(result is ReplyPopupEditor.SaveResult.Saved)
        assertEquals("my reply", session.comments[0].draftReply)
    }

    @Test
    fun `saveReply trims whitespace`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeResolvedComment(claudeResponse = "response")
        commentService.addComment(session, comment)

        val editor = ReplyPopupEditor(commentService, session, session.comments[0])

        editor.saveReply("  trimmed reply  ")

        assertEquals("trimmed reply", session.comments[0].draftReply)
    }

    @Test
    fun `saveReply returns validation error for empty text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeResolvedComment(claudeResponse = "response")
        commentService.addComment(session, comment)

        val editor = ReplyPopupEditor(commentService, session, session.comments[0])

        val result = editor.saveReply("")

        assertTrue(result is ReplyPopupEditor.SaveResult.ValidationError)
        assertNull(session.comments[0].draftReply)
    }

    @Test
    fun `saveReply returns validation error for whitespace text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val comment = makeResolvedComment(claudeResponse = "response")
        commentService.addComment(session, comment)

        val editor = ReplyPopupEditor(commentService, session, session.comments[0])

        val result = editor.saveReply("   \t  ")

        assertTrue(result is ReplyPopupEditor.SaveResult.ValidationError)
        assertTrue((result as ReplyPopupEditor.SaveResult.ValidationError).message.isNotEmpty())
    }

    // --- SaveResult ---

    @Test
    fun `SaveResult Saved is singleton`() {
        val r1 = ReplyPopupEditor.SaveResult.Saved
        val r2 = ReplyPopupEditor.SaveResult.Saved
        assertTrue(r1 === r2)
    }

    @Test
    fun `SaveResult ValidationError holds error message`() {
        val result = ReplyPopupEditor.SaveResult.ValidationError("error msg")
        assertEquals("error msg", result.message)
    }

    // --- Constants ---

    @Test
    fun `MAX_RESPONSE_PREVIEW_LENGTH is 200`() {
        assertEquals(200, ReplyPopupEditor.MAX_RESPONSE_PREVIEW_LENGTH)
    }

    // --- comment field access ---

    @Test
    fun `comment field is accessible`() {
        val comment = makeResolvedComment(claudeResponse = "response")
        val editor = createEditor(comment)
        assertEquals(comment, editor.comment)
    }

    // --- Helper ---

    private fun createEditor(comment: ReviewComment): ReplyPopupEditor {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        return ReplyPopupEditor(commentService, session, comment)
    }

    private fun makeResolvedComment(
        claudeResponse: String?,
        draftReply: String? = null
    ): ReviewComment {
        return ReviewComment(
            filePath = "docs/README.md",
            startLine = 10,
            endLine = 15,
            selectedText = "selected text",
            commentText = "original question",
            authorId = "vinay.yerra",
            createdAt = Instant.parse("2026-02-12T15:28:12Z"),
            status = CommentStatus.RESOLVED,
            claudeResponse = claudeResponse,
            draftReply = draftReply
        )
    }
}
