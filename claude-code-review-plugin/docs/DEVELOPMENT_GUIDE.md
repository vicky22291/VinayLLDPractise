# Development Guide

Conventions, patterns, and step-by-step guides for extending the plugin.

---

## Code Organization Conventions

1. **Model classes have zero IntelliJ dependencies** -- pure Kotlin data classes and enums in `model/`
2. **Services use dual constructors** -- `Project` for runtime, internal constructor for testing (see [TESTING.md](TESTING.md))
3. **UI split into logic and integration** -- logic classes are testable, integration classes adapt to IntelliJ APIs (see [ARCHITECTURE.md](ARCHITECTURE.md))
4. **Actions are thin** -- they resolve services, call methods, and trigger UI refresh; business logic lives in services or UI logic classes
5. **Listeners have default no-op methods** -- `ReviewModeListener` callbacks default to empty, so implementors only override what they need

---

## Sealed Class Dispatch

`ReviewSession` is a sealed class with `MarkdownReviewSession` and `GitDiffReviewSession` subclasses. Use exhaustive `when` expressions everywhere:

```
WHEN session IS:
    MarkdownReviewSession → use sourceFilePath
    GitDiffReviewSession  → use baseBranch, compareBranch, changedFiles
```

The compiler enforces exhaustiveness -- adding a new subclass (e.g., `PRReviewSession`) causes compile errors at every unhandled branch, making it impossible to miss a case.

**Where this pattern appears**:
- `ReviewFileManager.publish()` -- metadata construction
- `StorageManager.saveDrafts()` -- DTO type field
- `StorageManager.loadDrafts()` -- session reconstruction
- `ReviewSession.getReviewFileName()` -- file naming
- `ReviewSession.getDisplayName()` -- UI display

---

## Observer Pattern (ReviewModeListener)

The plugin uses the observer pattern for loose coupling between services and UI. `ReviewModeListener` has 4 callbacks:

| Callback | Fires When | Typical Subscribers |
|----------|-----------|---------------------|
| `onReviewModeEntered(session)` | New session started | Tool window, status bar, editor listener |
| `onReviewModeExited(session)` | Session completed/rejected | Tool window, status bar, editor listener |
| `onCommentsChanged(session)` | Comment added/updated/deleted | Tool window, editor listener |
| `onResponsesLoaded(session)` | Claude responses applied | Tool window, editor listener |

**Registration**: `reviewModeService.addListener(this)` / `removeListener(this)`

**Source**: `listeners/ReviewModeListener.kt:1-10`

---

## File Write Coordination

The plugin writes `.review.json` files (on publish, reply). The `ReviewFileWatcher` detects all `.review.json` changes. To avoid a reload loop:

1. **Before writing**: `ReviewFileManager` records the file path and timestamp
2. **On VFS event**: `ReviewFileWatcher` calls `ReviewFileManager.isInternalWrite(path)` -- returns `true` if the write happened within the last 2 seconds
3. **If internal**: Skip notification (it was our own write)
4. **If external**: Show "Claude responded" notification

**Rule**: Always use `ReviewFileManager` for `.review.json` writes. Never write directly with `Files.write()`.

**Source**: `services/ReviewFileManager.kt` (isInternalWrite), `listeners/ReviewFileWatcher.kt:1-74`

---

## Adding a New Review Type

To add a new review type (e.g., `PRReviewSession`):

### Step 1: Model

Add the subclass to `ReviewSession`:

```
class PRReviewSession(
    val prNumber: Int,
    val prUrl: String,
    // ... standard ReviewSession params
) : ReviewSession(...) {
    override fun getReviewFileName() = "pr-$prNumber.review.json"
    override fun getDisplayName() = "PR #$prNumber"
}
```

The compiler will flag every unhandled `when` branch.

### Step 2: Services

Update exhaustive `when` expressions in:
- `StorageManager.saveDrafts()` -- add serialization for new type
- `StorageManager.loadDrafts()` -- add deserialization for new type
- `ReviewFileManager.publish()` -- add metadata extraction for new type

### Step 3: Actions

Create `StartPRReviewAction`:
- Add to `plugin.xml` with menu group and shortcut
- Follow existing patterns in `StartMarkdownReviewAction` / `StartDiffReviewAction`

### Step 4: Tests

- Add `PRReviewSession` test cases to `ReviewSessionTest`
- Add serialization tests to `StorageManagerTest`
- Add publish tests to `ReviewFileManagerTest`
- Test the new action

### Step 5: Coverage

If the new action depends on IntelliJ APIs, add it to `platformDependentExcludes` in `build.gradle.kts`.

---

## Adding a New UI Component

To add a new visual overlay (e.g., a margin annotation):

### Step 1: Logic Class

Create `ui/MarginAnnotationProvider.kt` (no IntelliJ deps):
- Pure computation: given a session and file path, return annotation data
- Use simple data classes for output (position, text, color)

### Step 2: Integration Class

Create `ui/ReviewMarginAnnotationRenderer.kt` (IntelliJ-dependent):
- Wire to IntelliJ via appropriate extension point
- Call the logic class for data, render via platform APIs

### Step 3: Wire to Lifecycle

Update `ReviewEditorListener` to apply/clear the new component on:
- `editorCreated` -- apply on editor open
- `onCommentsChanged` / `onResponsesLoaded` -- refresh
- `editorReleased` -- cleanup

### Step 4: Tests and Coverage

- Write full unit tests for the logic class (95%+ coverage)
- Add the integration class to `platformDependentExcludes` in `build.gradle.kts`

---

## Important Constraints

1. **Never modify source files** -- The plugin only creates/modifies files in `.review/`. User's source code is read-only.

2. **No direct Claude API calls** -- The plugin generates `.review.json` files. Claude processes them via `review-cli`. The plugin never calls Claude directly.

3. **One session per file** -- `ReviewModeService` enforces that a file can only have one active review session at a time.

4. **Deterministic file names** -- Review file names are derived from the source (path or branch pair), not random. This ensures reuse after archival.

5. **JSON format** -- All review files use `kotlinx-serialization-json` with `prettyPrint = true`, `encodeDefaults = true`. This makes files human-readable and diff-friendly.

6. **Gutter icon performance** -- `ReviewGutterIconProvider` checks `isFileInReviewMode()` first and returns `null` immediately for non-reviewed files. Zero overhead on normal editing.

7. **Atomic persistence** -- Draft saves use temp file + move. Never write directly to the draft file (risk of corruption on crash).

8. **IDE compatibility** -- Target IntelliJ 2025.2+ (build 252+). Use only stable APIs. The plugin works across all JetBrains IDEs (IntelliJ, PyCharm, WebStorm, etc.).
