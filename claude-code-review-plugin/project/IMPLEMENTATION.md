# Claude Code Review Plugin - Implementation Design

**Package**: `com.uber.jetbrains.reviewplugin`
**Language**: Kotlin
**Build**: Gradle + `org.jetbrains.intellij.platform` plugin v2
**Target**: IntelliJ Platform 2025.2+

---

## 1. Project Structure

```
claude-code-review-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── src/
    ├── main/
    │   ├── kotlin/com/uber/jetbrains/reviewplugin/
    │   │   ├── model/
    │   │   │   ├── ReviewSessionStatus.kt
    │   │   │   ├── CommentStatus.kt
    │   │   │   ├── ChangeType.kt
    │   │   │   ├── ReviewComment.kt
    │   │   │   ├── ReviewSession.kt              # sealed class (base)
    │   │   │   ├── MarkdownReviewSession.kt      # markdown-specific
    │   │   │   └── GitDiffReviewSession.kt       # diff-specific
    │   │   ├── services/
    │   │   │   ├── ReviewModeService.kt
    │   │   │   ├── CommentService.kt
    │   │   │   ├── StorageManager.kt
    │   │   │   └── ReviewFileManager.kt         # publish/reload .review.json
    │   │   ├── ui/
    │   │   │   ├── ReviewGutterIconProvider.kt
    │   │   │   ├── CommentPopupEditor.kt
    │   │   │   ├── LineHighlighter.kt
    │   │   │   ├── ReviewToolWindowFactory.kt
    │   │   │   ├── ReviewToolWindowPanel.kt
    │   │   │   ├── BranchSelectionDialog.kt
    │   │   │   └── ReviewStatusBarWidgetFactory.kt
    │   │   ├── actions/
    │   │   │   ├── StartMarkdownReviewAction.kt
    │   │   │   ├── StartDiffReviewAction.kt
    │   │   │   ├── AddCommentAction.kt
    │   │   │   ├── PublishReviewAction.kt
    │   │   │   ├── ReloadResponsesAction.kt
    │   │   │   ├── CompleteReviewAction.kt
    │   │   │   └── RejectReviewAction.kt
    │   │   └── listeners/
    │   │       └── ReviewFileWatcher.kt
    │   └── resources/
    │       ├── META-INF/
    │       │   └── plugin.xml
    │       └── icons/
    │           ├── addComment.svg
    │           ├── commentExists.svg
    │           ├── commentResolved.svg
    │           └── reviewMode.svg
    └── test/
        └── kotlin/com/uber/jetbrains/reviewplugin/
            ├── services/
            │   ├── StorageManagerTest.kt
            │   └── ReviewFileManagerTest.kt
            └── model/
                └── ReviewCommentTest.kt
```

---

## 2. Class Glossary

### Model Layer

| Class | Intention |
|-------|-----------|
| `ReviewSession` | Sealed base class representing a review session. Holds session ID, status, comment list, and timestamps. Subclasses carry mode-specific data. Exhaustive `when` matching ensures all review types are handled at compile time |
| `MarkdownReviewSession` | A review session targeting a single Markdown file. Owns `sourceFile: VirtualFile`. Derives review file name from relative path (e.g., `docs--uscorer--ARCHITECTURE_OVERVIEW.review.json`) |
| `GitDiffReviewSession` | A review session targeting a branch diff. Owns `baseBranch`, `compareBranch`, optional commit SHAs, and list of changed files. Derives review file name from branches (e.g., `diff-main--feature-auth.review.json`) |
| `ReviewComment` | A single review comment anchored to a line range in a file. Tracks comment text, selected context, status (draft/pending/resolved), Claude's response, and a `RangeMarker` for auto-adjusting positions on document edits |
| `ReviewSessionStatus` | Enum: `ACTIVE`, `SUSPENDED`, `PUBLISHED`, `COMPLETED`, `REJECTED`. Drives the session lifecycle state machine |
| `CommentStatus` | Enum: `DRAFT`, `PENDING`, `RESOLVED`, `SKIPPED`, `REJECTED`. Tracks per-comment resolution state |
| `ChangeType` | Enum: `ADDED`, `MODIFIED`, `DELETED`. Used only in diff reviews to indicate the kind of change a comment refers to |

### Service Layer

| Class | Intention |
|-------|-----------|
| `ReviewModeService` | Project-scoped singleton that manages the review session lifecycle. Entry point for entering, exiting, completing, and rejecting reviews. Enforces one-session-per-file constraint. Maintains `activeReviews` map and notifies `ReviewModeListener` subscribers on state changes |
| `CommentService` | Project-scoped singleton for comment CRUD. Adds, updates, deletes comments within a session. Applies Claude's responses from a loaded `ReviewFile` by matching on comment index. Triggers draft persistence and UI refresh on every mutation |
| `StorageManager` | Project-scoped singleton for file I/O. Persists draft sessions as JSON to `.review/.drafts/`, restores them on IDE restart, archives completed/rejected review files to `.review/archives/` with a random suffix, and manages the `.review/` directory structure including `.gitignore` auto-management |
| `ReviewFileManager` | Stateless utility for the `.review.json` publish/load contract. Serializes a session's comments into the structured JSON format for CLI consumption, deserializes Claude's responses back, and handles reply appending. This is the sole bridge between the plugin and `review-cli` |

### UI Layer

| Class | Intention |
|-------|-----------|
| `ReviewGutterIconProvider` | `LineMarkerProvider` that renders gutter icons ("+" for uncommented lines, chat/clock/check for commented lines) only when review mode is active. Fast-path returns `null` for non-reviewed files to avoid performance impact |
| `CommentPopupEditor` | Popup dialog for creating and editing comments. Shows a text area, selected context preview, and Save/Cancel/Delete buttons. Built with `JBPopupFactory` |
| `LineHighlighter` | Applies colored background highlighting to commented lines using `MarkupModel.addRangeHighlighter()`. Color varies by status: yellow for draft, blue for pending, green for resolved |
| `ReviewToolWindowFactory` | `ToolWindowFactory` that creates the "Claude Code Review" side panel. Delegates content rendering to `ReviewToolWindowPanel` |
| `ReviewToolWindowPanel` | Main panel content for the tool window. Lists draft/published comments, provides Publish/Reload/Complete/Reject buttons, shows Claude's responses inline with reply capability, and supports jump-to-comment navigation |
| `BranchSelectionDialog` | `DialogWrapper` for selecting base and compare branches when starting a diff review. Uses `GitBranchUtil` to list branches and shows changed file count |
| `ReviewStatusBarWidgetFactory` | Factory for the status bar widget. Creates `ReviewStatusBarWidget` instances |
| `ReviewStatusBarWidget` | Status bar indicator showing "Review Mode: Active | N drafts" when a review session is active |

### Actions Layer

| Class | Intention |
|-------|-----------|
| `StartMarkdownReviewAction` | Context menu action ("Review this Markdown") on `.md` files. Calls `ReviewModeService.enterMarkdownReview()`. Visible only for `.md` files not already in review mode |
| `StartDiffReviewAction` | VCS menu action ("Review the Diff"). Opens `BranchSelectionDialog`, then calls `ReviewModeService.enterDiffReview()`. Visible only when project has a Git repository |
| `AddCommentAction` | Editor context menu action to add a comment at the current cursor/selection. Opens `CommentPopupEditor` |
| `PublishReviewAction` | Serializes all draft comments to `.review.json` via `ReviewFileManager.publish()` and copies the Claude command to clipboard. Enabled only when draft comments exist |
| `ReloadResponsesAction` | Reloads Claude's responses from the `.review.json` file via `ReviewFileManager.load()` and applies them to the session. Enabled when the file has been modified since last load |
| `CompleteReviewAction` | Marks the session as `COMPLETED`, archives the review file, and cleans up. Enabled when session is `ACTIVE` or `PUBLISHED` |
| `RejectReviewAction` | Marks the session as `REJECTED`, archives the review file, and cleans up. Same lifecycle as Complete but indicates the review was discarded |

