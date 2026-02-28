# Task 04: ReviewModeService + CommentService

**Phase**: 2 - Markdown Review
**LOC**: ~350
**Dependencies**: Task 02, Task 03
**Verification**: Services can be obtained from project, sessions can be entered/exited, comments can be CRUD'd, listeners are notified

---

## Objective

Implement the two core project-scoped services: `ReviewModeService` (session lifecycle management) and `CommentService` (comment CRUD). Together these form the central nervous system of the plugin — all UI components and actions interact through these services.

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/services/
├── ReviewModeService.kt           # ~180 LOC
└── CommentService.kt              # ~120 LOC

src/main/kotlin/com/uber/jetbrains/reviewplugin/listeners/
└── ReviewModeListener.kt          # ~20 LOC (interface)
```

Also update:
```
src/main/resources/META-INF/plugin.xml   # Register both services
```

---

## What to Implement

### `ReviewModeListener` (interface)

**Reference**: HLD Section 3.3

```kotlin
interface ReviewModeListener {
    fun onReviewModeEntered(session: ReviewSession) {}
    fun onReviewModeExited(session: ReviewSession) {}
    fun onCommentsChanged(session: ReviewSession) {}
    fun onResponsesLoaded(session: ReviewSession) {}
}
```

All methods have default empty implementations so listeners can override only what they need.

### `ReviewModeService` (project-scoped)

**Reference**: HLD Section 3.3, IMPLEMENTATION.md Sections 3.2, 4.1

Register as `@Service(Service.Level.PROJECT)`.

**State:**
- `project: Project`
- `activeReviews: MutableMap<String, ReviewSession>` — key is file path (Markdown) or `"diff-{base}--{compare}"` (Git diff)
- `listeners: MutableList<ReviewModeListener>`

**Session Key Logic:**
- For `MarkdownReviewSession`: key = `session.sourceFilePath` (relative path)
- For `GitDiffReviewSession`: key = `session.getReviewFileName()` (e.g., `"diff-main--feature-auth.review.json"`)

**Public API:**

```
enterMarkdownReview(sourceFilePath: String): MarkdownReviewSession
    → Check if session already exists for this file path
    → If exists and SUSPENDED: reactivate (status = ACTIVE), return it
    → If exists and ACTIVE: return existing session (no-op)
    → Otherwise: create new MarkdownReviewSession, add to activeReviews
    → Notify listeners: onReviewModeEntered
    → Return session

enterDiffReview(baseBranch: String, compareBranch: String, changedFiles: List<String>): GitDiffReviewSession
    → Similar logic to enterMarkdownReview
    → Key is derived from branch names
    → Create GitDiffReviewSession
    → Notify listeners: onReviewModeEntered

exitReview(session: ReviewSession, keepDrafts: Boolean)
    → If keepDrafts:
        → session.status = SUSPENDED
        → StorageManager.saveDrafts(session)
    → Else:
        → StorageManager.deleteDrafts(session.id)
    → Remove from activeReviews
    → Notify listeners: onReviewModeExited

completeReview(session: ReviewSession)
    → session.status = COMPLETED
    → StorageManager.archiveReviewFile(session)
    → StorageManager.deleteDrafts(session.id)
    → Remove from activeReviews
    → Notify listeners: onReviewModeExited

rejectReview(session: ReviewSession)
    → Same as completeReview but status = REJECTED

isInReviewMode(filePath: String): Boolean
    → Check if any active session covers this file
    → For Markdown: exact match on sourceFilePath
    → For GitDiff: check if filePath is in session.changedFiles

getActiveSession(filePath: String): ReviewSession?
    → Return matching active session or null

getAllActiveSessions(): List<ReviewSession>
    → Return all values from activeReviews

restoreSuspendedSession(session: ReviewSession)
    → Add to activeReviews with SUSPENDED status (called during startup)

addListener(listener: ReviewModeListener)
removeListener(listener: ReviewModeListener)
```

**Constraint**: One active session per file. `enterMarkdownReview()` on a file that has an `ACTIVE` session returns the existing session.

### `CommentService` (project-scoped)

**Reference**: IMPLEMENTATION.md Section 3.2

Register as `@Service(Service.Level.PROJECT)`.

**State:**
- `project: Project`

**Public API:**

```
addComment(session: ReviewSession, comment: ReviewComment)
    → session.addComment(comment)
    → StorageManager.saveDrafts(session) (async)
    → ReviewModeService.notifyCommentsChanged(session)

updateComment(session: ReviewSession, commentId: UUID, newText: String)
    → Find comment by ID in session
    → Update commentText
    → StorageManager.saveDrafts(session)
    → Notify listeners

deleteComment(session: ReviewSession, commentId: UUID)
    → session.removeComment(commentId)
    → StorageManager.saveDrafts(session)
    → Notify listeners

getCommentsForFile(session: ReviewSession, filePath: String): List<ReviewComment>
    → Filter session.comments where comment.filePath == filePath

getCommentsForLine(session: ReviewSession, filePath: String, line: Int): List<ReviewComment>
    → Filter where comment.filePath == filePath AND line in startLine..endLine range

applyResponses(session: ReviewSession, reviewFile: ReviewFile)
    → Match reviewFile.comments by index to session.comments (by order)
    → For each match: set claudeResponse, update status (PENDING -> RESOLVED), set resolvedAt
    → StorageManager.saveDrafts(session)
    → ReviewModeService.notifyResponsesLoaded(session)

setCommentStatus(session: ReviewSession, commentId: UUID, status: CommentStatus)
    → Find comment, update status
    → StorageManager.saveDrafts(session)
```

### `plugin.xml` Updates

Add service registrations:

```xml
<projectService
    serviceImplementation="com.uber.jetbrains.reviewplugin.services.ReviewModeService"/>
<projectService
    serviceImplementation="com.uber.jetbrains.reviewplugin.services.CommentService"/>
<projectService
    serviceImplementation="com.uber.jetbrains.reviewplugin.services.StorageManager"/>
```

---

## Key Design Points

1. **Listener pattern**: Services communicate to UI components via `ReviewModeListener`. This avoids direct coupling between services and UI.
2. **Auto-save**: Every comment mutation triggers an async draft save via `StorageManager`.
3. **One session per file**: Enforced by `ReviewModeService` — prevents state conflicts.
4. **Service access pattern**: Use `project.service<ReviewModeService>()` throughout the codebase.

---

## Verification

1. Create `ReviewModeService` and `CommentService` instances manually (or via light test fixtures)
2. `enterMarkdownReview()` creates session, `isInReviewMode()` returns true
3. `exitReview(keepDrafts=true)` sets status to SUSPENDED
4. `exitReview(keepDrafts=false)` removes session entirely
5. `addComment()` / `deleteComment()` / `updateComment()` modify session state
6. `getCommentsForLine()` filters correctly by line range
7. Listener receives `onReviewModeEntered`, `onCommentsChanged` callbacks
8. `completeReview()` and `rejectReview()` trigger archival
