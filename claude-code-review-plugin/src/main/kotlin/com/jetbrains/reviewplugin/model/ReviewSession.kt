package com.uber.jetbrains.reviewplugin.model

import java.time.Instant
import java.util.UUID

sealed class ReviewSession(
    val id: UUID = UUID.randomUUID(),
    var status: ReviewSessionStatus = ReviewSessionStatus.ACTIVE,
    val comments: MutableList<ReviewComment> = mutableListOf(),
    val createdAt: Instant = Instant.now(),
    var publishedAt: Instant? = null,
    var reviewFilePath: String? = null
) {
    abstract fun getReviewFileName(): String
    abstract fun getDisplayName(): String

    fun addComment(comment: ReviewComment) {
        comments.add(comment)
    }

    fun removeComment(commentId: UUID) {
        comments.removeAll { it.id == commentId }
    }

    fun getComment(commentId: UUID): ReviewComment? {
        return comments.find { it.id == commentId }
    }

    fun getDraftComments(): List<ReviewComment> {
        return comments.filter { it.status == CommentStatus.DRAFT }
    }

    fun getPendingComments(): List<ReviewComment> {
        return comments.filter { it.status == CommentStatus.PENDING }
    }

    fun getDraftReplyComments(): List<ReviewComment> {
        return comments.filter { it.draftReply != null }
    }
}

class MarkdownReviewSession(
    val sourceFilePath: String,
    id: UUID = UUID.randomUUID(),
    status: ReviewSessionStatus = ReviewSessionStatus.ACTIVE,
    comments: MutableList<ReviewComment> = mutableListOf(),
    createdAt: Instant = Instant.now(),
    publishedAt: Instant? = null,
    reviewFilePath: String? = null
) : ReviewSession(id, status, comments, createdAt, publishedAt, reviewFilePath) {
    override fun getReviewFileName(): String {
        val stem = sourceFilePath.substringBeforeLast(".")
        return stem.replace("/", "--") + ".review.json"
    }

    override fun getDisplayName(): String {
        val fileName = sourceFilePath.substringAfterLast("/")
        return "Markdown: $fileName"
    }
}

class GitDiffReviewSession(
    val baseBranch: String,
    val compareBranch: String,
    val baseCommit: String? = null,
    val compareCommit: String? = null,
    val changedFiles: List<String> = emptyList(),
    id: UUID = UUID.randomUUID(),
    status: ReviewSessionStatus = ReviewSessionStatus.ACTIVE,
    comments: MutableList<ReviewComment> = mutableListOf(),
    createdAt: Instant = Instant.now(),
    publishedAt: Instant? = null,
    reviewFilePath: String? = null
) : ReviewSession(id, status, comments, createdAt, publishedAt, reviewFilePath) {
    override fun getReviewFileName(): String {
        val base = baseBranch.replace("/", "-")
        val compare = compareBranch.replace("/", "-")
        return "diff-${base}--${compare}.review.json"
    }

    override fun getDisplayName(): String {
        return "Diff: $baseBranch -> $compareBranch"
    }
}
