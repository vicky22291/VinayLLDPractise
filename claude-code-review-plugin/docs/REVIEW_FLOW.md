# Review Flow

End-to-end lifecycle of a review session, from starting a review to archiving it.

---

## High-Level Flow

```mermaid
sequenceDiagram
    participant User as User (IDE)
    participant Plugin as Plugin
    participant FS as .review/ (Disk)
    participant Claude as Claude (Terminal)

    User->>Plugin: Start Review
    loop Add comments
        User->>Plugin: Add/edit inline comments
        Plugin->>FS: Auto-save drafts
    end
    User->>Plugin: Publish Review
    Plugin->>FS: Write .review.json
    Plugin->>User: Copy CLI command to clipboard
    User->>Claude: Paste /review-respond command
    Claude->>FS: review-cli respond (per comment)
    FS-->>Plugin: FileWatcher detects change
    Plugin->>User: "Claude responded" notification
    User->>Plugin: Reload Responses
    Plugin->>FS: Read updated .review.json
    Plugin->>User: Show responses inline

    opt Multi-round
        User->>Plugin: Reply to response
        Plugin->>FS: Append reply, reset to pending
        User->>Claude: Re-invoke /review-respond
        Claude->>FS: Respond to pending comments
    end

    User->>Plugin: Complete/Reject
    Plugin->>FS: Archive to .review/archives/
```

---

## Phase 1: Start Review

Two entry points, both resulting in an active `ReviewSession`.

### Markdown Review

```mermaid
sequenceDiagram
    participant User
    participant SMRA as StartMarkdownReviewAction
    participant RMS as ReviewModeService
    participant SM as StorageManager

    User->>SMRA: Right-click .md file → "Review this Markdown"
    SMRA->>SMRA: Compute relative path
    SMRA->>RMS: enterMarkdownReview(relativePath)
    RMS->>RMS: Create MarkdownReviewSession
    RMS->>SM: saveDrafts(session)
    RMS-->>SMRA: session
    SMRA->>SMRA: Restart DaemonCodeAnalyzer
    SMRA->>SMRA: Show "Claude Code Review" tool window
```

**Trigger**: Right-click `.md` file → "Review this Markdown" (Ctrl+Shift+R)
**Source**: `actions/StartMarkdownReviewAction.kt:1-47`

### Diff Review

```mermaid
sequenceDiagram
    participant User
    participant SDRA as StartDiffReviewAction
    participant BSD as BranchSelectionDialog
    participant GDS as GitDiffService
    participant RMS as ReviewModeService

    User->>SDRA: VCS → "Review the Diff" (Ctrl+Shift+D)
    SDRA->>BSD: Show branch picker
    BSD->>GDS: getAllBranchNames()
    BSD->>GDS: getDiffStats(base, compare)
    User->>BSD: Select branches → OK
    SDRA->>GDS: getChangedFiles(base, compare)
    SDRA->>RMS: enterDiffReview(base, compare, changedFiles)
    SDRA->>GDS: openDiffView(base, compare)
```

**Trigger**: VCS menu → "Review the Diff" (Ctrl+Shift+D)
**Source**: `actions/StartDiffReviewAction.kt:1-59`

---

## Phase 2: Add/Edit Comments

```mermaid
sequenceDiagram
    participant User
    participant ACA as AddCommentAction
    participant CPE as CommentPopupEditor
    participant CS as CommentService
    participant SM as StorageManager

    User->>ACA: Click "+" gutter icon or Ctrl+Shift+C
    ACA->>ACA: Dismiss any active popup
    ACA->>CPE: captureSelectionContext()
    ACA->>User: Show popup editor (text area + Save/Cancel)
    User->>ACA: Type comment → Save
    ACA->>CPE: saveComment(text)
    CPE->>CS: addComment(session, comment)
    CS->>SM: saveDrafts(session)
    CS->>CS: Notify listeners (onCommentsChanged)
    ACA->>ACA: Restart DaemonCodeAnalyzer (refresh gutter + highlights)
```

**Editing**: Same flow but through `EditCommentAction`, which pre-fills existing text and offers a Delete button.

**Popup management**: `CommentPopupTracker` ensures only one add/edit popup is active at a time.

**Source**: `actions/AddCommentAction.kt:1-108`, `actions/EditCommentAction.kt:1-105`, `ui/CommentPopupEditor.kt:1-141`