### Listeners Layer

| Class | Intention |
|-------|-----------|
| `ReviewFileWatcher` | `VirtualFileListener` that watches `.review/` for external modifications (Claude/review-cli writing responses). Ignores draft file changes and the plugin's own saves. Triggers a notification balloon when a response is detected |
| `ReviewFileWatcherStartup` | `PostStartupActivity` that registers `ReviewFileWatcher` on project open and restores any suspended draft sessions from `.review/.drafts/` |

### Data Transfer Objects

| Class | Intention |
|-------|-----------|
| `ReviewFile` | Serialization model for the `.review.json` file. Contains session ID, type, metadata, and indexed comment list. The sole communication contract between plugin and `review-cli` |
| `ReviewFileComment` | Single comment entry within `ReviewFile`. Carries index, file path, line range, user comment, Claude's response, status, and reply thread |
| `ReviewMetadata` | Metadata block within `ReviewFile`. Contains author, timestamp, and type-specific fields (source file for Markdown, branches/commits for diff) |

---

## 3. Class Diagrams

### 3.1 Model Layer

```mermaid
classDiagram
    direction TB

    class ReviewSessionStatus {
        <<enumeration>>
        ACTIVE "User is commenting"
        SUSPENDED "Exited with drafts kept"
        PUBLISHED "Comments exported to .review.json"
        COMPLETED "Review done, triggers archival"
        REJECTED "Review rejected, triggers archival"
    }

    class CommentStatus {
        <<enumeration>>
        DRAFT
        PENDING
        RESOLVED
        SKIPPED
        REJECTED
    }

    class ChangeType {
        <<enumeration>>
        ADDED
        MODIFIED
        DELETED
    }

    class ReviewComment {
        +id: UUID
        +filePath: String
        +startLine: Int
        +endLine: Int
        +selectedText: String
        +commentText: String
        +authorId: String
        +createdAt: Instant
        +status: CommentStatus
        +claudeResponse: String?
        +resolvedAt: Instant?
        +changeType: ChangeType?
        +rangeMarker: RangeMarker?
    }

    class ReviewSession {
        <<sealed>>
        +id: UUID
        +status: ReviewSessionStatus
        +comments: MutableList~ReviewComment~
        +createdAt: Instant
        +publishedAt: Instant?
        +reviewFilePath: String?
        +addComment(comment: ReviewComment)
        +removeComment(commentId: UUID)
        +getComment(commentId: UUID): ReviewComment?
        +getDraftComments(): List~ReviewComment~
        +getPendingComments(): List~ReviewComment~
        +getReviewFileName(): String*
        +getDisplayName(): String*
    }

    class MarkdownReviewSession {
        +sourceFile: VirtualFile
        +getReviewFileName(): String
        +getDisplayName(): String
    }

    class GitDiffReviewSession {
        +baseBranch: String
        +compareBranch: String
        +baseCommit: String?
        +compareCommit: String?
        +changedFiles: List~String~
        +getReviewFileName(): String
        +getDisplayName(): String
    }

    ReviewSession <|-- MarkdownReviewSession
    ReviewSession <|-- GitDiffReviewSession
    ReviewSession "1" *-- "0..*" ReviewComment : contains
    ReviewSession --> ReviewSessionStatus
    ReviewComment --> CommentStatus
    ReviewComment --> ChangeType
```

**Key design points:**

- `ReviewSession` is a **sealed class** — exhaustive `when` matching in Kotlin ensures all branches are handled at compile time. No `ReviewType` enum needed; the type hierarchy replaces it
- `MarkdownReviewSession` owns `sourceFile: VirtualFile` (non-nullable, always required)
- `GitDiffReviewSession` owns `baseBranch`, `compareBranch` (non-nullable), plus optional `baseCommit`/`compareCommit` and `changedFiles`
- No nullable fields for "the other mode" — each subclass only carries its own data
- `getReviewFileName()` is abstract — each subclass provides its own naming logic:
  - `MarkdownReviewSession` → `"ARCHITECTURE_OVERVIEW.review.json"` (derived from source file name)
  - `GitDiffReviewSession` → `"diff-main-feature-auth-20260212.review.json"` (derived from branches + timestamp)
- `getDisplayName()` is abstract — used by `ReviewToolWindowPanel` and `ReviewStatusBarWidget`:
  - `MarkdownReviewSession` → `"Markdown: ARCHITECTURE_OVERVIEW.md"`
  - `GitDiffReviewSession` → `"Diff: main → feature-auth"`
- Adding a new review type (e.g., `PRReviewSession`) requires adding a new subclass — the compiler flags every `when` that needs updating

### 3.2 Service Layer

```mermaid
classDiagram
    direction TB

    class ReviewModeService {
        <<@Service Level.PROJECT>>
        -project: Project
        -activeReviews: MutableMap~String, ReviewSession~
        -listeners: MutableList~ReviewModeListener~
        +enterMarkdownReview(file: VirtualFile): MarkdownReviewSession
        +enterDiffReview(baseBranch: String, compareBranch: String): GitDiffReviewSession
        +exitReview(session: ReviewSession, keepDrafts: Boolean)
        +completeReview(session: ReviewSession)
        +rejectReview(session: ReviewSession)
        +isInReviewMode(file: VirtualFile): Boolean
        +getActiveSession(file: VirtualFile): ReviewSession?
        +getAllActiveSessions(): List~ReviewSession~
        +addListener(listener: ReviewModeListener)
        +removeListener(listener: ReviewModeListener)
        -notifyReviewEntered(session: ReviewSession)
        -notifyReviewExited(session: ReviewSession)
        -notifyCommentsChanged(session: ReviewSession)
    }

    class ReviewModeListener {
        <<interface>>
        +onReviewModeEntered(session: ReviewSession)
        +onReviewModeExited(session: ReviewSession)
        +onCommentsChanged(session: ReviewSession)
        +onResponsesLoaded(session: ReviewSession)
    }

    class CommentService {
        <<@Service Level.PROJECT>>
        -project: Project
        -storageManager: StorageManager
        +addComment(session: ReviewSession, comment: ReviewComment)
        +updateComment(session: ReviewSession, commentId: UUID, text: String)
        +deleteComment(session: ReviewSession, commentId: UUID)
        +getCommentsForFile(filePath: String): List~ReviewComment~
        +getCommentsForLine(filePath: String, line: Int): List~ReviewComment~
        +applyResponses(session: ReviewSession, reviewFile: ReviewFile)
        +setCommentStatus(commentId: UUID, status: CommentStatus)
    }

    class StorageManager {
        <<@Service Level.PROJECT>>
        -project: Project
        -draftsDir: Path
        +saveDrafts(session: ReviewSession)
        +loadDrafts(): List~ReviewSession~
        +deleteDrafts(sessionId: UUID)
        +archiveReviewFile(session: ReviewSession)
        +ensureReviewDirectory()
        +ensureArchiveDirectory()
        +getReviewDirectory(): Path
        -serializeSession(session: ReviewSession): String
        -deserializeSession(json: String): ReviewSession
    }

    class ReviewFileManager {
        +publish(session: ReviewSession, outputDir: Path): Path
        +load(reviewFilePath: Path): ReviewFile
        +appendReply(reviewFilePath: Path, commentIndex: Int, reply: Reply)
        -buildReviewFile(session: ReviewSession): ReviewFile
        -generateCliCommand(reviewFilePath: String): String
    }

    class ReviewFile {
        +sessionId: UUID
        +type: String
        +metadata: ReviewMetadata
        +comments: List~ReviewFileComment~
        +toJson(): String
    }

    class ReviewFileComment {
        +index: Int
        +filePath: String
        +startLine: Int
        +endLine: Int
        +selectedText: String
        +userComment: String
        +status: String
        +claudeResponse: String?
        +replies: List~Reply~
    }

    ReviewModeService --> CommentService : uses
    ReviewModeService --> StorageManager : uses
    ReviewModeService --> ReviewModeListener : notifies
    CommentService --> StorageManager : persists via
    ReviewModeService --> ReviewSession : manages
    CommentService --> ReviewComment : CRUD
    ReviewFileManager --> ReviewSession : reads
    ReviewFileManager --> ReviewFile : produces
    ReviewFile "1" *-- "1..*" ReviewFileComment : contains
```

