package com.uber.jetbrains.reviewplugin.model

import org.junit.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviewSessionTest {

    // --- MarkdownReviewSession.getReviewFileName() ---

    @Test
    fun `markdown review file name for nested path`() {
        val session = MarkdownReviewSession("docs/uscorer/ARCHITECTURE_OVERVIEW.md")
        assertEquals("docs--uscorer--ARCHITECTURE_OVERVIEW.review.json", session.getReviewFileName())
    }

    @Test
    fun `markdown review file name for root level file`() {
        val session = MarkdownReviewSession("README.md")
        assertEquals("README.review.json", session.getReviewFileName())
    }

    @Test
    fun `markdown review file name for deep path`() {
        val session = MarkdownReviewSession("src/main/design.md")
        assertEquals("src--main--design.review.json", session.getReviewFileName())
    }

    // --- GitDiffReviewSession.getReviewFileName() ---

    @Test
    fun `diff review file name for simple branches`() {
        val session = GitDiffReviewSession(baseBranch = "main", compareBranch = "feature-auth")
        assertEquals("diff-main--feature-auth.review.json", session.getReviewFileName())
    }

    @Test
    fun `diff review file name for slash branches`() {
        val session = GitDiffReviewSession(baseBranch = "main", compareBranch = "feature/user-auth")
        assertEquals("diff-main--feature-user-auth.review.json", session.getReviewFileName())
    }

    // --- ReviewFile JSON round-trip ---

    @Test
    fun `review file json round trip`() {
        val original = ReviewFile(
            sessionId = UUID.randomUUID().toString(),
            type = "MARKDOWN",
            metadata = ReviewMetadata(
                author = "vinay.yerra",
                publishedAt = "2026-02-12T15:30:00Z",
                sourceFile = "docs/uscorer/ARCHITECTURE_OVERVIEW.md"
            ),
            comments = listOf(
                ReviewFileComment(
                    index = 1,
                    filePath = "docs/uscorer/ARCHITECTURE_OVERVIEW.md",
                    startLine = 42,
                    endLine = 45,
                    selectedText = "The proposed feature store...",
                    userComment = "How does this integrate with Feature Manager?",
                    status = "pending",
                    claudeResponse = null,
                    replies = listOf(
                        Reply(author = "vinay.yerra", timestamp = "2026-02-12T16:05:00Z", text = "Follow-up question")
                    )
                )
            )
        )

        val json = original.toJson()
        val deserialized = ReviewFile.fromJson(json)

        assertEquals(original, deserialized)
    }

    @Test
    fun `review file json round trip with git diff metadata`() {
        val original = ReviewFile(
            sessionId = UUID.randomUUID().toString(),
            type = "GIT_DIFF",
            metadata = ReviewMetadata(
                author = "vinay.yerra",
                publishedAt = "2026-02-12T15:30:00Z",
                baseBranch = "main",
                compareBranch = "feature-auth",
                baseCommit = "abc123",
                compareCommit = "def456",
                filesChanged = listOf("src/Main.kt", "src/Test.kt")
            ),
            comments = listOf(
                ReviewFileComment(
                    index = 1,
                    filePath = "src/Main.kt",
                    startLine = 10,
                    endLine = 15,
                    selectedText = "fun main()",
                    userComment = "Check this",
                    status = "draft",
                    claudeResponse = "Looks good",
                    changeType = "MODIFIED"
                )
            )
        )

        val json = original.toJson()
        val deserialized = ReviewFile.fromJson(json)

        assertEquals(original, deserialized)

        // Verify DTO field access
        assertEquals("GIT_DIFF", deserialized.type)
        assertEquals(original.sessionId, deserialized.sessionId)
        assertEquals("vinay.yerra", deserialized.metadata.author)
        assertEquals("2026-02-12T15:30:00Z", deserialized.metadata.publishedAt)
        assertNull(deserialized.metadata.sourceFile)
        assertEquals("main", deserialized.metadata.baseBranch)
        assertEquals("feature-auth", deserialized.metadata.compareBranch)
        assertEquals("abc123", deserialized.metadata.baseCommit)
        assertEquals("def456", deserialized.metadata.compareCommit)
        assertEquals(listOf("src/Main.kt", "src/Test.kt"), deserialized.metadata.filesChanged)

        val comment = deserialized.comments[0]
        assertEquals(1, comment.index)
        assertEquals("src/Main.kt", comment.filePath)
        assertEquals(10, comment.startLine)
        assertEquals(15, comment.endLine)
        assertEquals("fun main()", comment.selectedText)
        assertEquals("Check this", comment.userComment)
        assertEquals("draft", comment.status)
        assertEquals("Looks good", comment.claudeResponse)
        assertEquals("MODIFIED", comment.changeType)
        assertTrue(comment.replies.isEmpty())
    }

    @Test
    fun `reply fields are accessible`() {
        val reply = Reply(author = "claude", timestamp = "2026-02-12T16:00:00Z", text = "Here is my response")
        assertEquals("claude", reply.author)
        assertEquals("2026-02-12T16:00:00Z", reply.timestamp)
        assertEquals("Here is my response", reply.text)
    }

    // --- ReviewComment defaults ---

    @Test
    fun `review comment has default draft status and null claude response`() {
        val comment = ReviewComment(
            filePath = "README.md",
            startLine = 10,
            endLine = 10,
            selectedText = "Some text",
            commentText = "My comment",
            authorId = "vinay.yerra"
        )

        assertEquals(CommentStatus.DRAFT, comment.status)
        assertNull(comment.claudeResponse)
        assertNull(comment.resolvedAt)
        assertNull(comment.changeType)
        assertNull(comment.draftReply)
    }

    @Test
    fun `review comment all fields accessible`() {
        val now = Instant.now()
        val id = UUID.randomUUID()
        val comment = ReviewComment(
            id = id,
            filePath = "src/Main.kt",
            startLine = 5,
            endLine = 10,
            selectedText = "selected",
            commentText = "my comment",
            authorId = "vinay.yerra",
            createdAt = now,
            status = CommentStatus.PENDING,
            changeType = ChangeType.ADDED
        )

        assertEquals(id, comment.id)
        assertEquals("src/Main.kt", comment.filePath)
        assertEquals(5, comment.startLine)
        assertEquals(10, comment.endLine)
        assertEquals("selected", comment.selectedText)
        assertEquals("my comment", comment.commentText)
        assertEquals("vinay.yerra", comment.authorId)
        assertEquals(now, comment.createdAt)
        assertEquals(CommentStatus.PENDING, comment.status)
        assertEquals(ChangeType.ADDED, comment.changeType)
    }

    @Test
    fun `review comment mutable fields can be set`() {
        val comment = ReviewComment(
            filePath = "README.md",
            startLine = 1,
            endLine = 1,
            selectedText = "text",
            commentText = "comment",
            authorId = "vinay.yerra"
        )

        val resolvedTime = Instant.now()
        comment.status = CommentStatus.RESOLVED
        comment.claudeResponse = "Claude's answer"
        comment.resolvedAt = resolvedTime

        assertEquals(CommentStatus.RESOLVED, comment.status)
        assertEquals("Claude's answer", comment.claudeResponse)
        assertEquals(resolvedTime, comment.resolvedAt)
    }

    // --- ChangeType enum ---

    @Test
    fun `change type enum values`() {
        assertEquals(3, ChangeType.entries.size)
        assertEquals(ChangeType.ADDED, ChangeType.valueOf("ADDED"))
        assertEquals(ChangeType.MODIFIED, ChangeType.valueOf("MODIFIED"))
        assertEquals(ChangeType.DELETED, ChangeType.valueOf("DELETED"))
    }

    // --- ReviewSession comment operations ---

    @Test
    fun `add and remove comments`() {
        val session = MarkdownReviewSession("README.md")
        val comment = ReviewComment(
            filePath = "README.md",
            startLine = 1,
            endLine = 1,
            selectedText = "text",
            commentText = "comment",
            authorId = "vinay.yerra"
        )

        session.addComment(comment)
        assertEquals(1, session.comments.size)
        assertEquals(comment, session.getComment(comment.id))

        session.removeComment(comment.id)
        assertTrue(session.comments.isEmpty())
        assertNull(session.getComment(comment.id))
    }

    @Test
    fun `get draft and pending comments`() {
        val session = MarkdownReviewSession("README.md")

        val draft = ReviewComment(
            filePath = "README.md", startLine = 1, endLine = 1,
            selectedText = "a", commentText = "draft", authorId = "vinay.yerra"
        )
        val pending = ReviewComment(
            filePath = "README.md", startLine = 5, endLine = 5,
            selectedText = "b", commentText = "pending", authorId = "vinay.yerra",
            status = CommentStatus.PENDING
        )
        val resolved = ReviewComment(
            filePath = "README.md", startLine = 10, endLine = 10,
            selectedText = "c", commentText = "resolved", authorId = "vinay.yerra",
            status = CommentStatus.RESOLVED
        )

        session.addComment(draft)
        session.addComment(pending)
        session.addComment(resolved)

        assertEquals(listOf(draft), session.getDraftComments())
        assertEquals(listOf(pending), session.getPendingComments())
    }

    @Test
    fun `getDraftReplyComments returns only comments with draftReply`() {
        val session = MarkdownReviewSession("README.md")

        val noReply = ReviewComment(
            filePath = "README.md", startLine = 1, endLine = 1,
            selectedText = "a", commentText = "no reply", authorId = "vinay.yerra",
            status = CommentStatus.RESOLVED
        )
        val withReply = ReviewComment(
            filePath = "README.md", startLine = 5, endLine = 5,
            selectedText = "b", commentText = "has reply", authorId = "vinay.yerra",
            status = CommentStatus.RESOLVED, draftReply = "my follow-up"
        )
        val draft = ReviewComment(
            filePath = "README.md", startLine = 10, endLine = 10,
            selectedText = "c", commentText = "draft", authorId = "vinay.yerra"
        )

        session.addComment(noReply)
        session.addComment(withReply)
        session.addComment(draft)

        val result = session.getDraftReplyComments()
        assertEquals(1, result.size)
        assertEquals(withReply, result[0])
    }

    @Test
    fun `getDraftReplyComments returns empty when no replies`() {
        val session = MarkdownReviewSession("README.md")
        val comment = ReviewComment(
            filePath = "README.md", startLine = 1, endLine = 1,
            selectedText = "a", commentText = "c", authorId = "vinay.yerra"
        )
        session.addComment(comment)
        assertTrue(session.getDraftReplyComments().isEmpty())
    }

    // --- Session properties ---

    @Test
    fun `session properties and lifecycle`() {
        val session = MarkdownReviewSession("docs/README.md")

        assertNotNull(session.id)
        assertEquals(ReviewSessionStatus.ACTIVE, session.status)
        assertNotNull(session.createdAt)
        assertNull(session.publishedAt)
        assertNull(session.reviewFilePath)
        assertEquals("docs/README.md", session.sourceFilePath)

        val publishTime = Instant.now()
        session.status = ReviewSessionStatus.PUBLISHED
        session.publishedAt = publishTime
        session.reviewFilePath = ".review/docs--README.review.json"

        assertEquals(ReviewSessionStatus.PUBLISHED, session.status)
        assertEquals(publishTime, session.publishedAt)
        assertEquals(".review/docs--README.review.json", session.reviewFilePath)
    }

    @Test
    fun `git diff session properties`() {
        val session = GitDiffReviewSession(
            baseBranch = "main",
            compareBranch = "feature/auth",
            baseCommit = "abc123",
            compareCommit = "def456",
            changedFiles = listOf("src/Main.kt", "src/Auth.kt")
        )

        assertEquals("main", session.baseBranch)
        assertEquals("feature/auth", session.compareBranch)
        assertEquals("abc123", session.baseCommit)
        assertEquals("def456", session.compareCommit)
        assertEquals(listOf("src/Main.kt", "src/Auth.kt"), session.changedFiles)
    }

    // --- Display names ---

    @Test
    fun `markdown display name shows filename only`() {
        val session = MarkdownReviewSession("docs/uscorer/ARCHITECTURE_OVERVIEW.md")
        assertEquals("Markdown: ARCHITECTURE_OVERVIEW.md", session.getDisplayName())
    }

    @Test
    fun `diff display name shows branches`() {
        val session = GitDiffReviewSession(baseBranch = "main", compareBranch = "feature-auth")
        assertEquals("Diff: main -> feature-auth", session.getDisplayName())
    }

    // --- ReviewFile DTO field access ---

    @Test
    fun `review file fields accessible`() {
        val file = ReviewFile(
            sessionId = "test-id",
            type = "MARKDOWN",
            metadata = ReviewMetadata(author = "test", publishedAt = "now"),
            comments = emptyList()
        )

        assertEquals("test-id", file.sessionId)
        assertEquals("MARKDOWN", file.type)
        assertEquals("test", file.metadata.author)
        assertTrue(file.comments.isEmpty())
    }
}
