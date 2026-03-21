---
name: addressing-pr-comments
description: Systematically addresses PR review comments by fetching from GitHub, tracking in existing task file, fixing issues, testing, committing, and replying. Use when user asks to address/fix/respond to PR comments or review feedback.
allowed-tools: "Read, Write, Edit, Bash(gh *), Bash(git *), Bash(pytest *), Bash(make *), Grep, Glob, TodoWrite"
metadata:
  version: 3.0.0
  category: development
---

# Addressing PR Comments

**Core Rule**: Code Change → Mark Addressed → Post Reply (strict order)

## Instructions

<instructions>

## Workflow

### 1. Find Last Addressed Timestamp

Read task file (`docs/projects/*/tasks/*.md`), find most recent `PR Reply Posted: YYYY-MM-DD HH:MM`.
- If found: `LAST_ADDRESSED_TIME = "YYYY-MM-DD HH:MM"`
- If none: `LAST_ADDRESSED_TIME = ""` (process all)

### 2. Fetch Comments (Reverse Chronological)

```bash
gh pr view --json number -q .number
gh api repos/{owner}/{repo}/pulls/{pr}/comments --jq 'sort_by(.updated_at) | reverse | .[] | {id: .id, path: .path, line: .line, body: .body, created_at: .created_at, updated_at: .updated_at}'
```

**Filter**: Only process comments where `updated_at > LAST_ADDRESSED_TIME`

### 3. Save to Task File (BEFORE Code Changes!)

```markdown
## PR #{pr} Comment Tracking
**Last Addressing Session**: YYYY-MM-DD HH:MM
**Last Addressed Timestamp**: YYYY-MM-DD HH:MM

### ✅ ADDRESSED COMMENTS
#### Comment {id} - {description}
**Created**: {timestamp} | **Updated**: {timestamp} | **File**: {path}:{line}
**Priority**: 🔴 P1 / 🟡 Minor / 🔵 Trivial
**Comment**: {text}
**Workflow Steps:**
1. ✅ Code Changed: {hash} - {desc} ({timestamp})
2. ✅ Marked Addressed: {timestamp}
3. ✅ PR Reply Posted: {timestamp}
**Verification**: {line numbers, code}

### ⏳ PENDING COMMENTS
#### Comment {id} - {description}
**Created**: {timestamp} | **Updated**: {timestamp} | **File**: {path}:{line}
**Priority**: 🔴 P1 / 🟡 Minor / 🔵 Trivial
**Comment**: {text}
**Workflow Steps:**
1. ⏳ Code Changed: Pending
2. ⏳ Marked Addressed: Pending
3. ⏳ PR Reply Posted: Pending
**Action Required**: {what to do}
```

### 4. Validate & Prioritize

- Read files, check if already fixed
- Check CLAUDE.md for style conflicts
- Prioritize: 🔴 P1 → 🟡 Minor → 🔵 Trivial

### 5. Fix Each Comment (Strict Order!)

**5.1 Code Change First:**
```bash
# Make fix, run tests
PYTHONPATH=src pytest tests/path/ -v
# Use appropriate conventional commit type: fix:, feat:, refactor:, docs:, style:
git add . && git commit -m "fix: {desc}

Addresses PR #{pr} comment {id}"
# Update task file: Replace {commit-hash} placeholder with actual hash
# Update task: ✅ Code Changed: {hash} ({timestamp})
```

**5.2 Mark Addressed Second:**
- Move from PENDING → ADDRESSED
- Update: ✅ Marked Addressed: {timestamp}

**5.3 Reply Third:**
```bash
gh api repos/{owner}/{repo}/pulls/{pr}/comments/{id}/replies -X POST -f body="✅ Addressed in {hash}..."
# Update: ✅ PR Reply Posted: {timestamp}
```

**NEVER** reply or mark addressed before code is committed!

### 6. Push & Verify

```bash
make test && make analysis
SKIP_TESTS=1 git push  # Only if no src/tests changes
git push
```

Update task: `**Last Addressing Session**: {timestamp}`

Run `verify-pr-comments` skill before claiming completion.

</instructions>

## Examples

<examples>

## Reply Templates

**Fixed**: `✅ Addressed in {hash}\n{explanation}\nVerification: {details}`
**Already Fixed**: `✅ Already addressed in {hash}\nLine {X} in {file}`
**Style Conflict**: `📋 Per CLAUDE.md: "{quote}"\nKeeping current implementation`
**Question**: `❓ Need clarification: {question}`

</examples>

## Guidelines

<guidelines>

## Key Rules

**NON-NEGOTIABLE: ALWAYS SAVE COMMENTS TO TASK FILE BEFORE ANY CODE CHANGES**

✅ Fetch reverse chronological (newest first), filter by `updated_at > LAST_ADDRESSED_TIME`
✅ Strict order: Code → Mark → Reply
✅ Track both created_at and updated_at
✅ Use GitHub comment IDs
✅ Reply individually (not batch)
✅ Run verify-pr-comments before finishing

❌ Never process already-addressed comments
❌ Never mark addressed before commit
❌ Never reply before marking addressed
❌ Never skip workflow steps

</guidelines>