### 3.3 UI Layer

> **Note**: The plugin overlays on two **existing** editor surfaces provided by bundled plugins:
> - **Markdown editor** (source view) — from `org.intellij.plugins.markdown`
> - **Diff viewer** (split-pane) — from `Git4Idea` via `DiffManager.showDiff()`
>
> The classes below are **our custom UI components** that add review functionality on top of those editors. We do not reimplement any editor or diff viewer — we attach gutter icons, highlights, and popups to the existing editor instances.

```mermaid
classDiagram
    direction TB

    class ReviewGutterIconProvider {
        <<LineMarkerProvider>>
        +getLineMarkerInfo(element: PsiElement): LineMarkerInfo?
        -getIconForLine(file: VirtualFile, line: Int): Icon
        -createClickHandler(file: VirtualFile, line: Int): GutterIconNavigationHandler
    }

    class CommentPopupEditor {
        -session: ReviewSession
        -filePath: String
        -startLine: Int
        -endLine: Int
        -selectedText: String
        -existingComment: ReviewComment?
        +show(editor: Editor, point: Point)
        -createPopupContent(): JPanel
        -onSave(commentText: String)
        -onCancel()
        -onDelete()
    }

    class LineHighlighter {
        -project: Project
        -highlighters: MutableMap~String, List~RangeHighlighter~~
        +applyHighlights(editor: Editor, comments: List~ReviewComment~)
        +clearHighlights(editor: Editor)
        +refreshHighlights(editor: Editor)
        -getColorForStatus(status: CommentStatus): TextAttributes
    }

    class ReviewToolWindowFactory {
        <<ToolWindowFactory>>
        +createToolWindowContent(project: Project, toolWindow: ToolWindow)
    }

    class ReviewToolWindowPanel {
        -project: Project
        -reviewModeService: ReviewModeService
        -commentService: CommentService
        +updateContent()
        +showDraftComments(session: ReviewSession)
        +showPublishedComments(session: ReviewSession)
        +showResponseNotification()
        -createDraftCommentRow(comment: ReviewComment): JPanel
        -createResolvedCommentRow(comment: ReviewComment): JPanel
        -onPublishClicked()
        -onReloadClicked()
        -onJumpToComment(comment: ReviewComment)
    }

    class BranchSelectionDialog {
        <<DialogWrapper>>
        %% Custom dialog — Git4Idea provides branch listing APIs
        %% (GitRepositoryManager, GitBranchUtil) but no review-aware picker
        -project: Project
        -baseBranchCombo: ComboBox~String~
        -compareBranchCombo: ComboBox~String~
        -changedFilesLabel: JLabel
        +getBaseBranch(): String
        +getCompareBranch(): String
        #createCenterPanel(): JComponent
        #doOKAction()
        -loadBranches()
        -updateChangedFilesCount()
    }

    class ReviewStatusBarWidgetFactory {
        <<StatusBarWidgetFactory>>
        +getId(): String
        +getDisplayName(): String
        +createWidget(project: Project): StatusBarWidget
    }

    class ReviewStatusBarWidget {
        <<StatusBarWidget.TextPresentation>>
        -project: Project
        +getText(): String
        +getTooltipText(): String
        +getClickConsumer(): Consumer~MouseEvent~?
    }

    ReviewToolWindowFactory --> ReviewToolWindowPanel : creates
    ReviewToolWindowPanel --> CommentPopupEditor : opens
    ReviewGutterIconProvider --> CommentPopupEditor : opens on click
    ReviewStatusBarWidgetFactory --> ReviewStatusBarWidget : creates
```

### 3.4 Actions Layer

```mermaid
classDiagram
    direction TB

    class StartMarkdownReviewAction {
        <<AnAction>>
        +actionPerformed(e: AnActionEvent)
        +update(e: AnActionEvent)
        -isMarkdownFile(file: VirtualFile): Boolean
    }

    class StartDiffReviewAction {
        <<AnAction>>
        +actionPerformed(e: AnActionEvent)
        +update(e: AnActionEvent)
        -hasGitRepository(project: Project): Boolean
    }

    class AddCommentAction {
        <<AnAction>>
        +actionPerformed(e: AnActionEvent)
        +update(e: AnActionEvent)
    }

    class PublishReviewAction {
        <<AnAction>>
        +actionPerformed(e: AnActionEvent)
        +update(e: AnActionEvent)
        -hasDraftComments(project: Project): Boolean
    }

    class ReloadResponsesAction {
        <<AnAction>>
        +actionPerformed(e: AnActionEvent)
        +update(e: AnActionEvent)
        -hasPublishedReview(project: Project): Boolean
    }

    class CompleteReviewAction {
        <<AnAction>>
        +actionPerformed(e: AnActionEvent)
        +update(e: AnActionEvent)
        -isActiveOrPublished(project: Project): Boolean
    }

    class RejectReviewAction {
        <<AnAction>>
        +actionPerformed(e: AnActionEvent)
        +update(e: AnActionEvent)
        -isActiveOrPublished(project: Project): Boolean
    }

    StartMarkdownReviewAction --> ReviewModeService : calls enterMarkdownReview
    StartDiffReviewAction --> BranchSelectionDialog : opens
    StartDiffReviewAction --> ReviewModeService : calls enterDiffReview
    AddCommentAction --> CommentPopupEditor : opens
    PublishReviewAction --> ReviewFileManager : calls publish
    ReloadResponsesAction --> ReviewFileManager : calls load
    CompleteReviewAction --> ReviewModeService : calls completeReview
    RejectReviewAction --> ReviewModeService : calls rejectReview
```

### 3.5 Listeners Layer

```mermaid
classDiagram
    direction TB

    class ReviewFileWatcher {
        <<VirtualFileListener>>
        -project: Project
        -reviewModeService: ReviewModeService
        +contentsChanged(event: VirtualFileEvent)
        +fileCreated(event: VirtualFileEvent)
        -isReviewFile(file: VirtualFile): Boolean
        -notifyResponsesAvailable(file: VirtualFile)
    }

    class ReviewFileWatcherStartup {
        <<ProjectActivity>>
        +execute(project: Project)
    }

    ReviewFileWatcherStartup --> ReviewFileWatcher : registers
    ReviewFileWatcher --> ReviewModeService : notifies
```

### 3.6 Full System Class Diagram

