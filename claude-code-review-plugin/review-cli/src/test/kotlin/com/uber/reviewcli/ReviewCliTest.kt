package com.uber.reviewcli

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewCliTest {

    private lateinit var tempDir: Path
    private lateinit var cli: ReviewCli

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("review-cli-test")
        cli = ReviewCli()
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- list ---

    @Test
    fun `list shows all comments with status`() {
        val file = createTestReviewFile()
        val result = cli.list(file)

        assertTrue(result.success)
        assertTrue(result.output.contains("[1] pending"))
        assertTrue(result.output.contains("[2] resolved"))
        assertTrue(result.output.contains("docs/README.md:10-15"))
        assertTrue(result.output.contains("docs/README.md:20-25"))
        assertTrue(result.output.contains("How does this work?"))
        assertTrue(result.output.contains("Caching concern"))
    }

    @Test
    fun `list shows summary line`() {
        val file = createTestReviewFile()
        val result = cli.list(file)

        assertTrue(result.output.contains("Summary: 2 total | 1 pending | 1 resolved"))
    }

    @Test
    fun `list truncates long comments`() {
        val longComment = "A".repeat(100)
        val file = createReviewFile(listOf(
            makeComment(1, "f.md", 1, 1, "ctx", longComment, "pending")
        ))
        val result = cli.list(file)

        assertTrue(result.output.contains("A".repeat(80) + "..."))
    }

    @Test
    fun `list returns error for missing file`() {
        val result = cli.list(tempDir.resolve("nonexistent.review.json"))

        assertFalse(result.success)
        assertTrue(result.output.contains("Error: File not found"))
    }

    // --- show ---

    @Test
    fun `show displays full comment detail`() {
        val file = createTestReviewFile()
        val result = cli.show(file, mapOf("comment" to "1"))

        assertTrue(result.success)
        assertTrue(result.output.contains("Comment 1"))
        assertTrue(result.output.contains("File:    docs/README.md"))
        assertTrue(result.output.contains("Lines:   10-15"))
        assertTrue(result.output.contains("Status:  pending"))
        assertTrue(result.output.contains("selected text one"))
        assertTrue(result.output.contains("How does this work?"))
        assertTrue(result.output.contains("(none)"))
        assertTrue(result.output.contains("Replies: 0"))
    }

    @Test
    fun `show displays claude response when present`() {
        val file = createTestReviewFile()
        val result = cli.show(file, mapOf("comment" to "2"))

        assertTrue(result.success)
        assertTrue(result.output.contains("Here is the answer."))
        assertFalse(result.output.contains("(none)"))
    }

    @Test
    fun `show displays replies`() {
        val comments = listOf(makeComment(
            1, "f.md", 1, 1, "ctx", "q", "resolved",
            claudeResponse = "answer",
            replies = listOf(Reply("vinay", "2026-02-12T16:00:00Z", "Follow up?"))
        ))
        val file = createReviewFile(comments)
        val result = cli.show(file, mapOf("comment" to "1"))

        assertTrue(result.output.contains("Replies: 1"))
        assertTrue(result.output.contains("[vinay] Follow up?"))
    }

    @Test
    fun `show returns error for missing --comment flag`() {
        val file = createTestReviewFile()
        val result = cli.show(file, emptyMap())

        assertFalse(result.success)
        assertTrue(result.output.contains("Missing required flag: --comment"))
    }

    @Test
    fun `show returns error for invalid comment index`() {
        val file = createTestReviewFile()
        val result = cli.show(file, mapOf("comment" to "99"))

        assertFalse(result.success)
        assertTrue(result.output.contains("Comment 99 not found"))
        assertTrue(result.output.contains("Valid range: 1-2"))
    }

    @Test
    fun `show returns error for non-numeric index`() {
        val file = createTestReviewFile()
        val result = cli.show(file, mapOf("comment" to "abc"))

        assertFalse(result.success)
        assertTrue(result.output.contains("Invalid comment index"))
    }

    @Test
    fun `show single line format`() {
        val file = createReviewFile(listOf(
            makeComment(1, "f.md", 42, 42, "ctx", "question", "pending")
        ))
        val result = cli.show(file, mapOf("comment" to "1"))

        assertTrue(result.output.contains("Lines:   42"))
        assertFalse(result.output.contains("42-42"))
    }

    // --- respond ---

    @Test
    fun `respond sets claude response and resolves comment`() {
        val file = createTestReviewFile()
        val result = cli.respond(file, mapOf("comment" to "1", "response" to "My detailed response."))

        assertTrue(result.success)
        assertTrue(result.output.contains("Comment 1 resolved."))

        val updated = ReviewFile.fromJson(Files.readString(file))
        assertEquals("resolved", updated.comments[0].status)
        assertEquals("My detailed response.", updated.comments[0].claudeResponse)
    }

    @Test
    fun `respond preserves other comments`() {
        val file = createTestReviewFile()
        cli.respond(file, mapOf("comment" to "1", "response" to "Response"))

        val updated = ReviewFile.fromJson(Files.readString(file))
        assertEquals("resolved", updated.comments[1].status)
        assertEquals("Here is the answer.", updated.comments[1].claudeResponse)
    }

    @Test
    fun `respond handles multiline response`() {
        val file = createTestReviewFile()
        val multiline = "Line 1\nLine 2\n```code```\nLine 4"
        cli.respond(file, mapOf("comment" to "1", "response" to multiline))

        val updated = ReviewFile.fromJson(Files.readString(file))
        assertEquals(multiline, updated.comments[0].claudeResponse)
    }

    @Test
    fun `respond returns error for missing --comment`() {
        val file = createTestReviewFile()
        val result = cli.respond(file, mapOf("response" to "text"))

        assertFalse(result.success)
        assertTrue(result.output.contains("Missing required flag: --comment"))
    }

    @Test
    fun `respond returns error for missing --response`() {
        val file = createTestReviewFile()
        val result = cli.respond(file, mapOf("comment" to "1"))

        assertFalse(result.success)
        assertTrue(result.output.contains("Missing required flag: --response"))
    }

    @Test
    fun `respond returns error for invalid comment index`() {
        val file = createTestReviewFile()
        val result = cli.respond(file, mapOf("comment" to "99", "response" to "text"))

        assertFalse(result.success)
        assertTrue(result.output.contains("Comment 99 not found"))
    }

    // --- reply ---

    @Test
    fun `reply appends reply and sets status to pending`() {
        val file = createTestReviewFile()
        val result = cli.reply(file, mapOf("comment" to "2", "text" to "What about caching?"))

        assertTrue(result.success)
        assertTrue(result.output.contains("Reply added to comment 2."))

        val updated = ReviewFile.fromJson(Files.readString(file))
        assertEquals("pending", updated.comments[1].status)
        assertEquals(1, updated.comments[1].replies.size)
        assertEquals("user", updated.comments[1].replies[0].author)
        assertEquals("What about caching?", updated.comments[1].replies[0].text)
    }

    @Test
    fun `reply preserves existing replies`() {
        val comments = listOf(makeComment(
            1, "f.md", 1, 1, "ctx", "q", "resolved",
            claudeResponse = "answer",
            replies = listOf(Reply("vinay", "2026-02-12T16:00:00Z", "First reply"))
        ))
        val file = createReviewFile(comments)
        cli.reply(file, mapOf("comment" to "1", "text" to "Second reply"))

        val updated = ReviewFile.fromJson(Files.readString(file))
        assertEquals(2, updated.comments[0].replies.size)
        assertEquals("First reply", updated.comments[0].replies[0].text)
        assertEquals("Second reply", updated.comments[0].replies[1].text)
    }

    @Test
    fun `reply returns error for missing --comment`() {
        val file = createTestReviewFile()
        val result = cli.reply(file, mapOf("text" to "reply"))

        assertFalse(result.success)
        assertTrue(result.output.contains("Missing required flag: --comment"))
    }

    @Test
    fun `reply returns error for missing --text`() {
        val file = createTestReviewFile()
        val result = cli.reply(file, mapOf("comment" to "1"))

        assertFalse(result.success)
        assertTrue(result.output.contains("Missing required flag: --text"))
    }

    // --- status ---

    @Test
    fun `status shows review summary for markdown`() {
        val file = createTestReviewFile()
        val result = cli.status(file)

        assertTrue(result.success)
        assertTrue(result.output.contains("Type:   MARKDOWN"))
        assertTrue(result.output.contains("Source: docs/README.md"))
        assertTrue(result.output.contains("Total: 2 | Pending: 1 | Resolved: 1 | Skipped: 0"))
    }

    @Test
    fun `status shows review summary for git diff`() {
        val reviewFile = ReviewFile(
            sessionId = "test-id",
            type = "GIT_DIFF",
            metadata = ReviewMetadata(
                author = "vinay",
                publishedAt = "2026-02-12T15:00:00Z",
                baseBranch = "main",
                compareBranch = "feature-auth"
            ),
            comments = listOf(
                makeComment(1, "src/Auth.kt", 10, 20, "ctx", "question", "pending")
            )
        )
        val file = tempDir.resolve("diff.review.json")
        Files.writeString(file, reviewFile.toJson())

        val result = cli.status(file)

        assertTrue(result.success)
        assertTrue(result.output.contains("Type:   GIT_DIFF"))
        assertTrue(result.output.contains("Diff:   main -> feature-auth"))
        assertTrue(result.output.contains("Total: 1 | Pending: 1 | Resolved: 0 | Skipped: 0"))
    }

    @Test
    fun `status returns error for missing file`() {
        val result = cli.status(tempDir.resolve("nonexistent.review.json"))

        assertFalse(result.success)
        assertTrue(result.output.contains("Error: File not found"))
    }

    // --- run (arg parsing) ---

    @Test
    fun `run with no args shows usage`() {
        val result = cli.run(emptyArray())

        assertFalse(result.success)
        assertTrue(result.output.contains("Usage:"))
    }

    @Test
    fun `run with --help shows usage`() {
        val result = cli.run(arrayOf("--help"))

        assertTrue(result.success)
        assertTrue(result.output.contains("Usage:"))
    }

    @Test
    fun `run with unknown command shows error`() {
        val file = createTestReviewFile()
        val result = cli.run(arrayOf("unknown", file.toString()))

        assertFalse(result.success)
        assertTrue(result.output.contains("Unknown command: unknown"))
    }

    @Test
    fun `run list command`() {
        val file = createTestReviewFile()
        val result = cli.run(arrayOf("list", file.toString()))

        assertTrue(result.success)
        assertTrue(result.output.contains("[1]"))
    }

    @Test
    fun `run show command with flags`() {
        val file = createTestReviewFile()
        val result = cli.run(arrayOf("show", file.toString(), "--comment", "1"))

        assertTrue(result.success)
        assertTrue(result.output.contains("Comment 1"))
    }

    @Test
    fun `run respond command with flags`() {
        val file = createTestReviewFile()
        val result = cli.run(arrayOf(
            "respond", file.toString(),
            "--comment", "1",
            "--response", "My response"
        ))

        assertTrue(result.success)
        assertTrue(result.output.contains("Comment 1 resolved."))
    }

    @Test
    fun `run status command`() {
        val file = createTestReviewFile()
        val result = cli.run(arrayOf("status", file.toString()))

        assertTrue(result.success)
        assertTrue(result.output.contains("MARKDOWN"))
    }

    // --- parseFlags ---

    @Test
    fun `parseFlags parses key-value pairs`() {
        val flags = ReviewCli.parseFlags(listOf("--comment", "1", "--response", "text"))
        assertEquals("1", flags["comment"])
        assertEquals("text", flags["response"])
    }

    @Test
    fun `parseFlags handles empty input`() {
        val flags = ReviewCli.parseFlags(emptyList())
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `parseFlags ignores orphan flags`() {
        val flags = ReviewCli.parseFlags(listOf("--comment"))
        assertTrue(flags.isEmpty())
    }

    // --- truncate ---

    @Test
    fun `truncate short text unchanged`() {
        assertEquals("hello", ReviewCli.truncate("hello"))
    }

    @Test
    fun `truncate at exactly max length`() {
        val text = "A".repeat(80)
        assertEquals(text, ReviewCli.truncate(text))
    }

    @Test
    fun `truncate over max length adds ellipsis`() {
        val text = "A".repeat(81)
        assertEquals("A".repeat(80) + "...", ReviewCli.truncate(text))
    }

    @Test
    fun `truncate replaces newlines with spaces`() {
        assertEquals("line one line two", ReviewCli.truncate("line one\nline two"))
    }

    // --- formatLines ---

    @Test
    fun `formatLines single line`() {
        assertEquals("42", ReviewCli.formatLines(42, 42))
    }

    @Test
    fun `formatLines range`() {
        assertEquals("42-45", ReviewCli.formatLines(42, 45))
    }

    // --- Helpers ---

    private fun createTestReviewFile(): Path {
        val comments = listOf(
            makeComment(1, "docs/README.md", 10, 15, "selected text one", "How does this work?", "pending"),
            makeComment(2, "docs/README.md", 20, 25, "selected text two", "Caching concern", "resolved",
                claudeResponse = "Here is the answer.")
        )
        return createReviewFile(comments)
    }

    private fun createReviewFile(comments: List<ReviewFileComment>): Path {
        val reviewFile = ReviewFile(
            sessionId = "test-session-id",
            type = "MARKDOWN",
            metadata = ReviewMetadata(
                author = "vinay.yerra",
                publishedAt = "2026-02-12T15:00:00Z",
                sourceFile = "docs/README.md"
            ),
            comments = comments
        )
        val path = tempDir.resolve("test.review.json")
        Files.writeString(path, reviewFile.toJson())
        return path
    }

    private fun makeComment(
        index: Int,
        filePath: String,
        startLine: Int,
        endLine: Int,
        selectedText: String,
        userComment: String,
        status: String,
        claudeResponse: String? = null,
        replies: List<Reply> = emptyList()
    ): ReviewFileComment {
        return ReviewFileComment(
            index = index,
            filePath = filePath,
            startLine = startLine,
            endLine = endLine,
            selectedText = selectedText,
            userComment = userComment,
            status = status,
            claudeResponse = claudeResponse,
            replies = replies
        )
    }
}
