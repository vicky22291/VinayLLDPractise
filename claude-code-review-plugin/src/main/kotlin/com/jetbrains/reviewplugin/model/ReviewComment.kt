package com.uber.jetbrains.reviewplugin.model

import java.time.Instant
import java.util.UUID

data class ReviewComment(
    val id: UUID = UUID.randomUUID(),
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val selectedText: String,
    val commentText: String,
    val authorId: String,
    val createdAt: Instant = Instant.now(),
    var status: CommentStatus = CommentStatus.DRAFT,
    var claudeResponse: String? = null,
    var resolvedAt: Instant? = null,
    val changeType: ChangeType? = null,
    var draftReply: String? = null
)
