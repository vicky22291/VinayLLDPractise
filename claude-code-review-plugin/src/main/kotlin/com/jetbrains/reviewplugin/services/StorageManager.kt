package com.uber.jetbrains.reviewplugin.services

import com.intellij.openapi.project.Project
import com.uber.jetbrains.reviewplugin.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID

// --- Draft Serialization DTOs (internal to storage layer) ---

@Serializable
internal data class DraftCommentDto(
    val id: String,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val selectedText: String,
    val commentText: String,
    val authorId: String,
    val createdAt: String,
    val status: String,
    val claudeResponse: String? = null,
    val resolvedAt: String? = null,
    val changeType: String? = null,
    val draftReply: String? = null
)

@Serializable
internal data class DraftSessionDto(
    val sessionId: String,
    val type: String,
    val status: String,
    val createdAt: String,
    val sourceFile: String? = null,
    val baseBranch: String? = null,
    val compareBranch: String? = null,
    val baseCommit: String? = null,
    val compareCommit: String? = null,
    val changedFiles: List<String>? = null,
    val comments: List<DraftCommentDto> = emptyList()
)

class StorageManager(private val projectBasePath: Path) {

    constructor(project: Project) : this(Path.of(project.basePath!!))

    val reviewDir: Path = projectBasePath.resolve(".review")
    val draftsDir: Path = reviewDir.resolve(".drafts")

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun saveDrafts(session: ReviewSession) {
        ensureReviewDirectory()
        val dto = sessionToDto(session)
        val jsonString = json.encodeToString(DraftSessionDto.serializer(), dto)
        val path = draftsDir.resolve("session-${session.id}.json")
        val tempFile = Files.createTempFile(draftsDir, "session-", ".tmp")
        try {
            Files.writeString(tempFile, jsonString)
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            Files.deleteIfExists(tempFile)
            throw e
        }
    }

    fun loadDrafts(): List<ReviewSession> {
        if (!Files.exists(draftsDir)) return emptyList()
        return Files.list(draftsDir).use { stream ->
            stream
                .filter { path ->
                    val name = path.fileName.toString()
                    name.startsWith("session-") && name.endsWith(".json")
                }
                .map { file ->
                    val jsonString = Files.readString(file)
                    val dto = json.decodeFromString(DraftSessionDto.serializer(), jsonString)
                    dtoToSession(dto)
                }
                .toList()
        }
    }

    fun deleteDrafts(sessionId: UUID) {
        val path = draftsDir.resolve("session-$sessionId.json")
        Files.deleteIfExists(path)
    }

    fun archiveReviewFile(session: ReviewSession) {
        val reviewFilePath = session.reviewFilePath ?: return
        val sourcePath = Path.of(reviewFilePath).let { path ->
            if (path.isAbsolute) path else projectBasePath.resolve(path)
        }
        if (!Files.exists(sourcePath)) return

        ensureArchiveDirectory()
        val suffix = generateSuffix()
        val archiveName = session.getReviewFileName()
            .replace(".review.json", "-$suffix.review.json")
        val archivePath = reviewDir.resolve("archives").resolve(archiveName)
        Files.move(sourcePath, archivePath, StandardCopyOption.REPLACE_EXISTING)
    }

    fun ensureReviewDirectory() {
        Files.createDirectories(reviewDir)
        Files.createDirectories(draftsDir)
        manageGitignore()
    }

    fun ensureArchiveDirectory() {
        Files.createDirectories(reviewDir.resolve("archives"))
    }

    fun getReviewDirectory(): Path = reviewDir

    // --- Private helpers ---

    private fun manageGitignore() {
        val gitignorePath = projectBasePath.resolve(".gitignore")
        if (Files.exists(gitignorePath)) {
            val content = Files.readString(gitignorePath)
            if (!content.contains(".review/")) {
                Files.writeString(gitignorePath, content + "\n.review/\n")
            }
        } else {
            Files.writeString(gitignorePath, ".review/\n")
        }
    }

    private fun generateSuffix(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..5).map { chars.random() }.joinToString("")
    }

    private fun sessionToDto(session: ReviewSession): DraftSessionDto {
        val commentDtos = session.comments.map { comment ->
            DraftCommentDto(
                id = comment.id.toString(),
                filePath = comment.filePath,
                startLine = comment.startLine,
                endLine = comment.endLine,
                selectedText = comment.selectedText,
                commentText = comment.commentText,
                authorId = comment.authorId,
                createdAt = comment.createdAt.toString(),
                status = comment.status.name,
                claudeResponse = comment.claudeResponse,
                resolvedAt = comment.resolvedAt?.toString(),
                changeType = comment.changeType?.name,
                draftReply = comment.draftReply
            )
        }

        return when (session) {
            is MarkdownReviewSession -> DraftSessionDto(
                sessionId = session.id.toString(),
                type = "MARKDOWN",
                status = session.status.name,
                createdAt = session.createdAt.toString(),
                sourceFile = session.sourceFilePath,
                comments = commentDtos
            )
            is GitDiffReviewSession -> DraftSessionDto(
                sessionId = session.id.toString(),
                type = "GIT_DIFF",
                status = session.status.name,
                createdAt = session.createdAt.toString(),
                baseBranch = session.baseBranch,
                compareBranch = session.compareBranch,
                baseCommit = session.baseCommit,
                compareCommit = session.compareCommit,
                changedFiles = session.changedFiles,
                comments = commentDtos
            )
        }
    }

    private fun dtoToSession(dto: DraftSessionDto): ReviewSession {
        val comments = dto.comments.map { c ->
            ReviewComment(
                id = UUID.fromString(c.id),
                filePath = c.filePath,
                startLine = c.startLine,
                endLine = c.endLine,
                selectedText = c.selectedText,
                commentText = c.commentText,
                authorId = c.authorId,
                createdAt = Instant.parse(c.createdAt),
                status = CommentStatus.valueOf(c.status),
                claudeResponse = c.claudeResponse,
                resolvedAt = c.resolvedAt?.let { Instant.parse(it) },
                changeType = c.changeType?.let { ChangeType.valueOf(it) },
                draftReply = c.draftReply
            )
        }.toMutableList()

        val sessionId = UUID.fromString(dto.sessionId)
        val createdAt = Instant.parse(dto.createdAt)

        return when (dto.type) {
            "MARKDOWN" -> MarkdownReviewSession(
                sourceFilePath = dto.sourceFile!!,
                id = sessionId,
                status = ReviewSessionStatus.SUSPENDED,
                comments = comments,
                createdAt = createdAt
            )
            "GIT_DIFF" -> GitDiffReviewSession(
                baseBranch = dto.baseBranch!!,
                compareBranch = dto.compareBranch!!,
                baseCommit = dto.baseCommit,
                compareCommit = dto.compareCommit,
                changedFiles = dto.changedFiles ?: emptyList(),
                id = sessionId,
                status = ReviewSessionStatus.SUSPENDED,
                comments = comments,
                createdAt = createdAt
            )
            else -> throw IllegalArgumentException("Unknown session type: ${dto.type}")
        }
    }
}