```mermaid
classDiagram
    direction LR

    %% Models
    class ReviewSession {
        <<sealed>>
        +id: UUID
        +status: ReviewSessionStatus
        +comments: MutableList~ReviewComment~
        +reviewFilePath: String?
        +getReviewFileName(): String*
        +getDisplayName(): String*
    }

    class MarkdownReviewSession {
        +sourceFile: VirtualFile
        +getReviewFileName(): String
        +getDisplayName(): String
    }

    class GitDiffReviewSession {
        +baseBranch: String
        +compareBranch: String
        +baseCommit: String?
        +compareCommit: String?
        +changedFiles: List~String~
        +getReviewFileName(): String
        +getDisplayName(): String
    }

    class ReviewComment {
        +id: UUID
        +filePath: String
        +startLine: Int
        +endLine: Int
        +selectedText: String
        +commentText: String
        +status: CommentStatus
        +claudeResponse: String?
        +changeType: ChangeType?
    }

    %% Services
    class ReviewModeService {
        +enterMarkdownReview(file): MarkdownReviewSession
        +enterDiffReview(base, compare): GitDiffReviewSession
        +exitReview(session, keepDrafts)
        +completeReview(session)
        +rejectReview(session)
        +isInReviewMode(file): Boolean
        +getActiveSession(file): ReviewSession?
    }

    class CommentService {
        +addComment(session, comment)
        +updateComment(session, id, text)
        +deleteComment(session, id)
        +getCommentsForLine(file, line): List
        +applyResponses(session, reviewFile)
    }

    class StorageManager {
        +saveDrafts(session)
        +loadDrafts(): List~ReviewSession~
        +deleteDrafts(sessionId)
        +archiveReviewFile(session)
    }

    class ReviewFileManager {
        +publish(session, outputDir): Path
        +load(reviewFilePath): ReviewFile
        +appendReply(reviewFilePath, commentIndex, reply)
    }

    %% UI
    class ReviewGutterIconProvider {
        +getLineMarkerInfo(element): LineMarkerInfo?
    }

    class CommentPopupEditor {
        +show(editor, point)
    }

    class ReviewToolWindowPanel {
        +updateContent()
        +showDraftComments(session)
        +showPublishedComments(session)
    }

    %% Actions
    class StartMarkdownReviewAction {
        +actionPerformed(e)
    }

    class StartDiffReviewAction {
        +actionPerformed(e)
    }

    class PublishReviewAction {
        +actionPerformed(e)
    }

    class ReloadResponsesAction {
        +actionPerformed(e)
    }

    class CompleteReviewAction {
        +actionPerformed(e)
    }

    class RejectReviewAction {
        +actionPerformed(e)
    }

    %% Listeners
    class ReviewFileWatcher {
        +contentsChanged(event)
    }

    %% Relationships
    ReviewSession <|-- MarkdownReviewSession
    ReviewSession <|-- GitDiffReviewSession
    ReviewSession "1" *-- "0..*" ReviewComment

    ReviewModeService --> ReviewSession
    ReviewModeService --> CommentService
    ReviewModeService --> StorageManager

    CommentService --> ReviewComment
    CommentService --> StorageManager

    ReviewGutterIconProvider --> ReviewModeService
    ReviewGutterIconProvider --> CommentService
    ReviewGutterIconProvider ..> CommentPopupEditor : opens

    ReviewToolWindowPanel --> ReviewModeService
    ReviewToolWindowPanel --> CommentService
    ReviewToolWindowPanel --> ReviewFileManager

    StartMarkdownReviewAction --> ReviewModeService
    StartDiffReviewAction --> ReviewModeService
    PublishReviewAction --> ReviewFileManager
    ReloadResponsesAction --> ReviewFileManager
    CompleteReviewAction --> ReviewModeService
    RejectReviewAction --> ReviewModeService

    ReviewFileWatcher --> ReviewModeService
```

---

## 4. Sequence Diagrams

### 4.1 Start Markdown Review

```mermaid
sequenceDiagram
    actor User
    participant Action as StartMarkdownReviewAction
    participant RMS as ReviewModeService
    participant SM as StorageManager
    participant GIP as ReviewGutterIconProvider
    participant TW as ReviewToolWindowPanel
    participant SB as ReviewStatusBarWidget

    User->>Action: Right-click .md file → "Review this Markdown"
    Action->>Action: update(): verify file is .md and not in review mode
    Action->>RMS: enterMarkdownReview(virtualFile)
    RMS->>SM: loadDrafts() — check for existing suspended session
    alt Suspended session exists
        SM-->>RMS: restored MarkdownReviewSession (SUSPENDED)
        RMS->>RMS: session.status = ACTIVE
    else No prior session
        RMS->>RMS: create new MarkdownReviewSession(sourceFile)
    end
    RMS->>RMS: activeReviews[filePath] = session
    RMS-->>Action: session
    RMS->>GIP: notifyReviewEntered(session) → triggers DaemonCodeAnalyzer.restart()
    RMS->>TW: notifyReviewEntered(session) → show draft panel
    RMS->>SB: notifyReviewEntered(session) → "Review Mode: Active | 0 drafts"
    Note over GIP: Editor gutter now shows "+" icons on every line
```

### 4.2 Add Comment

```mermaid
sequenceDiagram
    actor User
    participant Editor as IntelliJ Editor
    participant GIP as ReviewGutterIconProvider
    participant Popup as CommentPopupEditor
    participant CS as CommentService
    participant SM as StorageManager
    participant LH as LineHighlighter
    participant TW as ReviewToolWindowPanel

    User->>Editor: Click "+" icon in gutter (or select text → right-click → Add Comment)
    Editor->>GIP: gutter click handler invoked
    GIP->>GIP: Capture startLine, endLine, selectedText from editor
    GIP->>Popup: show(editor, clickPoint)
    Popup->>Popup: Display comment dialog with context preview
    User->>Popup: Type comment text, click "Save"
    Popup->>CS: addComment(session, ReviewComment)
    CS->>CS: session.comments.add(comment)
    CS->>SM: saveDrafts(session) — async on pooled thread
    SM->>SM: Write session-{uuid}.json to .review/.drafts/
    CS->>LH: applyHighlights(editor, updatedComments)
    LH->>Editor: addRangeHighlighter() — yellow background on commented line
    CS->>TW: onCommentsChanged(session) — refresh draft list
    CS->>GIP: DaemonCodeAnalyzer.restart() — "+" icon changes to chat bubble
```

### 4.3 Publish Review

```mermaid
sequenceDiagram
    actor User
    participant TW as ReviewToolWindowPanel
    participant CS as CommentService
    participant RFM as ReviewFileManager
    participant SM as StorageManager
    participant Clipboard as System Clipboard
    participant SB as ReviewStatusBarWidget

    User->>TW: Click "Publish Review" button
    TW->>CS: getDraftComments(session)
    CS-->>TW: List of ReviewComment (N drafts)
    TW->>RFM: publish(session, .review/)
    RFM->>RFM: buildReviewFile(session) — sealed class dispatch
    alt session is MarkdownReviewSession
        RFM->>RFM: Set metadata.sourceFile = session.sourceFile.path
    else session is GitDiffReviewSession
        RFM->>RFM: Set metadata.baseBranch, compareBranch, commits
    end
    RFM->>RFM: Convert comments to ReviewFileComment list (indexed)
    RFM->>RFM: Serialize ReviewFile to JSON
    RFM->>RFM: Write .review/name.review.json
    RFM-->>TW: reviewFilePath (.review/name.review.json)

    TW->>RMS: setSessionStatus(session, PUBLISHED)
    RMS->>RMS: session.status = PUBLISHED
    TW->>CS: setAllDraftsToPending(session)
    CS->>SM: saveDrafts(session) — status now PENDING
    TW->>Clipboard: Copy: claude "/review-respond .review/name.review.json"
    TW-->>User: Notification: "Review published. Command copied to clipboard."
    TW->>SB: Update status bar: Published, N pending
```

### 4.4 Claude Processes via CLI and User Reloads Responses

