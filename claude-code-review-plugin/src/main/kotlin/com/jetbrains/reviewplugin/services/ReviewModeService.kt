package com.uber.jetbrains.reviewplugin.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.uber.jetbrains.reviewplugin.listeners.ReviewModeListener
import com.uber.jetbrains.reviewplugin.model.*

class ReviewModeService {

    companion object {
        /** Stored on LightVirtualFile to carry the real review-relative file path through diff editors. */
        val REVIEW_FILE_PATH_KEY: Key<String> = Key.create("ReviewPlugin.FilePath")
    }

    private val storageManager: StorageManager

    constructor(project: Project) {
        storageManager = project.getService(StorageManager::class.java)
    }

    internal constructor(storageManager: StorageManager) {
        this.storageManager = storageManager
    }

    private val activeReviews: MutableMap<String, ReviewSession> = mutableMapOf()
    private val listeners: MutableList<ReviewModeListener> = mutableListOf()

    fun enterMarkdownReview(sourceFilePath: String): MarkdownReviewSession {
        val key = sourceFilePath
        val existing = activeReviews[key]
        if (existing != null) {
            if (existing.status == ReviewSessionStatus.SUSPENDED) {
                existing.status = ReviewSessionStatus.ACTIVE
                notifyReviewModeEntered(existing)
            }
            return existing as MarkdownReviewSession
        }
        val session = MarkdownReviewSession(sourceFilePath = sourceFilePath)
        activeReviews[key] = session
        notifyReviewModeEntered(session)
        return session
    }

    fun enterDiffReview(
        baseBranch: String,
        compareBranch: String,
        changedFiles: List<String> = emptyList()
    ): GitDiffReviewSession {
        val session = GitDiffReviewSession(
            baseBranch = baseBranch,
            compareBranch = compareBranch,
            changedFiles = changedFiles
        )
        val key = session.getReviewFileName()
        val existing = activeReviews[key]
        if (existing != null) {
            if (existing.status == ReviewSessionStatus.SUSPENDED) {
                existing.status = ReviewSessionStatus.ACTIVE
                notifyReviewModeEntered(existing)
            }
            return existing as GitDiffReviewSession
        }
        activeReviews[key] = session
        notifyReviewModeEntered(session)
        return session
    }

    fun exitReview(session: ReviewSession, keepDrafts: Boolean) {
        if (keepDrafts) {
            session.status = ReviewSessionStatus.SUSPENDED
            storageManager.saveDrafts(session)
        } else {
            storageManager.deleteDrafts(session.id)
        }
        removeSession(session)
        notifyReviewModeExited(session)
    }

    fun completeReview(session: ReviewSession) {
        session.status = ReviewSessionStatus.COMPLETED
        storageManager.archiveReviewFile(session)
        storageManager.deleteDrafts(session.id)
        removeSession(session)
        notifyReviewModeExited(session)
    }

    fun rejectReview(session: ReviewSession) {
        session.status = ReviewSessionStatus.REJECTED
        storageManager.archiveReviewFile(session)
        storageManager.deleteDrafts(session.id)
        removeSession(session)
        notifyReviewModeExited(session)
    }

    /**
     * Resolves a raw file path from a diff editor (which may have a "base/" prefix
     * for the left side) to the actual changed file path in an active diff session.
     * Returns null if no active diff session contains a matching file.
     */
    fun resolveDiffFilePath(rawPath: String): String? {
        for (session in activeReviews.values) {
            if (session !is GitDiffReviewSession) continue
            if (rawPath in session.changedFiles) return rawPath
            if (rawPath.startsWith("base/")) {
                val stripped = rawPath.removePrefix("base/")
                if (stripped in session.changedFiles) return stripped
            }
        }
        return null
    }

    fun isInReviewMode(filePath: String): Boolean {
        val resolved = resolveDiffFilePath(filePath)
        return activeReviews.values.any { session ->
            when (session) {
                is MarkdownReviewSession -> session.sourceFilePath == filePath
                is GitDiffReviewSession -> (resolved ?: filePath) in session.changedFiles
            }
        }
    }

    fun getActiveSession(filePath: String): ReviewSession? {
        val resolved = resolveDiffFilePath(filePath)
        return activeReviews.values.find { session ->
            when (session) {
                is MarkdownReviewSession -> session.sourceFilePath == filePath
                is GitDiffReviewSession -> (resolved ?: filePath) in session.changedFiles
            }
        }
    }

    fun getAllActiveSessions(): List<ReviewSession> {
        return activeReviews.values.toList()
    }

    fun restoreSuspendedSession(session: ReviewSession) {
        val key = getSessionKey(session)
        session.status = ReviewSessionStatus.ACTIVE
        activeReviews[key] = session
        notifyReviewModeEntered(session)
    }

    fun addListener(listener: ReviewModeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ReviewModeListener) {
        listeners.remove(listener)
    }

    fun notifyCommentsChanged(session: ReviewSession) {
        listeners.forEach { it.onCommentsChanged(session) }
    }

    fun notifyResponsesLoaded(session: ReviewSession) {
        listeners.forEach { it.onResponsesLoaded(session) }
    }

    private fun notifyReviewModeEntered(session: ReviewSession) {
        listeners.forEach { it.onReviewModeEntered(session) }
    }

    private fun notifyReviewModeExited(session: ReviewSession) {
        listeners.forEach { it.onReviewModeExited(session) }
    }

    private fun removeSession(session: ReviewSession) {
        val key = getSessionKey(session)
        activeReviews.remove(key)
    }

    private fun getSessionKey(session: ReviewSession): String {
        return when (session) {
            is MarkdownReviewSession -> session.sourceFilePath
            is GitDiffReviewSession -> session.getReviewFileName()
        }
    }
}
