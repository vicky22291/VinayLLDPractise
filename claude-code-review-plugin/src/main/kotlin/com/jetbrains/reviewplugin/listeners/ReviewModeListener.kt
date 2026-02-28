package com.uber.jetbrains.reviewplugin.listeners

import com.uber.jetbrains.reviewplugin.model.ReviewSession

interface ReviewModeListener {
    fun onReviewModeEntered(session: ReviewSession) {}
    fun onReviewModeExited(session: ReviewSession) {}
    fun onCommentsChanged(session: ReviewSession) {}
    fun onResponsesLoaded(session: ReviewSession) {}
}
