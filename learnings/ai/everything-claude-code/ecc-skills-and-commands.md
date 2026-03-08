# ECC - Skills & Commands System

## Skills Architecture

Skills are workflow rules with specific scopes stored as `SKILL.md` files in directories.

### Storage Locations
- User-level: `~/.claude/skills/<skill-name>/SKILL.md`
- Project-level: `.claude/skills/<skill-name>/SKILL.md`

### Skill Format
```markdown
---
name: skill-name
description: "When this skill activates and what it does"
origin: ECC  # or community
---

# Skill Title

## When to Activate
- Trigger conditions

## Workflow
- Step-by-step instructions

## Key Patterns
- Domain-specific patterns
```

---

## Key Skills (56+ total)

### Workflow & Quality Skills

| Skill | Purpose |
|-------|---------|
| **tdd-workflow** | Enforces TDD with 80%+ coverage: unit, integration, E2E |
| **verification-loop** | Multi-step verification: build, lint, test, security scan before completion |
| **eval-harness** | Formal evaluation framework for eval-driven development (EDD) |
| **strategic-compact** | Suggests manual context compaction at logical intervals |
| **continuous-learning** | Auto-extracts reusable patterns from sessions as learned skills |
| **continuous-learning-v2** | Instinct-based learning with confidence scoring |
| **iterative-retrieval** | Progressive context refinement for subagent context problem |
| **search-first** | Research-first development — search before coding |
| **security-review** | Security checklist: auth, input validation, secrets, API, payment |
| **autonomous-loops** | Patterns for autonomous agent execution loops |

### Plankton Code Quality (Community)
Write-time enforcement via PostToolUse hooks:

**Three-Phase Architecture:**
1. **Auto-Format (Silent)** — Runs formatters, fixes 40-50% of issues
2. **Collect Violations (JSON)** — Structured violation output
3. **Delegate + Verify** — Spawns claude subprocess, routes by complexity:
   - Haiku: formatting, imports, style (120s timeout)
   - Sonnet: complexity, refactoring (300s timeout)
   - Opus: type system, deep reasoning (600s timeout)

**Config Protection**: Blocks LLMs from modifying linter configs to pass instead of fixing code. Three layers:
- PreToolUse hook blocks config file edits
- Stop hook detects config changes via `git diff`
- Protected files list

### Cost-Aware LLM Pipeline
Patterns for controlling API costs:
- Model routing by task complexity (text length, item count)
- Immutable cost tracking with budget limits
- Narrow retry logic (only transient errors)
- Prompt caching for system prompts > 1024 tokens

### Framework-Specific Skills

| Category | Skills |
|----------|--------|
| **Django** | django-patterns, django-security, django-tdd, django-verification |
| **Spring Boot** | springboot-patterns, springboot-security, springboot-tdd, springboot-verification |
| **Go** | golang-patterns, golang-testing |
| **Python** | python-patterns, python-testing |
| **Java** | java-coding-standards, jpa-patterns |
| **C++** | cpp-coding-standards, cpp-testing |
| **Frontend** | frontend-patterns, frontend-slides |
| **Backend** | backend-patterns, api-design |
| **Database** | postgres-patterns, database-migrations, clickhouse-io |
| **DevOps** | deployment-patterns, docker-patterns, e2e-testing |

### Business & Content Skills
| Skill | Purpose |
|-------|---------|
| **article-writing** | Long-form writing in supplied voice |
| **content-engine** | Multi-platform social content and repurposing |
| **market-research** | Source-attributed market/competitor research |
| **investor-materials** | Pitch decks, memos, financial models |
| **investor-outreach** | Personalized investor emails and follow-ups |

---

## Commands System (32+)

Commands are slash commands invoked by users. Stored in `commands/*.md`.

### Core Commands

| Command | Purpose |
|---------|---------|
| `/plan` | Create implementation plan with verification criteria |
| `/tdd` | Test-driven development workflow |
| `/e2e` | End-to-end test writing and execution |
| `/code-review` | Trigger code review via agent |
| `/build-fix` | Fix build errors via agent |
| `/refactor-clean` | Code cleanup and refactoring |
| `/verify` | Run verification loop (build + lint + test + security) |
| `/checkpoint` | Create file-level undo point |
| `/test-coverage` | Run tests and report coverage |

### Learning & Evolution Commands

| Command | Purpose |
|---------|---------|
| `/learn` | Extract reusable patterns from current session |
| `/learn-eval` | Evaluate learned patterns against real usage |
| `/evolve` | Evolve instincts into skills/commands/agents |
| `/skill-create` | Create new skill from git history analysis |

### Instinct Management

| Command | Purpose |
|---------|---------|
| `/instinct-status` | View current instinct confidence levels |
| `/instinct-import` | Import instincts from external source |
| `/instinct-export` | Export instincts for sharing |

### Session & Documentation

| Command | Purpose |
|---------|---------|
| `/sessions` | Manage session context and memory |
| `/update-docs` | Update documentation from code |
| `/update-codemaps` | Generate/update architectural codemaps |
| `/eval` | Run evaluation harness |

### Language-Specific

| Command | Purpose |
|---------|---------|
| `/go-review` | Go code review |
| `/go-test` | Go test execution |
| `/go-build` | Go build error resolution |
| `/python-review` | Python code review |

---

## Verification Loop Pattern

The verification skill enforces a multi-step quality gate:

```
1. BUILD     — Compile/transpile successfully
2. LINT      — Pass all linting rules
3. TEST      — Pass all tests with coverage threshold
4. SECURITY  — Pass security scan
5. REVIEW    — Pass automated code review
```

Each step must pass before moving to next. Failures loop back to fix.

---

## Eval-Driven Development (EDD)

### Benchmarking Workflow
1. Fork conversations
2. Run with skill vs without skill
3. Compare diffs

### Key Metrics
```
pass@k: At least ONE of k attempts succeeds
        k=1: 70%  k=3: 91%  k=5: 97%

pass^k: ALL k attempts must succeed
        k=1: 70%  k=3: 34%  k=5: 17%
```

- Use **pass@k** when only one success needed
- Use **pass^k** when consistency is essential

### Evaluation Types
- **Checkpoint-Based**: Set explicit checkpoints, verify against criteria, fix before proceeding
- **Continuous**: Run every N minutes or after major changes (full test suite + linting)
