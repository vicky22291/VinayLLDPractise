# Task 06: Gutter Icon Provider + Line Highlighting

**Phase**: 2 - Markdown Review
**LOC**: ~300
**Dependencies**: Task 04 (ReviewModeService, CommentService)
**Verification**: In review mode, gutter shows "+" on uncommented lines and status icons on commented lines. Commented lines have colored background highlights.

---

## Objective

Implement the visual overlay that makes review mode visible: gutter icons on every line ("+", chat bubble, clock, checkmark) and background highlighting on commented lines. These components attach to **existing editors** — they do NOT create custom editors.

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/ui/
├── ReviewGutterIconProvider.kt     # ~160 LOC
└── LineHighlighter.kt              # ~100 LOC
```

Also update:
```
src/main/resources/META-INF/plugin.xml   # Register LineMarkerProvider
```

---

## What to Implement

### `ReviewGutterIconProvider` (LineMarkerProvider)

**Reference**: HLD Section 7, IMPLEMENTATION.md Section 6.1

Implements `LineMarkerProvider`. Registered with `language=""` to apply to **all file types**.

**Critical performance constraint**: This runs on every editor repaint for every file. The fast-path MUST return `null` immediately for non-reviewed files.

```
CLASS ReviewGutterIconProvider : LineMarkerProvider

  FUNCTION getLineMarkerInfo(element: PsiElement): LineMarkerInfo?
      // Fast path #1: Only process first leaf element per line
      IF element is not the first leaf on its line:
          RETURN null

      file = element.containingFile?.virtualFile ?: RETURN null
      project = element.project

      // Fast path #2: Skip non-reviewed files
      reviewModeService = project.service<ReviewModeService>()
      IF NOT reviewModeService.isInReviewMode(file.path):
          RETURN null

      // Get the active session for this file
      session = reviewModeService.getActiveSession(file.path) ?: RETURN null

      // Calculate 1-based line number
      document = PsiDocumentManager.getInstance(project).getDocument(element.containingFile) ?: RETURN null
      line = document.getLineNumber(element.textOffset) + 1

      // Check for existing comments on this line
      commentService = project.service<CommentService>()
      comments = commentService.getCommentsForLine(session, file.path, line)

      IF comments.isNotEmpty():
          // Use the "worst" status: DRAFT > PENDING > RESOLVED
          icon = selectIconForStatus(comments)
          tooltip = buildTooltip(comments)
          RETURN LineMarkerInfo(element, icon, tooltip, createViewHandler(session, comments))
      ELSE:
          RETURN LineMarkerInfo(element, Icons.ADD_COMMENT, "Add comment", createAddHandler(session, file, line))
```

**Icon selection logic:**

| Comment Status | Icon | Color |
|---------------|------|-------|
| `DRAFT` | `commentExists.svg` | Blue chat bubble |
| `PENDING` | `commentPending.svg` (need to add) | Yellow clock |
| `RESOLVED` | `commentResolved.svg` | Green checkmark |
| No comment | `addComment.svg` | Blue "+" |

If multiple comments on one line have different statuses, show the highest-priority status icon (DRAFT > PENDING > RESOLVED).

**Click handlers:**
- "+" icon click: Opens `CommentPopupEditor.show()` (Task 07) for adding a new comment
- Comment icon click: Opens `CommentPopupEditor.show()` with existing comment for viewing/editing

**For now**: Click handlers can be stubs (print to log or show a simple notification). They will be connected to `CommentPopupEditor` in Task 07.

**"First leaf" detection**: Use `PsiTreeUtil.prevLeaf(element)` to check if the previous leaf is on a different line. If the previous leaf is on the same line, return `null`.

### `LineHighlighter`

**Reference**: IMPLEMENTATION.md Section 3.3

Manages background highlighting on commented lines using `MarkupModel.addRangeHighlighter()`.

**State:**
- `highlighters: MutableMap<String, MutableList<RangeHighlighter>>` — keyed by file path

**Public API:**

```
applyHighlights(editor: Editor, session: ReviewSession)
    → Clear existing highlights for this editor's file
    → For each comment in session where comment.filePath matches editor file:
        → Get line range from comment.startLine to comment.endLine
        → Convert to document offset using editor.document
        → Add range highlighter with appropriate color
        → Store in highlighters map

clearHighlights(editor: Editor)
    → Remove all range highlighters for this editor's file
    → Clear from highlighters map

refreshHighlights(editor: Editor, session: ReviewSession)
    → clearHighlights(editor)
    → applyHighlights(editor, session)
```

**Color by status:**

| Status | Background Color | TextAttributes |
|--------|-----------------|----------------|
| `DRAFT` | Light yellow `#FFF9C4` | Background only |
| `PENDING` | Light blue `#BBDEFB` | Background only |
| `RESOLVED` | Light green `#C8E6C9` | Background only |

Use `TextAttributes()` with `setBackgroundColor()`. Set the highlighter layer to `HighlighterLayer.SELECTION - 1` (below selection, above syntax).

### `plugin.xml` Update

```xml
<codeInsight.lineMarkerProvider language=""
    implementationClass="com.uber.jetbrains.reviewplugin.ui.ReviewGutterIconProvider"/>
```

---

## Icon Files Update

Add one more SVG icon (if not already present):
- `commentPending.svg` — yellow clock icon (for PENDING status)

---

## Key Implementation Notes

1. **Performance**: The `getLineMarkerInfo()` method is called thousands of times per repaint. The two fast-path checks (first leaf + review mode) must be at the top.
2. **Editor independence**: This works on ANY editor — Markdown source editor, diff viewer, regular code editor. The `language=""` registration ensures universal applicability.
3. **DaemonCodeAnalyzer**: When comments change, the caller must invoke `DaemonCodeAnalyzer.getInstance(project).restart()` to trigger a repaint that updates gutter icons. This will be called from `CommentService` (Task 04) via the listener pattern.
4. **RangeHighlighter lifecycle**: Highlighters are per-editor. When an editor is closed, its highlighters are automatically disposed. We just need to clear our tracking map.

---

## Verification

1. Enter review mode on a `.md` file -> every line shows a blue "+" icon in the gutter
2. Add a comment on line 42 -> "+" changes to chat bubble icon on that line
3. Line 42 gets a light yellow background highlight
4. Exit review mode -> all gutter icons and highlights disappear
5. Re-enter review mode -> icons and highlights reappear for existing comments
6. Non-reviewed files show NO gutter icons (fast-path works)
