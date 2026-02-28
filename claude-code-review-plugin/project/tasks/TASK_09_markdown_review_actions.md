# Task 09: Markdown Review Actions + Status Bar Widget

**Phase**: 2 - Markdown Review
**LOC**: ~300
**Dependencies**: Task 04, Task 06, Task 07, Task 08
**Verification**: Right-click .md file shows "Review this Markdown". Clicking it enters review mode. "Add Review Comment" appears in editor context menu. Status bar shows review state.

---

## Objective

Implement the action classes that wire user interactions to the service layer: `StartMarkdownReviewAction`, `AddCommentAction`, `PublishReviewAction`, plus the status bar widget. After this task, the full Markdown review workflow is functional (minus Claude response reload).

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/actions/
├── StartMarkdownReviewAction.kt       # ~80 LOC
├── AddCommentAction.kt                # ~60 LOC
└── PublishReviewAction.kt             # ~50 LOC

src/main/kotlin/com/uber/jetbrains/reviewplugin/ui/
├── ReviewStatusBarWidgetFactory.kt    # ~25 LOC
└── ReviewStatusBarWidget.kt           # ~50 LOC
```

Also update:
```
src/main/resources/META-INF/plugin.xml   # Register all actions + status bar
```

---

## What to Implement

### `StartMarkdownReviewAction`

**Reference**: HLD Section 12, IMPLEMENTATION.md Section 3.4

Extends `AnAction`. Registered in `EditorPopupMenu` and `ProjectViewPopupMenu`.

```
CLASS StartMarkdownReviewAction : AnAction("Review this Markdown")

  FUNCTION update(e: AnActionEvent)
      // Control visibility and enablement
      file = getTargetFile(e)
      visible = file != null AND file.extension == "md"
      enabled = visible AND NOT reviewModeService.isInReviewMode(file.path)
      e.presentation.isVisible = visible
      e.presentation.isEnabled = enabled

  FUNCTION actionPerformed(e: AnActionEvent)
      project = e.project ?: return
      file = getTargetFile(e) ?: return

      reviewModeService = project.service<ReviewModeService>()
      // Use relative path from project root
      relativePath = project.basePath?.let { file.path.removePrefix(it + "/") } ?: file.path
      session = reviewModeService.enterMarkdownReview(relativePath)

      // Trigger gutter icon refresh
      DaemonCodeAnalyzer.getInstance(project).restart()

      // Open/activate the Review Tool Window
      toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Claude Code Review")
      toolWindow?.show()

  FUNCTION getTargetFile(e: AnActionEvent): VirtualFile?
      // Try editor file first, then project view selection
      return e.getData(CommonDataKeys.VIRTUAL_FILE)
```

### `AddCommentAction`

**Reference**: IMPLEMENTATION.md Section 3.4

Extends `AnAction`. Registered in `EditorPopupMenu`. Keyboard shortcut: Ctrl+Shift+C.

```
CLASS AddCommentAction : AnAction("Add Review Comment")

  FUNCTION update(e: AnActionEvent)
      editor = e.getData(CommonDataKeys.EDITOR)
      file = e.getData(CommonDataKeys.VIRTUAL_FILE)
      project = e.project

      visible = editor != null AND file != null AND project != null
      enabled = visible AND reviewModeService.isInReviewMode(file.path)
      e.presentation.isVisible = visible
      e.presentation.isEnabled = enabled

  FUNCTION actionPerformed(e: AnActionEvent)
      editor = e.getData(CommonDataKeys.EDITOR) ?: return
      file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
      project = e.project ?: return

      reviewModeService = project.service<ReviewModeService>()
      relativePath = project.basePath?.let { file.path.removePrefix(it + "/") } ?: file.path
      session = reviewModeService.getActiveSession(relativePath) ?: return

      // Capture selection or cursor position
      (lineRange, selectedText) = captureSelectionContext(editor)

      // Open comment popup
      popup = CommentPopupEditor(
          project, session, relativePath,
          lineRange.first, lineRange.last, selectedText, null
      )
      popup.show(editor, editor.visualPositionToXY(editor.caretModel.visualPosition))
