# ECC - Agents, Subagents & Orchestration

## Agent Architecture

Each agent is a markdown file with YAML frontmatter specifying:
- `name` - Agent identifier
- `description` - When to use (triggers automatic selection)
- `tools` - Allowed tools (scoped permissions)
- `model` - Which model to use (haiku/sonnet/opus)

### Agent Format
```markdown
---
name: code-reviewer
description: Expert code reviewer for quality, security, and best practices.
tools: ["Read", "Grep", "Glob", "Bash"]
model: sonnet
---

# Code Reviewer

You are a senior code reviewer...

## Review Priorities
### CRITICAL - Security
...
### HIGH - Code Quality
...
```

---

## 14 Specialized Agents

### Core Workflow Agents

| Agent | Model | Tools | Purpose |
|-------|-------|-------|---------|
| **planner** | opus | Read, Grep, Glob, Bash | Break tasks into phases with verification criteria |
| **architect** | opus | Read, Grep, Glob, Bash | System design, tech stack decisions, architecture docs |
| **tdd-guide** | sonnet | Read, Write, Edit, Bash, Grep, Glob | Test-driven development enforcement |
| **code-reviewer** | sonnet | Read, Grep, Glob, Bash | Quality, security, performance review |
| **security-reviewer** | opus | Read, Grep, Glob, Bash | Security-focused audit (OWASP, auth, injection) |
| **build-error-resolver** | sonnet | Read, Write, Edit, Bash, Grep, Glob | Fix build/compile errors surgically |
| **e2e-runner** | sonnet | Read, Write, Edit, Bash, Grep, Glob | End-to-end test writing and execution |
| **refactor-cleaner** | sonnet | Read, Write, Edit, Bash, Grep, Glob | Code cleanup, dead code removal, pattern improvement |
| **doc-updater** | haiku | Read, Write, Edit, Bash, Grep, Glob | Documentation and codemap generation |

### Language-Specific Agents

| Agent | Model | Purpose |
|-------|-------|---------|
| **go-reviewer** | sonnet | Go code review (concurrency, error handling, idiomatic Go) |
| **go-build-resolver** | sonnet | Fix Go build/vet/lint errors |
| **python-reviewer** | sonnet | Python code review (PEP 8, type hints, security) |
| **database-reviewer** | sonnet | PostgreSQL optimization, schema design, RLS, indexing |

### Special Agents

| Agent | Model | Purpose |
|-------|-------|---------|
| **chief-of-staff** | opus | Multi-channel communication triage (email, Slack, LINE, Messenger) with 4-tier classification |

---

## Key Agent Design Patterns

### 1. Tool Scoping (Least Privilege)
- **Read-only agents** (reviewers): `["Read", "Grep", "Glob", "Bash"]`
- **Write agents** (builders/fixers): `["Read", "Write", "Edit", "Bash", "Grep", "Glob"]`
- Never give more tools than needed

### 2. Model Selection by Task
- **Haiku**: Simple tasks (docs, file discovery, single-file edits)
- **Sonnet**: 90% of coding (multi-file, reviews, implementation)
- **Opus**: Complex reasoning (architecture, security, debugging)

### 3. Structured Output
Every agent defines a clear output format:
```text
[SEVERITY] Issue title
File: path/to/file.py:42
Issue: Description
Fix: What to change
```

### 4. Stop Conditions
Agents define when to stop and escalate:
- Same error persists after 3 fix attempts
- Fix introduces more errors than it resolves
- Error requires architectural changes beyond scope

---

## Subagent Orchestration

### The Subagent Context Problem
Sub-agents return summaries to save context but lack semantic understanding of the orchestrator's objectives. They only receive literal queries, not the full context.

### Solution: Iterative Retrieval Pattern
```
1. Orchestrator sends query + objective context to sub-agent
2. Sub-agent returns initial results
3. Orchestrator evaluates results against objective
4. Orchestrator asks follow-up questions (max 3 cycles)
5. Accept final results when sufficient
```

**Key**: Pass objective context, not just the query.

### Sequential Phase Orchestration
```
Phase 1: RESEARCH    (Explore agent)           -> research-summary.md
Phase 2: PLAN        (Planner agent)           -> plan.md
Phase 3: IMPLEMENT   (TDD-guide agent)         -> code changes
Phase 4: REVIEW      (Code-reviewer agent)     -> review-comments.md
Phase 5: VERIFY      (Build-error-resolver)    -> complete or loop
```

**Rules**:
1. Each agent receives ONE clear input, produces ONE clear output
2. Outputs become next phase inputs
3. Never skip phases
4. Use `/clear` between agents
5. Store intermediate outputs in files