```mermaid
sequenceDiagram
    actor User
    participant Terminal as Terminal
    participant Claude as Claude Code
    participant CLI as review-cli
    participant Disk as .review/name.review.json
    participant FW as ReviewFileWatcher
    participant TW as ReviewToolWindowPanel
    participant RFM as ReviewFileManager
    participant CS as CommentService
    participant GIP as ReviewGutterIconProvider
    participant LH as LineHighlighter

    User->>Terminal: claude (with /review-respond skill active)
    Claude->>CLI: review-cli list .review/name.review.json
    CLI->>Disk: Read JSON
    CLI-->>Claude: List of pending comments with index, file, lines, text

    loop For each pending comment
        Claude->>Claude: Research source file at specified lines
        Claude->>Claude: Analyze and generate response
        Claude->>CLI: review-cli respond .review/name.review.json --comment 1 --response "..."
        CLI->>Disk: Update comment 1: status=resolved, claudeResponse="..."
        CLI-->>Claude: OK, comment 1 resolved
    end

    Disk-->>FW: VirtualFileListener.contentsChanged()
    FW->>FW: isReviewFile(file)? true
    FW->>TW: Show notification: Claude responded. Reload?

    User->>TW: Click "Reload Responses"
    TW->>RFM: load(reviewFilePath)
    RFM->>Disk: Read JSON
    RFM->>RFM: Deserialize to ReviewFile
    RFM-->>TW: ReviewFile with responses populated

    TW->>CS: applyResponses(session, reviewFile)
    CS->>CS: Match reviewFile.comments[i] to session.comments[i]
    CS->>CS: Set comment.claudeResponse = response text
    CS->>CS: Set comment.status = RESOLVED
    CS->>GIP: DaemonCodeAnalyzer.restart() — icons update
    CS->>LH: refreshHighlights() — color changes to green
    CS->>TW: Refresh panel — show Claude's responses inline
```

### 4.5 Start Git Diff Review

```mermaid
sequenceDiagram
    actor User
    participant Action as StartDiffReviewAction
    participant Dialog as BranchSelectionDialog
    participant Git as Git4Idea API
    participant RMS as ReviewModeService
    participant DiffMgr as DiffManager
    participant GIP as ReviewGutterIconProvider

    User->>Action: VCS menu → "Review the Diff"
    Action->>Action: update(): verify project has Git repo
    Action->>Dialog: show BranchSelectionDialog

    Dialog->>Git: GitRepositoryManager.getRepositories()
    Git-->>Dialog: List<GitRepository>
    Dialog->>Git: GitBranchUtil.getBranches(repo)
    Git-->>Dialog: List<String> (local + remote branches)
    Dialog->>Dialog: Populate baseBranch and compareBranch dropdowns

    User->>Dialog: Select base=main, compare=feature-auth
    Dialog->>Git: GitLineHandler("diff", "--stat", "main...feature-auth")
    Git-->>Dialog: Changed files with +X -Y stats
    Dialog->>Dialog: Show: "5 files changed (+247 -89)"

    User->>Dialog: Click OK
    Dialog->>Git: Compute full Change list (committed + uncommitted)
    Dialog->>RMS: enterDiffReview(main, feature-auth)
    RMS->>RMS: Create GitDiffReviewSession(baseBranch, compareBranch, changedFiles)
    RMS-->>Dialog: session

    Dialog->>DiffMgr: Build DiffRequestChain from changes
    Dialog->>DiffMgr: DiffManager.showDiff(project, requestChain)
    Note over DiffMgr: Split diff view opens with changed files

    RMS->>GIP: notifyReviewEntered(session)
    Note over GIP: "+" icons appear on changed lines in diff view
```

### 4.6 Comment Thread (Reply Flow)

```mermaid
sequenceDiagram
    actor User
    participant TW as ReviewToolWindowPanel
    participant CS as CommentService
    participant CLI as review-cli
    participant Disk as .review/name.review.json
    participant Claude as Claude Code
    participant FW as ReviewFileWatcher
    participant RFM as ReviewFileManager

    Note over TW: User sees Claude's resolved response for Comment 1

    User->>TW: Click "Reply" on Comment 1
    TW->>TW: Open reply text area
    User->>TW: Type follow-up question, click "Send"

    TW->>RFM: appendReply(reviewFilePath, commentIndex=1, Reply("vinay.yerra", now(), "What about caching?"))
    RFM->>Disk: Read .review/name.review.json
    RFM->>RFM: Deserialize, append reply to comment 1, set status = pending
    RFM->>Disk: Write updated .review/name.review.json
    RFM-->>TW: OK

    TW->>CS: setCommentStatus(comment1.id, PENDING)

    User->>Claude: Re-invoke Claude with /review-respond skill
    Claude->>CLI: review-cli list ... (finds pending comment 1)
    Claude->>CLI: review-cli respond ... --comment 1 --response "..."
    CLI->>Disk: Append response to replies, set status resolved

    FW->>TW: Notification: Claude responded. Reload?
    User->>TW: Click "Reload"
    TW->>RFM: load(reviewFilePath)
    RFM-->>CS: Updated review file with reply thread
    CS->>TW: Refresh — show threaded conversation
```

### 4.7 Exit Review (with Draft Persistence)

```mermaid
sequenceDiagram
    actor User
    participant RMS as ReviewModeService
    participant SM as StorageManager
    participant GIP as ReviewGutterIconProvider
    participant LH as LineHighlighter
    participant TW as ReviewToolWindowPanel
    participant SB as ReviewStatusBarWidget

    User->>RMS: Exit Review (via menu or toolbar)

    alt Keep drafts
        RMS->>SM: saveDrafts(session) — persist current state
        RMS->>RMS: session.status = SUSPENDED
        RMS->>RMS: activeReviews.remove(filePath)
        Note over SM: Drafts saved to .review/.drafts/session-{uuid}.json<br/>Will restore when review mode re-entered on same file
    else Discard drafts
        RMS->>SM: deleteDrafts(session.id)
        RMS->>RMS: activeReviews.remove(filePath)
    end

    RMS->>GIP: notifyReviewExited(session) → DaemonCodeAnalyzer.restart()
    Note over GIP: Gutter icons disappear
    RMS->>LH: clearHighlights(editor)
    Note over LH: Background colors removed
    RMS->>TW: notifyReviewExited(session) → clear panel
    RMS->>SB: Update: status bar returns to normal
```

### 4.8 Complete / Reject Review (Archival)

```mermaid
sequenceDiagram
    actor User
    participant TW as ReviewToolWindowPanel
    participant RMS as ReviewModeService
    participant SM as StorageManager
    participant CS as CommentService
    participant GIP as ReviewGutterIconProvider
    participant LH as LineHighlighter
    participant SB as ReviewStatusBarWidget

    User->>TW: Click "Complete Review" (or "Reject Review")

    TW->>RMS: completeReview(session) (or rejectReview(session))
    RMS->>RMS: session.status = COMPLETED (or REJECTED)

    alt Published .review.json exists
        RMS->>SM: archiveReviewFile(session)
        SM->>SM: ensureArchiveDirectory()
        SM->>SM: suffix = randomString(5, "a-z0-9")
        SM->>SM: archiveName = name-{suffix}.review.json
        SM->>SM: Move .review/name.review.json → .review/archives/archiveName
        SM-->>RMS: archived
    end

    RMS->>SM: deleteDrafts(session.id)
    SM->>SM: Delete .review/.drafts/session-{uuid}.json

    RMS->>RMS: activeReviews.remove(sessionKey)

    RMS->>GIP: notifyReviewExited(session) → DaemonCodeAnalyzer.restart()
    Note over GIP: Gutter icons disappear
    RMS->>LH: clearHighlights(editor)
    RMS->>TW: notifyReviewExited(session) → clear panel
    RMS->>SB: Update: status bar returns to normal

    Note over RMS: Review file name is now freed —<br/>a new session on the same file reuses the same name
```

### 4.9 IDE Restart / Draft Restore

