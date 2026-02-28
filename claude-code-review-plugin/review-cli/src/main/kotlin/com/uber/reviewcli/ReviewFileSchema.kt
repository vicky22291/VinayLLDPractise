package com.uber.reviewcli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val reviewJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Serializable
data class ReviewFile(
    val sessionId: String,
    val type: String,
    val metadata: ReviewMetadata,
    val comments: List<ReviewFileComment>
) {
    fun toJson(): String = reviewJson.encodeToString(serializer(), this)

    companion object {
        fun fromJson(jsonString: String): ReviewFile =
            reviewJson.decodeFromString(serializer(), jsonString)
    }
}

@Serializable
data class ReviewFileComment(
    val index: Int,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val selectedText: String,
    val userComment: String,
    val status: String,
    val claudeResponse: String? = null,
    val changeType: String? = null,
    val replies: List<Reply> = emptyList()
)

@Serializable
data class ReviewMetadata(
    val author: String,
    val publishedAt: String,
    val sourceFile: String? = null,
    val baseBranch: String? = null,
    val compareBranch: String? = null,
    val baseCommit: String? = null,
    val compareCommit: String? = null,
    val filesChanged: List<String>? = null
)

@Serializable
data class Reply(
    val author: String,
    val timestamp: String,
    val text: String
)