```

`captureSelectionContext` helper (from Task 07 spec):
- If selection exists: use selected lines and text
- If no selection: use caret line and full line text

### `PublishReviewAction`

**Reference**: IMPLEMENTATION.md Section 3.4

Extends `AnAction`. Keyboard shortcut: Ctrl+Shift+P.

```
CLASS PublishReviewAction : AnAction("Publish Review")

  FUNCTION update(e: AnActionEvent)
      project = e.project
      enabled = project != null AND hasActiveDraftComments(project)
      e.presentation.isEnabled = enabled

  FUNCTION actionPerformed(e: AnActionEvent)
      project = e.project ?: return
      // Delegate to ReviewToolWindowPanel.onPublishClicked()
      // OR implement publish logic directly here (same as Task 08 publish)

      reviewModeService = project.service<ReviewModeService>()
      session = reviewModeService.getAllActiveSessions().firstOrNull() ?: return

      storageManager = project.service<StorageManager>()
      storageManager.ensureReviewDirectory()
      outputDir = storageManager.getReviewDirectory()

      reviewFileManager = ReviewFileManager()
      reviewFilePath = reviewFileManager.publish(session, outputDir)

      session.reviewFilePath = reviewFilePath.toString()
      session.publishedAt = Instant.now()
      session.status = ReviewSessionStatus.PUBLISHED
      session.comments.forEach { it.status = CommentStatus.PENDING }

      // Copy command to clipboard
      val command = reviewFileManager.generateCliCommand(reviewFilePath.toString())
      CopyPasteManager.getInstance().setContents(StringSelection(command))

      // Notification
      NotificationGroupManager.getInstance()
          .getNotificationGroup("ReviewPlugin")
          .createNotification("Review published. Command copied.", NotificationType.INFORMATION)
          .notify(project)

  FUNCTION hasActiveDraftComments(project: Project): Boolean
      return project.service<ReviewModeService>()
          .getAllActiveSessions()
          .any { it.getDraftComments().isNotEmpty() }
```

### `ReviewStatusBarWidgetFactory` + `ReviewStatusBarWidget`

**Reference**: IMPLEMENTATION.md Section 3.3

**Factory:**
```kotlin
class ReviewStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId() = "ReviewModeStatus"
    override fun getDisplayName() = "Review Mode"
    override fun createWidget(project: Project) = ReviewStatusBarWidget(project)
}
```

**Widget** (implements `StatusBarWidget.TextPresentation`):
```kotlin
class ReviewStatusBarWidget(private val project: Project) :
    StatusBarWidget, StatusBarWidget.TextPresentation, ReviewModeListener {

    override fun ID() = "ReviewModeStatus"

    override fun getText(): String {
        val sessions = project.service<ReviewModeService>().getAllActiveSessions()
        if (sessions.isEmpty()) return ""
        val session = sessions.first()
        val draftCount = session.getDraftComments().size
        return "Review Mode: Active | $draftCount drafts"
    }

    override fun getTooltipText() = "Claude Code Review status"
    override fun getAlignment() = Component.CENTER_ALIGNMENT

    // Register as listener on install, deregister on dispose
    override fun install(statusBar: StatusBar) {
        project.service<ReviewModeService>().addListener(this)
    }

    override fun dispose() {
        project.service<ReviewModeService>().removeListener(this)
    }

    // Listener callbacks trigger status bar update
    override fun onReviewModeEntered(session: ReviewSession) = updateWidget()
    override fun onReviewModeExited(session: ReviewSession) = updateWidget()
    override fun onCommentsChanged(session: ReviewSession) = updateWidget()

    private fun updateWidget() {
        // Trigger status bar repaint
    }
}
```

### `plugin.xml` Updates

Register all actions and widgets:

```xml
<!-- Actions -->
<action id="ReviewPlugin.StartMarkdownReview"
    class="...StartMarkdownReviewAction"
    text="Review this Markdown">
    <add-to-group group-id="EditorPopupMenu" anchor="last"/>
    <add-to-group group-id="ProjectViewPopupMenu" anchor="last"/>
    <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift R"/>
</action>

<action id="ReviewPlugin.AddComment"
    class="...AddCommentAction"
    text="Add Review Comment">
    <add-to-group group-id="EditorPopupMenu" anchor="last"/>
    <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift C"/>
</action>

<action id="ReviewPlugin.PublishReview"
    class="...PublishReviewAction"
    text="Publish Review">
    <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift P"/>
</action>

<!-- Status Bar -->
<statusBarWidgetFactory id="ReviewModeStatus"
    implementation="...ReviewStatusBarWidgetFactory"/>
```

---

## Verification

1. Right-click `.md` file in editor -> "Review this Markdown" appears
2. Right-click `.md` file in project tree -> "Review this Markdown" appears
3. Click action -> review mode enters, gutter icons appear, tool window opens
4. "Review this Markdown" is hidden for non-.md files
5. "Review this Markdown" is disabled when file is already in review mode
6. Select text in editor -> right-click -> "Add Review Comment" -> popup opens with selection
7. Ctrl+Shift+C opens comment popup at cursor position
8. Ctrl+Shift+P publishes review and copies command to clipboard
9. Status bar shows "Review Mode: Active | N drafts" when in review mode
10. Status bar is empty when no review is active
