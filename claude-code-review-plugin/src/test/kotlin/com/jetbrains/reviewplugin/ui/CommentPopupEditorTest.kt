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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommentPopupEditorTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService
    private lateinit var commentService: CommentService

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("popup-editor-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
        commentService = CommentService(storageManager, reviewModeService)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- isEditMode ---

    @Test
    fun `isEditMode returns false for new comment`() {
        val editor = createPopup(existingComment = null)
        assertFalse(editor.isEditMode)
    }

    @Test
    fun `isEditMode returns true for existing comment`() {
        val comment = makeComment("file.md", 10, 15, "text", "existing")
        val editor = createPopup(existingComment = comment)
        assertTrue(editor.isEditMode)
    }

    // --- initialCommentText ---

    @Test
    fun `initialCommentText is empty for new comment`() {
        val editor = createPopup(existingComment = null)
        assertEquals("", editor.initialCommentText)
    }

    @Test
    fun `initialCommentText returns existing comment text`() {
        val comment = makeComment("file.md", 10, 15, "text", "Hello world")
        val editor = createPopup(existingComment = comment)
        assertEquals("Hello world", editor.initialCommentText)
    }

    // --- contextPreview ---

    @Test
    fun `contextPreview returns short text unchanged`() {
        val editor = createPopup(selectedText = "short text")
        assertEquals("short text", editor.contextPreview)
    }

    @Test
    fun `contextPreview truncates long text`() {
        val longText = "A".repeat(150)
        val editor = createPopup(selectedText = longText)
        assertEquals("A".repeat(100) + "...", editor.contextPreview)
    }

    // --- buildHeaderText ---

    @Test
    fun `buildHeaderText for single line`() {
        val editor = createPopup(startLine = 42, endLine = 42)
        assertEquals("Comment on line 42", editor.buildHeaderText())
    }

    @Test
    fun `buildHeaderText for multi-line range`() {
        val editor = createPopup(startLine = 42, endLine = 45)
        assertEquals("Comment on lines 42-45", editor.buildHeaderText())
    }

    @Test
    fun `buildHeaderText for line 1`() {
        val editor = createPopup(startLine = 1, endLine = 1)
        assertEquals("Comment on line 1", editor.buildHeaderText())
    }

    // --- validateCommentText ---

    @Test
    fun `validateCommentText returns error for empty text`() {
        val editor = createPopup()
        assertNotNull(editor.validateCommentText(""))
    }

    @Test
    fun `validateCommentText returns error for whitespace-only text`() {
        val editor = createPopup()
        assertNotNull(editor.validateCommentText("   \t\n  "))
    }

    @Test
    fun `validateCommentText returns null for valid text`() {
        val editor = createPopup()
        assertNull(editor.validateCommentText("This is valid"))
    }

    @Test
    fun `validateCommentText returns null for text with surrounding whitespace`() {
        val editor = createPopup()
        assertNull(editor.validateCommentText("  valid  "))
    }

    // --- buildNewComment ---

    @Test
    fun `buildNewComment creates comment with correct fields`() {
        val editor = createPopup(
            filePath = "docs/README.md",
            startLine = 42,
            endLine = 45,
            selectedText = "context text"
        )

        val comment = editor.buildNewComment("My comment")

        assertEquals("docs/README.md", comment.filePath)
        assertEquals(42, comment.startLine)
        assertEquals(45, comment.endLine)
        assertEquals("context text", comment.selectedText)
        assertEquals("My comment", comment.commentText)
        assertEquals(CommentStatus.DRAFT, comment.status)
        assertNull(comment.claudeResponse)
        assertNull(comment.resolvedAt)
        assertNull(comment.changeType)
    }

    @Test
    fun `buildNewComment trims whitespace from comment text`() {
        val editor = createPopup()
        val comment = editor.buildNewComment("  trimmed comment  ")
        assertEquals("trimmed comment", comment.commentText)
    }

    @Test
    fun `buildNewComment generates unique IDs`() {
        val editor = createPopup()
        val c1 = editor.buildNewComment("comment 1")
        val c2 = editor.buildNewComment("comment 2")
        assertNotEquals(c1.id, c2.id)
    }

    @Test
    fun `buildNewComment sets author from system property`() {
        val editor = createPopup()
        val comment = editor.buildNewComment("comment")
        val expectedAuthor = System.getProperty("user.name") ?: "unknown"
        assertEquals(expectedAuthor, comment.authorId)
    }

    // --- saveComment (new) ---

    @Test
    fun `saveComment creates new comment when no existing`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val editor = CommentPopupEditor(
            commentService, session, "docs/README.md", 10, 15, "context", null
        )

        val result = editor.saveComment("New comment text")

        assertTrue(result is CommentPopupEditor.SaveResult.Created)
        assertEquals(1, session.comments.size)
        assertEquals("New comment text", session.comments[0].commentText)
    }

    @Test
    fun `saveComment returns validation error for empty text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val editor = CommentPopupEditor(
            commentService, session, "docs/README.md", 10, 15, "context", null
        )

        val result = editor.saveComment("")

        assertTrue(result is CommentPopupEditor.SaveResult.ValidationError)
        assertEquals(0, session.comments.size)
    }

    @Test
    fun `saveComment returns validation error for whitespace text`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val editor = CommentPopupEditor(
            commentService, session, "docs/README.md", 10, 15, "context", null
        )

        val result = editor.saveComment("   \t  ")

        assertTrue(result is CommentPopupEditor.SaveResult.ValidationError)
        assertTrue((result as CommentPopupEditor.SaveResult.ValidationError).message.isNotEmpty())
    }

    // --- saveComment (edit) ---

    @Test
    fun `saveComment updates existing comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val existing = makeComment("docs/README.md", 10, 15, "context", "original text")
        commentService.addComment(session, existing)

        val editor = CommentPopupEditor(
            commentService, session, "docs/README.md", 10, 15, "context", existing
        )

        val result = editor.saveComment("updated text")

        assertTrue(result is CommentPopupEditor.SaveResult.Updated)
        assertEquals("updated text", session.comments[0].commentText)
    }

    @Test
    fun `saveComment trims text before saving new comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val editor = CommentPopupEditor(
            commentService, session, "docs/README.md", 10, 15, "context", null
        )

        editor.saveComment("  trimmed  ")

        assertEquals("trimmed", session.comments[0].commentText)
    }

    @Test
    fun `saveComment trims text before updating existing comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val existing = makeComment("docs/README.md", 10, 15, "context", "original")
        commentService.addComment(session, existing)

        val editor = CommentPopupEditor(
            commentService, session, "docs/README.md", 10, 15, "context", existing
        )

        editor.saveComment("  updated  ")

        assertEquals("updated", session.comments[0].commentText)
    }

    // --- deleteComment ---

    @Test
    fun `deleteComment removes existing comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val existing = makeComment("docs/README.md", 10, 15, "context", "to delete")
        commentService.addComment(session, existing)

        val editor = CommentPopupEditor(
            commentService, session, "docs/README.md", 10, 15, "context", existing
        )

        val result = editor.deleteComment()

        assertTrue(result)
        assertEquals(0, session.comments.size)
    }

    @Test
    fun `deleteComment returns false when no existing comment`() {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        val editor = CommentPopupEditor(
            commentService, session, "docs/README.md", 10, 15, "context", null
        )

        val result = editor.deleteComment()

        assertFalse(result)
    }

    // --- truncateContext (companion) ---

    @Test
    fun `truncateContext returns short text unchanged`() {
        assertEquals("hello", CommentPopupEditor.truncateContext("hello"))
    }

    @Test
    fun `truncateContext returns text at max length unchanged`() {
        val text = "A".repeat(100)
        assertEquals(text, CommentPopupEditor.truncateContext(text))
    }

    @Test
    fun `truncateContext truncates text over max length`() {
        val text = "A".repeat(101)
        val result = CommentPopupEditor.truncateContext(text)
        assertEquals("A".repeat(100) + "...", result)
    }

    @Test
    fun `truncateContext with custom max length`() {
        val result = CommentPopupEditor.truncateContext("Hello World", 5)
        assertEquals("Hello...", result)
    }

    @Test
    fun `truncateContext with empty text`() {
        assertEquals("", CommentPopupEditor.truncateContext(""))
    }

    // --- captureContextAtLine (companion) ---

    @Test
    fun `captureContextAtLine returns line content`() {
        val doc = "line one\nline two\nline three"
        assertEquals("line two", CommentPopupEditor.captureContextAtLine(doc, 3, 2))
    }

    @Test
    fun `captureContextAtLine trims whitespace`() {
        val doc = "  line one  \n  line two  \n  line three  "
        assertEquals("line two", CommentPopupEditor.captureContextAtLine(doc, 3, 2))
    }

    @Test
    fun `captureContextAtLine returns empty for line below 1`() {
        val doc = "line one\nline two"
        assertEquals("", CommentPopupEditor.captureContextAtLine(doc, 2, 0))
    }

    @Test
    fun `captureContextAtLine returns empty for line above lineCount`() {
        val doc = "line one\nline two"
        assertEquals("", CommentPopupEditor.captureContextAtLine(doc, 2, 3))
    }

    @Test
    fun `captureContextAtLine returns first line`() {
        val doc = "first\nsecond\nthird"
        assertEquals("first", CommentPopupEditor.captureContextAtLine(doc, 3, 1))
    }

    @Test
    fun `captureContextAtLine returns last line`() {
        val doc = "first\nsecond\nthird"
        assertEquals("third", CommentPopupEditor.captureContextAtLine(doc, 3, 3))
    }

    @Test
    fun `captureContextAtLine handles single line document`() {
        val doc = "only line"
        assertEquals("only line", CommentPopupEditor.captureContextAtLine(doc, 1, 1))
    }

    @Test
    fun `captureContextAtLine returns empty for negative line`() {
        val doc = "line one"
        assertEquals("", CommentPopupEditor.captureContextAtLine(doc, 1, -1))
    }

    @Test
    fun `captureContextAtLine returns empty when lineCount exceeds actual lines`() {
        val doc = "only one line"
        // lineCount says 5 but there's only 1 line
        assertEquals("", CommentPopupEditor.captureContextAtLine(doc, 5, 3))
    }

    @Test
    fun `captureContextAtLine handles empty document`() {
        assertEquals("", CommentPopupEditor.captureContextAtLine("", 0, 1))
    }

    // --- captureSelectionContext (companion) ---

    @Test
    fun `captureSelectionContext with selection returns selected range and text`() {
        val (range, text) = CommentPopupEditor.captureSelectionContext(
            documentText = "line1\nline2\nline3",
            hasSelection = true,
            selectionStartLine = 1,
            selectionEndLine = 2,
            selectedText = "line1\nline2",
            caretLine = 1
        )

        assertEquals(1..2, range)
        assertEquals("line1\nline2", text)
    }

    @Test
    fun `captureSelectionContext with selection and null text returns empty string`() {
        val (range, text) = CommentPopupEditor.captureSelectionContext(
            documentText = "line1\nline2",
            hasSelection = true,
            selectionStartLine = 1,
            selectionEndLine = 1,
            selectedText = null,
            caretLine = 1
        )

        assertEquals(1..1, range)
        assertEquals("", text)
    }

    @Test
    fun `captureSelectionContext without selection captures caret line`() {
        val doc = "first line\nsecond line\nthird line"
        val (range, text) = CommentPopupEditor.captureSelectionContext(
            documentText = doc,
            hasSelection = false,
            selectionStartLine = 0,
            selectionEndLine = 0,
            selectedText = null,
            caretLine = 2
        )

        assertEquals(2..2, range)
        assertEquals("second line", text)
    }

    @Test
    fun `captureSelectionContext without selection at last line`() {
        val doc = "first\nsecond\nthird"
        val (range, text) = CommentPopupEditor.captureSelectionContext(
            documentText = doc,
            hasSelection = false,
            selectionStartLine = 0,
            selectionEndLine = 0,
            selectedText = null,
            caretLine = 3
        )

        assertEquals(3..3, range)
        assertEquals("third", text)
    }

    // --- Constants ---

    @Test
    fun `MAX_CONTEXT_LENGTH is 100`() {
        assertEquals(100, CommentPopupEditor.MAX_CONTEXT_LENGTH)
    }

    @Test
    fun `MIN_POPUP_WIDTH is 350`() {
        assertEquals(350, CommentPopupEditor.MIN_POPUP_WIDTH)
    }

    @Test
    fun `MIN_POPUP_HEIGHT is 200`() {
        assertEquals(200, CommentPopupEditor.MIN_POPUP_HEIGHT)
    }

    @Test
    fun `TEXT_AREA_ROWS is 4`() {
        assertEquals(4, CommentPopupEditor.TEXT_AREA_ROWS)
    }

    @Test
    fun `TEXT_AREA_COLUMNS is 40`() {
        assertEquals(40, CommentPopupEditor.TEXT_AREA_COLUMNS)
    }

    // --- Constructor field access ---

    @Test
    fun `constructor stores filePath`() {
        val editor = createPopup(filePath = "docs/README.md")
        assertEquals("docs/README.md", editor.filePath)
    }

    @Test
    fun `constructor stores startLine and endLine`() {
        val editor = createPopup(startLine = 10, endLine = 20)
        assertEquals(10, editor.startLine)
        assertEquals(20, editor.endLine)
    }

    @Test
    fun `constructor stores selectedText`() {
        val editor = createPopup(selectedText = "captured context")
        assertEquals("captured context", editor.selectedText)
    }

    @Test
    fun `constructor stores existingComment`() {
        val comment = makeComment("f.md", 1, 1, "t", "c")
        val editor = createPopup(existingComment = comment)
        assertEquals(comment, editor.existingComment)
    }

    @Test
    fun `constructor stores null existingComment`() {
        val editor = createPopup(existingComment = null)
        assertNull(editor.existingComment)
    }

    // --- SaveResult ---

    @Test
    fun `SaveResult Created holds comment reference`() {
        val comment = makeComment("f.md", 1, 1, "t", "c")
        val result = CommentPopupEditor.SaveResult.Created(comment)
        assertEquals(comment, result.comment)
    }

    @Test
    fun `SaveResult ValidationError holds error message`() {
        val result = CommentPopupEditor.SaveResult.ValidationError("error msg")
        assertEquals("error msg", result.message)
    }

    @Test
    fun `SaveResult Updated is singleton`() {
        val r1 = CommentPopupEditor.SaveResult.Updated
        val r2 = CommentPopupEditor.SaveResult.Updated
        assertTrue(r1 === r2)
    }

    // --- Helper ---

    private fun createPopup(
        filePath: String = "docs/README.md",
        startLine: Int = 10,
        endLine: Int = 15,
        selectedText: String = "selected text",
        existingComment: ReviewComment? = null
    ): CommentPopupEditor {
        val session = reviewModeService.enterMarkdownReview("docs/README.md")
        return CommentPopupEditor(
            commentService, session, filePath, startLine, endLine, selectedText, existingComment
        )
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
            createdAt = Instant.parse("2026-02-12T15:28:12Z")
        )
    }
}