```mermaid
sequenceDiagram
    participant IDE as IntelliJ Platform
    participant Startup as ReviewFileWatcherStartup<br/>(PostStartupActivity)
    participant FW as ReviewFileWatcher
    participant RMS as ReviewModeService
    participant SM as StorageManager
    participant TW as ReviewToolWindowPanel
    participant SB as ReviewStatusBarWidget

    IDE->>Startup: execute(project) — after project opens
    Startup->>FW: Register VirtualFileListener on .review/ directory

    Startup->>SM: loadDrafts()
    SM->>SM: Scan .review/.drafts/ for session-*.json files
    SM->>SM: Deserialize each JSON → ReviewSession (SUSPENDED)
    SM-->>Startup: List<ReviewSession>

    loop For each restored session
        Startup->>RMS: restoreSuspendedSession(session)
        RMS->>RMS: activeReviews[sessionKey] = session (status remains SUSPENDED)
    end

    alt User opens a file with a suspended session
        RMS->>RMS: session.status = ACTIVE
        RMS->>TW: notifyReviewEntered(session) → show draft panel
        RMS->>SB: Update: "Review Mode: Active | N drafts"
        Note over RMS: Gutter icons appear when editor opens for the file
    end
```

---

## 5. Plugin Descriptor (plugin.xml)

```xml
<idea-plugin>
    <id>com.uber.jetbrains.reviewplugin</id>
    <name>Claude Code Review</name>
    <vendor>Vinay Yerra</vendor>
    <description>Inline review comments with Claude Code integration</description>

    <!-- Dependencies -->
    <depends>com.intellij.modules.platform</depends>
    <depends>Git4Idea</depends>
    <depends>org.intellij.plugins.markdown</depends>

    <extensions defaultExtensionNs="com.intellij">
        <!-- Project-level Services -->
        <projectService
            serviceImplementation="com.uber.jetbrains.reviewplugin.services.ReviewModeService"/>
        <projectService
            serviceImplementation="com.uber.jetbrains.reviewplugin.services.CommentService"/>
        <projectService
            serviceImplementation="com.uber.jetbrains.reviewplugin.services.StorageManager"/>

        <!-- Gutter Icons -->
        <codeInsight.lineMarkerProvider language=""
            implementationClass="com.uber.jetbrains.reviewplugin.ui.ReviewGutterIconProvider"/>

        <!-- Tool Window -->
        <toolWindow id="Claude Code Review" anchor="right" secondary="true"
            factoryClass="com.uber.jetbrains.reviewplugin.ui.ReviewToolWindowFactory"/>

        <!-- Status Bar -->
        <statusBarWidgetFactory id="ReviewModeStatus"
            implementation="com.uber.jetbrains.reviewplugin.ui.ReviewStatusBarWidgetFactory"/>

        <!-- File Watcher Startup -->
        <postStartupActivity
            implementation="com.uber.jetbrains.reviewplugin.listeners.ReviewFileWatcherStartup"/>

        <!-- Notification Group -->
        <notificationGroup id="ReviewPlugin"
            displayType="BALLOON"/>
    </extensions>

    <actions>
        <!-- Markdown Review -->
        <action id="ReviewPlugin.StartMarkdownReview"
                class="com.uber.jetbrains.reviewplugin.actions.StartMarkdownReviewAction"
                text="Review this Markdown"
                description="Start inline review of this Markdown file">
            <add-to-group group-id="EditorPopupMenu" anchor="last"/>
            <add-to-group group-id="ProjectViewPopupMenu" anchor="last"/>
            <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift R"/>
        </action>

        <!-- Diff Review -->
        <action id="ReviewPlugin.StartDiffReview"
                class="com.uber.jetbrains.reviewplugin.actions.StartDiffReviewAction"
                text="Review the Diff"
                description="Review branch changes with inline comments">
            <add-to-group group-id="VcsGroups" anchor="last"/>
            <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift D"/>
        </action>

        <!-- Add Comment -->
        <action id="ReviewPlugin.AddComment"
                class="com.uber.jetbrains.reviewplugin.actions.AddCommentAction"
                text="Add Review Comment"
                description="Add a review comment at the current line">
            <add-to-group group-id="EditorPopupMenu" anchor="last"/>
            <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift C"/>
        </action>

        <!-- Publish -->
        <action id="ReviewPlugin.PublishReview"
                class="com.uber.jetbrains.reviewplugin.actions.PublishReviewAction"
                text="Publish Review"
                description="Publish all comments to .review/ file">
            <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift P"/>
        </action>

        <!-- Reload -->
        <action id="ReviewPlugin.ReloadResponses"
                class="com.uber.jetbrains.reviewplugin.actions.ReloadResponsesAction"
                text="Reload Responses"
                description="Reload Claude responses from .review/ file">
            <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift L"/>
        </action>

        <!-- Complete Review -->
        <action id="ReviewPlugin.CompleteReview"
                class="com.uber.jetbrains.reviewplugin.actions.CompleteReviewAction"
                text="Complete Review"
                description="Mark review as complete and archive the review file">
        </action>

        <!-- Reject Review -->
        <action id="ReviewPlugin.RejectReview"
                class="com.uber.jetbrains.reviewplugin.actions.RejectReviewAction"
                text="Reject Review"
                description="Reject review and archive the review file">
        </action>
    </actions>
</idea-plugin>
```

---

## 6. Key Implementation Details

### 6.1 ReviewGutterIconProvider — Conditional Activation

The `LineMarkerProvider` must be fast. It runs on every editor repaint for every file.

```
CLASS ReviewGutterIconProvider : LineMarkerProvider

  FUNCTION getLineMarkerInfo(element: PsiElement) → LineMarkerInfo?:
      // Fast path: only process first element per line (leaf at column 0)
      IF element is not first leaf on its line:
          RETURN null

      file = element.containingFile.virtualFile
      project = element.project
      reviewModeService = project.service<ReviewModeService>()

      // Fast path: skip non-reviewed files
      IF NOT reviewModeService.isInReviewMode(file):
          RETURN null

      line = document.getLineNumber(element.textOffset) + 1  // 1-based
      commentService = project.service<CommentService>()
      comments = commentService.getCommentsForLine(file.path, line)

      IF comments.isNotEmpty():
          icon = SELECT based on comments[0].status:
              DRAFT    → Icons.COMMENT_EXISTS  (blue chat bubble)
              PENDING  → Icons.COMMENT_PENDING (yellow clock)
              RESOLVED → Icons.COMMENT_RESOLVED (green check)
          RETURN LineMarkerInfo(element, icon, tooltip, clickHandler)
      ELSE:
          RETURN LineMarkerInfo(element, Icons.ADD_COMMENT, "Add comment", addClickHandler)
```

### 6.2 StorageManager — Draft Serialization