---

## Phase 3: Publish

```mermaid
sequenceDiagram
    participant User
    participant PRA as PublishReviewAction
    participant RFM as ReviewFileManager
    participant SM as StorageManager
    participant Clipboard

    User->>PRA: Ctrl+Shift+P or tool window "Publish"
    PRA->>RFM: publish(session, reviewDir)
    RFM->>RFM: Build ReviewFile from session
    RFM->>RFM: Serialize to JSON (prettyPrint)
    RFM->>RFM: Write .review/<name>.review.json
    RFM->>RFM: Record internal write timestamp
    RFM-->>PRA: reviewFilePath
    PRA->>PRA: Set session status → PUBLISHED
    PRA->>PRA: Set all comments → PENDING
    PRA->>SM: saveDrafts(session)
    PRA->>RFM: generateCliCommand(path)
    RFM-->>PRA: claude "/review-respond <path>"
    PRA->>Clipboard: Copy command
    PRA->>User: Notification: "Review published. Command copied."
```

**Output**: `.review/<name>.review.json` file on disk, CLI command in clipboard.

**Source**: `actions/PublishReviewAction.kt:1-57`, `services/ReviewFileManager.kt:1-102`

---

## Phase 4: Claude Responds

This phase happens entirely in the terminal. The user pastes the clipboard command:

```
claude "/review-respond .review/docs--example.review.json"
```

Claude uses the `/review-respond` skill which calls `review-cli`:

```mermaid
sequenceDiagram
    participant Claude
    participant CLI as review-cli
    participant FS as .review.json

    Claude->>CLI: review-cli list <file>
    CLI->>FS: Read file
    CLI-->>Claude: Comment list with statuses
    loop For each pending comment
        Claude->>CLI: review-cli show <file> --comment N
        CLI-->>Claude: Full context (file, lines, text, question)
        Claude->>Claude: Research source code at specified lines
        Claude->>CLI: review-cli respond <file> --comment N --response "..."
        CLI->>FS: Update claudeResponse, set status → resolved
    end
    Claude->>CLI: review-cli status <file>
    CLI-->>Claude: Summary (all resolved)
```

**Source**: `.claude/commands/review-respond.md`, `review-cli/.../ReviewCli.kt:1-191`

See [CLI.md](CLI.md) for full command reference.

---

## Phase 5: Reload Responses

```mermaid
sequenceDiagram
    participant FS as .review.json
    participant RFW as ReviewFileWatcher
    participant User
    participant RRA as ReloadResponsesAction
    participant RFM as ReviewFileManager
    participant CS as CommentService

    FS-->>RFW: VFS change event
    RFW->>RFW: Check: is review file? not internal write?
    RFW->>RFW: Find matching session
    RFW->>User: Notification: "Claude responded. Click to reload."
    User->>RRA: Click notification or Ctrl+Shift+L
    RRA->>RFM: load(reviewFilePath)
    RFM-->>RRA: ReviewFile
    RRA->>CS: applyResponses(session, reviewFile)
    CS->>CS: Match by index, set claudeResponse, status → RESOLVED
    CS->>CS: Notify listeners (onResponsesLoaded)
    RRA->>RRA: Restart DaemonCodeAnalyzer
```

**Internal write filtering**: `ReviewFileManager.isInternalWrite()` uses a 2-second timestamp window so the FileWatcher ignores the plugin's own writes.

**Source**: `actions/ReloadResponsesAction.kt:1-40`, `listeners/ReviewFileWatcher.kt:1-74`

---

## Phase 6: Reply (Multi-Round)

Users can reply to Claude's responses from within the IDE, creating a back-and-forth conversation per comment.

### Enter Reply Mode

```mermaid
sequenceDiagram
    participant User
    participant RTRA as ReplyToReviewAction
    participant RMS as ReviewModeService
    participant SM as StorageManager

    User->>RTRA: Ctrl+Shift+Y or tool window "Reply to Review"
    RTRA->>RTRA: Check: session PUBLISHED + has RESOLVED comments
    RTRA->>RMS: Set session status → ACTIVE
    RTRA->>SM: saveDrafts(session)
    RTRA->>User: Notification: "Reply mode active"
```

**Source**: `actions/ReplyToReviewAction.kt:1-48`

