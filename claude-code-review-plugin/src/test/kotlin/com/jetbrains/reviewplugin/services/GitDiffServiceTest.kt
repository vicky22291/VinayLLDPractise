package com.uber.jetbrains.reviewplugin.services

import com.uber.jetbrains.reviewplugin.model.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitDiffServiceTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var reviewModeService: ReviewModeService

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("git-diff-test")
        storageManager = StorageManager(tempDir)
        reviewModeService = ReviewModeService(storageManager)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- Diff review session integration with ReviewModeService ---

    @Test
    fun `enterDiffReview creates session with correct branches`() {
        val session = reviewModeService.enterDiffReview("main", "feature-auth", listOf("src/Auth.kt"))
        assertEquals("main", session.baseBranch)
        assertEquals("feature-auth", session.compareBranch)
        assertEquals(listOf("src/Auth.kt"), session.changedFiles)
    }

    @Test
    fun `enterDiffReview session review file name uses sanitized branches`() {
        val session = reviewModeService.enterDiffReview("main", "feature/auth")
        assertEquals("diff-main--feature-auth.review.json", session.getReviewFileName())
    }

    @Test
    fun `enterDiffReview session display name shows branches`() {
        val session = reviewModeService.enterDiffReview("main", "feature-auth")
        assertEquals("Diff: main -> feature-auth", session.getDisplayName())
    }

    @Test
    fun `isInReviewMode returns true for changed files in diff session`() {
        reviewModeService.enterDiffReview("main", "feature", listOf("src/A.kt", "src/B.kt"))
        assertTrue(reviewModeService.isInReviewMode("src/A.kt"))
        assertTrue(reviewModeService.isInReviewMode("src/B.kt"))
        assertFalse(reviewModeService.isInReviewMode("src/C.kt"))
    }

    @Test
    fun `getActiveSession returns diff session for changed file`() {
        val session = reviewModeService.enterDiffReview("main", "feature", listOf("src/A.kt"))
        val found = reviewModeService.getActiveSession("src/A.kt")
        assertEquals(session.id, found?.id)
    }

    @Test
    fun `hasDiffReviewActive checks for GitDiffReviewSession`() {
        assertFalse(reviewModeService.getAllActiveSessions().any { it is GitDiffReviewSession })

        reviewModeService.enterDiffReview("main", "feature")
        assertTrue(reviewModeService.getAllActiveSessions().any { it is GitDiffReviewSession })
    }

    @Test
    fun `markdown and diff sessions coexist independently`() {
        reviewModeService.enterMarkdownReview("docs/README.md")
        reviewModeService.enterDiffReview("main", "feature", listOf("src/A.kt"))

        assertEquals(2, reviewModeService.getAllActiveSessions().size)
        assertTrue(reviewModeService.isInReviewMode("docs/README.md"))
        assertTrue(reviewModeService.isInReviewMode("src/A.kt"))
    }

    @Test
    fun `diff session publishes with correct metadata`() {
        val session = reviewModeService.enterDiffReview(
            "main", "feature-auth",
            listOf("src/Auth.kt", "src/Login.kt")
        )
        val comment = ReviewComment(
            filePath = "src/Auth.kt",
            startLine = 10,
            endLine = 20,
            selectedText = "auth code",
            commentText = "How does this authenticate?",
            authorId = "vinay.yerra"
        )
        session.addComment(comment)

        storageManager.ensureReviewDirectory()
        val path = ReviewFileManager.publish(session, storageManager.getReviewDirectory())
        val loaded = ReviewFileManager.load(path)

        assertEquals("GIT_DIFF", loaded.type)
        assertEquals("main", loaded.metadata.baseBranch)
        assertEquals("feature-auth", loaded.metadata.compareBranch)
        assertEquals(1, loaded.comments.size)
        assertEquals("src/Auth.kt", loaded.comments[0].filePath)
    }

    @Test
    fun `re-entering same diff session reuses existing`() {
        val session1 = reviewModeService.enterDiffReview("main", "feature")
        val session2 = reviewModeService.enterDiffReview("main", "feature")
        assertEquals(session1.id, session2.id)
        assertEquals(1, reviewModeService.getAllActiveSessions().size)
    }

    @Test
    fun `different diff branches create separate sessions`() {
        reviewModeService.enterDiffReview("main", "feature-a")
        reviewModeService.enterDiffReview("main", "feature-b")
        assertEquals(2, reviewModeService.getAllActiveSessions().size)
    }
}
