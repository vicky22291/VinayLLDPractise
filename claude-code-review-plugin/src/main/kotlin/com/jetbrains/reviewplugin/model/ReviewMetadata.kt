package com.uber.jetbrains.reviewplugin.model

import kotlinx.serialization.Serializable

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