### Add Replies

```mermaid
sequenceDiagram
    participant User
    participant ARA as AddReplyAction
    participant RPE as ReplyPopupEditor
    participant CS as CommentService
    participant SM as StorageManager

    User->>ARA: Click gutter icon on RESOLVED comment
    ARA->>ARA: Dismiss any active popup
    ARA->>RPE: Create editor with comment
    ARA->>User: Show popup (Claude response + reply text area)
    User->>ARA: Type reply → Save Reply
    ARA->>RPE: saveReply(text)
    RPE->>CS: addReply(session, commentId, text)
    CS->>SM: saveDrafts(session)
    CS->>CS: Notify listeners (onCommentsChanged)
    ARA->>ARA: Restart DaemonCodeAnalyzer
```

**Source**: `actions/AddReplyAction.kt:1-106`, `ui/ReplyPopupEditor.kt:1-56`

### Publish Replies

```mermaid
sequenceDiagram
    participant User
    participant PRA as PublishReviewAction
    participant RFM as ReviewFileManager
    participant SM as StorageManager
    participant FS as .review.json
    participant Clipboard

    User->>PRA: Ctrl+Shift+P (Publish)
    PRA->>PRA: Detect reply round (reviewFilePath != null, draftReplies exist)
    PRA->>RFM: publishReplies(session, reviewFilePath)
    RFM->>FS: Load → append Reply entries → set status "pending" → write
    RFM-->>PRA: reviewFilePath
    PRA->>PRA: Clear draftReply on comments, set PENDING
    PRA->>PRA: Set session status → PUBLISHED
    PRA->>SM: deleteDrafts(sessionId)
    PRA->>RFM: generateCliCommand(path)
    PRA->>Clipboard: Copy command
    PRA->>User: Notification: "Replies published. Command copied."
    User->>User: Re-invoke /review-respond
```

**Key detail**: `publishReplies()` appends to the `replies` array in the existing `.review.json` file (not a new file) and resets comment status to `pending`. When Claude re-runs `/review-respond`, it sees only pending comments and responds to the follow-ups.

**Source**: `actions/PublishReviewAction.kt:1-86`, `services/ReviewFileManager.kt:56-82`

---

## Phase 7: Complete/Reject

```mermaid
sequenceDiagram
    participant User
    participant CRA as CompleteReviewAction
    participant RMS as ReviewModeService
    participant SM as StorageManager

    User->>CRA: Tool window "Complete" (or "Reject")
    CRA->>CRA: Check for pending comments (warn if any)
    CRA->>RMS: completeReview(session) or rejectReview(session)
    RMS->>RMS: Set status → COMPLETED or REJECTED
    RMS->>SM: archiveReviewFile(session)
    SM->>SM: Move .review.json → archives/<name>-<suffix>.review.json
    RMS->>SM: deleteDrafts(sessionId)
    RMS->>RMS: Remove from active sessions
    RMS->>RMS: Notify listeners (onReviewModeExited)
    CRA->>CRA: Restart DaemonCodeAnalyzer
    CRA->>User: Notification: "Review completed/rejected and archived."
```

**Archive naming**: The deterministic name gets a 5-character random suffix, freeing the original name for reuse.

**Source**: `actions/CompleteReviewAction.kt:1-54`, `actions/RejectReviewAction.kt:1-52`, `services/StorageManager.kt` (archiveReviewFile)

---

## Session Persistence

Sessions survive IDE restarts through draft auto-saving and startup restoration.

```mermaid
sequenceDiagram
    participant IDE as IDE Shutdown
    participant RMS as ReviewModeService
    participant SM as StorageManager

    IDE->>RMS: IDE closing
    RMS->>RMS: Set active sessions → SUSPENDED
    RMS->>SM: saveDrafts(session) for each

    Note over IDE: IDE restarts

    participant RFWS as ReviewFileWatcherStartup
    RFWS->>SM: loadDrafts()
    SM-->>RFWS: List<ReviewSession> (suspended)
    loop For each suspended session
        RFWS->>RMS: restoreSuspendedSession(session)
        RMS->>RMS: Set status → ACTIVE
    end
    RFWS->>RFWS: Show notification: "N session(s) restored"
```

**Source**: `listeners/ReviewFileWatcherStartup.kt:1-40`
