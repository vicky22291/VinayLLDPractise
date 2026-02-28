package com.uber.jetbrains.reviewplugin.services

import com.intellij.openapi.project.Project
import com.uber.jetbrains.reviewplugin.model.*
import java.time.Instant
import java.util.UUID

class CommentService {

    private val storageManager: StorageManager
    private val reviewModeService: ReviewModeService

    constructor(project: Project) {
        storageManager = project.getService(StorageManager::class.java)
        reviewModeService = project.getService(ReviewModeService::class.java)
    }

    internal constructor(storageManager: StorageManager, reviewModeService: ReviewModeService) {
        this.storageManager = storageManager
        this.reviewModeService = reviewModeService
    }

    fun addComment(session: ReviewSession, comment: ReviewComment) {
        session.addComment(comment)
        storageManager.saveDrafts(session)
        reviewModeService.notifyCommentsChanged(session)
    }

    fun updateComment(session: ReviewSession, commentId: UUID, newText: String) {
        val comment = session.getComment(commentId) ?: return
        val index = session.comments.indexOf(comment)
        session.comments[index] = comment.copy(commentText = newText)
        storageManager.saveDrafts(session)
        reviewModeService.notifyCommentsChanged(session)
    }

    fun deleteComment(session: ReviewSession, commentId: UUID) {
        session.removeComment(commentId)
        storageManager.saveDrafts(session)
        reviewModeService.notifyCommentsChanged(session)
    }

    fun getCommentsForFile(session: ReviewSession, filePath: String): List<ReviewComment> {
        return session.comments.filter { it.filePath == filePath }
    }

    fun getCommentsForLine(session: ReviewSession, filePath: String, line: Int): List<ReviewComment> {
        return session.comments.filter {
            it.filePath == filePath && line in it.startLine..it.endLine
        }
    }

    fun applyResponses(session: ReviewSession, reviewFile: ReviewFile) {
        for (fileComment in reviewFile.comments) {
            val response = fileComment.claudeResponse ?: continue
            val index = fileComment.index - 1
            if (index < 0 || index >= session.comments.size) continue
            val comment = session.comments[index]
            val updated = comment.copy(
                claudeResponse = response,
                status = CommentStatus.RESOLVED,
                resolvedAt = Instant.now()
            )
            session.comments[index] = updated
        }
        storageManager.saveDrafts(session)
        reviewModeService.notifyResponsesLoaded(session)
    }

    fun setCommentStatus(session: ReviewSession, commentId: UUID, status: CommentStatus) {
        val comment = session.getComment(commentId) ?: return
        val index = session.comments.indexOf(comment)
        session.comments[index] = comment.copy(status = status)
        storageManager.saveDrafts(session)
        reviewModeService.notifyCommentsChanged(session)
    }

    fun addReply(session: ReviewSession, commentId: UUID, replyText: String) {
        val comment = session.getComment(commentId) ?: return
        val index = session.comments.indexOf(comment)
        session.comments[index] = comment.copy(draftReply = replyText)
        storageManager.saveDrafts(session)
        reviewModeService.notifyCommentsChanged(session)
    }
}
