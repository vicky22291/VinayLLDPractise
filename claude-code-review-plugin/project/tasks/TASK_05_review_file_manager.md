# Task 05: ReviewFileManager — Publish & Load `.review.json`

**Phase**: 2 - Markdown Review
**LOC**: ~280
**Dependencies**: Task 02, Task 03
**Verification**: Unit tests pass for publish (serialize to JSON), load (deserialize), and appendReply operations

---

## Objective

Implement `ReviewFileManager` — the stateless utility that bridges the plugin and `review-cli`. It serializes a session's comments into the published `.review.json` format, deserializes Claude's responses back, and handles reply appending. This is the **sole communication contract** between the plugin and the CLI tool.

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/services/
└── ReviewFileManager.kt                  # ~130 LOC

src/test/kotlin/com/uber/jetbrains/reviewplugin/services/
└── ReviewFileManagerTest.kt              # ~150 LOC
```

---

## What to Implement

### `ReviewFileManager`

**Reference**: HLD Section 9.1-9.3, IMPLEMENTATION.md Section 6.4

This is a stateless utility — NOT a project service. It operates on paths and session objects.

**Public API:**

```
publish(session: ReviewSession, outputDir: Path): Path
    → Build ReviewFile from session
    → Serialize to JSON (pretty-printed)
    → Write to outputDir / session.getReviewFileName()
    → Return the written file path

load(reviewFilePath: Path): ReviewFile
    → Read file contents
    → Deserialize JSON to ReviewFile
    → Return ReviewFile

appendReply(reviewFilePath: Path, commentIndex: Int, reply: Reply)
    → Load existing ReviewFile
    → Find comment by index
    → Append reply to comment's replies list
    → Set comment status to "pending" (triggers re-processing by Claude)
    → Write updated JSON back to file

generateCliCommand(reviewFilePath: String): String
    → Return: claude "/review-respond <reviewFilePath>"
```

### `buildReviewFile()` (private)

**Reference**: IMPLEMENTATION.md Section 6.4

Uses sealed class dispatch to populate metadata correctly:

```
FUNCTION buildReviewFile(session: ReviewSession): ReviewFile
    metadata = WHEN session:
        is MarkdownReviewSession →
            ReviewMetadata(
                author = System.getProperty("user.name"),
                publishedAt = Instant.now().toString(),
                sourceFile = session.sourceFilePath,
                baseBranch = null, compareBranch = null,
                baseCommit = null, compareCommit = null,
                filesChanged = null
            )
        is GitDiffReviewSession →
            ReviewMetadata(
                author = System.getProperty("user.name"),
                publishedAt = Instant.now().toString(),
                sourceFile = null,
                baseBranch = session.baseBranch,
                compareBranch = session.compareBranch,
                baseCommit = session.baseCommit,
                compareCommit = session.compareCommit,
                filesChanged = session.changedFiles
            )

    type = WHEN session:
        is MarkdownReviewSession → "MARKDOWN"
        is GitDiffReviewSession → "GIT_DIFF"

    comments = session.comments.mapIndexed { i, comment →
        ReviewFileComment(
            index = i + 1,           // 1-based index
            filePath = comment.filePath,
            startLine = comment.startLine,
            endLine = comment.endLine,
            selectedText = comment.selectedText,
            userComment = comment.commentText,
            status = "pending",      // all become pending on publish
            claudeResponse = null,
            changeType = comment.changeType?.name?.lowercase(),
            replies = emptyList()
        )
    }

    RETURN ReviewFile(
        sessionId = session.id.toString(),
        type = type,
        metadata = metadata,
        comments = comments
    )
```

### JSON Output Format

**Reference**: HLD Section 9.3

The output must match this exact schema (pretty-printed for readability):

```json
{
  "sessionId": "uuid-string",
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
      "selectedText": "The proposed feature store...",
      "userComment": "How does this integrate?",
      "status": "pending",
      "claudeResponse": null,
      "changeType": null,
      "replies": []
    }
  ]
}
```

Use `Json { prettyPrint = true; encodeDefaults = true }` for serialization.

---

## Test Scenarios (`ReviewFileManagerTest.kt`)

All tests should use temp directories.

1. **Publish Markdown session**: Create `MarkdownReviewSession` with 3 comments -> `publish()` -> verify file exists at correct path, JSON is valid, all comments have status "pending"
2. **Publish GitDiff session**: Create `GitDiffReviewSession` -> `publish()` -> verify metadata has branches, no sourceFile
3. **Load published file**: `publish()` -> `load()` -> verify all fields match
4. **Comment indexing**: Publish session with 5 comments -> load -> verify indices are 1, 2, 3, 4, 5
5. **Load with Claude responses**: Create a `.review.json` with `claudeResponse` and `status: "resolved"` for some comments -> `load()` -> verify responses are parsed
6. **Append reply**: `publish()` -> `appendReply(path, index=1, reply)` -> `load()` -> verify reply is present and comment status reset to "pending"
7. **Append multiple replies**: Append 3 replies to same comment -> verify thread order preserved
8. **generateCliCommand()**: Returns correct format: `claude "/review-respond .review/name.review.json"`
9. **Review file naming**: Verify file is written with correct name from `session.getReviewFileName()`
