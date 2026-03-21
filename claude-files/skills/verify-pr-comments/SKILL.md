---
name: verify-pr-comments
description: Verifies all PR comments are properly addressed with complete workflow (code changed, marked addressed, PR reply posted). Use before finalizing PR comment work or when verifying comment resolution compliance.
allowed-tools: "Read, Bash(gh *), Bash(git *), Grep, Glob"
metadata:
  version: 1.0.0
  category: verification
---

# Verify PR Comments

**Purpose**: Ensure ALL PR comments follow complete 3-step workflow (Code → Mark → Reply)

## Workflow

### 1. Read Task File

Find and read `docs/projects/*/tasks/*.md`

### 2. Fetch GitHub Comments

```bash
gh pr view --json number -q .number
gh api repos/{owner}/{repo}/pulls/{pr}/comments --jq '.[] | {id: .id, path: .path, body: .body, created_at: .created_at, updated_at: .updated_at}'
```

### 3. Verify Each Comment

For each GitHub comment, check:

1. **In Task File?**
   - ❌ FAIL: Not tracked → "Add to task file"

2. **All 3 Steps Complete?**
   - ✅ Code Changed: {hash} ({timestamp}) - or documented why not needed
   - ✅ Marked Addressed: {timestamp}
   - ✅ PR Reply Posted: {timestamp}
   - ❌ FAIL if any missing

3. **Correct Order?**
   - Timestamps: Code Change ≤ Mark ≤ Reply
   - ❌ FAIL if out of order

### 4. Generate Report

```markdown
## PR Comment Verification Report
**PR**: #{pr} | **Date**: {timestamp}
**Total**: X | **Status**: ✅ COMPLETE / ❌ INCOMPLETE

### ✅ PROPERLY ADDRESSED (X)
- Comment {id}: All 3 steps ✅

### ❌ INCOMPLETE (Y)
#### Comment {id} - {description}
**Missing**:
- ❌ Code Changed: Not done
- ✅ Marked Addressed: {timestamp}
- ❌ PR Reply: Not posted
**Action**: Make code change, post reply

### Summary
**Compliance**: X/Y (Z%)
**Pass Criteria**: 100% required
**Result**: ✅ PASS / ❌ FAIL
```

## Failure Types

**Not in task file**: `Comment {id} missing → Add to task file and address`
**No code change**: `Code Changed missing → Make fix or document why not needed`
**Not marked**: `Marked Addressed missing → Update task file status`
**No reply**: `PR Reply missing → Post reply to comment {id}`
**Wrong order**: `Reply before code → Fix timestamps`

## Key Rules

✅ Check EVERY comment from GitHub
✅ Verify all 3 steps completed
✅ Require 100% compliance (no exceptions)
✅ Report specific missing actions
✅ Verify timestamps in correct order

❌ Never pass with incomplete comments
❌ Never skip verification steps
