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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReviewModeServiceTest {

    private lateinit var tempDir: Path
    private lateinit var storageManager: StorageManager
    private lateinit var service: ReviewModeService

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("review-mode-test")
        storageManager = StorageManager(tempDir)
        service = ReviewModeService(storageManager)
    }

    @After
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    // --- enterMarkdownReview ---

    @Test
    fun `enterMarkdownReview creates new session`() {
        val session = service.enterMarkdownReview("docs/README.md")

        assertEquals("docs/README.md", session.sourceFilePath)
        assertEquals(ReviewSessionStatus.ACTIVE, session.status)
        assertTrue(service.isInReviewMode("docs/README.md"))
    }

    @Test
    fun `enterMarkdownReview returns existing active session`() {
        val first = service.enterMarkdownReview("docs/README.md")
        val second = service.enterMarkdownReview("docs/README.md")

        assertSame(first, second)
        assertEquals(1, service.getAllActiveSessions().size)
    }

    @Test
    fun `enterMarkdownReview reactivates suspended session`() {
        val session = service.enterMarkdownReview("docs/README.md")
        service.exitReview(session, keepDrafts = true)
        assertFalse(service.isInReviewMode("docs/README.md"))

        // restoreSuspendedSession now activates immediately
        service.restoreSuspendedSession(session)
        assertEquals(ReviewSessionStatus.ACTIVE, session.status)

        val reactivated = service.enterMarkdownReview("docs/README.md")
        assertSame(session, reactivated)
        assertEquals(ReviewSessionStatus.ACTIVE, reactivated.status)
    }

    @Test
    fun `enterMarkdownReview notifies listener`() {
        val tracker = TestListener()
        service.addListener(tracker)

        val session = service.enterMarkdownReview("docs/README.md")

        assertEquals(1, tracker.entered.size)
        assertSame(session, tracker.entered[0])
    }

    @Test
    fun `enterMarkdownReview on active session does not notify again`() {
        val tracker = TestListener()
        service.addListener(tracker)

        service.enterMarkdownReview("docs/README.md")
        service.enterMarkdownReview("docs/README.md")

        assertEquals(1, tracker.entered.size)
    }

    @Test
    fun `enterMarkdownReview on suspended session notifies listener`() {
        val session = service.enterMarkdownReview("docs/README.md")
        service.exitReview(session, keepDrafts = true)

        val tracker = TestListener()
        service.addListener(tracker)

        // restoreSuspendedSession now activates and notifies immediately
        service.restoreSuspendedSession(session)
        assertEquals(1, tracker.entered.size)
    }

    // --- enterDiffReview ---

    @Test
    fun `enterDiffReview creates new session`() {
        val session = service.enterDiffReview("main", "feature-auth", listOf("src/Main.kt"))

        assertEquals("main", session.baseBranch)
        assertEquals("feature-auth", session.compareBranch)
        assertEquals(listOf("src/Main.kt"), session.changedFiles)
        assertEquals(ReviewSessionStatus.ACTIVE, session.status)
    }

    @Test
    fun `enterDiffReview returns existing active session`() {
        val first = service.enterDiffReview("main", "feature-auth")
        val second = service.enterDiffReview("main", "feature-auth")

        assertSame(first, second)
        assertEquals(1, service.getAllActiveSessions().size)
    }

    @Test
    fun `enterDiffReview reactivates suspended session`() {
        val session = service.enterDiffReview("main", "feature-auth")
        service.exitReview(session, keepDrafts = true)
        service.restoreSuspendedSession(session)

        val reactivated = service.enterDiffReview("main", "feature-auth")
        assertSame(session, reactivated)
        assertEquals(ReviewSessionStatus.ACTIVE, reactivated.status)
    }

    @Test
    fun `enterDiffReview notifies listener`() {
        val tracker = TestListener()
        service.addListener(tracker)

        val session = service.enterDiffReview("main", "feature-auth")

        assertEquals(1, tracker.entered.size)
        assertSame(session, tracker.entered[0])
    }

    @Test
    fun `enterDiffReview with different branches creates different sessions`() {
        val s1 = service.enterDiffReview("main", "feature-auth")
        val s2 = service.enterDiffReview("main", "feature-payment")

        assertEquals(2, service.getAllActiveSessions().size)
        assertEquals("feature-auth", s1.compareBranch)
        assertEquals("feature-payment", s2.compareBranch)
    }

    // --- exitReview ---

    @Test
    fun `exitReview with keepDrafts suspends and saves`() {
        val session = service.enterMarkdownReview("docs/README.md")

        service.exitReview(session, keepDrafts = true)

        assertEquals(ReviewSessionStatus.SUSPENDED, session.status)
        assertFalse(service.isInReviewMode("docs/README.md"))
        assertEquals(0, service.getAllActiveSessions().size)
        // Verify draft was saved
        val loaded = storageManager.loadDrafts()
        assertEquals(1, loaded.size)
    }

    @Test
    fun `exitReview without keepDrafts removes session and deletes drafts`() {
        val session = service.enterMarkdownReview("docs/README.md")
        storageManager.saveDrafts(session) // Pre-save a draft

        service.exitReview(session, keepDrafts = false)

        assertFalse(service.isInReviewMode("docs/README.md"))
        assertEquals(0, storageManager.loadDrafts().size)
    }

    @Test
    fun `exitReview notifies listener`() {
        val session = service.enterMarkdownReview("docs/README.md")
        val tracker = TestListener()
        service.addListener(tracker)

        service.exitReview(session, keepDrafts = false)

        assertEquals(1, tracker.exited.size)
        assertSame(session, tracker.exited[0])
    }

    // --- completeReview ---

    @Test
    fun `completeReview sets status and archives`() {
        storageManager.ensureReviewDirectory()
        val session = service.enterMarkdownReview("docs/README.md")
        val reviewFilePath = storageManager.reviewDir.resolve("docs--README.review.json")
        Files.writeString(reviewFilePath, "{}")
        session.reviewFilePath = ".review/docs--README.review.json"
        storageManager.saveDrafts(session)

        service.completeReview(session)

        assertEquals(ReviewSessionStatus.COMPLETED, session.status)
        assertFalse(service.isInReviewMode("docs/README.md"))
        assertEquals(0, storageManager.loadDrafts().size)
        // Check archive was created
        val archives = Files.list(storageManager.reviewDir.resolve("archives")).use { it.toList() }
        assertEquals(1, archives.size)
    }

    @Test
    fun `completeReview notifies listener`() {
        val session = service.enterMarkdownReview("docs/README.md")
        val tracker = TestListener()
        service.addListener(tracker)

        service.completeReview(session)

        assertEquals(1, tracker.exited.size)
        assertEquals(ReviewSessionStatus.COMPLETED, tracker.exited[0].status)
    }

    // --- rejectReview ---

    @Test
    fun `rejectReview sets status and archives`() {
        storageManager.ensureReviewDirectory()
        val session = service.enterMarkdownReview("docs/README.md")
        val reviewFilePath = storageManager.reviewDir.resolve("docs--README.review.json")
        Files.writeString(reviewFilePath, "{}")
        session.reviewFilePath = ".review/docs--README.review.json"
        storageManager.saveDrafts(session)

        service.rejectReview(session)

        assertEquals(ReviewSessionStatus.REJECTED, session.status)
        assertFalse(service.isInReviewMode("docs/README.md"))
        assertEquals(0, storageManager.loadDrafts().size)
        val archives = Files.list(storageManager.reviewDir.resolve("archives")).use { it.toList() }
        assertEquals(1, archives.size)
    }

    @Test
    fun `rejectReview notifies listener`() {
        val session = service.enterMarkdownReview("docs/README.md")
        val tracker = TestListener()
        service.addListener(tracker)

        service.rejectReview(session)

        assertEquals(1, tracker.exited.size)
        assertEquals(ReviewSessionStatus.REJECTED, tracker.exited[0].status)
    }

    // --- isInReviewMode ---

    @Test
    fun `isInReviewMode returns false for unknown file`() {
        assertFalse(service.isInReviewMode("unknown.md"))
    }

    @Test
    fun `isInReviewMode matches markdown source file`() {
        service.enterMarkdownReview("docs/README.md")

        assertTrue(service.isInReviewMode("docs/README.md"))
        assertFalse(service.isInReviewMode("docs/OTHER.md"))
    }

    @Test
    fun `isInReviewMode matches git diff changed files`() {
        service.enterDiffReview("main", "feature", listOf("src/A.kt", "src/B.kt"))

        assertTrue(service.isInReviewMode("src/A.kt"))
        assertTrue(service.isInReviewMode("src/B.kt"))
        assertFalse(service.isInReviewMode("src/C.kt"))
    }

    // --- getActiveSession ---

    @Test
    fun `getActiveSession returns matching markdown session`() {
        val session = service.enterMarkdownReview("docs/README.md")

        assertSame(session, service.getActiveSession("docs/README.md"))
    }

    @Test
    fun `getActiveSession returns matching diff session by changed file`() {
        val session = service.enterDiffReview("main", "feature", listOf("src/A.kt"))

        assertSame(session, service.getActiveSession("src/A.kt"))
    }

    @Test
    fun `getActiveSession returns null for unknown file`() {
        assertNull(service.getActiveSession("unknown.md"))
    }

    // --- getAllActiveSessions ---

    @Test
    fun `getAllActiveSessions returns empty when no sessions`() {
        assertTrue(service.getAllActiveSessions().isEmpty())
    }

    @Test
    fun `getAllActiveSessions returns all sessions`() {
        service.enterMarkdownReview("file1.md")
        service.enterMarkdownReview("file2.md")
        service.enterDiffReview("main", "feature")

        assertEquals(3, service.getAllActiveSessions().size)
    }

    // --- restoreSuspendedSession ---

    @Test
    fun `restoreSuspendedSession adds session to active reviews`() {
        val session = MarkdownReviewSession(
            sourceFilePath = "docs/README.md",
            status = ReviewSessionStatus.SUSPENDED
        )
        service.restoreSuspendedSession(session)

        assertEquals(1, service.getAllActiveSessions().size)
        assertTrue(service.isInReviewMode("docs/README.md"))
    }

    @Test
    fun `restoreSuspendedSession works with git diff session`() {
        val session = GitDiffReviewSession(
            baseBranch = "main",
            compareBranch = "feature",
            changedFiles = listOf("src/A.kt"),
            status = ReviewSessionStatus.SUSPENDED
        )
        service.restoreSuspendedSession(session)

        assertEquals(1, service.getAllActiveSessions().size)
        assertTrue(service.isInReviewMode("src/A.kt"))
    }

    // --- Listener management ---

    @Test
    fun `addListener and removeListener`() {
        val tracker = TestListener()
        service.addListener(tracker)
        service.enterMarkdownReview("docs/README.md")
        assertEquals(1, tracker.entered.size)

        service.removeListener(tracker)
        service.enterMarkdownReview("other.md")
        assertEquals(1, tracker.entered.size) // no new notification
    }

    @Test
    fun `multiple listeners all receive notifications`() {
        val tracker1 = TestListener()
        val tracker2 = TestListener()
        service.addListener(tracker1)
        service.addListener(tracker2)

        service.enterMarkdownReview("docs/README.md")

        assertEquals(1, tracker1.entered.size)
        assertEquals(1, tracker2.entered.size)
    }

    // --- notifyCommentsChanged / notifyResponsesLoaded ---

    @Test
    fun `notifyCommentsChanged notifies all listeners`() {
        val tracker = TestListener()
        service.addListener(tracker)
        val session = service.enterMarkdownReview("docs/README.md")

        service.notifyCommentsChanged(session)

        assertEquals(1, tracker.commentsChanged.size)
        assertSame(session, tracker.commentsChanged[0])
    }

    @Test
    fun `notifyResponsesLoaded notifies all listeners`() {
        val tracker = TestListener()
        service.addListener(tracker)
        val session = service.enterMarkdownReview("docs/README.md")

        service.notifyResponsesLoaded(session)

        assertEquals(1, tracker.responsesLoaded.size)
        assertSame(session, tracker.responsesLoaded[0])
    }

    // --- Edge cases ---

    // --- resolveDiffFilePath ---

    @Test
    fun `resolveDiffFilePath returns direct match`() {
        service.enterDiffReview("main", "feature", listOf("src/A.kt", "src/B.kt"))

        assertEquals("src/A.kt", service.resolveDiffFilePath("src/A.kt"))
    }

    @Test
    fun `resolveDiffFilePath strips base prefix`() {
        service.enterDiffReview("main", "feature", listOf("src/A.kt"))

        assertEquals("src/A.kt", service.resolveDiffFilePath("base/src/A.kt"))
    }

    @Test
    fun `resolveDiffFilePath returns null for non-matching path`() {
        service.enterDiffReview("main", "feature", listOf("src/A.kt"))

        assertNull(service.resolveDiffFilePath("src/C.kt"))
    }

    @Test
    fun `resolveDiffFilePath returns null with no diff sessions`() {
        service.enterMarkdownReview("docs/README.md")

        assertNull(service.resolveDiffFilePath("docs/README.md"))
    }

    @Test
    fun `resolveDiffFilePath returns null when no sessions exist`() {
        assertNull(service.resolveDiffFilePath("src/A.kt"))
    }

    @Test
    fun `resolveDiffFilePath with base prefix not matching changed files`() {
        service.enterDiffReview("main", "feature", listOf("src/A.kt"))

        assertNull(service.resolveDiffFilePath("base/src/C.kt"))
    }

    @Test
    fun `isInReviewMode matches base-prefixed path for diff session`() {
        service.enterDiffReview("main", "feature", listOf("src/A.kt"))

        assertTrue(service.isInReviewMode("base/src/A.kt"))
    }

    @Test
    fun `getActiveSession resolves base-prefixed path for diff session`() {
        val session = service.enterDiffReview("main", "feature", listOf("src/A.kt"))

        assertSame(session, service.getActiveSession("base/src/A.kt"))
    }

    // --- Other edge cases ---

    @Test
    fun `exitReview for diff session`() {
        val session = service.enterDiffReview("main", "feature", listOf("src/A.kt"))

        service.exitReview(session, keepDrafts = false)

        assertFalse(service.isInReviewMode("src/A.kt"))
        assertEquals(0, service.getAllActiveSessions().size)
    }

    @Test
    fun `completeReview for session without review file path`() {
        val session = service.enterMarkdownReview("docs/README.md")
        // reviewFilePath is null — archival should be a no-op

        service.completeReview(session)

        assertEquals(ReviewSessionStatus.COMPLETED, session.status)
        assertFalse(service.isInReviewMode("docs/README.md"))
    }

    @Test
    fun `rejectReview for session without review file path`() {
        val session = service.enterMarkdownReview("docs/README.md")

        service.rejectReview(session)

        assertEquals(ReviewSessionStatus.REJECTED, session.status)
        assertFalse(service.isInReviewMode("docs/README.md"))
    }

    @Test
    fun `enterDiffReview on active diff session does not notify`() {
        val tracker = TestListener()
        service.addListener(tracker)

        service.enterDiffReview("main", "feature")
        service.enterDiffReview("main", "feature")

        assertEquals(1, tracker.entered.size)
    }

    @Test
    fun `enterDiffReview on suspended diff session notifies`() {
        val session = service.enterDiffReview("main", "feature")
        service.exitReview(session, keepDrafts = true)

        val tracker = TestListener()
        service.addListener(tracker)

        // restoreSuspendedSession now activates and notifies immediately
        service.restoreSuspendedSession(session)
        assertEquals(1, tracker.entered.size)
    }

    @Test
    fun `exitReview with keepDrafts for diff session`() {
        val session = service.enterDiffReview("main", "feature", listOf("src/A.kt"))

        service.exitReview(session, keepDrafts = true)

        assertEquals(ReviewSessionStatus.SUSPENDED, session.status)
        val loaded = storageManager.loadDrafts()
        assertEquals(1, loaded.size)
    }

    // --- Test listener helper ---

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