```
CLASS StorageManager(@Service Level.PROJECT)

  FUNCTION saveDrafts(session: ReviewSession):
      // Run on pooled thread to avoid blocking EDT
      ApplicationManager.getApplication().executeOnPooledThread {
          ensureReviewDirectory()
          path = draftsDir / "session-${session.id}.json"

          dto = WHEN session:
              is MarkdownReviewSession → MarkdownSessionDto(
                  sessionId = session.id,
                  type = "MARKDOWN",
                  sourceFile = session.sourceFile.path,
                  comments = session.comments.map { it.toDto() }
              )
              is GitDiffReviewSession → GitDiffSessionDto(
                  sessionId = session.id,
                  type = "GIT_DIFF",
                  baseBranch = session.baseBranch,
                  compareBranch = session.compareBranch,
                  baseCommit = session.baseCommit,
                  compareCommit = session.compareCommit,
                  changedFiles = session.changedFiles,
                  comments = session.comments.map { it.toDto() }
              )

          json = Json.encodeToString(dto)
          path.writeText(json)
      }

  FUNCTION loadDrafts() → List<ReviewSession>:
      IF NOT draftsDir.exists():
          RETURN emptyList()

      RETURN draftsDir.listFiles("session-*.json")
          .map { file →
              json = Json.parseToJsonElement(file.readText())
              type = json["type"]
              WHEN type:
                  "MARKDOWN"  → Json.decodeFromString<MarkdownSessionDto>(file.readText())
                                    .toMarkdownReviewSession(project)
                  "GIT_DIFF"  → Json.decodeFromString<GitDiffSessionDto>(file.readText())
                                    .toGitDiffReviewSession(project)
          }

  FUNCTION archiveReviewFile(session: ReviewSession):
      IF session.reviewFilePath == null: RETURN

      ensureArchiveDirectory()
      suffix = randomString(5, "a-z0-9")  // e.g., "a3k9m"
      archiveName = session.getReviewFileName()
          .replace(".review.json", "-${suffix}.review.json")
      archivePath = reviewDir / "archives" / archiveName
      move(session.reviewFilePath → archivePath)

  FUNCTION ensureReviewDirectory():
      reviewDir = project.basePath / ".review"
      IF NOT reviewDir.exists(): reviewDir.mkdirs()
      draftsDir = reviewDir / ".drafts"
      IF NOT draftsDir.exists(): draftsDir.mkdirs()

      // Auto-manage .gitignore on first use
      gitignore = project.basePath / ".gitignore"
      IF gitignore.exists() AND NOT gitignore.readText().contains(".review/"):
          WriteAction.run { gitignore.appendText("\n.review/\n") }

  FUNCTION ensureArchiveDirectory():
      archiveDir = reviewDir / "archives"
      IF NOT archiveDir.exists(): archiveDir.mkdirs()
```

### 6.3 Review File JSON Schema

The `.review.json` file is the sole communication format between the plugin and the CLI.

```json
{
  "sessionId": "uuid",
  "type": "MARKDOWN",
  "metadata": {
    "author": "vinay.yerra",
    "publishedAt": "2026-02-12T15:30:00Z",
    "sourceFile": "docs/uscorer/ARCHITECTURE_OVERVIEW.md",
    "baseBranch": null,
    "compareBranch": null,
    "baseCommit": null,
    "compareCommit": null,
    "filesChanged": null
  },
  "comments": [
    {
      "index": 1,
      "filePath": "docs/uscorer/ARCHITECTURE_OVERVIEW.md",
      "startLine": 42,
      "endLine": 45,
      "selectedText": "The proposed feature store architecture...",
      "userComment": "How does this integrate with Feature Manager?",
      "status": "pending",
      "claudeResponse": null,
      "changeType": null,
      "replies": []
    },
    {
      "index": 2,
      "filePath": "docs/uscorer/ARCHITECTURE_OVERVIEW.md",
      "startLine": 78,
      "endLine": 82,
      "selectedText": "All features are cached with a uniform TTL...",
      "userComment": "Caching strategy doesn't account for TTL variations.",
      "status": "resolved",
      "claudeResponse": "Based on Feature Manager architecture...",
      "changeType": null,
      "replies": [
        {
          "author": "vinay.yerra",
          "timestamp": "2026-02-12T16:05:00Z",
          "text": "What about the caching layer?"
        },
        {
          "author": "claude",
          "timestamp": "2026-02-12T16:06:30Z",
          "text": "The caching layer uses..."
        }
      ]
    }
  ]
}
```

**Status values**: `"draft"`, `"pending"`, `"resolved"`, `"skipped"`, `"rejected"`

**Type-specific metadata**: `sourceFile` is set for `MARKDOWN` reviews; `baseBranch`/`compareBranch`/`baseCommit`/`compareCommit`/`filesChanged` are set for `GIT_DIFF` reviews. Unused fields are `null`.

### 6.4 ReviewFileManager — Publish and Load

```
CLASS ReviewFileManager

  FUNCTION publish(session: ReviewSession, outputDir: Path) → Path:
      reviewFile = buildReviewFile(session)
      fileName = session.getReviewFileName()  // e.g., "ARCHITECTURE_OVERVIEW.review.json"
      path = outputDir / fileName
      path.writeText(reviewFile.toJson())
      RETURN path

  FUNCTION load(reviewFilePath: Path) → ReviewFile:
      json = reviewFilePath.readText()
      RETURN Json.decodeFromString<ReviewFile>(json)

  FUNCTION buildReviewFile(session: ReviewSession) → ReviewFile:
      metadata = WHEN session:
          is MarkdownReviewSession → ReviewMetadata(
              author = systemUser,
              publishedAt = Instant.now(),
              sourceFile = session.sourceFile.path
          )
          is GitDiffReviewSession → ReviewMetadata(
              author = systemUser,
              publishedAt = Instant.now(),
              baseBranch = session.baseBranch,
              compareBranch = session.compareBranch,
              baseCommit = session.baseCommit,
              compareCommit = session.compareCommit,
              filesChanged = session.changedFiles
          )

      comments = session.comments.mapIndexed { i, comment →
          ReviewFileComment(
              index = i + 1,
              filePath = comment.filePath,
              startLine = comment.startLine,
              endLine = comment.endLine,
              selectedText = comment.selectedText,
              userComment = comment.commentText,
              status = "pending",
              claudeResponse = null,
              changeType = comment.changeType?.name,
              replies = emptyList()
          )
      }

      RETURN ReviewFile(session.id, session.type, metadata, comments)

  FUNCTION appendReply(reviewFilePath: Path, commentIndex: Int, reply: Reply):
      reviewFile = load(reviewFilePath)
      comment = reviewFile.comments.find { it.index == commentIndex }
      comment.replies.add(reply)
      comment.status = "pending"
      reviewFilePath.writeText(Json.encodeToString(reviewFile))
```

### 6.5 review-cli — Standalone CLI Tool

A lightweight CLI (Kotlin script or Go binary) that Claude invokes to interact with `.review.json` files comment-by-comment.

**Commands:**

```
review-cli list <file.review.json>                              # List all comments with status
review-cli show <file.review.json> --comment <N>                # Show full detail for comment N
review-cli respond <file.review.json> --comment <N> --response "..." # Write response for comment N
review-cli reply <file.review.json> --comment <N> --text "..."  # Append user reply to comment N
review-cli status <file.review.json>                            # Summary: N pending, M resolved
```

**Pseudocode:**

```
PROGRAM review-cli

  FUNCTION main(args):
      command = args[0]
      filePath = args[1]

      reviewFile = Json.decodeFromString<ReviewFile>(readFile(filePath))

      WHEN command:
          "list" →
              FOR comment in reviewFile.comments:
                  PRINT "[${comment.index}] ${comment.status} | ${comment.filePath}:${comment.startLine}-${comment.endLine}"
                  PRINT "    ${comment.userComment.take(80)}"

          "show" →
              index = parseArg("--comment")
              comment = reviewFile.comments.find { it.index == index }
              PRINT "File: ${comment.filePath}"
              PRINT "Lines: ${comment.startLine}-${comment.endLine}"
              PRINT "Context:\n${comment.selectedText}"
              PRINT "Comment: ${comment.userComment}"
              IF comment.claudeResponse != null:
                  PRINT "Response: ${comment.claudeResponse}"
              FOR reply in comment.replies:
                  PRINT "[${reply.author}] ${reply.text}"

          "respond" →
              index = parseArg("--comment")
              response = parseArg("--response")
              comment = reviewFile.comments.find { it.index == index }
              comment.claudeResponse = response
              comment.status = "resolved"
              writeFile(filePath, Json.encodeToString(reviewFile))
              PRINT "Comment $index resolved."

          "reply" →
              index = parseArg("--comment")
              text = parseArg("--text")
              comment = reviewFile.comments.find { it.index == index }
              comment.replies.add(Reply(author="user", timestamp=now(), text=text))
              comment.status = "pending"
              writeFile(filePath, Json.encodeToString(reviewFile))
              PRINT "Reply added to comment $index."

          "status" →
              pending = reviewFile.comments.count { it.status == "pending" }
              resolved = reviewFile.comments.count { it.status == "resolved" }
              PRINT "Total: ${reviewFile.comments.size} | Pending: $pending | Resolved: $resolved"
```

