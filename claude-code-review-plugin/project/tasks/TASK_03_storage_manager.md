# Task 03: StorageManager — Draft Persistence & Directory Management

**Phase**: 1 - Foundation
**LOC**: ~300
**Dependencies**: Task 01, Task 02
**Verification**: Unit tests pass for save/load/delete drafts, archive, and `.gitignore` management

---

## Objective

Implement `StorageManager` — the project-scoped service that handles all file I/O for draft sessions, directory creation, `.gitignore` auto-management, and review file archival. After this task, draft sessions can be persisted to disk as JSON, restored on IDE restart, and completed/rejected reviews can be archived.

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/services/
└── StorageManager.kt                    # ~180 LOC

src/test/kotlin/com/uber/jetbrains/reviewplugin/services/
└── StorageManagerTest.kt                # ~120 LOC
```

---

## What to Implement

### `StorageManager` (project-scoped service)

**Reference**: HLD Section 6, IMPLEMENTATION.md Section 6.2

Register as `@Service(Service.Level.PROJECT)`.

**State:**
- `project: Project` (injected)
- `reviewDir: Path` — `<projectRoot>/.review/`
- `draftsDir: Path` — `<projectRoot>/.review/.drafts/`

**Public API:**

```
saveDrafts(session: ReviewSession)
    → Serializes session to JSON, writes to .review/.drafts/session-{uuid}.json
    → Runs on pooled thread: ApplicationManager.getApplication().executeOnPooledThread {}
    → Must handle both MarkdownReviewSession and GitDiffReviewSession via sealed class dispatch

loadDrafts(): List<ReviewSession>
    → Scans .review/.drafts/ for session-*.json files
    → Deserializes each to the correct session type based on "type" field
    → Returns sessions with status = SUSPENDED
    → Returns empty list if drafts dir doesn't exist

deleteDrafts(sessionId: UUID)
    → Deletes .review/.drafts/session-{sessionId}.json

archiveReviewFile(session: ReviewSession)
    → If session.reviewFilePath is null, return (nothing to archive)
    → Generate 5-char random suffix [a-z0-9]
    → Move: .review/name.review.json → .review/archives/name-{suffix}.review.json
    → Example: .review/docs--uscorer--ARCHITECTURE_OVERVIEW.review.json
      → .review/archives/docs--uscorer--ARCHITECTURE_OVERVIEW-a3k9m.review.json

ensureReviewDirectory()
    → Creates .review/ and .review/.drafts/ if they don't exist
    → Auto-manages .gitignore (see below)

ensureArchiveDirectory()
    → Creates .review/archives/ if it doesn't exist

getReviewDirectory(): Path
    → Returns .review/ path
```

### `.gitignore` Auto-Management

**Reference**: HLD Section 6.3

On first use (`ensureReviewDirectory()`):
1. Check if `<projectRoot>/.gitignore` exists
2. If it exists, check if it already contains `.review/`
3. If not present, append `\n.review/\n` using `WriteAction.run {}`
4. If `.gitignore` doesn't exist, create it with `.review/\n` content

### Draft Serialization Format

**Reference**: HLD Section 6.1

The draft JSON format is **different from the published `.review.json` format**. Drafts store the full session state for IDE restart recovery:

```json
{
  "sessionId": "uuid-string",
  "type": "MARKDOWN",
  "status": "SUSPENDED",
  "createdAt": "2026-02-12T15:28:12Z",
  "sourceFile": "docs/uscorer/ARCHITECTURE_OVERVIEW.md",
  "comments": [
    {
      "id": "uuid-string",
      "filePath": "docs/uscorer/ARCHITECTURE_OVERVIEW.md",
      "startLine": 42,
      "endLine": 45,
      "selectedText": "The proposed feature store...",
      "commentText": "How does this integrate?",
      "authorId": "vinay.yerra",
      "createdAt": "2026-02-12T15:28:12Z",
      "status": "DRAFT",
      "claudeResponse": null,
      "changeType": null
    }
  ]
}
```

For `GitDiffReviewSession`, replace `sourceFile` with `baseBranch`, `compareBranch`, `baseCommit`, `compareCommit`, `changedFiles`.

Use `kotlinx.serialization` with a discriminator on the `type` field to handle the sealed class dispatch. Create internal DTO classes for draft serialization if needed (separate from the published `ReviewFile` DTOs).

### Thread Safety

- `saveDrafts()` must run on a pooled thread (not EDT)
- `loadDrafts()` is called during startup, can run on any thread
- File writes should be atomic where possible (write to temp file, then rename)

---

## Directory Layout After Use

**Reference**: HLD Section 6.2

```
<project-root>/
└── .review/
    ├── .drafts/
    │   └── session-{uuid}.json          # Draft sessions
    ├── docs--uscorer--ARCHITECTURE_OVERVIEW.review.json  # Published (Task 05)
    └── archives/
        └── docs--uscorer--ARCHITECTURE_OVERVIEW-a3k9m.review.json  # Archived
```

---

## Test Scenarios (`StorageManagerTest.kt`)

Tests should use a temporary directory (not actual project directory):

1. **Save and load round-trip (Markdown)**: Create `MarkdownReviewSession` with 3 comments -> `saveDrafts()` -> `loadDrafts()` -> verify all fields match
2. **Save and load round-trip (GitDiff)**: Create `GitDiffReviewSession` -> same round-trip verification
3. **Delete drafts**: Save session -> `deleteDrafts(sessionId)` -> `loadDrafts()` returns empty
4. **Load from empty directory**: `loadDrafts()` returns empty list when no drafts exist
5. **Archive review file**: Create a `.review.json` file -> `archiveReviewFile()` -> verify moved to `archives/` with suffix
6. **Archive idempotent**: `archiveReviewFile()` with null `reviewFilePath` does nothing
7. **Ensure directory creates structure**: `ensureReviewDirectory()` creates `.review/` and `.review/.drafts/`
8. **Gitignore management**: `ensureReviewDirectory()` appends `.review/` to `.gitignore` only once
