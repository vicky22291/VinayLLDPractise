package com.uber.jetbrains.reviewplugin.model

import kotlinx.serialization.Serializable

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
