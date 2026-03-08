# VQTS-Python Workspace Improvements Plan

**Date**: 2026-03-06
**Scope**: All 3 VQTS-Python workspaces (TechnicalAnalysis, TechnicalAnalysis2, TechnicalAnalysis3)
**Based on**: ECC best practices vs current workspace audit

---

## Table of Contents

1. [Current State Assessment](#current-state-assessment)
2. [Improvement 1: PostToolUse Hooks for Auto-Linting](#improvement-1-posttooluse-hooks-for-auto-linting)
3. [Improvement 2: Code Reviewer Agent](#improvement-2-code-reviewer-agent)
4. [Improvement 3: Write-Then-Review Pipeline Skill](#improvement-3-write-then-review-pipeline-skill)
5. [Improvement 4: Rules Directory for Enforced Guidelines](#improvement-4-rules-directory-for-enforced-guidelines)
6. [Improvement 5: Session Memory Persistence via Hooks](#improvement-5-session-memory-persistence-via-hooks)
7. [Improvement 6: PreToolUse Safety Hooks](#improvement-6-pretooluse-safety-hooks)
8. [Improvement 7: Missing Agents (TDD, Planner, Build Resolver)](#improvement-7-missing-agents)
9. [Improvement 8: Git Worktrees Instead of Multiple Clones](#improvement-8-git-worktrees-instead-of-multiple-clones)
10. [Improvement 9: Config Sync Across Workspaces](#improvement-9-config-sync-across-workspaces)
11. [Improvement 10: Context-Specific Aliases](#improvement-10-context-specific-aliases)
12. [Implementation Priority Matrix](#implementation-priority-matrix)

---

## Current State Assessment

### What You Have (Good Foundation)

| Area | Status | Details |
|------|--------|---------|
| CLAUDE.md | Solid | Comprehensive doc pointers, strategy README conventions, architecture references |
| Permissions | Well-scoped | Granular bash permissions for git, python, pytest, linters, gh CLI |
| Skills (9) | Good coverage | addressing-pr-comments, debugging-strategies, planning-tests, project-planning, run-tasks, skill-creator, strategy-development, strategy-documentation, verify-pr-comments |
| Agents (2) | Partial | python-pro (write agent), search-specialist (research agent) |
| 3 Workspaces | Functional | All on main branch, separate clones |

### What's Missing (Gaps)

| Area | Status | Impact |
|------|--------|--------|
| Hooks | Empty `{}` | No auto-linting, no safety gates, no memory persistence |
| Rules directory | Does not exist | No system-injected guidelines that Claude cannot ignore |
| Code reviewer agent | Missing | Cannot do write-then-review workflow |
| Session memory | None | Context lost between sessions |
| Git worktrees | Not used | 3 full clones instead of lightweight worktrees |
| Config sync | Manual | Minor drift between workspace settings |

---

## Improvement 1: PostToolUse Hooks for Auto-Linting

### What It Does
Automatically runs `ruff` and `black` on every file Claude edits or writes. Catches formatting and lint errors at write-time, not after the fact.

### Why It Matters
- ECC finding: hooks fire 100% of the time vs skills at 50-80%
- Currently, lint issues accumulate and require manual cleanup passes
- Auto-formatting prevents style debates and keeps diffs clean

### User Stories

**US-1.1: Consistent code formatting during strategy development**
> As a developer working on a new strategy version (e.g., options-v8), I want every file Claude edits to be automatically formatted with black and checked by ruff, so that when I push to GitHub, the PR doesn't have unrelated formatting changes mixed in with logic changes.

**US-1.2: Catch import errors immediately**
> As a developer adding new modules to the strategy pipeline, I want ruff to flag unused imports and missing imports the moment Claude writes a file, so I don't discover import errors only when running the backtest 10 minutes later.

**US-1.3: Prevent common Python anti-patterns**
> As a developer, I want ruff rules to catch things like bare `except:`, mutable default arguments, and f-string issues at write-time, so Claude fixes them immediately instead of me finding them during manual review.

### Configuration

Add to `.claude/settings.local.json` in each workspace:

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "command": "if echo \"$CLAUDE_FILE_PATH\" | grep -q '\\.py$'; then cd \"$(git rev-parse --show-toplevel)\" && .venv/bin/ruff check --fix \"$CLAUDE_FILE_PATH\" 2>&1 | head -20; fi"
      },
      {
        "matcher": "Edit|Write",
        "command": "if echo \"$CLAUDE_FILE_PATH\" | grep -q '\\.py$'; then cd \"$(git rev-parse --show-toplevel)\" && .venv/bin/black --quiet \"$CLAUDE_FILE_PATH\" 2>&1 | head -10; fi"
      }
    ]
  }
}
```

### Notes
- The `grep '\\.py$'` guard ensures hooks only run on Python files
- `head -20` prevents large lint output from flooding Claude's context
- ruff `--fix` auto-fixes what it can; remaining issues are reported back to Claude
- black runs in quiet mode to minimize noise

---

## Improvement 2: Code Reviewer Agent

### What It Does
A read-only agent specialized in reviewing Python trading system code. It cannot write files - it only reads, searches, and reports findings.

### Why It Matters
- ECC pattern: "Two focused agents > one constrained agent"
- The De-Sloppify pattern requires a separate review context window
- Your current agents (python-pro, search-specialist) are both "do" agents - you have no "review" agent
- Read-only tool scoping prevents the reviewer from "fixing" issues itself (which could introduce new bugs)

### User Stories

**US-2.1: Strategy logic review before going live**
> As a trader deploying a new strategy version (e.g., v8 options), I want a code reviewer agent to check all entry/exit signal logic, position sizing calculations, and risk management boundaries before I push to the live EC2 instance, so I don't lose money due to a logic bug.

**US-2.2: Cross-strategy consistency check**
> As a developer who maintains multiple strategy versions (v5, v7, v8), I want the reviewer to check that shared utility functions and base classes are used consistently, so changes to one version don't silently break assumptions in another.

**US-2.3: Performance review for backtest pipelines**
> As a developer running backtests over years of data, I want the reviewer to flag N+1 data loading patterns, unnecessary DataFrame copies, and inefficient loops, so my backtests complete in minutes instead of hours.

**US-2.4: Review before PR creation**
> As a developer, after Claude implements a feature, I want to invoke the code-reviewer agent in the same session (after /clear) to review the changes with fresh eyes, so I can fix issues before creating the PR rather than during PR review.

### Agent Definition

Create `.claude/agents/code-reviewer.md`:

```markdown
---
name: code-reviewer
description: Expert code reviewer for Python quantitative trading systems. Reviews for correctness, edge cases, performance, and strategy logic. Use for reviewing changes before PRs or before deploying to live.
tools: Read, Grep, Glob, Bash
model: sonnet
---

# Code Reviewer - VQTS Python

You are a senior code reviewer specializing in quantitative trading systems built with Python. You review code for correctness, safety, performance, and maintainability. You CANNOT edit files - you only report findings.

## Review Process

1. Understand what changed: `git diff`, `git log`, read modified files
2. Check strategy logic correctness
3. Check error handling and edge cases
4. Check performance implications
5. Check test coverage
6. Produce structured findings

## Output Format

For each finding:
```
[SEVERITY] Title
File: path/to/file.py:line_number
Issue: What's wrong and why it matters
Fix: Specific suggestion for how to fix it
```

Severity levels:
- [CRITICAL] - Will cause incorrect trades, data loss, or crashes in production
- [HIGH] - Significant bug or performance issue
- [MEDIUM] - Code quality, maintainability, or minor correctness issue
- [LOW] - Style, naming, or minor improvement

## Review Priorities

### CRITICAL - Strategy Logic
- Entry/exit signal calculations must be mathematically correct
- Position sizing must respect risk limits
- Stop-loss and trailing stop logic must never be bypassed
- Live vs backtest initialization paths must be correct
- Timezone handling (IST for Indian markets) must be consistent

### CRITICAL - Data Integrity
- DataFrame operations must not silently drop rows
- NaN handling must be explicit (not silently filled or dropped)
- Date/time index operations must handle market holidays
- Options chain data must validate strike prices and expiry dates

### HIGH - Error Handling
- API calls (broker, data provider) must have retry logic
- Network failures must not cause position state corruption
- CloudWatch metric publishing must not block trading logic

### HIGH - Performance
- Avoid loading full history when only recent data is needed
- Use vectorized pandas/numpy operations over row-by-row loops
- Watch for DataFrame copies vs views (`.copy()` awareness)
- Check for O(n^2) patterns in indicator calculations

### MEDIUM - Code Quality
- Type hints on function signatures
- Docstrings on public methods
- No hardcoded magic numbers (use config parameters)
- No commented-out code blocks

## Stop Conditions
- Stop and report if: same issue appears in 5+ places (it's a pattern, not individual bugs)
- Stop and escalate if: you discover the strategy has no tests at all
- Stop and escalate if: live trading config files are being modified
```

---

## Improvement 3: Write-Then-Review Pipeline Skill

### What It Does
A skill that orchestrates the full write-then-review workflow: implement -> clear context -> review -> fix -> verify. This is your stated goal of having "1 session writes code and other session reviews it."

### Why It Matters
- ECC's De-Sloppify pattern: implementation agent writes freely, cleanup agent reviews with fresh context
- Negative instructions ("don't over-engineer", "don't add extra comments") degrade quality because they consume reasoning budget on what NOT to do
- Separate context windows mean the reviewer has no "author bias"

### User Stories

**US-3.1: End-to-end feature development with built-in review**
> As a developer implementing a new indicator (e.g., ATR trailing stop), I want to run `/review-pipeline` which implements the feature, then automatically reviews it with a fresh context, fixes issues found, and runs tests - all in sequence - so I get production-quality code without manually switching between write and review modes.

**US-3.2: Bug fix with regression checking**
> As a developer fixing a bug in the v7 futures strategy, I want the pipeline to: (1) implement the fix, (2) review it for unintended side effects on other strategy paths, (3) verify existing tests still pass, so I don't introduce regressions.

**US-3.3: Cross-workspace write-review using WS1 and WS2**
> As a developer using multiple workspaces, I want to implement a feature in WS1, push the branch, then in WS2 run `/review-branch` which pulls the branch and reviews all changes with the code-reviewer agent, so I get a completely independent review with zero shared context.

### Skill Definition

Create `.claude/skills/review-pipeline/SKILL.md`:

```markdown
---
name: review-pipeline
description: Orchestrates a write-then-review pipeline. Implements changes, then reviews with fresh context, fixes issues, and verifies with tests. Use when you want code written AND reviewed in a single workflow.
allowed-tools: "Read, Write, Edit, Bash, Grep, Glob, Agent"
metadata:
  version: 1.0.0
  category: workflow
---

# Review Pipeline

Orchestrates: IMPLEMENT -> REVIEW -> FIX -> VERIFY

## Instructions

<instructions>

### Phase 1: IMPLEMENT
1. Understand the user's request fully
2. Read all relevant existing code and documentation
3. Implement the changes
4. Run basic smoke tests if available
5. Create a summary of all changes made (files modified, logic added/changed)
6. Write the summary to `.claude/pipeline-handoff.md`

### Phase 2: REVIEW (Fresh Context)
1. Use the code-reviewer agent to review all changes
2. Pass the agent the contents of `.claude/pipeline-handoff.md` and the git diff
3. The reviewer agent will produce structured findings with severity levels
4. Save findings to `.claude/pipeline-review.md`

### Phase 3: FIX
1. Read `.claude/pipeline-review.md`
2. Address all CRITICAL and HIGH findings
3. Address MEDIUM findings if the fix is straightforward
4. Skip LOW findings unless trivial
5. Update `.claude/pipeline-handoff.md` with fixes applied

### Phase 4: VERIFY
1. Run the full test suite for affected modules: `PYTHONPATH=src .venv/bin/pytest <test_paths> -v`
2. Run ruff check: `.venv/bin/ruff check <changed_files>`
3. Run black check: `.venv/bin/black --check <changed_files>`
4. If tests fail, go back to Phase 3 (max 2 loops)
5. Report final status to user

### Output to User
```
## Pipeline Results

### Implementation
- Files modified: [list]
- Summary: [what was done]

### Review Findings
- Critical: [count] (all addressed)
- High: [count] (all addressed)
- Medium: [count] ([addressed count] addressed)
- Low: [count] (skipped)

### Verification
- Tests: PASS/FAIL
- Lint: PASS/FAIL
- Format: PASS/FAIL

### Ready for PR: YES/NO
```

</instructions>
```

### Cross-Workspace Review Skill

Create `.claude/skills/review-branch/SKILL.md`:

```markdown
---
name: review-branch
description: Reviews changes on a git branch using the code-reviewer agent. Use in a separate workspace to review code written in another workspace. Pulls the branch, runs the reviewer, and reports findings.
allowed-tools: "Read, Bash(git *), Bash(gh *), Grep, Glob, Agent"
metadata:
  version: 1.0.0
  category: workflow
---

# Review Branch

Reviews a feature branch with the code-reviewer agent.

## Instructions

<instructions>

### Step 1: Identify the Branch
Ask the user which branch to review, or detect it from the current checkout.

### Step 2: Fetch and Diff
```bash
git fetch origin
git diff main...<branch> --stat
git diff main...<branch>
```

### Step 3: Invoke Code Reviewer
Use the code-reviewer agent with the full diff and file list.
Pass context about what the branch is intended to do (from commit messages or PR description).

### Step 4: Report
Produce structured review findings.
If using GitHub, optionally post findings as PR comments using `gh pr comment`.

</instructions>
```

---

## Improvement 4: Rules Directory for Enforced Guidelines

### What It Does
Rules in `.claude/rules/` are **system-injected** every session. Unlike CLAUDE.md (which Claude can de-prioritize under context pressure), rules are loaded at the system level and cannot be ignored.

### Why It Matters
- ECC finding: "Rules are system-injected - LLM cannot ignore them"
- Your CLAUDE.md is comprehensive but advisory - it guides, it doesn't enforce
- Critical safety rules for a live trading system should be non-negotiable

### User Stories

**US-4.1: Never modify live trading configs accidentally**
> As a developer who runs live strategies on EC2, I want Claude to ALWAYS confirm before modifying any file in `config/live/` or any cron job configuration, so I never accidentally break live trading during a development session.

**US-4.2: Always run tests before committing**
> As a developer, I want Claude to always run relevant tests before creating a commit, so I never push broken code to the shared repo.

**US-4.3: Strategy safety guardrails**
> As a developer working on strategy logic, I want Claude to always read the strategy's README.md before making changes, so it understands the full context of entry/exit rules and doesn't break existing logic.

### Rule Files

Create `.claude/rules/strategy-safety.md`:

```markdown
# Strategy Safety Rules

## Live Config Protection
- NEVER modify files in `config/live/` without explicit user confirmation
- NEVER modify cron jobs or EC2 deployment scripts without explicit user confirmation
- ALWAYS warn when changes could affect currently running live strategies

## Strategy Modification Protocol
- Before modifying ANY strategy file, READ the strategy's README.md first
- Before modifying entry/exit logic, state the current logic and the proposed change
- After modifying strategy logic, suggest running the relevant backtest

## Risk Management
- NEVER remove or weaken stop-loss logic
- NEVER increase position size limits without explicit user request
- NEVER modify risk parameters (max loss, max positions) without confirmation
```

Create `.claude/rules/testing.md`:

```markdown
# Testing Rules

## Before Committing
- Run pytest for all modified modules before creating a commit
- If no tests exist for the modified code, mention this to the user

## Test Commands
- Unit tests: `PYTHONPATH=src .venv/bin/pytest tests/ -v --tb=short`
- Strategy-specific: `PYTHONPATH=src .venv/bin/pytest tests/strategies/<strategy>/ -v`
- Quick smoke test: `PYTHONPATH=src .venv/bin/pytest tests/ -x --tb=short -q`

## Test Quality
- New strategy features should include at least one test for the happy path
- Bug fixes should include a regression test
```

Create `.claude/rules/git-workflow.md`:

```markdown
# Git Workflow Rules

## Branch Naming
- Features: `feature/<short-description>`
- Bug fixes: `fix/<short-description>`
- Strategy versions: `strategy/<type>-<version>` (e.g., `strategy/options-v8`)

## Commits
- Use conventional commit format: `type(scope): description`
- Types: feat, fix, refactor, test, docs, chore, style
- Scope should be the strategy or module name

## Before Pushing
- Ensure all tests pass
- Ensure ruff and black are clean
- Review the diff one more time
```

---

## Improvement 5: Session Memory Persistence via Hooks

### What It Does
Stop hooks save session context (what was worked on, decisions made, files changed) to a file. Next session can load this context to continue where you left off.

### Why It Matters
- Currently, every new Claude session starts cold - no memory of previous work
- For multi-day strategy development, you lose context about design decisions, partially completed work, and test results
- ECC recommends Stop + PreCompact hooks for persistence

### User Stories

**US-5.1: Resume strategy development across sessions**
> As a developer working on options-v8 over multiple days, I want Claude to remember what was completed, what's pending, and what design decisions were made, so I don't have to re-explain the full context each session.

**US-5.2: Preserve context before auto-compact**
> As a developer in a long session exploring multiple strategy approaches, I want important decisions and findings to be saved before Claude's context window compacts, so I don't lose the reasoning behind design choices.

**US-5.3: Hand off between workspaces**
> As a developer who implements in WS1 and reviews in WS2, I want the implementation session's summary to be available as context in the review session, so the reviewer understands the intent behind changes.

### Configuration

Add to hooks in `.claude/settings.local.json`:

```json
{
  "hooks": {
    "Stop": [
      {
        "command": "cd \"$(git rev-parse --show-toplevel)\" && echo \"## Session $(date '+%Y-%m-%d %H:%M')\" > .claude/last-session.md && echo '' >> .claude/last-session.md && echo '### Changes' >> .claude/last-session.md && git diff --stat HEAD >> .claude/last-session.md 2>/dev/null && echo '' >> .claude/last-session.md && echo '### Staged' >> .claude/last-session.md && git diff --cached --stat >> .claude/last-session.md 2>/dev/null && echo '' >> .claude/last-session.md && echo '### Recent Commits' >> .claude/last-session.md && git log --oneline -5 >> .claude/last-session.md 2>/dev/null"
      }
    ],
    "PreCompact": [
      {
        "command": "cd \"$(git rev-parse --show-toplevel)\" && echo \"## Pre-Compact Save $(date '+%Y-%m-%d %H:%M')\" > .claude/pre-compact-state.md && echo '' >> .claude/pre-compact-state.md && echo '### Working Changes' >> .claude/pre-compact-state.md && git diff --stat HEAD >> .claude/pre-compact-state.md 2>/dev/null && echo '' >> .claude/pre-compact-state.md && echo '### Uncommitted Files' >> .claude/pre-compact-state.md && git status --short >> .claude/pre-compact-state.md 2>/dev/null"
      }
    ]
  }
}
```

### CLAUDE.md Addition

Add to CLAUDE.md so Claude reads session memory on startup:

```markdown
## Session Continuity
- On session start, check `.claude/last-session.md` for previous session context
- On session start, check `.claude/pre-compact-state.md` for pre-compact saves
- Use these to understand what was previously worked on and continue seamlessly
```

---

## Improvement 6: PreToolUse Safety Hooks

### What It Does
Blocks or warns before dangerous operations: destructive file operations, force pushes, live config modifications, and linter config changes (LLMs will modify linter configs to pass instead of fixing code).

### Why It Matters
- ECC finding: "LLMs will modify linter configs to pass instead of fixing code"
- A live trading system cannot afford accidental deletions or config corruption
- PreToolUse hooks provide a hard gate before dangerous actions execute

### User Stories

**US-6.1: Prevent accidental deletion of strategy data**
> As a developer, I want Claude to be blocked from running `rm -rf` on any directory inside `src/` or `data/`, so I never accidentally lose strategy code or historical data.

**US-6.2: Block linter config modifications**
> As a developer, I want Claude to be prevented from editing `.ruff.toml`, `pyproject.toml` linting sections, or `.flake8` configs, so it fixes the actual code instead of loosening the rules.

**US-6.3: Warn before modifying live configs**
> As a developer, I want Claude to show a warning before editing any file matching `**/live/**` or `**/production/**`, so I'm always aware when changes could affect running strategies.

### Configuration

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Edit|Write",
        "command": "if echo \"$CLAUDE_FILE_PATH\" | grep -qE '(\\.ruff\\.toml|ruff\\.toml|\\.flake8|setup\\.cfg)$'; then echo 'BLOCKED: Do not modify linter configuration. Fix the code instead.' >&2; exit 2; fi"
      },
      {
        "matcher": "Edit|Write",
        "command": "if echo \"$CLAUDE_FILE_PATH\" | grep -qE '(config/live/|/production/)'; then echo 'WARNING: You are about to modify a LIVE configuration file. Proceed with extreme caution.' >&2; fi"
      }
    ]
  }
}
```

---

## Improvement 7: Missing Agents

### What's Missing

You have `python-pro` (implementation) and `search-specialist` (research). Based on ECC's 14-agent architecture, you're missing key agents for a complete workflow.

### User Stories

**US-7.1: Test-driven strategy development**
> As a developer building a new strategy version, I want a TDD guide agent that writes tests first, then helps me implement against those tests, so I have comprehensive test coverage from the start instead of bolting on tests after.

**US-7.2: Architecture planning for complex features**
> As a developer adding a major feature (e.g., multi-timeframe analysis), I want a planner agent that breaks the work into phases with verification criteria, so I don't get lost in a large implementation.

**US-7.3: Surgical build error resolution**
> As a developer who just merged a branch and tests are failing, I want a build-error-resolver agent that focuses solely on fixing the failures without changing unrelated code, so I get back to green quickly.

### Agent Definitions

Create `.claude/agents/tdd-guide.md`:

```markdown
---
name: tdd-guide
description: Test-driven development guide for Python trading strategies. Writes tests first, then implements. Use when developing new features or strategy versions.
tools: Read, Write, Edit, Bash, Grep, Glob
model: sonnet
---

# TDD Guide - VQTS Python

You enforce test-driven development for trading strategy code.

## Process
1. Understand the requirement
2. Write failing tests FIRST
3. Implement minimal code to pass tests
4. Refactor while keeping tests green
5. Add edge case tests

## Testing Conventions
- Test files: `tests/` mirror `src/` structure
- Fixtures: use conftest.py for shared fixtures
- Mocking: mock external APIs (broker, data provider), never mock strategy logic
- Run: `PYTHONPATH=src .venv/bin/pytest <test_file> -v`

## Strategy Test Patterns
- Test entry signals with known data
- Test exit signals with known data
- Test position sizing with edge cases (0 capital, max positions)
- Test stop-loss triggers
- Test with NaN/missing data
- Test timezone boundaries (market open/close)
```

Create `.claude/agents/planner.md`:

```markdown
---
name: planner
description: Breaks complex tasks into phased plans with verification criteria. Use for large features, refactors, or multi-day projects.
tools: Read, Grep, Glob, Bash
model: opus
---

# Planner - VQTS Python

You create structured implementation plans for complex tasks.

## Output Format
```
## Plan: [Task Name]

### Phase 1: [Name]
- [ ] Step 1
- [ ] Step 2
- Verify: [How to verify this phase is complete]
- Files: [Files that will be modified]

### Phase 2: [Name]
...
```

## Planning Rules
- Read existing code before planning changes
- Each phase should be independently verifiable
- Phases should be small enough for a single session
- Identify dependencies between phases
- Call out risks and unknowns upfront
```

Create `.claude/agents/build-error-resolver.md`:

```markdown
---
name: build-error-resolver
description: Fixes test failures and build errors surgically without changing unrelated code. Use when tests are failing and you need to get back to green.
tools: Read, Write, Edit, Bash, Grep, Glob
model: sonnet
---

# Build Error Resolver - VQTS Python

You fix build and test failures with minimal changes. You do NOT refactor, add features, or improve code quality. You ONLY fix what's broken.

## Process
1. Run the failing tests to see exact errors
2. Read the failing test and the code it tests
3. Identify the root cause
4. Make the minimal fix
5. Re-run tests to confirm

## Rules
- NEVER change test expectations unless the test is genuinely wrong
- NEVER modify unrelated files
- NEVER add new features while fixing
- If the fix requires architectural changes, STOP and report
- Max 3 fix attempts per error, then escalate

## Commands
- Run all tests: `PYTHONPATH=src .venv/bin/pytest tests/ -v --tb=short`
- Run specific: `PYTHONPATH=src .venv/bin/pytest <test_file>::<test_name> -v`
- Run with output: `PYTHONPATH=src .venv/bin/pytest <test_file> -v -s`
```

---

## Improvement 8: Git Worktrees Instead of Multiple Clones

### What It Does
Replaces 3 separate git clones with git worktrees that share a single `.git` directory. Each worktree is an independent checkout on its own branch but shares history, remotes, and reflog.

### Why It Matters
- Less disk usage (shared `.git` directory)
- No clone drift (all worktrees see the same remotes and branches)
- Branches are visible across worktrees (can't accidentally be on the same branch)
- ECC explicitly recommends worktrees for parallel Claude Code sessions

### User Stories

**US-8.1: Parallel feature development without conflicts**
> As a developer working on two features simultaneously (e.g., options-v8 and ATR trailing stops), I want each Claude session to work on its own branch in its own worktree, so changes don't interfere with each other and I can merge them independently.

**US-8.2: Write in one worktree, review in another**
> As a developer, I want to implement a feature in worktree-1 on branch `feature/x`, then switch to worktree-2 and review that branch, so the reviewer has a completely separate working directory and context.

**US-8.3: Keep main always clean**
> As a developer, I want the main worktree to always be on `main` and never have uncommitted changes, so I can always run backtests against the known-good baseline.

### Migration Plan

```bash
# From your current TechnicalAnalysis workspace (keep as the main worktree):
cd /Users/vicky/workspaces/TechnicalAnalysis/VQTS-Python

# Create worktrees for writer and reviewer roles:
git worktree add /Users/vicky/workspaces/VQTS-Writer writer-workspace
git worktree add /Users/vicky/workspaces/VQTS-Reviewer reviewer-workspace

# Each worktree gets its own .claude/ config (copy from main):
cp -r .claude/ /Users/vicky/workspaces/VQTS-Writer/.claude/
cp -r .claude/ /Users/vicky/workspaces/VQTS-Reviewer/.claude/

# Open Claude sessions:
# Terminal 1: cd /Users/vicky/workspaces/VQTS-Writer && claude
# Terminal 2: cd /Users/vicky/workspaces/VQTS-Reviewer && claude
```

### Worktree Workflow

```
Main Worktree (TechnicalAnalysis/VQTS-Python)
  |-- branch: main (always clean, baseline for backtests)
  |
Writer Worktree (VQTS-Writer)
  |-- branch: feature/current-work
  |-- Claude session: implements features
  |-- Pushes branch when done
  |
Reviewer Worktree (VQTS-Reviewer)
  |-- branch: feature/current-work (fetched)
  |-- Claude session: reviews code, runs tests
  |-- Pushes fixes or creates PR comments
```

---

## Improvement 9: Config Sync Across Workspaces

### What It Does
Ensures all workspaces share the same Claude configuration (settings, agents, skills, rules) from a single source of truth.

### Why It Matters
- Currently WS3 has extra Read permissions that WS1/WS2 don't have
- When you add a new skill or agent, you'd need to copy it to all 3 workspaces manually
- Config drift leads to inconsistent behavior between workspaces

### User Stories

**US-9.1: Add a skill once, available everywhere**
> As a developer, when I create a new skill (e.g., review-pipeline), I want it to be available in all my workspaces without manually copying files, so I maintain one configuration.

**US-9.2: Consistent permissions across workspaces**
> As a developer, I want all workspaces to have the same permission settings, so Claude behaves the same way regardless of which workspace I'm in.

### Implementation

**Option A: Symlinks (simplest)**
```bash
# Designate WS1 as canonical:
CANONICAL=/Users/vicky/workspaces/TechnicalAnalysis/VQTS-Python/.claude

# Replace WS2 and WS3 .claude dirs with symlinks:
rm -rf /Users/vicky/workspaces/TechnicalAnalysis2/VQTS-Python/.claude
ln -s $CANONICAL /Users/vicky/workspaces/TechnicalAnalysis2/VQTS-Python/.claude

rm -rf /Users/vicky/workspaces/TechnicalAnalysis3/VQTS-Python/.claude
ln -s $CANONICAL /Users/vicky/workspaces/TechnicalAnalysis3/VQTS-Python/.claude
```

**Option B: Sync script**
```bash
#!/bin/bash
# sync-claude-config.sh
CANONICAL=/Users/vicky/workspaces/TechnicalAnalysis/VQTS-Python/.claude
TARGETS=(
  /Users/vicky/workspaces/TechnicalAnalysis2/VQTS-Python/.claude
  /Users/vicky/workspaces/TechnicalAnalysis3/VQTS-Python/.claude
)

for target in "${TARGETS[@]}"; do
  rsync -av --delete \
    --exclude 'last-session.md' \
    --exclude 'pre-compact-state.md' \
    --exclude 'pipeline-*.md' \
    "$CANONICAL/" "$target/"
done
echo "Config synced to ${#TARGETS[@]} workspaces"
```

Note: The rsync excludes session-specific files so each workspace maintains its own session state.

---

## Improvement 10: Context-Specific Aliases

### What It Does
Shell aliases that launch Claude with different system prompts for different roles: development, review, research, debugging.

### Why It Matters
- ECC recommends context-specific modes via `--system-prompt`
- System prompts have the highest authority (above CLAUDE.md and tool results)
- Different tasks benefit from different Claude "personas"

### User Stories

**US-10.1: Launch Claude in review mode**
> As a developer, I want to type `claude-review` and get a Claude session that's preconfigured for code review (read-only mindset, structured output, focus on correctness), so I don't have to explain the review protocol each time.

**US-10.2: Launch Claude in strategy development mode**
> As a developer, I want to type `claude-strategy` and get a Claude session that automatically loads the strategy development skill and relevant docs, so I can jump straight into strategy work.

**US-10.3: Launch Claude in debugging mode**
> As a developer debugging a failing backtest, I want to type `claude-debug` and get a Claude session focused on systematic debugging (reproduce, isolate, fix, verify), so it doesn't try to refactor or improve while I'm trying to find a bug.

### Implementation

Add to `~/.zshrc`:

```bash
# VQTS Claude Aliases
alias claude-review='claude --system-prompt "You are in REVIEW MODE. Focus only on reviewing code for correctness, edge cases, and bugs. Do not write code. Report findings in structured format with severity levels. Prioritize strategy logic and data integrity issues."'

alias claude-strategy='claude --system-prompt "You are in STRATEGY DEVELOPMENT MODE. Always read the relevant strategy README.md first. Follow TDD: write tests before implementation. Run backtests to verify. Never modify live configs."'

alias claude-debug='claude --system-prompt "You are in DEBUG MODE. Follow systematic debugging: 1) Reproduce the issue 2) Isolate the root cause 3) Fix minimally 4) Verify the fix 5) Add a regression test. Do NOT refactor or improve unrelated code."'

alias claude-plan='claude --system-prompt "You are in PLANNING MODE. Do not write code. Break the task into phases with verification criteria. Identify risks, dependencies, and unknowns. Output a structured plan in markdown."'
```

---

## Implementation Priority Matrix

| # | Improvement | Impact | Effort | Dependencies | Priority |
|---|-------------|--------|--------|--------------|----------|
| 1 | PostToolUse hooks (auto-lint) | High | Low | None | **P0 - Do first** |
| 2 | Code reviewer agent | High | Low | None | **P0 - Do first** |
| 6 | PreToolUse safety hooks | High | Low | None | **P0 - Do first** |
| 4 | Rules directory | High | Medium | None | **P1 - Do second** |
| 3 | Review pipeline skill | High | Medium | #2 (code-reviewer) | **P1 - Do second** |
| 5 | Session memory hooks | Medium | Low | None | **P2 - Do third** |
| 7 | Missing agents (TDD, planner, build-resolver) | Medium | Medium | None | **P2 - Do third** |
| 10 | Context-specific aliases | Medium | Low | None | **P2 - Do third** |
| 8 | Git worktrees migration | Medium | High | Backup existing work | **P3 - Plan carefully** |
| 9 | Config sync | Low | Low | #8 or current setup | **P3 - After worktrees** |

### Suggested Rollout

**Week 1**: Improvements 1, 2, 6 (hooks + code-reviewer agent)
- Add PostToolUse hooks to all 3 workspaces
- Add PreToolUse safety hooks
- Create code-reviewer agent
- Test by running a review on recent commits

**Week 2**: Improvements 3, 4 (pipeline + rules)
- Create rules directory with strategy-safety, testing, git-workflow
- Create review-pipeline and review-branch skills
- Test the full write-then-review pipeline on a small feature

**Week 3**: Improvements 5, 7, 10 (memory + agents + aliases)
- Add Stop/PreCompact hooks for session memory
- Create TDD guide, planner, and build-error-resolver agents
- Set up shell aliases

**Week 4**: Improvements 8, 9 (worktrees + sync)
- Plan the migration from 3 clones to worktrees
- Ensure all branches are pushed and nothing is lost
- Create worktrees and copy config
- Set up config sync mechanism

---

## Combined settings.local.json (All Hooks)

Here is the complete hooks section combining all hook improvements:

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Edit|Write",
        "command": "if echo \"$CLAUDE_FILE_PATH\" | grep -qE '(\\.ruff\\.toml|ruff\\.toml|\\.flake8|setup\\.cfg)$'; then echo 'BLOCKED: Do not modify linter configuration. Fix the code instead.' >&2; exit 2; fi"
      },
      {
        "matcher": "Edit|Write",
        "command": "if echo \"$CLAUDE_FILE_PATH\" | grep -qE '(config/live/|/production/)'; then echo 'WARNING: Modifying a LIVE configuration file. Proceed with extreme caution.' >&2; fi"
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "command": "if echo \"$CLAUDE_FILE_PATH\" | grep -q '\\.py$'; then cd \"$(git rev-parse --show-toplevel)\" && .venv/bin/ruff check --fix \"$CLAUDE_FILE_PATH\" 2>&1 | head -20; fi"
      },
      {
        "matcher": "Edit|Write",
        "command": "if echo \"$CLAUDE_FILE_PATH\" | grep -q '\\.py$'; then cd \"$(git rev-parse --show-toplevel)\" && .venv/bin/black --quiet \"$CLAUDE_FILE_PATH\" 2>&1 | head -10; fi"
      }
    ],
    "Stop": [
      {
        "command": "cd \"$(git rev-parse --show-toplevel)\" && mkdir -p .claude && echo \"## Session $(date '+%Y-%m-%d %H:%M')\" > .claude/last-session.md && echo '' >> .claude/last-session.md && echo '### Changes' >> .claude/last-session.md && git diff --stat HEAD >> .claude/last-session.md 2>/dev/null && echo '' >> .claude/last-session.md && echo '### Staged' >> .claude/last-session.md && git diff --cached --stat >> .claude/last-session.md 2>/dev/null && echo '' >> .claude/last-session.md && echo '### Recent Commits' >> .claude/last-session.md && git log --oneline -5 >> .claude/last-session.md 2>/dev/null"
      }
    ],
    "PreCompact": [
      {
        "command": "cd \"$(git rev-parse --show-toplevel)\" && mkdir -p .claude && echo \"## Pre-Compact $(date '+%Y-%m-%d %H:%M')\" > .claude/pre-compact-state.md && echo '' >> .claude/pre-compact-state.md && echo '### Working Changes' >> .claude/pre-compact-state.md && git diff --stat HEAD >> .claude/pre-compact-state.md 2>/dev/null && echo '' >> .claude/pre-compact-state.md && echo '### Status' >> .claude/pre-compact-state.md && git status --short >> .claude/pre-compact-state.md 2>/dev/null"
      }
    ]
  }
}
```