### 6.6 Claude Code Skill — /review-respond

A Claude Code skill (`.claude/commands/review-respond.md`) that teaches Claude how to use `review-cli`.

**Skill file**: `.claude/commands/review-respond.md`

```markdown
Process a review file by responding to all pending comments.

## Instructions

1. Run `review-cli list $ARGUMENTS` to see all pending comments
2. For each pending comment:
   a. Run `review-cli show $ARGUMENTS --comment N` to get full context
   b. Read the source file at the specified lines
   c. Research related systems and documentation
   d. Run `review-cli respond $ARGUMENTS --comment N --response "your detailed response"`
3. After all comments are processed, run `review-cli status $ARGUMENTS` to confirm

## Guidelines

- For each response, cite source files with line numbers
- Include Mermaid diagrams when explaining flows
- Keep responses concise but complete
- If a comment is unclear, respond with a clarifying question rather than skipping
```

**Usage**: User invokes `/review-respond .review/ARCHITECTURE_OVERVIEW.review.json` in Claude Code. Claude reads the skill instructions and uses `review-cli` commands to process each comment.

### 6.7 ReviewFileWatcher — External Change Detection

```
CLASS ReviewFileWatcher : VirtualFileListener

  FUNCTION contentsChanged(event: VirtualFileEvent):
      file = event.file
      IF NOT isReviewFile(file): RETURN

      // Check if change was external (not from our plugin)
      IF event.isFromSave: RETURN  // our own save, ignore

      // Notify on EDT
      ApplicationManager.getApplication().invokeLater {
          reviewModeService = project.service<ReviewModeService>()

          // Find session that matches this review file
          session = reviewModeService.getAllActiveSessions()
              .find { it.reviewFilePath == file.path }

          IF session != null:
              // Show notification balloon
              NotificationGroupManager.getInstance()
                  .getNotificationGroup("ReviewPlugin")
                  .createNotification(
                      "Claude responded to ${session.getPendingComments().size} comments. Reload?",
                      NotificationType.INFORMATION
                  )
                  .addAction(ReloadResponsesAction())
                  .notify(project)
      }

  FUNCTION isReviewFile(file: VirtualFile) → Boolean:
      RETURN file.path.contains("/.review/")
          AND file.name.endsWith(".review.json")
          AND NOT file.path.contains("/.drafts/")
```

---

## 7. Build Configuration

### build.gradle.kts

```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.uber.jetbrains.reviewplugin"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.2")
        bundledPlugin("Git4Idea")
        bundledPlugin("org.intellij.plugins.markdown")
        instrumentationTools()
    }

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

intellijPlatform {
    pluginConfiguration {
        id = "com.uber.jetbrains.reviewplugin"
        name = "Claude Code Review"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "252"
        }
    }
}

kotlin {
    jvmToolchain(21)
}
```

---

## 8. Phase Implementation Order

```mermaid
flowchart TD
    subgraph Phase1["Phase 1: Foundation"]
        M1["model/ReviewSession.kt (sealed)"]
        M2["model/MarkdownReviewSession.kt"]
        M3["model/GitDiffReviewSession.kt"]
        M4["model/ReviewComment.kt"]
        M5["model/CommentStatus.kt + enums"]
        S1["services/StorageManager.kt"]
        T1["test/StorageManagerTest.kt"]
        T2["test/ReviewCommentTest.kt"]
        PX["plugin.xml scaffold"]
    end

    subgraph Phase2["Phase 2: Markdown Review"]
        S2["services/ReviewModeService.kt"]
        S3["services/CommentService.kt"]
        U1["ui/ReviewGutterIconProvider.kt"]
        U2["ui/CommentPopupEditor.kt"]
        U3["ui/LineHighlighter.kt"]
        U4["ui/ReviewToolWindowFactory.kt"]
        U5["ui/ReviewToolWindowPanel.kt"]
        U6["ui/ReviewStatusBarWidgetFactory.kt"]
        A1["actions/StartMarkdownReviewAction.kt"]
        A2["actions/AddCommentAction.kt"]
        S4["services/ReviewFileManager.kt"]
        A3["actions/PublishReviewAction.kt"]
        T3["test/ReviewFileManagerTest.kt"]
    end

    subgraph Phase3["Phase 3: Claude Integration"]
        CLI["review-cli (standalone)"]
        SKILL[".claude/commands/review-respond.md"]
        L1["listeners/ReviewFileWatcher.kt"]
        A4["actions/ReloadResponsesAction.kt"]
    end

    subgraph Phase4["Phase 4: Git Diff Review"]
        A5["actions/StartDiffReviewAction.kt"]
        U7["ui/BranchSelectionDialog.kt"]
    end

    Phase1 --> Phase2
    Phase2 --> Phase3
    Phase1 --> Phase4
    Phase3 --> Phase4

    style Phase1 fill:#74c0fc
    style Phase2 fill:#51cf66
    style Phase3 fill:#ffd43b
    style Phase4 fill:#ff6b6b
```

| Phase | Files | Deliverable |
|-------|-------|-------------|
| **Phase 1** | 8 files (5 model + 1 service + 2 tests + plugin.xml) | Plugin loads, sealed class hierarchy serializes, draft storage works |
| **Phase 2** | 12 files (2 services + 6 UI + 2 actions + 1 service + 1 test) | Add comments on .md files, publish to `.review.json` |
| **Phase 3** | 4 files (1 CLI tool + 1 Claude skill + 1 listener + 1 action) | Full bidirectional loop: publish JSON, Claude uses `review-cli`, reload responses |
| **Phase 4** | 2 files (1 action + 1 UI dialog) | Git diff review with branch selection |

**Total: 26 plugin source files + review-cli + Claude skill + plugin.xml + build config**

---

## 9. Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Review file format** | JSON (`.review.json`) | Strict schema with definitive field boundaries. No regex parsing. Both plugin and CLI serialize/deserialize cleanly. Machine-friendly for atomic updates |
| **Claude interaction** | Standalone CLI (`review-cli`) + Claude Code skill (`/review-respond`) | CLI enables comment-by-comment processing with atomic JSON updates. Skill teaches Claude the CLI commands. Decoupled from plugin internals |
| **Session type hierarchy** | Sealed class (`ReviewSession` → `MarkdownReviewSession`, `GitDiffReviewSession`) | No nullable fields for "the other mode". Exhaustive `when` matching catches missing branches at compile time. Extensible for future review types |
| **Comment position tracking** | `RangeMarker` API | Auto-adjusts line positions when user edits file during review |
| **One session per file** | Enforced by `ReviewModeService` | Prevents state conflicts from concurrent reviews |
| **Gutter icon activation** | Check `isInReviewMode()` first | Zero performance impact on non-reviewed files |
| **Draft auto-save** | On every comment change, async | No data loss on crash or IDE restart |
| **Response matching** | By comment `index` field in JSON | Exact match — no ambiguity. Plugin assigns index on publish, CLI uses same index |
| **File watcher** | `VirtualFileListener` on `.review/` dir | Detects CLI/Claude writes to `.review.json` without polling |
