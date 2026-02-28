package com.uber.jetbrains.reviewplugin.ui

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Rectangle

/**
 * Renders an inline block below a commented line showing the full comment and Claude's response.
 * Thin IntelliJ integration layer — annotation data is computed by [InlayAnnotationProvider].
 */
class ReviewBlockInlayRenderer(
    private val commentText: String,
    private val response: String?,
    private val backgroundColor: Color,
    private val textColor: Color
) : EditorCustomElementRenderer {

    companion object {
        const val PADDING_X = 12
        const val PADDING_Y = 4
        const val LEFT_BORDER_WIDTH = 3
        const val LINE_SPACING = 2
        val RESPONSE_COLOR = Color(0x42, 0x42, 0x42) // Dark gray
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        return inlay.editor.scrollingModel.visibleArea.width.coerceAtLeast(400)
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        val lineHeight = editor.lineHeight + LINE_SPACING
        val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        val metrics = editor.contentComponent.getFontMetrics(font)
        val maxWidth = calcContentWidth(inlay)

        val commentLines = wrapText(commentText, metrics, maxWidth)
        val responseLines = if (!response.isNullOrBlank()) {
            wrapText("Claude: $response", metrics, maxWidth)
        } else {
            emptyList()
        }

        val totalLines = commentLines.size + responseLines.size
        return totalLines * lineHeight + PADDING_Y * 2
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor
        val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        val italicFont = editor.colorsScheme.getFont(EditorFontType.ITALIC)
        val metrics = g.getFontMetrics(font)
        val lineHeight = editor.lineHeight + LINE_SPACING
        val maxWidth = calcContentWidth(inlay)

        // Background
        g.color = backgroundColor
        g.fillRect(targetRegion.x, targetRegion.y, targetRegion.width, targetRegion.height)

        // Left border
        g.color = textColor
        g.fillRect(targetRegion.x, targetRegion.y, LEFT_BORDER_WIDTH, targetRegion.height)

        val commentLines = wrapText(commentText, metrics, maxWidth)
        val textX = targetRegion.x + LEFT_BORDER_WIDTH + PADDING_X
        var textY = targetRegion.y + PADDING_Y + metrics.ascent

        // Comment text
        g.font = font
        g.color = textColor
        for (line in commentLines) {
            g.drawString(line, textX, textY)
            textY += lineHeight
        }

        // Response text
        if (!response.isNullOrBlank()) {
            val responseLines = wrapText("Claude: $response", metrics, maxWidth)
            g.font = italicFont
            g.color = RESPONSE_COLOR
            for (line in responseLines) {
                g.drawString(line, textX, textY)
                textY += lineHeight
            }
        }
    }

    private fun calcContentWidth(inlay: Inlay<*>): Int {
        return (inlay.editor.scrollingModel.visibleArea.width - PADDING_X * 2 - LEFT_BORDER_WIDTH)
            .coerceAtLeast(200)
    }

    private fun wrapText(text: String, metrics: FontMetrics, maxWidth: Int): List<String> {
        val collapsed = text.replace('\n', ' ').replace('\r', ' ')
        if (maxWidth <= 0) return listOf(collapsed)

        val words = collapsed.split(" ")
        val lines = mutableListOf<String>()
        val currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else {
                val candidate = "$currentLine $word"
                if (metrics.stringWidth(candidate) <= maxWidth) {
                    currentLine.append(" ").append(word)
                } else {
                    lines.add(currentLine.toString())
                    currentLine.clear()
                    currentLine.append(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines.ifEmpty { listOf("") }
    }
}
