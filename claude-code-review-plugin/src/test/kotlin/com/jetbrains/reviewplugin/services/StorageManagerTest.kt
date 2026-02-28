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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorageManagerTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("review-test")
        storageManager = StorageManager(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- Save and Load Round-Trip ---

    @Test
    fun `save and load markdown session round trip`() {
        val sessionId = UUID.randomUUID()
        val createdAt = Instant.parse("2026-02-12T15:28:12Z")
        val session = MarkdownReviewSession(
            sourceFilePath = "docs/uscorer/ARCHITECTURE_OVERVIEW.md",
            id = sessionId,
            createdAt = createdAt
        )
        session.addComment(makeComment("comment-1", "file.md", 1, 5, "text1", "How?", CommentStatus.DRAFT))
        session.addComment(makeComment("comment-2", "file.md", 10, 15, "text2", "Why?", CommentStatus.DRAFT))
        session.addComment(makeComment("comment-3", "file.md", 20, 25, "text3", "What?", CommentStatus.DRAFT))

        storageManager.saveDrafts(session)
        val loaded = storageManager.loadDrafts()

        assertEquals(1, loaded.size)
        val restored = loaded[0] as MarkdownReviewSession
        assertEquals(sessionId, restored.id)
        assertEquals(ReviewSessionStatus.SUSPENDED, restored.status)
        assertEquals(createdAt, restored.createdAt)
        assertEquals("docs/uscorer/ARCHITECTURE_OVERVIEW.md", restored.sourceFilePath)
        assertEquals(3, restored.comments.size)
        assertEquals("How?", restored.comments[0].commentText)
        assertEquals("Why?", restored.comments[1].commentText)
        assertEquals("What?", restored.comments[2].commentText)
    }

    @Test
    fun `save and load git diff session round trip`() {
        val sessionId = UUID.randomUUID()
        val createdAt = Instant.parse("2026-02-12T16:00:00Z")
        val session = GitDiffReviewSession(
            baseBranch = "main",
            compareBranch = "feature/user-auth",
            baseCommit = "abc123",
            compareCommit = "def456",
            changedFiles = listOf("src/Main.kt", "src/Auth.kt"),
            id = sessionId,
            createdAt = createdAt
        )
        session.addComment(makeComment("c1", "src/Main.kt", 10, 15, "code", "Check this", CommentStatus.DRAFT, ChangeType.MODIFIED))

        storageManager.saveDrafts(session)
        val loaded = storageManager.loadDrafts()

        assertEquals(1, loaded.size)
        val restored = loaded[0] as GitDiffReviewSession
        assertEquals(sessionId, restored.id)
        assertEquals(ReviewSessionStatus.SUSPENDED, restored.status)
        assertEquals(createdAt, restored.createdAt)
        assertEquals("main", restored.baseBranch)
        assertEquals("feature/user-auth", restored.compareBranch)
        assertEquals("abc123", restored.baseCommit)
        assertEquals("def456", restored.compareCommit)
        assertEquals(listOf("src/Main.kt", "src/Auth.kt"), restored.changedFiles)
        assertEquals(1, restored.comments.size)
        assertEquals(ChangeType.MODIFIED, restored.comments[0].changeType)
    }

    @Test
    fun `save and load session with all comment fields populated`() {
        val session = MarkdownReviewSession(sourceFilePath = "README.md")
        val resolvedAt = Instant.parse("2026-02-12T17:00:00Z")
        val comment = ReviewComment(
            filePath = "README.md",
            startLine = 42,
            endLine = 45,
            selectedText = "selected text",
            commentText = "my comment",
            authorId = "vinay.yerra",
            status = CommentStatus.RESOLVED,
            claudeResponse = "Claude's response here",
            resolvedAt = resolvedAt,
            changeType = ChangeType.ADDED
        )
        session.addComment(comment)

        storageManager.saveDrafts(session)
        val restored = storageManager.loadDrafts()[0]
        val restoredComment = restored.comments[0]

        assertEquals(comment.id, restoredComment.id)
        assertEquals("README.md", restoredComment.filePath)
        assertEquals(42, restoredComment.startLine)
        assertEquals(45, restoredComment.endLine)
        assertEquals("selected text", restoredComment.selectedText)
        assertEquals("my comment", restoredComment.commentText)
        assertEquals("vinay.yerra", restoredComment.authorId)
        assertEquals(CommentStatus.RESOLVED, restoredComment.status)
        assertEquals("Claude's response here", restoredComment.claudeResponse)
        assertEquals(resolvedAt, restoredComment.resolvedAt)
        assertEquals(ChangeType.ADDED, restoredComment.changeType)
    }

    @Test
    fun `save and load session with null optional comment fields`() {
        val session = MarkdownReviewSession(sourceFilePath = "README.md")
        val comment = ReviewComment(
            filePath = "README.md",
            startLine = 1,
            endLine = 1,
            selectedText = "text",
            commentText = "comment",
            authorId = "vinay.yerra"
        )
        session.addComment(comment)

        storageManager.saveDrafts(session)
        val restoredComment = storageManager.loadDrafts()[0].comments[0]

        assertNull(restoredComment.claudeResponse)
        assertNull(restoredComment.resolvedAt)
        assertNull(restoredComment.changeType)
        assertNull(restoredComment.draftReply)
    }

    @Test
    fun `save and load session with draftReply populated`() {
        val session = MarkdownReviewSession(sourceFilePath = "README.md")
        val comment = ReviewComment(
            filePath = "README.md",
            startLine = 42,
            endLine = 45,
            selectedText = "selected text",
            commentText = "my comment",
            authorId = "vinay.yerra",
            status = CommentStatus.RESOLVED,
            claudeResponse = "Claude's response",
            draftReply = "my follow-up question"
        )
        session.addComment(comment)

        storageManager.saveDrafts(session)
        val restoredComment = storageManager.loadDrafts()[0].comments[0]

        assertEquals("my follow-up question", restoredComment.draftReply)
        assertEquals("Claude's response", restoredComment.claudeResponse)
        assertEquals(CommentStatus.RESOLVED, restoredComment.status)
    }

    @Test
    fun `git diff session with null optional fields round trips`() {
        val session = GitDiffReviewSession(
            baseBranch = "main",
            compareBranch = "feature-x"
        )

        storageManager.saveDrafts(session)
        val restored = storageManager.loadDrafts()[0] as GitDiffReviewSession

        assertNull(restored.baseCommit)
        assertNull(restored.compareCommit)
        assertEquals(emptyList<String>(), restored.changedFiles)
    }

    @Test
    fun `save overwrites existing draft for same session`() {
        val session = MarkdownReviewSession(sourceFilePath = "README.md")
        session.addComment(makeComment("c1", "README.md", 1, 1, "a", "first", CommentStatus.DRAFT))
        storageManager.saveDrafts(session)

        session.addComment(makeComment("c2", "README.md", 5, 5, "b", "second", CommentStatus.DRAFT))
        storageManager.saveDrafts(session)

        val loaded = storageManager.loadDrafts()
        assertEquals(1, loaded.size)
        assertEquals(2, loaded[0].comments.size)
    }

    @Test
    fun `multiple sessions save and load`() {
        val session1 = MarkdownReviewSession(sourceFilePath = "file1.md")
        val session2 = MarkdownReviewSession(sourceFilePath = "file2.md")
        storageManager.saveDrafts(session1)
        storageManager.saveDrafts(session2)

        val loaded = storageManager.loadDrafts()
        assertEquals(2, loaded.size)
        val paths = loaded.map { (it as MarkdownReviewSession).sourceFilePath }.toSet()
        assertEquals(setOf("file1.md", "file2.md"), paths)
    }

    // --- Delete Drafts ---

    @Test
    fun `delete drafts removes session file`() {
        val session = MarkdownReviewSession(sourceFilePath = "README.md")
        storageManager.saveDrafts(session)
        assertEquals(1, storageManager.loadDrafts().size)

        storageManager.deleteDrafts(session.id)
        assertEquals(0, storageManager.loadDrafts().size)
    }

    @Test
    fun `delete non-existent drafts does not throw`() {
        storageManager.ensureReviewDirectory()
        storageManager.deleteDrafts(UUID.randomUUID())
        // No exception thrown
    }

    // --- Load Drafts Edge Cases ---

    @Test
    fun `load drafts returns empty when directory does not exist`() {
        assertEquals(emptyList(), storageManager.loadDrafts())
    }

    @Test
    fun `load drafts returns empty when directory is empty`() {
        storageManager.ensureReviewDirectory()
        assertEquals(emptyList(), storageManager.loadDrafts())
    }

    @Test
    fun `load drafts ignores non-session files`() {
        storageManager.ensureReviewDirectory()
        Files.writeString(storageManager.draftsDir.resolve("notes.txt"), "not a session")
        Files.writeString(storageManager.draftsDir.resolve("other.json"), "{}")
        assertEquals(emptyList(), storageManager.loadDrafts())
    }

    @Test
    fun `load drafts throws for unknown session type`() {
        storageManager.ensureReviewDirectory()
        val unknownJson = """{
            "sessionId": "550e8400-e29b-41d4-a716-446655440000",
            "type": "UNKNOWN",
            "status": "ACTIVE",
            "createdAt": "2026-02-12T15:28:12Z",
            "comments": []
        }"""
        Files.writeString(
            storageManager.draftsDir.resolve("session-550e8400-e29b-41d4-a716-446655440000.json"),
            unknownJson
        )
        assertFailsWith<IllegalArgumentException> {
            storageManager.loadDrafts()
        }
    }

    // --- Archive Review File ---

    @Test
    fun `archive review file moves to archives with suffix`() {
        storageManager.ensureReviewDirectory()
        val session = MarkdownReviewSession(sourceFilePath = "docs/uscorer/ARCHITECTURE_OVERVIEW.md")
        val reviewFileName = session.getReviewFileName()
        val reviewFilePath = storageManager.reviewDir.resolve(reviewFileName)
        Files.writeString(reviewFilePath, """{"test": true}""")
        session.reviewFilePath = ".review/$reviewFileName"

        storageManager.archiveReviewFile(session)

        assertFalse(Files.exists(reviewFilePath))
        val archiveDir = storageManager.reviewDir.resolve("archives")
        assertTrue(Files.exists(archiveDir))
        val archived = Files.list(archiveDir).use { it.toList() }
        assertEquals(1, archived.size)
        val archivedName = archived[0].fileName.toString()
        assertTrue(archivedName.startsWith("docs--uscorer--ARCHITECTURE_OVERVIEW-"))
        assertTrue(archivedName.endsWith(".review.json"))
        assertEquals(5, archivedName.substringAfter("ARCHITECTURE_OVERVIEW-").substringBefore(".review.json").length)
    }

    @Test
    fun `archive review file with absolute path`() {
        storageManager.ensureReviewDirectory()
        val session = MarkdownReviewSession(sourceFilePath = "README.md")
        val reviewFilePath = storageManager.reviewDir.resolve("README.review.json")
        Files.writeString(reviewFilePath, """{"test": true}""")
        session.reviewFilePath = reviewFilePath.toString()

        storageManager.archiveReviewFile(session)

        assertFalse(Files.exists(reviewFilePath))
        val archived = Files.list(storageManager.reviewDir.resolve("archives")).use { it.toList() }
        assertEquals(1, archived.size)
    }

    @Test
    fun `archive review file with null path does nothing`() {
        val session = MarkdownReviewSession(sourceFilePath = "README.md")
        // reviewFilePath is null by default
        storageManager.archiveReviewFile(session)
        // No exception, no directories created
        assertFalse(Files.exists(storageManager.reviewDir.resolve("archives")))
    }

    @Test
    fun `archive review file when source does not exist does nothing`() {
        storageManager.ensureReviewDirectory()
        val session = MarkdownReviewSession(sourceFilePath = "README.md")
        session.reviewFilePath = ".review/README.review.json"

        storageManager.archiveReviewFile(session)
        // No exception, archives dir not created since we return early
    }

    // --- Directory Management ---

    @Test
    fun `ensure review directory creates structure`() {
        assertFalse(Files.exists(storageManager.reviewDir))
        assertFalse(Files.exists(storageManager.draftsDir))

        storageManager.ensureReviewDirectory()

        assertTrue(Files.isDirectory(storageManager.reviewDir))
        assertTrue(Files.isDirectory(storageManager.draftsDir))
    }

    @Test
    fun `ensure review directory is idempotent`() {
        storageManager.ensureReviewDirectory()
        storageManager.ensureReviewDirectory()

        assertTrue(Files.isDirectory(storageManager.reviewDir))
        assertTrue(Files.isDirectory(storageManager.draftsDir))
    }

    @Test
    fun `ensure archive directory creates archives subdirectory`() {
        storageManager.ensureArchiveDirectory()

        assertTrue(Files.isDirectory(storageManager.reviewDir.resolve("archives")))
    }

    @Test
    fun `get review directory returns correct path`() {
        assertEquals(tempDir.resolve(".review"), storageManager.getReviewDirectory())
    }

    // --- .gitignore Management ---

    @Test
    fun `gitignore created when not present`() {
        storageManager.ensureReviewDirectory()

        val gitignore = tempDir.resolve(".gitignore")
        assertTrue(Files.exists(gitignore))
        assertEquals(".review/\n", Files.readString(gitignore))
    }

    @Test
    fun `gitignore appends review entry when not present`() {
        val gitignore = tempDir.resolve(".gitignore")
        Files.writeString(gitignore, "node_modules/\n*.class\n")

        storageManager.ensureReviewDirectory()

        val content = Files.readString(gitignore)
        assertTrue(content.contains("node_modules/"))
        assertTrue(content.contains("*.class"))
        assertTrue(content.contains(".review/"))
    }

    @Test
    fun `gitignore not modified when already contains review entry`() {
        val gitignore = tempDir.resolve(".gitignore")
        val originalContent = "node_modules/\n.review/\n*.class\n"
        Files.writeString(gitignore, originalContent)

        storageManager.ensureReviewDirectory()

        assertEquals(originalContent, Files.readString(gitignore))
    }

    @Test
    fun `saveDrafts leaves no temp files`() {
        val session = MarkdownReviewSession(sourceFilePath = "README.md")
        storageManager.saveDrafts(session)

        val files = Files.list(storageManager.draftsDir).use { it.toList() }
        assertTrue(files.all { it.fileName.toString().endsWith(".json") })
        assertFalse(files.any { it.fileName.toString().endsWith(".tmp") })
    }

    @Test
    fun `loaded sessions preserve comment ids across round trip`() {
        val commentId = UUID.randomUUID()
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        session.addComment(
            ReviewComment(
                id = commentId,
                filePath = "test.md",
                startLine = 1,
                endLine = 1,
                selectedText = "text",
                commentText = "comment",
                authorId = "vinay.yerra"
            )
        )

        storageManager.saveDrafts(session)
        val restored = storageManager.loadDrafts()[0]

        assertEquals(commentId, restored.comments[0].id)
    }

    @Test
    fun `archive suffix contains only valid characters`() {
        storageManager.ensureReviewDirectory()
        val session = MarkdownReviewSession(sourceFilePath = "test.md")
        val reviewFilePath = storageManager.reviewDir.resolve("test.review.json")
        Files.writeString(reviewFilePath, "{}")
        session.reviewFilePath = ".review/test.review.json"

        storageManager.archiveReviewFile(session)

        val archived = Files.list(storageManager.reviewDir.resolve("archives")).use { it.toList() }
        val suffix = archived[0].fileName.toString()
            .substringAfter("test-")
            .substringBefore(".review.json")
        assertEquals(5, suffix.length)
        assertTrue(suffix.all { it in 'a'..'z' || it in '0'..'9' })
    }

    // --- Helper ---

    private fun makeComment(
        idSeed: String,
        filePath: String,
        startLine: Int,
        endLine: Int,
        selectedText: String,
        commentText: String,
        status: CommentStatus,
        changeType: ChangeType? = null
    ): ReviewComment {
        return ReviewComment(
            id = UUID.nameUUIDFromBytes(idSeed.toByteArray()),
            filePath = filePath,
            startLine = startLine,
            endLine = endLine,
            selectedText = selectedText,
            commentText = commentText,
            authorId = "vinay.yerra",
            createdAt = Instant.parse("2026-02-12T15:28:12Z"),
            status = status,
            changeType = changeType
        )
    }
}
