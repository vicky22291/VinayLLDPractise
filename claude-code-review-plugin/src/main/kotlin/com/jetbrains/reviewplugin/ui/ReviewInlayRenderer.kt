package com.uber.jetbrains.reviewplugin.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

/**
 * Renders an inline review comment annotation after the end of a line.
 * Thin IntelliJ integration layer — all logic is in [InlayAnnotationProvider].
 */
class ReviewInlayRenderer(
    val text: String,
    val color: Color
) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        val font = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.ITALIC)
        val metrics = editor.contentComponent.getFontMetrics(font)
        return metrics.stringWidth("  $text  ")
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor
        val font = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.ITALIC)
        g.font = font
        g.color = color
        val metrics = g.fontMetrics
        val y = targetRegion.y + targetRegion.height - metrics.descent
        g.drawString("  $text  ", targetRegion.x, y)
    }
}
