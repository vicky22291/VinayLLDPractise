# Task 07: Comment Popup Editor

**Phase**: 2 - Markdown Review
**LOC**: ~250
**Dependencies**: Task 04 (CommentService), Task 06 (gutter icon click handlers)
**Verification**: Clicking "+" opens popup with text area and context. Save creates a comment. Edit shows existing text. Delete removes comment.

---

## Objective

Implement `CommentPopupEditor` — the inline dialog that appears when users click gutter icons. It supports creating new comments (from "+" icon clicks), editing existing comments (from chat bubble clicks), and deleting comments. The popup captures the selected text as context and creates/updates `ReviewComment` objects via `CommentService`.

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/ui/
└── CommentPopupEditor.kt          # ~250 LOC
```

---

## What to Implement

### `CommentPopupEditor`

**Reference**: HLD Section 7.4, IMPLEMENTATION.md Section 3.3

**Constructor parameters:**
- `project: Project`
- `session: ReviewSession`
- `filePath: String` (relative path)
- `startLine: Int` (1-based)
- `endLine: Int` (1-based)
- `selectedText: String` (auto-captured context)
- `existingComment: ReviewComment?` (null for new, non-null for edit)

**Public API:**

```
show(editor: Editor, point: Point)
    → Build popup content panel
    → Create popup via JBPopupFactory.getInstance().createComponentPopupBuilder()
    → Set popup properties: movable, resizable, focusable, cancel-on-click-outside
    → Show relative to the gutter click point
```

### Popup Content Layout

**Reference**: HLD Section 7.4

```
+------------------------------------------+
|  Comment on lines 42-45                  |
+------------------------------------------+
|  ┌──────────────────────────────────┐    |
|  │ [Comment text area]              │    |
|  │ (Multiline, ~4 rows)            │    |
|  │                                  │    |
|  └──────────────────────────────────┘    |
|                                          |
|  Context: "The proposed feature store…"  |
|  (Read-only, truncated to 2 lines)       |
|                                          |
|  [Delete]           [Cancel]  [Save]     |
+------------------------------------------+
```

Build with standard Swing/IntelliJ UI components:

**Header**: `JBLabel("Comment on lines ${startLine}-${endLine}")` — bold, at top

**Text area**: `JBTextArea(4, 40)` wrapped in `JBScrollPane`
- If editing: pre-populate with `existingComment.commentText`
- If new: empty
- Supports basic text input (no Markdown rendering needed)

**Context preview**: `JBLabel` with truncated `selectedText` (max 100 chars + "...")
- Gray color, italic, wrapped in a light background panel
- Shows what text the comment refers to

**Buttons** (bottom bar using `JPanel` with `FlowLayout.RIGHT`):
- **Delete** (only visible if `existingComment != null`): Red text, left-aligned
- **Cancel**: Closes popup, no action
- **Save**: Creates or updates comment

### Button Actions

**Save (new comment):**
```
commentText = textArea.text.trim()
IF commentText.isEmpty(): show error tooltip, return

comment = ReviewComment(
    id = UUID.randomUUID(),
    filePath = filePath,
    startLine = startLine,
    endLine = endLine,
    selectedText = selectedText,
    commentText = commentText,
    authorId = System.getProperty("user.name"),
    createdAt = Instant.now(),
    status = CommentStatus.DRAFT
)

commentService.addComment(session, comment)
popup.cancel()  // close the popup
```

**Save (edit existing):**
```
commentText = textArea.text.trim()
IF commentText.isEmpty(): show error tooltip, return

commentService.updateComment(session, existingComment.id, commentText)
popup.cancel()
```

**Delete:**
```
confirmDialog = Messages.showYesNoDialog(
    project, "Delete this comment?", "Delete Comment", Messages.getWarningIcon()
)
IF confirmed:
    commentService.deleteComment(session, existingComment.id)
    popup.cancel()
```

**Cancel:**
```
popup.cancel()
```

### Integration with GutterIconProvider (Task 06)

Update the click handlers in `ReviewGutterIconProvider` to open `CommentPopupEditor`:

**"+" icon click handler** (new comment):
```
val selectedText = captureContextAtLine(editor, line)
val popup = CommentPopupEditor(project, session, filePath, line, line, selectedText, null)
popup.show(editor, clickPoint)
```

**Comment icon click handler** (view/edit existing):
```
val comment = comments.first()  // or show a list if multiple
val popup = CommentPopupEditor(project, session, comment.filePath, comment.startLine, comment.endLine, comment.selectedText, comment)
popup.show(editor, clickPoint)
```

### Context Capture Helper

```
FUNCTION captureContextAtLine(editor: Editor, line: Int): String
    document = editor.document
    IF line < 1 OR line > document.lineCount: RETURN ""
    lineStart = document.getLineStartOffset(line - 1)  // 0-based for document
    lineEnd = document.getLineEndOffset(line - 1)
    RETURN document.getText(TextRange(lineStart, lineEnd)).trim()
```

For text selection (from `AddCommentAction`, Task 09):
```
FUNCTION captureSelectionContext(editor: Editor): Pair<IntRange, String>
    selectionModel = editor.selectionModel
    IF selectionModel.hasSelection():
        startLine = document.getLineNumber(selectionModel.selectionStart) + 1
        endLine = document.getLineNumber(selectionModel.selectionEnd) + 1
        text = selectionModel.selectedText ?: ""
        RETURN (startLine..endLine) to text
    ELSE:
        caretLine = editor.caretModel.logicalPosition.line + 1
        text = captureContextAtLine(editor, caretLine)
        RETURN (caretLine..caretLine) to text
```

---

## Key Implementation Notes

1. **Popup sizing**: Use `setMinimumSize(Dimension(350, 200))` on the popup builder to prevent tiny popups
2. **Focus**: Set focus to the text area when popup opens: `textArea.requestFocusInWindow()`
3. **Escape key**: `JBPopupFactory` handles Escape to close by default
4. **Keyboard shortcut**: Ctrl+Enter should trigger Save (add key listener to text area)
5. **Thread safety**: All UI operations on EDT (which they are since this is triggered by user click)

---

## Verification

1. Click "+" icon in gutter -> popup appears with empty text area, header shows line number
2. Type comment text, click Save -> comment appears in session, gutter icon changes to chat bubble
3. Click chat bubble icon -> popup shows existing comment text, can edit
4. Click Save after editing -> comment text updated
5. Click Delete -> confirmation dialog -> comment removed, icon reverts to "+"
6. Click Cancel -> popup closes, no changes
7. Context preview shows the text at the clicked line
8. Empty comment text cannot be saved (shows error)
