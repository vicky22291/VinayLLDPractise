package com.uber.jetbrains.reviewplugin.services

import com.uber.jetbrains.reviewplugin.model.*
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

object ReviewFileManager {

    /** Tracks timestamps of internal writes so the file watcher can ignore them. */
    private val internalWriteTimestamps = mutableMapOf<String, Long>()
    private const val INTERNAL_WRITE_WINDOW_MS = 2000L

    fun isInternalWrite(filePath: String): Boolean {
        val writeTime = internalWriteTimestamps[filePath] ?: return false
        return System.currentTimeMillis() - writeTime < INTERNAL_WRITE_WINDOW_MS
    }

    private fun recordInternalWrite(filePath: Path) {
        internalWriteTimestamps[filePath.toAbsolutePath().toString()] = System.currentTimeMillis()
    }

    fun publish(session: ReviewSession, outputDir: Path): Path {
        val reviewFile = buildReviewFile(session)
        Files.createDirectories(outputDir)
        val filePath = outputDir.resolve(session.getReviewFileName())
        Files.writeString(filePath, reviewFile.toJson())
        recordInternalWrite(filePath)
        return filePath
    }

    fun load(reviewFilePath: Path): ReviewFile {
        val json = Files.readString(reviewFilePath)
        return ReviewFile.fromJson(json)
    }

    fun appendReply(reviewFilePath: Path, commentIndex: Int, reply: Reply) {
        val reviewFile = load(reviewFilePath)
        val commentIdx = reviewFile.comments.indexOfFirst { it.index == commentIndex }
        require(commentIdx >= 0) { "Comment with index $commentIndex not found" }
        val comment = reviewFile.comments[commentIdx]
        val updatedComment = comment.copy(
            replies = comment.replies + reply,
            status = "pending"
        )
        val updatedComments = reviewFile.comments.toMutableList()
        updatedComments[commentIdx] = updatedComment
        val updatedFile = reviewFile.copy(comments = updatedComments)
        Files.writeString(reviewFilePath, updatedFile.toJson())
        recordInternalWrite(reviewFilePath)
    }

    fun generateCliCommand(reviewFilePath: String): String {
        return """claude "/review-respond $reviewFilePath""""
    }

    fun publishReplies(session: ReviewSession, reviewFilePath: Path): Path {
        val reviewFile = load(reviewFilePath)
        val draftReplyComments = session.getDraftReplyComments()
        var updatedComments = reviewFile.comments.toMutableList()

        for (comment in draftReplyComments) {
            val commentIndex = session.comments.indexOf(comment) + 1
            val fileCommentIdx = updatedComments.indexOfFirst { it.index == commentIndex }
            if (fileCommentIdx < 0) continue
            val fileComment = updatedComments[fileCommentIdx]
            val reply = Reply(
                author = System.getProperty("user.name") ?: "unknown",
                timestamp = Instant.now().toString(),
                text = comment.draftReply!!
            )
            updatedComments[fileCommentIdx] = fileComment.copy(
                replies = fileComment.replies + reply,
                status = "pending"
            )
        }

        val updatedFile = reviewFile.copy(comments = updatedComments)
        Files.writeString(reviewFilePath, updatedFile.toJson())
        recordInternalWrite(reviewFilePath)
        return reviewFilePath
    }

    private fun buildReviewFile(session: ReviewSession): ReviewFile {
        val metadata = when (session) {
            is MarkdownReviewSession -> ReviewMetadata(
                author = System.getProperty("user.name"),
                publishedAt = Instant.now().toString(),
                sourceFile = session.sourceFilePath
            )
            is GitDiffReviewSession -> ReviewMetadata(
                author = System.getProperty("user.name"),
                publishedAt = Instant.now().toString(),
                baseBranch = session.baseBranch,
                compareBranch = session.compareBranch,
                baseCommit = session.baseCommit,
                compareCommit = session.compareCommit,
                filesChanged = session.changedFiles
            )
        }

        val type = when (session) {
            is MarkdownReviewSession -> "MARKDOWN"
            is GitDiffReviewSession -> "GIT_DIFF"
        }

        val comments = session.comments.mapIndexed { i, comment ->
            ReviewFileComment(
                index = i + 1,
                filePath = comment.filePath,
                startLine = comment.startLine,
                endLine = comment.endLine,
                selectedText = comment.selectedText,
                userComment = comment.commentText,
                status = "pending",
                changeType = comment.changeType?.name?.lowercase(),
                replies = emptyList()
            )
        }

        return ReviewFile(
            sessionId = session.id.toString(),
            type = type,
            metadata = metadata,
            comments = comments
        )
    }
}
