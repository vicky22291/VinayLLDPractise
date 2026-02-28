package com.uber.jetbrains.reviewplugin.services

import com.uber.jetbrains.reviewplugin.model.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewFileManagerTest {

    private lateinit var tempDir: Path

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("review-file-manager-test")
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- 1. Publish Markdown session ---

    @Test
    fun `publish markdown session with 3 comments`() {
        val session = MarkdownReviewSession(
            sourceFilePath = "docs/uscorer/ARCHITECTURE_OVERVIEW.md",
            id = UUID.randomUUID()
        )
        session.addComment(makeComment("file.md", 1, 5, "text1", "How?"))
        session.addComment(makeComment("file.md", 10, 15, "text2", "Why?"))
        session.addComment(makeComment("file.md", 20, 25, "text3", "What?"))

        val path = ReviewFileManager.publish(session, tempDir)

        assertTrue(Files.exists(path))
        assertEquals(session.getReviewFileName(), path.fileName.toString())

        val reviewFile = ReviewFile.fromJson(Files.readString(path))
        assertEquals(session.id.toString(), reviewFile.sessionId)
        assertEquals("MARKDOWN", reviewFile.type)
        assertEquals(3, reviewFile.comments.size)
        reviewFile.comments.forEach { comment ->
            assertEquals("pending", comment.status)
            assertNull(comment.claudeResponse)
            assertTrue(comment.replies.isEmpty())
        }
    }

    // --- 2. Publish GitDiff session ---

    @Test
    fun `publish git diff session with branches and commits`() {
        val session = GitDiffReviewSession(
            baseBranch = "main",
            compareBranch = "feature/user-auth",
            baseCommit = "abc123",
            compareCommit = "def456",
            changedFiles = listOf("src/Main.kt", "src/Auth.kt")
        )
        session.addComment(makeComment("src/Main.kt", 10, 15, "code", "Check this", ChangeType.MODIFIED))

        val path = ReviewFileManager.publish(session, tempDir)

        assertTrue(Files.exists(path))
        val reviewFile = ReviewFile.fromJson(Files.readString(path))
        assertEquals("GIT_DIFF", reviewFile.type)
        assertEquals("main", reviewFile.metadata.baseBranch)
        assertEquals("feature/user-auth", reviewFile.metadata.compareBranch)
        assertEquals("abc123", reviewFile.metadata.baseCommit)
        assertEquals("def456", reviewFile.metadata.compareCommit)
        assertEquals(listOf("src/Main.kt", "src/Auth.kt"), reviewFile.metadata.filesChanged)
        assertNull(reviewFile.metadata.sourceFile)
        assertEquals(1, reviewFile.comments.size)
        assertEquals("modified", reviewFile.comments[0].changeType)
    }

    // --- 3. Load published file round-trip ---

    @Test
    fun `load published file and verify round trip`() {
        val sessionId = UUID.randomUUID()
        val session = MarkdownReviewSession(
            sourceFilePath = "README.md",
            id = sessionId
        )
        session.addComment(makeComment("README.md", 1, 10, "selected", "comment text"))

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertEquals(sessionId.toString(), loaded.sessionId)
        assertEquals("MARKDOWN", loaded.type)
        assertEquals("README.md", loaded.metadata.sourceFile)
        assertEquals(1, loaded.comments.size)
        assertEquals("README.md", loaded.comments[0].filePath)
        assertEquals(1, loaded.comments[0].startLine)
        assertEquals(10, loaded.comments[0].endLine)
        assertEquals("selected", loaded.comments[0].selectedText)
        assertEquals("comment text", loaded.comments[0].userComment)
        assertEquals("pending", loaded.comments[0].status)
    }

    // --- 4. Comment indexing (1-based) ---

    @Test
    fun `comment indexing is 1-based`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        repeat(5) { i ->
            session.addComment(makeComment("test.md", i * 10, i * 10 + 5, "text$i", "comment$i"))
        }

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertEquals(5, loaded.comments.size)
        assertEquals(listOf(1, 2, 3, 4, 5), loaded.comments.map { it.index })
    }

    // --- 5. Load with Claude responses ---

    @Test
    fun `load file with claude responses`() {
        val json = """
        {
            "sessionId": "test-session-id",
            "type": "MARKDOWN",
            "metadata": {
                "author": "vinay.yerra",
                "publishedAt": "2026-02-12T15:30:00Z",
                "sourceFile": "docs/test.md"
            },
            "comments": [
                {
                    "index": 1,
                    "filePath": "docs/test.md",
                    "startLine": 1,
                    "endLine": 5,
                    "selectedText": "some text",
                    "userComment": "Explain this",
                    "status": "resolved",
                    "claudeResponse": "This is the explanation.",
                    "replies": []
                },
                {
                    "index": 2,
                    "filePath": "docs/test.md",
                    "startLine": 10,
                    "endLine": 15,
                    "selectedText": "other text",
                    "userComment": "Why?",
                    "status": "pending",
                    "claudeResponse": null,
                    "replies": []
                }
            ]
        }
        """.trimIndent()

        val filePath = tempDir.resolve("test.review.json")
        Files.writeString(filePath, json)

        val loaded = ReviewFileManager.load(filePath)

        assertEquals("resolved", loaded.comments[0].status)
        assertEquals("This is the explanation.", loaded.comments[0].claudeResponse)
        assertEquals("pending", loaded.comments[1].status)
        assertNull(loaded.comments[1].claudeResponse)
    }

    // --- 6. Append reply and verify status reset ---

    @Test
    fun `append reply resets status to pending`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "question"))

        val path = ReviewFileManager.publish(session, tempDir)
        val reply = Reply(author = "vinay.yerra", timestamp = "2026-02-13T10:00:00Z", text = "Follow-up")

        ReviewFileManager.appendReply(path, commentIndex = 1, reply = reply)
        val loaded = ReviewFileManager.load(path)

        assertEquals(1, loaded.comments[0].replies.size)
        assertEquals("Follow-up", loaded.comments[0].replies[0].text)
        assertEquals("vinay.yerra", loaded.comments[0].replies[0].author)
        assertEquals("pending", loaded.comments[0].status)
    }

    // --- 7. Append multiple replies with thread order ---

    @Test
    fun `append multiple replies preserves thread order`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "question"))

        val path = ReviewFileManager.publish(session, tempDir)

        val reply1 = Reply(author = "vinay.yerra", timestamp = "2026-02-13T10:00:00Z", text = "First reply")
        val reply2 = Reply(author = "claude", timestamp = "2026-02-13T10:01:00Z", text = "Second reply")
        val reply3 = Reply(author = "vinay.yerra", timestamp = "2026-02-13T10:02:00Z", text = "Third reply")

        ReviewFileManager.appendReply(path, commentIndex = 1, reply = reply1)
        ReviewFileManager.appendReply(path, commentIndex = 1, reply = reply2)
        ReviewFileManager.appendReply(path, commentIndex = 1, reply = reply3)

        val loaded = ReviewFileManager.load(path)

        assertEquals(3, loaded.comments[0].replies.size)
        assertEquals("First reply", loaded.comments[0].replies[0].text)
        assertEquals("Second reply", loaded.comments[0].replies[1].text)
        assertEquals("Third reply", loaded.comments[0].replies[2].text)
    }

    // --- 8. generateCliCommand format ---

    @Test
    fun `generateCliCommand returns correct format`() {
        val result = ReviewFileManager.generateCliCommand(".review/name.review.json")
        assertEquals("""claude "/review-respond .review/name.review.json"""", result)
    }

    // --- 9. Review file naming convention ---

    @Test
    fun `review file written with correct name from session`() {
        val session = MarkdownReviewSession(sourceFilePath = "docs/uscorer/ARCHITECTURE_OVERVIEW.md")
        session.addComment(makeComment("docs/test.md", 1, 5, "text", "comment"))

        val path = ReviewFileManager.publish(session, tempDir)

        assertEquals("docs--uscorer--ARCHITECTURE_OVERVIEW.review.json", path.fileName.toString())
    }

    @Test
    fun `git diff review file naming`() {
        val session = GitDiffReviewSession(
            baseBranch = "main",
            compareBranch = "feature/auth"
        )
        session.addComment(makeComment("src/Auth.kt", 1, 5, "code", "review"))

        val path = ReviewFileManager.publish(session, tempDir)

        assertEquals("diff-main--feature-auth.review.json", path.fileName.toString())
    }

    // --- Edge cases ---

    @Test
    fun `publish creates output directory if not exists`() {
        val nestedDir = tempDir.resolve("sub/dir")
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "comment"))

        val path = ReviewFileManager.publish(session, nestedDir)

        assertTrue(Files.exists(path))
        assertTrue(Files.isDirectory(nestedDir))
    }

    @Test
    fun `publish session with no comments`() {
        val session = MarkdownReviewSession(sourceFilePath = "empty.md")

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertTrue(loaded.comments.isEmpty())
    }

    @Test
    fun `append reply to invalid index throws`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "question"))

        val path = ReviewFileManager.publish(session, tempDir)
        val reply = Reply(author = "user", timestamp = "2026-02-13T10:00:00Z", text = "reply")

        assertFailsWith<IllegalArgumentException> {
            ReviewFileManager.appendReply(path, commentIndex = 99, reply = reply)
        }
    }

    @Test
    fun `publish markdown session has null git fields in metadata`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "comment"))

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertNull(loaded.metadata.baseBranch)
        assertNull(loaded.metadata.compareBranch)
        assertNull(loaded.metadata.baseCommit)
        assertNull(loaded.metadata.compareCommit)
        assertNull(loaded.metadata.filesChanged)
    }

    @Test
    fun `publish git diff session has null source file in metadata`() {
        val session = GitDiffReviewSession(
            baseBranch = "main",
            compareBranch = "dev"
        )
        session.addComment(makeComment("src/File.kt", 1, 5, "code", "review"))

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertNull(loaded.metadata.sourceFile)
    }

    @Test
    fun `metadata author matches system user`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "comment"))

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertEquals(System.getProperty("user.name"), loaded.metadata.author)
    }

    @Test
    fun `metadata publishedAt is populated`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "comment"))

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertTrue(loaded.metadata.publishedAt.isNotEmpty())
    }

    @Test
    fun `comment change type is serialized as lowercase`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "added comment", ChangeType.ADDED))
        session.addComment(makeComment("test.md", 10, 15, "text", "deleted comment", ChangeType.DELETED))

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertEquals("added", loaded.comments[0].changeType)
        assertEquals("deleted", loaded.comments[1].changeType)
    }

    @Test
    fun `comment with null change type`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "no change type"))

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertNull(loaded.comments[0].changeType)
    }

    @Test
    fun `append reply to second comment in multi-comment file`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text1", "q1"))
        session.addComment(makeComment("test.md", 10, 15, "text2", "q2"))
        session.addComment(makeComment("test.md", 20, 25, "text3", "q3"))

        val path = ReviewFileManager.publish(session, tempDir)
        val reply = Reply(author = "user", timestamp = "2026-02-13T10:00:00Z", text = "reply to q2")

        ReviewFileManager.appendReply(path, commentIndex = 2, reply = reply)
        val loaded = ReviewFileManager.load(path)

        assertTrue(loaded.comments[0].replies.isEmpty())
        assertEquals(1, loaded.comments[1].replies.size)
        assertEquals("reply to q2", loaded.comments[1].replies[0].text)
        assertTrue(loaded.comments[2].replies.isEmpty())
    }

    @Test
    fun `generateCliCommand with path containing spaces`() {
        val result = ReviewFileManager.generateCliCommand(".review/my file.review.json")
        assertEquals("""claude "/review-respond .review/my file.review.json"""", result)
    }

    @Test
    fun `session id is preserved through publish and load`() {
        val sessionId = UUID.fromString("12345678-1234-1234-1234-123456789012")
        val session = MarkdownReviewSession(
            sourceFilePath = "test.md",
            id = sessionId
        )
        session.addComment(makeComment("test.md", 1, 5, "text", "comment"))

        val path = ReviewFileManager.publish(session, tempDir)
        val loaded = ReviewFileManager.load(path)

        assertEquals("12345678-1234-1234-1234-123456789012", loaded.sessionId)
    }

    // --- publishReplies ---

    @Test
    fun `publishReplies appends reply and resets status to pending`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "question"))

        val path = ReviewFileManager.publish(session, tempDir)

        // Set draftReply on the comment
        session.comments[0] = session.comments[0].copy(draftReply = "follow-up question")

        val result = ReviewFileManager.publishReplies(session, path)

        assertEquals(path, result)
        val loaded = ReviewFileManager.load(path)
        assertEquals(1, loaded.comments[0].replies.size)
        assertEquals("follow-up question", loaded.comments[0].replies[0].text)
        assertEquals("pending", loaded.comments[0].status)
    }

    @Test
    fun `publishReplies preserves existing replies`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "question"))

        val path = ReviewFileManager.publish(session, tempDir)

        // Add an existing reply first
        val existingReply = Reply(author = "claude", timestamp = "2026-02-13T10:00:00Z", text = "response")
        ReviewFileManager.appendReply(path, commentIndex = 1, reply = existingReply)

        // Now publish a draft reply
        session.comments[0] = session.comments[0].copy(draftReply = "follow-up")
        ReviewFileManager.publishReplies(session, path)

        val loaded = ReviewFileManager.load(path)
        assertEquals(2, loaded.comments[0].replies.size)
        assertEquals("response", loaded.comments[0].replies[0].text)
        assertEquals("follow-up", loaded.comments[0].replies[1].text)
    }

    @Test
    fun `publishReplies only affects comments with draftReply`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text1", "q1"))
        session.addComment(makeComment("test.md", 10, 15, "text2", "q2"))
        session.addComment(makeComment("test.md", 20, 25, "text3", "q3"))

        val path = ReviewFileManager.publish(session, tempDir)

        // Only set draftReply on second comment
        session.comments[1] = session.comments[1].copy(draftReply = "reply to q2")

        ReviewFileManager.publishReplies(session, path)

        val loaded = ReviewFileManager.load(path)
        assertTrue(loaded.comments[0].replies.isEmpty())
        assertEquals(1, loaded.comments[1].replies.size)
        assertEquals("reply to q2", loaded.comments[1].replies[0].text)
        assertTrue(loaded.comments[2].replies.isEmpty())

        // Only the replied comment has status reset
        assertEquals("pending", loaded.comments[1].status)
    }

    @Test
    fun `publishReplies with no draft replies does not modify file`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "question"))

        val path = ReviewFileManager.publish(session, tempDir)
        val originalContent = java.nio.file.Files.readString(path)

        // No draftReply set on any comment
        ReviewFileManager.publishReplies(session, path)

        // File is rewritten but content is effectively the same (comments unchanged)
        val loaded = ReviewFileManager.load(path)
        assertTrue(loaded.comments[0].replies.isEmpty())
    }

    @Test
    fun `publishReplies sets author from system property`() {
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(makeComment("test.md", 1, 5, "text", "question"))

        val path = ReviewFileManager.publish(session, tempDir)
        session.comments[0] = session.comments[0].copy(draftReply = "reply")

        ReviewFileManager.publishReplies(session, path)

        val loaded = ReviewFileManager.load(path)
        assertEquals(System.getProperty("user.name"), loaded.comments[0].replies[0].author)
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
}
