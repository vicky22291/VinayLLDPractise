package com.uber.jetbrains.reviewplugin.listeners

import com.uber.jetbrains.reviewplugin.model.MarkdownReviewSession
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import org.junit.Test

class ReviewModeListenerTest {

    @Test
    fun `default methods are no-ops and do not throw`() {
        val listener = object : ReviewModeListener {}
        val session = MarkdownReviewSession(sourceFilePath = "test.md")

        listener.onReviewModeEntered(session)
        listener.onReviewModeExited(session)
        listener.onCommentsChanged(session)
        listener.onResponsesLoaded(session)
    }

    @Test
    fun `listener can override individual methods`() {
        var entered = false
        val listener = object : ReviewModeListener {
            override fun onReviewModeEntered(session: ReviewSession) {
                entered = true
            }
        }
        val session = MarkdownReviewSession(sourceFilePath = "test.md")

        listener.onReviewModeEntered(session)
        assert(entered)

        // Non-overridden methods remain no-ops
        listener.onReviewModeExited(session)
        listener.onCommentsChanged(session)
        listener.onResponsesLoaded(session)
    }
}
