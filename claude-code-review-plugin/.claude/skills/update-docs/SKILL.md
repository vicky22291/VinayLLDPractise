---
name: update-docs
description: "Updates plugin documentation in docs/ to reflect code changes made during the current session. Inspects git diff, maps changed files to affected docs, and makes surgical edits. Use when user asks to 'update docs', 'update documentation', 'sync docs', or 'reflect changes in docs'."
metadata:
  author: vinay.yerra
  version: 1.0.0
  category: documentation
  tags: [docs, documentation, maintenance, sync]
---

# Update Documentation

Automatically update the plugin's `docs/` files to reflect code changes made in the current session.

## Instructions

### Step 1: Identify Changed Files

Run `git diff --name-only` to find all modified, added, or deleted source files. If changes are staged, also check `git diff --cached --name-only`. Focus only on files under:
- `src/main/kotlin/` (plugin source)
- `src/test/kotlin/` (tests)
- `review-cli/` (CLI)
- `src/main/resources/META-INF/plugin.xml`
- `build.gradle.kts`
- `.claude/commands/`

If no source files changed, report "No code changes detected" and stop.

### Step 2: Map Changes to Documentation

Use this mapping to determine which doc files need updates:

| Changed File Location | Docs to Update |
|----------------------|----------------|
| `model/` (data classes, enums) | `docs/DATA_MODEL.md`, `docs/ARCHITECTURE.md` (class inventory) |
| `services/` (business logic) | `docs/ARCHITECTURE.md`, `docs/REVIEW_FLOW.md` |
| `actions/` (IDE actions) | `docs/ARCHITECTURE.md`, `docs/INTELLIJ_PLATFORM.md` |
| `listeners/` (event handlers) | `docs/ARCHITECTURE.md`, `docs/INTELLIJ_PLATFORM.md` |
| `ui/` (UI logic or integration) | `docs/ARCHITECTURE.md`, `docs/INTELLIJ_PLATFORM.md` |
| `review-cli/` (CLI tool) | `docs/CLI.md` |
| `plugin.xml` | `docs/INTELLIJ_PLATFORM.md` |
| `build.gradle.kts` | `docs/TESTING.md` (if coverage config changed) |
| Test files (`src/test/`) | `docs/TESTING.md` |
| `.claude/commands/` or `.claude/skills/` | `docs/CLI.md` |

### Step 3: Read Affected Docs and Changed Source

For each affected doc file:
1. Read the current doc to understand its structure and content
2. Read the changed source files (use `git diff` to see exactly what changed)
3. Identify what needs updating: new classes, renamed methods, changed fields, new tests, etc.

### Step 4: Apply Documentation Updates

Make surgical edits to each affected doc:

- **New class added** → Add to class inventory table in `docs/ARCHITECTURE.md`, add details to the relevant specialized doc
- **Class removed** → Remove from inventory and specialized docs
- **New enum value** → Update state machine diagram in `docs/DATA_MODEL.md`
- **New action** → Add to actions table in `docs/INTELLIJ_PLATFORM.md`
- **New extension point** → Add to extensions table in `docs/INTELLIJ_PLATFORM.md`
- **New test file** → Add to test inventory in `docs/TESTING.md`
- **Changed service logic** → Update service description in `docs/ARCHITECTURE.md`, update flow in `docs/REVIEW_FLOW.md` if the lifecycle changed
- **New CLI command** → Add to commands reference in `docs/CLI.md`
- **Coverage config change** → Update excludes list in `docs/TESTING.md`

### Step 5: Update Index and Stats

If file counts, test counts, or other stats changed, update `docs/INDEX.md` quick stats table.

### Step 6: Verify Consistency

- Cross-references between docs should remain valid
- Mermaid diagram syntax should be correct
- Source references (file:line) should point to current locations
- All new source files should appear in at least one doc

### Step 7: Report Changes

Summarize what was updated:
```
Updated docs:
- docs/ARCHITECTURE.md: Added NewClass to class inventory
- docs/DATA_MODEL.md: Added NEW_STATUS to CommentStatus state diagram
- docs/INDEX.md: Updated source file count (42 → 43)
```

## Rules

- **Only update docs affected by the changes.** Don't touch unrelated docs.
- **Match existing style.** Follow formatting conventions already in each doc file.
- **Include source references.** Every new piece of information needs a `file:line` citation.
- **Keep it concise.** Update the minimum necessary to reflect the change accurately.
- **Never remove existing content** unless the corresponding code was deleted.
- **Update, don't rewrite.** Make surgical edits to existing sections rather than rewriting entire docs.
- **Update line counts** in the class inventory table when files change significantly.

---

## Examples

### Example 1: New Model Class Added

**Scenario**: Added `PRReviewSession` subclass to `model/ReviewSession.kt`

**Process**:
1. `git diff --name-only` → `src/main/kotlin/.../model/ReviewSession.kt`
2. Maps to: `docs/DATA_MODEL.md`, `docs/ARCHITECTURE.md`
3. Read both docs + the diff
4. Updates:
   - `docs/DATA_MODEL.md`: Add PRReviewSession to class diagram, document fields
   - `docs/ARCHITECTURE.md`: Update Model Layer table (line count), add to class inventory
   - `docs/INDEX.md`: Update source file count if new file created

### Example 2: New Action and Test

**Scenario**: Added `StartPRReviewAction.kt` and `StartPRReviewActionTest.kt`

**Process**:
1. `git diff --name-only` → two new files in `actions/` and test
2. Maps to: `docs/ARCHITECTURE.md`, `docs/INTELLIJ_PLATFORM.md`, `docs/TESTING.md`
3. Updates:
   - `docs/ARCHITECTURE.md`: Add to Actions Layer inventory
   - `docs/INTELLIJ_PLATFORM.md`: Add to Actions table
   - `docs/TESTING.md`: Add to test inventory, update test count
   - `docs/INDEX.md`: Update file and test counts

### Example 3: Coverage Config Change

**Scenario**: Added `BranchSelectionDialog*` to `platformDependentExcludes` in `build.gradle.kts`

**Process**:
1. `git diff --name-only` → `build.gradle.kts`
2. Maps to: `docs/TESTING.md`
3. Updates:
   - `docs/TESTING.md`: Add new pattern to Platform-Dependent Excludes table

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| No changes detected | Check if changes are committed vs staged vs working tree |
| Doc file not found | Verify `docs/` directory exists with all 7 files |
| Mermaid syntax broken | Validate diagram syntax after editing (no unescaped special chars) |
| Line counts stale | Re-count with `wc -l` on the affected source files |
| Cross-reference broken | Search for the old reference text and update all occurrences |