---

## Parallelization Strategies

### Git Worktrees (Preferred for Parallel Code Changes)
```bash
git worktree add ../project-feature-a feature-a
git worktree add ../project-feature-b feature-b
cd ../project-feature-a && claude
```

### Fork Conversations
Use `/fork` for non-overlapping tasks. Best for:
- Main chat: code changes
- Forks: codebase questions, external service research

### Cascade Method (Multiple Claude Instances)
- Open new tasks in new tabs to the right
- Process left to right, oldest to newest
- Focus on at most 3-4 simultaneous tasks
- Use `/rename` to label all chats

### Two-Instance Kickoff Pattern (New Projects)
**Instance 1 (Scaffolding)**: Project structure, configs, CLAUDE.md, rules
**Instance 2 (Research)**: PRD, architecture diagrams, service connections, documentation

### Philosophy
Minimize parallelization while meeting needs. Add terminals only when necessary.

---

## Multi-Agent Commands

| Command | Purpose |
|---------|---------|
| `/orchestrate` | Full orchestration pipeline across agents |
| `/multi-plan` | Multi-agent planning across different areas |
| `/multi-execute` | Parallel execution of planned tasks |
| `/multi-backend` | Backend-focused multi-agent work |
| `/multi-frontend` | Frontend-focused multi-agent work |
| `/multi-workflow` | Custom multi-agent workflow definition |
| `/pm2` | PM2 process management for long-running agents |

### Multi-Model Collaboration (CCG System)

The `/multi-*` commands implement a Claude-Codex-Gemini orchestration:
- **Claude** = orchestrator with "code sovereignty" (ONLY Claude writes to filesystem)
- **Codex** = backend authority (trusted for backend decisions)
- **Gemini** = frontend authority (trusted for frontend decisions)

**Trust hierarchy**: Backend decisions follow Codex. Frontend decisions follow Gemini.
External models provide "dirty prototypes" only — Claude refactors to production-grade.
Session IDs enable context reuse across phases via `resume <SESSION_ID>`.

---

## Autonomous Loop Patterns (6 Patterns)

### 1. Sequential Pipeline (`claude -p`)
Chain of non-interactive calls, each a focused step with fresh context. Use `--allowedTools` for read-only vs write passes.

### 2. NanoClaw REPL
Persistent session REPL using Markdown-as-database for conversation history.

### 3. Infinite Agentic Loop (credit: @disler)
Orchestrator parses spec + assigns creative directions to N parallel sub-agents. Prevents duplicates via explicit assignment, not self-differentiation.

### 4. Continuous Claude PR Loop (credit: @AnandChowdhary)
Creates branches -> runs `claude -p` -> commits -> creates PRs -> waits for CI -> auto-fixes failures -> merges. Uses `SHARED_TASK_NOTES.md` for cross-iteration context. Completion signal: magic phrase with threshold (3 consecutive signals = done).

### 5. De-Sloppify Pattern
Separate cleanup pass after implementation. **Key insight**: negative instructions ("don't do X") degrade quality. Two focused agents outperform one constrained agent.

### 6. Ralphinho / RFC-Driven DAG (credit: @enitrat)
Most sophisticated. RFC decomposition into work units with dependency DAG. Complexity tiers determine pipeline depth. Separate context windows eliminate author bias. Model routing per stage (Research=Sonnet, Plan=Opus, Implement=Codex, Review=Opus).

### Decision Matrix
- Single focused change -> Sequential Pipeline
- Written spec + parallel needed -> Ralphinho
- Many variations of same task -> Infinite Loop
- Iterative PR workflow -> Continuous Claude

---

## Chief-of-Staff Agent (Communication Triage)

### 4-Tier Classification
1. **skip** (auto-archive): Bot messages, notifications, automated alerts
2. **info_only** (summary only): CC'd emails, group chat chatter, @channel announcements
3. **meeting_info** (calendar cross-reference): Zoom/Teams URLs, date + meeting context
4. **action_required** (draft reply): Direct questions, @mentions, scheduling requests

### Key Design Principles
- **Hooks over prompts**: LLMs forget instructions ~20% of the time. PostToolUse hooks enforce checklists at the tool level
- **Scripts for deterministic logic**: Calendar math, timezone handling → use scripts, not the LLM
- **Knowledge files as memory**: `relationships.md`, `preferences.md`, `todo.md` persist via git
- **Rules are system-injected**: `.claude/rules/*.md` load automatically — LLM cannot ignore them
