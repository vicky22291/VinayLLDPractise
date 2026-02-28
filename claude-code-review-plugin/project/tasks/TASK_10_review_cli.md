# Task 10: review-cli — Standalone CLI Tool

**Phase**: 3 - Claude Integration
**LOC**: ~350
**Dependencies**: Task 02 (data models / JSON DTOs)
**Verification**: CLI can list, show, respond, reply, and report status on a `.review.json` file

---

## Objective

Build `review-cli` — a standalone CLI tool that Claude invokes to interact with `.review.json` files comment-by-comment. This tool enables Claude to process review comments atomically (one at a time) rather than needing to read/write the entire file in one pass. The CLI is the bridge between Claude Code and the plugin.

---

## Files to Create

```
claude-code-review-plugin/
├── settings.gradle.kts              # UPDATE: include review-cli module
└── review-cli/
    ├── build.gradle.kts             # ~40 LOC
    └── src/main/kotlin/
        └── com/uber/reviewcli/
            ├── ReviewCli.kt         # ~250 LOC (main entry point)
            └── ReviewFileSchema.kt  # ~60 LOC (shared DTOs, or reuse from plugin)
```

---

## What to Implement

### Module Setup

**`settings.gradle.kts`** update:
```kotlin
include("review-cli")
```

**`review-cli/build.gradle.kts`**:
```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
}

application {
    mainClass.set("com.uber.reviewcli.ReviewCliKt")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

kotlin {
    jvmToolchain(21)
}
```

The CLI must be a standalone executable — it does NOT depend on the IntelliJ Platform SDK. It shares only the JSON schema (copy the DTO classes or create a shared module).

### `ReviewFileSchema.kt`

Copy (or share) these `@Serializable` data classes from Task 02:
- `ReviewFile`
- `ReviewFileComment`
- `ReviewMetadata`
- `Reply`

These must match the exact same JSON schema as the plugin produces. If refactoring into a shared module is simpler, do that instead of copying.

### `ReviewCli.kt` — Commands

**Reference**: HLD Section 10, IMPLEMENTATION.md Section 6.5

**Entry point**: `fun main(args: Array<String>)`

Parse `args[0]` as command, `args[1]` as file path, remaining as flags.

#### Command: `list`

```
review-cli list <file.review.json>
```

Output:
```
[1] pending  | docs/uscorer/ARCHITECTURE_OVERVIEW.md:42-45
    How does this integrate with Feature Manager?
[2] resolved | docs/uscorer/ARCHITECTURE_OVERVIEW.md:78-82
    Caching strategy doesn't account for TTL variations.
[3] pending  | docs/uscorer/ARCHITECTURE_OVERVIEW.md:120-125
    Consider adding circuit breaker pattern here.

Summary: 3 total | 2 pending | 1 resolved
```

Logic:
1. Read and deserialize JSON file
2. For each comment: print index, status, file:lines, truncated userComment (80 chars max)
3. Print summary line

#### Command: `show`

```
review-cli show <file.review.json> --comment <N>
```

Output:
```
Comment 1
File:    docs/uscorer/ARCHITECTURE_OVERVIEW.md
Lines:   42-45
Status:  pending

Context:
  The proposed feature store architecture enables Palette to serve as a
  centralized feature computation and storage layer.

User Comment:
  How does this integrate with Feature Manager?

Claude Response:
  (none)

Replies: 0
```

Logic:
1. Parse `--comment N` flag
2. Find comment by index
3. Print all fields in readable format
4. If `claudeResponse` is set, print it
5. Print each reply in thread order

#### Command: `respond`

```
review-cli respond <file.review.json> --comment <N> --response "Your response text..."
```

Logic:
1. Parse `--comment N` and `--response "text"` flags
2. Read and deserialize JSON
3. Find comment by index
4. Set `claudeResponse = response text`
5. Set `status = "resolved"`
6. Write updated JSON back to file (pretty-printed)
7. Print: `Comment N resolved.`

**Important**: The response text may contain newlines, markdown, code blocks, etc. The `--response` flag value should support multiline (via shell quoting or heredoc).

#### Command: `reply`

```
review-cli reply <file.review.json> --comment <N> --text "Follow-up question..."
```

Logic:
1. Parse flags
2. Find comment by index
3. Append new `Reply(author = "user", timestamp = now, text = text)` to replies
4. Set `status = "pending"` (triggers re-processing)
5. Write updated JSON
6. Print: `Reply added to comment N.`

#### Command: `status`

```
review-cli status <file.review.json>
```

Output:
```
Review: docs--uscorer--ARCHITECTURE_OVERVIEW.review.json
Type:   MARKDOWN
Source: docs/uscorer/ARCHITECTURE_OVERVIEW.md

Total: 5 | Pending: 2 | Resolved: 3 | Skipped: 0
```

### Error Handling

- File not found: `Error: File not found: <path>`
- Invalid JSON: `Error: Invalid review file format: <details>`
- Comment index not found: `Error: Comment <N> not found. Valid range: 1-<max>`
- Missing required flags: `Error: Missing required flag: --comment`
- Unknown command: `Usage: review-cli <list|show|respond|reply|status> <file> [options]`

All errors should exit with code 1. Success exits with code 0.

### JSON Formatting

When writing JSON back to file, use:
```kotlin
val json = Json {
    prettyPrint = true
    encodeDefaults = true
}
```

This ensures the file remains human-readable after CLI updates.

---

## Building the CLI

```bash
cd claude-code-review-plugin
./gradlew :review-cli:installDist
# Produces: review-cli/build/install/review-cli/bin/review-cli
```

The user should add the CLI to their PATH or create an alias:
```bash
alias review-cli="/path/to/review-cli/build/install/review-cli/bin/review-cli"
```

---

## Verification

Create a test `.review.json` file manually (or use output from Task 05 tests):

1. `review-cli list test.review.json` -> lists all comments with status
2. `review-cli show test.review.json --comment 1` -> shows full detail
3. `review-cli respond test.review.json --comment 1 --response "This integrates via..."` -> updates file, status becomes "resolved"
4. `review-cli list test.review.json` -> comment 1 now shows "resolved"
5. `review-cli reply test.review.json --comment 1 --text "What about caching?"` -> adds reply, status back to "pending"
6. `review-cli show test.review.json --comment 1` -> shows reply thread
7. `review-cli status test.review.json` -> shows correct counts
8. Error cases: missing file, invalid index, missing flags -> appropriate error messages
