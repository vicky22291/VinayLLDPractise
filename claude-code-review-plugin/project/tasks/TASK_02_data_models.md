# Task 02: Data Models & Serialization DTOs

**Phase**: 1 - Foundation
**LOC**: ~300
**Dependencies**: Task 01
**Verification**: Unit tests pass for model construction, `getReviewFileName()`, and JSON round-trip serialization of DTOs

---

## Objective

Implement all data model classes: enums, the `ReviewComment` data class, the `ReviewSession` sealed class hierarchy, and the JSON serialization DTOs (`ReviewFile`, `ReviewFileComment`, `ReviewMetadata`, `Reply`). After this task, sessions can be created, comments added, and the full review file schema can be serialized/deserialized.

---

## Files to Create

```
src/main/kotlin/com/uber/jetbrains/reviewplugin/model/
├── ReviewSessionStatus.kt        # ~10 LOC
├── CommentStatus.kt              # ~10 LOC
├── ChangeType.kt                 # ~8 LOC
├── ReviewComment.kt              # ~35 LOC
├── ReviewSession.kt              # ~50 LOC (sealed base + 2 subclasses)
├── ReviewFile.kt                 # ~20 LOC (DTO)
├── ReviewFileComment.kt          # ~25 LOC (DTO)
├── ReviewMetadata.kt             # ~20 LOC (DTO)
└── Reply.kt                      # ~12 LOC (DTO)

src/test/kotlin/com/uber/jetbrains/reviewplugin/model/
└── ReviewSessionTest.kt          # ~80 LOC
```

---

## What to Implement

### Enums

**Reference**: IMPLEMENTATION.md Section 3.1

```
ReviewSessionStatus: ACTIVE, SUSPENDED, PUBLISHED, COMPLETED, REJECTED
CommentStatus: DRAFT, PENDING, RESOLVED, SKIPPED, REJECTED
ChangeType: ADDED, MODIFIED, DELETED
```

All three are simple Kotlin enums. No extra methods needed.

### `ReviewComment`

**Reference**: HLD Section 4.1, IMPLEMENTATION.md Section 3.1

Data class with:
- `id: UUID` (generated at creation)
- `filePath: String` (relative to project root)
- `startLine: Int` (1-based)
- `endLine: Int` (1-based, same as startLine for single-line)
- `selectedText: String` (captured context)
- `commentText: String` (user's review comment)
- `authorId: String` (e.g., "vinay.yerra")
- `createdAt: Instant`
- `status: CommentStatus` (defaults to `DRAFT`)
- `claudeResponse: String?` (null until Claude responds)
- `resolvedAt: Instant?`
- `changeType: ChangeType?` (null for Markdown reviews)

**Do NOT include** `rangeMarker: RangeMarker?` in the data class — that is a runtime-only field managed by `LineHighlighter` (Task 06). Keep the model serialization-clean.

### `ReviewSession` (sealed class)

**Reference**: HLD Section 3.4, IMPLEMENTATION.md Section 3.1

Sealed class with:
- `id: UUID`
- `status: ReviewSessionStatus` (mutable, starts as `ACTIVE`)
- `comments: MutableList<ReviewComment>`
- `createdAt: Instant`
- `publishedAt: Instant?`
- `reviewFilePath: String?` (set after publish)

Abstract methods:
- `getReviewFileName(): String` — deterministic name for the `.review.json` file
- `getDisplayName(): String` — for UI display

Helper methods (in base class):
- `addComment(comment)`, `removeComment(commentId)`, `getComment(commentId)`
- `getDraftComments()`, `getPendingComments()`

### `MarkdownReviewSession`

**Reference**: HLD Section 5.3

- `sourceFilePath: String` (relative path to project root — use String, not VirtualFile, to keep model serializable)
- `getReviewFileName()`: Derives from relative path with `--` separator
  - `docs/uscorer/ARCHITECTURE_OVERVIEW.md` -> `docs--uscorer--ARCHITECTURE_OVERVIEW.review.json`
- `getDisplayName()`: `"Markdown: ARCHITECTURE_OVERVIEW.md"` (filename only)

### `GitDiffReviewSession`

**Reference**: HLD Section 5.3

- `baseBranch: String`
- `compareBranch: String`
- `baseCommit: String?`
- `compareCommit: String?`
- `changedFiles: List<String>`
- `getReviewFileName()`: `"diff-main--feature-auth.review.json"` (branches with `/` replaced by `-`)
- `getDisplayName()`: `"Diff: main -> feature-auth"`

### JSON DTOs (for `.review.json` serialization)

**Reference**: HLD Section 9.3, IMPLEMENTATION.md Section 6.3

These are `@Serializable` data classes using `kotlinx.serialization`:

**`ReviewFile`**: `sessionId`, `type` ("MARKDOWN" | "GIT_DIFF"), `metadata: ReviewMetadata`, `comments: List<ReviewFileComment>`
- Include `fun toJson(): String` and companion `fun fromJson(json: String): ReviewFile`

**`ReviewMetadata`**: `author`, `publishedAt`, `sourceFile?`, `baseBranch?`, `compareBranch?`, `baseCommit?`, `compareCommit?`, `filesChanged: List<String>?`

**`ReviewFileComment`**: `index: Int`, `filePath`, `startLine`, `endLine`, `selectedText`, `userComment`, `status`, `claudeResponse?`, `changeType?`, `replies: List<Reply>`

**`Reply`**: `author`, `timestamp`, `text`

---

## Naming Rules (must match exactly)

**Reference**: HLD Section 5.2-5.3

| Source | Review File Name |
|--------|-----------------|
| `docs/uscorer/ARCHITECTURE_OVERVIEW.md` | `docs--uscorer--ARCHITECTURE_OVERVIEW.review.json` |
| `README.md` | `README.review.json` |
| `src/main/design.md` | `src--main--design.review.json` |
| base=`main`, compare=`feature-auth` | `diff-main--feature-auth.review.json` |
| base=`main`, compare=`feature/user-auth` | `diff-main--feature-user-auth.review.json` |

---

## Test Scenarios (`ReviewSessionTest.kt`)

1. `MarkdownReviewSession.getReviewFileName()` produces correct names for:
   - Nested path: `docs/uscorer/ARCHITECTURE_OVERVIEW.md`
   - Root-level: `README.md`
   - Deep path: `src/main/design.md`
2. `GitDiffReviewSession.getReviewFileName()` produces correct names for:
   - Simple branches: `main`, `feature-auth`
   - Slash branches: `main`, `feature/user-auth`
3. `ReviewFile` serialization round-trip: create -> `toJson()` -> `fromJson()` -> assert equality
4. `ReviewComment` creation with defaults: status is `DRAFT`, claudeResponse is `null`
5. `ReviewSession.addComment()` / `removeComment()` / `getDraftComments()` work correctly
