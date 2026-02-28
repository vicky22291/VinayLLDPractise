package com.uber.jetbrains.reviewplugin.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ReviewFile(
    val sessionId: String,
    val type: String,
    val metadata: ReviewMetadata,
    val comments: List<ReviewFileComment>
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        fun fromJson(jsonString: String): ReviewFile = json.decodeFromString(serializer(), jsonString)
    }
}
