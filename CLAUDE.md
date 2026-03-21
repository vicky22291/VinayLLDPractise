# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

## 5. Workspace Skills (`claude-files/skills/`)

This workspace includes custom Claude Code skills in `claude-files/skills/`. These are also installed globally at `~/.claude/skills/`.

| Skill | Purpose |
|-------|---------|
| `kotlin-pro` | Staff SWE persona for production-grade Kotlin code. Loads on-demand references for OOPs/design patterns, logging, naming/docs/collections conventions |
| `skill-creator` | Guides creation of new Claude Code skills |
| `planning-tests` | Creates test plans before writing test code |
| `project-planning` | Requirements gathering to design documentation |
| `task-breakdown` | Generates task files and YAML from design docs |
| `run-tasks` | Automates task implementation via task runner |
| `addressing-pr-comments` | Systematically addresses PR review feedback |
| `verify-pr-comments` | Verifies all PR comments are resolved |

### Kotlin Projects

When writing or modifying Kotlin code in this workspace, the `kotlin-pro` skill should be used. It references:
- `resources/oops-standards-and-design-patterns.md` — SOLID, GoF patterns, Kotlin idioms
- `resources/logging-best-practices.md` — kotlin-logging, SLF4J, MDC, log levels
- `resources/coding-conventions.md` — naming, documentation, collections/sequences/flows

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
