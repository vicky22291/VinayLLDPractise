package com.uber.jetbrains.reviewplugin.actions

import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent

/**
 * Ensures only one comment popup (add or edit) is shown at a time.
 */
object CommentPopupTracker {
    private var activePopup: JBPopup? = null

    fun dismissActive() {
        activePopup?.cancel()
        activePopup = null
    }

    fun track(popup: JBPopup) {
        activePopup = popup
        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                if (activePopup === popup) activePopup = null
            }
        })
    }
}
