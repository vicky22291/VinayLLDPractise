package com.uber.jetbrains.reviewplugin.ui

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.uber.jetbrains.reviewplugin.listeners.ReviewModeListener
import com.uber.jetbrains.reviewplugin.model.ReviewSession
import com.uber.jetbrains.reviewplugin.services.ReviewModeService
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Swing panel that renders review data inside the tool window.
 * Delegates all business logic to [ReviewToolWindowPanel].
 */
class ReviewToolWindowSwingPanel(
    private val project: Project,
    private val panel: ReviewToolWindowPanel
) : JPanel(BorderLayout()), ReviewModeListener {

    private val reviewModeService = project.getService(ReviewModeService::class.java)

    private val cardLayout = CardLayout()
    private val cardPanel = JPanel(cardLayout)

    private val draftListModel = DefaultListModel<ReviewToolWindowPanel.DraftCommentRow>()
    private val draftList = JBList(draftListModel)

    private val responseListModel = DefaultListModel<ReviewToolWindowPanel.ResolvedCommentRow>()
    private val responseList = JBList(responseListModel)

    private val publishButton = JButton("Publish")
    private val completeButton = JButton("Complete")
    private val rejectButton = JButton("Reject")
    private val reloadButton = JButton("Reload Responses")

    private val fileListLabel = JLabel()

    companion object {
        private const val CARD_EMPTY = "empty"
        private const val CARD_DRAFTS = "drafts"
        private const val CARD_RESPONSES = "responses"
    }

    private val buttonBar = JPanel(FlowLayout(FlowLayout.LEFT))

    init {
        buildEmptyCard()
        buildDraftsCard()
        buildResponsesCard()
        add(cardPanel, BorderLayout.CENTER)

        buttonBar.add(publishButton)
        buttonBar.add(reloadButton)
        buttonBar.add(completeButton)
        buttonBar.add(rejectButton)
        add(buttonBar, BorderLayout.SOUTH)

        wireActions()
        wireLists()
        reviewModeService.addListener(this)
        rebuild()
    }

    fun dispose() {
        reviewModeService.removeListener(this)
    }

    // ---- Card construction ----

    private fun buildEmptyCard() {
        val emptyPanel = JPanel(BorderLayout())
        val label = JLabel(
            "<html><center><b>${ReviewToolWindowPanel.EMPTY_STATE_TITLE}</b><br><br>" +
                "${ReviewToolWindowPanel.EMPTY_STATE_MESSAGE}<br><br>" +
                "<font size='-1'>${ReviewToolWindowPanel.EMPTY_STATE_HINT.replace("\n", "<br>")}</font></center></html>"
        )
        label.horizontalAlignment = SwingConstants.CENTER
        emptyPanel.add(label, BorderLayout.CENTER)
        cardPanel.add(emptyPanel, CARD_EMPTY)
    }

    private fun buildDraftsCard() {
        val draftsPanel = JPanel(BorderLayout())
        draftList.cellRenderer = ListCellRenderer { _, value, _, isSelected, cellHasFocus ->
            val displayText = if (panel.isDiffReview()) {
                val fileName = value.filePath.substringAfterLast("/")
                "$fileName:${value.lineRange}  ${value.previewText}"
            } else {
                "${value.lineRange}  ${value.previewText}"
            }
            val label = JLabel(displayText)
            if (isSelected) {
                label.background = draftList.selectionBackground
                label.foreground = draftList.selectionForeground
                label.isOpaque = true
            }
            label
        }
        draftsPanel.add(JBScrollPane(draftList), BorderLayout.CENTER)

        fileListLabel.border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
        draftsPanel.add(fileListLabel, BorderLayout.NORTH)

        cardPanel.add(draftsPanel, CARD_DRAFTS)
    }

    private fun buildResponsesCard() {
        val responsesPanel = JPanel(BorderLayout())
        responseList.cellRenderer = ListCellRenderer { _, value, _, isSelected, _ ->
            val responsePreview = value.claudeResponse?.take(60) ?: ""
            val lineInfo = if (panel.isDiffReview()) {
                val fileName = value.filePath.substringAfterLast("/")
                "$fileName:${value.lineRange}"
            } else {
                value.lineRange
            }
            val text = "#${value.index} $lineInfo [${value.statusLabel}] $responsePreview"
            val label = JLabel(text)
            if (isSelected) {
                label.background = responseList.selectionBackground
                label.foreground = responseList.selectionForeground
                label.isOpaque = true
            }
            label
        }
        responsesPanel.add(JBScrollPane(responseList), BorderLayout.CENTER)

        cardPanel.add(responsesPanel, CARD_RESPONSES)
    }

    // ---- Wiring ----

    private fun wireActions() {
        publishButton.addActionListener {
            val result = panel.publishReview() ?: return@addActionListener
            Messages.showInfoMessage(
                project,
                "Published to ${result.reviewFilePath}\nCLI: ${result.cliCommand}",
                "Review Published"
            )
            rebuild()
        }
        completeButton.addActionListener {
            panel.completeReview()
            rebuild()
        }
        rejectButton.addActionListener {
            panel.rejectReview()
            rebuild()
        }
        reloadButton.addActionListener {
            panel.reloadResponses()
            rebuild()
        }
    }

    private fun wireLists() {
        val jumpOnDoubleClick = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 2) return
                val list = e.source
                val target = when (list) {
                    is JBList<*> -> {
                        val index = (list as JBList<*>).locationToIndex(e.point)
                        if (index < 0) return
                        when (val item = (list as JBList<*>).model.getElementAt(index)) {
                            is ReviewToolWindowPanel.DraftCommentRow ->
                                panel.getJumpTarget(item.commentId)
                            is ReviewToolWindowPanel.ResolvedCommentRow ->
                                panel.getJumpTarget(item.commentId)
                            else -> null
                        }
                    }
                    else -> null
                }
                target?.let { jumpToTarget(it) }
            }
        }
        draftList.addMouseListener(jumpOnDoubleClick)
        responseList.addMouseListener(jumpOnDoubleClick)

        val contextMenu = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = maybeShowPopup(e)
            override fun mouseReleased(e: MouseEvent) = maybeShowPopup(e)

            private fun maybeShowPopup(e: MouseEvent) {
                if (!e.isPopupTrigger) return
                val list = e.source as? JBList<*> ?: return
                val index = list.locationToIndex(e.point)
                if (index < 0) return
                list.selectedIndex = index
                val item = list.model.getElementAt(index)
                val commentId = when (item) {
                    is ReviewToolWindowPanel.DraftCommentRow -> item.commentId
                    is ReviewToolWindowPanel.ResolvedCommentRow -> item.commentId
                    else -> return
                }
                val popup = JPopupMenu()
                val editItem = JMenuItem("Edit")
                editItem.addActionListener {
                    val newText = Messages.showInputDialog(
                        project, "Edit comment:", "Edit Comment", null
                    )
                    if (!newText.isNullOrBlank()) {
                        panel.editComment(commentId, newText)
                        rebuild()
                    }
                }
                val deleteItem = JMenuItem("Delete")
                deleteItem.addActionListener {
                    panel.deleteComment(commentId)
                    rebuild()
                }
                popup.add(editItem)
                popup.add(deleteItem)
                popup.show(e.component, e.x, e.y)
            }
        }
        draftList.addMouseListener(contextMenu)
        responseList.addMouseListener(contextMenu)
    }

    private fun jumpToTarget(target: ReviewToolWindowPanel.JumpTarget) {
        val vFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
            .findFileByPath(target.filePath) ?: return
        val descriptor = OpenFileDescriptor(project, vFile, target.line - 1, 0)
        descriptor.navigate(true)
    }

    // ---- State rebuild ----

    private fun rebuild() {
        val state = panel.getCurrentState()
        when (state) {
            ReviewToolWindowPanel.PanelState.NO_REVIEW -> {
                buttonBar.isVisible = false
                cardLayout.show(cardPanel, CARD_EMPTY)
            }
            ReviewToolWindowPanel.PanelState.DRAFTS -> {
                draftListModel.clear()
                panel.getDraftCommentRows().forEach { draftListModel.addElement(it) }
                publishButton.isVisible = panel.isPublishButtonVisible()
                reloadButton.isVisible = false
                completeButton.isEnabled = panel.isCompleteEnabled()
                rejectButton.isEnabled = panel.isRejectEnabled()
                buttonBar.isVisible = true
                if (panel.isDiffReview()) {
                    val fileRows = panel.getChangedFileRows()
                    val summary = fileRows.joinToString(", ") { row ->
                        val name = row.filePath.substringAfterLast("/")
                        if (row.commentCount > 0) "$name (${row.commentCount})" else name
                    }
                    fileListLabel.text = "<html><b>Files:</b> $summary</html>"
                    fileListLabel.isVisible = true
                } else {
                    fileListLabel.isVisible = false
                }
                cardLayout.show(cardPanel, CARD_DRAFTS)
            }
            ReviewToolWindowPanel.PanelState.RESPONSES -> {
                responseListModel.clear()
                panel.getResolvedCommentRows().forEach { responseListModel.addElement(it) }
                publishButton.isVisible = panel.isPublishButtonVisible()
                reloadButton.isVisible = true
                completeButton.isEnabled = panel.isCompleteEnabled()
                rejectButton.isEnabled = panel.isRejectEnabled()
                buttonBar.isVisible = true
                cardLayout.show(cardPanel, CARD_RESPONSES)
            }
        }
    }

    // ---- ReviewModeListener ----

    override fun onReviewModeEntered(session: ReviewSession) = rebuild()
    override fun onReviewModeExited(session: ReviewSession) = rebuild()
    override fun onCommentsChanged(session: ReviewSession) = rebuild()
    override fun onResponsesLoaded(session: ReviewSession) = rebuild()
}
