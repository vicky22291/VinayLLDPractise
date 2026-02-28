package com.uber.jetbrains.reviewplugin.model

import kotlinx.serialization.Serializable

@Serializable
data class Reply(
    val author: String,
    val timestamp: String,
    val text: String
)
