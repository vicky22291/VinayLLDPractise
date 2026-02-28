# Task 08: Review Tool Window (Side Panel)

**Phase**: 2 - Markdown Review
**LOC**: ~450
**Dependencies**: Task 04 (ReviewModeService, CommentService), Task 05 (ReviewFileManager)
**Verification**: Tool window opens on review mode entry, shows draft comments, supports Publish/Reload, displays responses after reload

---

## Objective

Implement the Review Tool Window — the side panel that shows all comments for the active review session. It serves as the command center: listing drafts, triggering publish, displaying Claude's responses, and providing navigation back to comment locations. This is the most complex UI component.

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/ui/
├── ReviewToolWindowFactory.kt      # ~30 LOC
└── ReviewToolWindowPanel.kt        # ~400 LOC
```

Also update:
```
src/main/resources/META-INF/plugin.xml   # Register tool window
```

---

## What to Implement

### `ReviewToolWindowFactory`

**Reference**: IMPLEMENTATION.md Section 3.3

Implements `ToolWindowFactory`.

```kotlin
class ReviewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ReviewToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
```

### `ReviewToolWindowPanel`

**Reference**: HLD Section 13, IMPLEMENTATION.md Section 3.3

Extends `JPanel`. Implements `ReviewModeListener` to receive session updates.

**Lifecycle:**
- On creation: register as listener on `ReviewModeService`
- On `onReviewModeEntered`: rebuild UI with session data
- On `onReviewModeExited`: show empty state
- On `onCommentsChanged`: refresh comment list
- On `onResponsesLoaded`: refresh with responses

### Panel Layout

**Reference**: HLD Section 13.1, 13.2

The panel has three visual states:

**State 1: No active review**
```
+-------------------------------------------------------+
|  Claude Code Review                                    |
+-------------------------------------------------------+
|                                                        |
|  No active review session.                             |
|                                                        |
|  Right-click a .md file → "Review this Markdown"       |
|  or VCS → "Review the Diff" to start.                  |
|                                                        |
+-------------------------------------------------------+
```

**State 2: Active review with drafts (before publish)**
```
+-------------------------------------------------------+
|  [Session display name]              [Publish ▶]       |
+-------------------------------------------------------+
|                                                        |
|  --- Draft Comments (3) ---                            |
|                                                        |
|  📝 Line 42-45  "How does this integrate with..."      |
|     [Edit] [Delete] [Jump]                             |
|                                                        |
|  📝 Line 78-82  "Caching strategy doesn't account..."  |
|     [Edit] [Delete] [Jump]                             |
|                                                        |
|  📝 Line 120-125  "Consider circuit breaker..."        |
|     [Edit] [Delete] [Jump]                             |
|                                                        |
+-------------------------------------------------------+
|  [Complete ✓] [Reject ✗]          Sort: [Line# ▼]     |
+-------------------------------------------------------+
```

**State 3: After Claude responds (post-reload)**
```
+-------------------------------------------------------+
|  [Session display name]                                |
+-------------------------------------------------------+
|                                                        |
|  --- Comment 1 (Line 42-45) ---              [Jump]    |
|                                                        |
|  User:                                                 |
|  How does this integrate with Feature Manager?          |
|                                                        |
|  Claude:                                    ✅ Resolved |
|  Based on Feature Manager architecture                  |
|  (docs/feature-manager/ARCHITECTURE_02.md:156-234)...   |
|                                                        |
|  [Reply]                                               |
|                                                        |
|  --- Comment 2 (Line 78-82) ---              [Jump]    |
|                                                        |
|  User:                                                 |
|  Caching strategy doesn't account for TTL              |
|                                                        |
|  Claude:                                    🔄 Pending  |
|  (awaiting response)                                   |
|                                                        |
+-------------------------------------------------------+
|  [Complete ✓] [Reject ✗]                               |
+-------------------------------------------------------+
```

### UI Components

Use standard IntelliJ UI toolkit:

**Header bar**: `JPanel` with `BorderLayout`
- Left: `JBLabel(session.getDisplayName())` — bold
- Right: `JButton("Publish Review")` — only visible when drafts exist

**Comment list**: `JBScrollPane` containing a `JPanel` with `BoxLayout.Y_AXIS`
- Each comment is a `createCommentRow()` panel
- Sorted by line number (default)

**Footer bar**: `JPanel` with buttons
- `JButton("Complete ✓")` — calls `ReviewModeService.completeReview()`
- `JButton("Reject ✗")` — calls `ReviewModeService.rejectReview()`
- Complete/Reject only enabled when session is ACTIVE or PUBLISHED

### Comment Row Components

**Draft comment row** (`createDraftCommentRow`):
```
JPanel (BoxLayout.Y_AXIS):
  Line 1: JBLabel("Line ${startLine}-${endLine}") + status icon
  Line 2: JBLabel(commentText, truncated to 80 chars)
  Line 3: [Edit] [Delete] [Jump] buttons (link-styled)
```

**Resolved comment row** (`createResolvedCommentRow`):
```
JPanel (BoxLayout.Y_AXIS):
  Header: JBLabel("Comment ${index} (Line ${startLine}-${endLine})") + [Jump]
  User section:
    JBLabel("User:", bold)
    JBTextArea(commentText, read-only, wrapped)
  Claude section (if response exists):
    JBLabel("Claude:", bold) + status badge
    JBTextArea(claudeResponse, read-only, wrapped)
    [Reply] button
  Thread section (if replies exist):
    For each reply: JBLabel("[author]: text")
```

### Button Actions

**Publish button:**
```
FUNCTION onPublishClicked()
    session = getCurrentSession() ?: return
    reviewFileManager = ReviewFileManager()
    storageManager = project.service<StorageManager>()
    outputDir = storageManager.getReviewDirectory()
    reviewFilePath = reviewFileManager.publish(session, outputDir)

    session.reviewFilePath = reviewFilePath.toString()
    session.publishedAt = Instant.now()

    reviewModeService.setStatus(session, PUBLISHED)
    // Set all draft comments to PENDING
    session.comments.forEach { it.status = CommentStatus.PENDING }

    // Copy Claude command to clipboard
    val command = reviewFileManager.generateCliCommand(reviewFilePath.toString())
    CopyPasteManager.getInstance().setContents(StringSelection(command))

    // Show notification
    NotificationGroupManager.getInstance()
        .getNotificationGroup("ReviewPlugin")
        .createNotification("Review published. Command copied to clipboard.", NotificationType.INFORMATION)
        .notify(project)

    updateContent()  // refresh panel
```

**Jump button:**
```
FUNCTION onJumpToComment(comment: ReviewComment)
    // Open the file and navigate to the comment's line
    virtualFile = LocalFileSystem.getInstance().findFileByPath(projectRoot + "/" + comment.filePath)
    IF virtualFile != null:
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
        editor = FileEditorManager.getInstance(project).selectedTextEditor
        editor?.caretModel?.moveToLogicalPosition(LogicalPosition(comment.startLine - 1, 0))
        editor?.scrollingModel?.scrollToCaret(ScrollType.CENTER)
```

**Reply button:**
```
FUNCTION onReplyClicked(comment: ReviewComment)
    // Show a small dialog to type reply text
    replyText = Messages.showInputDialog(project, "Reply to comment:", "Reply", null)
    IF replyText != null AND replyText.isNotBlank():
        reply = Reply(
            author = System.getProperty("user.name"),
            timestamp = Instant.now().toString(),
            text = replyText
        )
        reviewFileManager.appendReply(Path.of(session.reviewFilePath), comment.index, reply)
        comment.status = CommentStatus.PENDING
        updateContent()
```

**Reload button** (shown as notification action — see Task 11):
```
FUNCTION onReloadClicked()
    session = getCurrentSession() ?: return
    IF session.reviewFilePath == null: return
    reviewFile = reviewFileManager.load(Path.of(session.reviewFilePath))
    commentService.applyResponses(session, reviewFile)
    updateContent()
```

### `plugin.xml` Update

```xml
<toolWindow id="Claude Code Review" anchor="right" secondary="true"
    factoryClass="com.uber.jetbrains.reviewplugin.ui.ReviewToolWindowFactory"/>
```

---

## Key Implementation Notes

1. **Thread safety**: All UI updates must happen on EDT. Listener callbacks from services may come from any thread — use `ApplicationManager.getApplication().invokeLater {}` if needed.
2. **Panel rebuilding**: `updateContent()` should `removeAll()` and rebuild. For simple panels this is fine; no need for a complex diff-based update.
3. **Scrolling**: Wrap the comment list in `JBScrollPane` for long review sessions.
4. **Reply index**: When calling `appendReply`, use the 1-based index from `ReviewFileComment.index`, not the 0-based list position.
5. **Tool window visibility**: The tool window is always registered but shows the "no active review" state when no session exists.

---

## Verification

1. Tool window appears in right sidebar when plugin is loaded
2. Shows "no active review" message initially
3. Enter review mode -> panel updates with session name and empty draft list
4. Add comments -> draft list shows each comment with line numbers and text preview
5. Click "Jump" -> editor navigates to the comment's line
6. Click "Publish" -> notification appears, command copied to clipboard
7. After Claude responds and reload: comments show user text + Claude response
8. "Complete" and "Reject" buttons archive and close the session
