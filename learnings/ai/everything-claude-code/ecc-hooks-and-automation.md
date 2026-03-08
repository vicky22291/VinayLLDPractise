# ECC - Hooks & Automation

## Hook Types

Claude Code hooks are event-triggered automations executing on lifecycle events:

| Hook Event | When | Use Cases |
|-----------|------|-----------|
| **PreToolUse** | Before tool execution | Validation, reminders, blocking dangerous actions |
| **PostToolUse** | After tool completion | Formatting, linting, feedback, quality checks |
| **UserPromptSubmit** | When user sends message | Context injection, workflow routing |
| **Stop** | When Claude finishes responding | Session end persistence, learning extraction |
| **PreCompact** | Before context compaction | Save important state before context shrinks |
| **Notification** | On permission requests | Custom notification handling |

---

## Hook Configuration

Hooks are defined in `.claude/settings.json`:

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "command": "npx prettier --write $CLAUDE_FILE_PATH"
      },
      {
        "matcher": "Edit|Write",
        "command": "npx tsc --noEmit 2>&1 | head -20"
      }
    ],
    "PreToolUse": [
      {
        "matcher": "Bash",
        "command": "echo 'Remember: use tmux for long-running commands'"
      }
    ],
    "Stop": [
      {
        "command": "node scripts/hooks/session-end.js"
      }
    ],
    "PreCompact": [
      {
        "command": "node scripts/hooks/pre-compact.js"
      }
    ]
  }
}
```

---

## Key Hook Patterns

### 1. Auto-Format on Edit (PostToolUse)
```json
{
  "matcher": "Edit|Write",
  "command": "npx prettier --write $CLAUDE_FILE_PATH"
}
```

### 2. TypeScript Type-Check on Edit (PostToolUse)
```json
{
  "matcher": "Edit|Write",
  "command": "npx tsc --noEmit 2>&1 | head -20"
}
```

### 3. Console.log Warning (PostToolUse)
Warn when console.log statements are added to code.

### 4. Block Unauthorized Markdown Edits (PreToolUse)
Prevent accidental modification of critical documentation files.

### 5. Git Push Review (PreToolUse)
Open editor for review before git push operations.

### 6. Memory Persistence (Stop)
```javascript
// scripts/hooks/session-end.js
// Extracts patterns, decisions, and progress from session
// Saves to .claude/session-memory.md for next session
```

### 7. Pre-Compact State Save (PreCompact)
```javascript
// scripts/hooks/pre-compact.js
// Before auto-compact shrinks context:
// - Saves current plan/decisions
// - Saves file modification list
// - Saves test status
```

### 8. Session Start Context Load
```javascript
// scripts/hooks/session-start.js
// On new session:
// - Loads .claude/session-memory.md
// - Injects previous context
```

### 9. Strategic Compact Suggestion (suggest-compact.js)
Instead of auto-compact, suggests manual compact at logical breakpoints.

### 10. Session Evaluation (evaluate-session.js)
Evaluates session quality metrics at end.

---

## Plankton-Style Write-Time Enforcement

### Three-Phase Hook Architecture
Every file edit triggers:

```
Phase 1: Auto-Format (Silent)
├── Runs formatters (ruff format, biome, shfmt, taplo, markdownlint)
├── Fixes 40-50% of issues silently
└── No output to main agent

Phase 2: Collect Violations (JSON)
├── Runs linters, collects unfixable violations
├── Returns structured JSON: {line, column, code, message, linter}
└── Still no output to main agent

Phase 3: Delegate + Verify
├── Spawns claude -p subprocess with violations JSON
├── Routes to model tier by complexity:
│   ├── Haiku: formatting, imports, style — 120s timeout
│   ├── Sonnet: complexity, refactoring — 300s timeout
│   └── Opus: type system, deep reasoning — 600s timeout
├── Re-runs Phase 1+2 to verify fixes
└── Exit 0 if clean, Exit 2 if violations remain
```

### Config Protection (Critical)
LLMs will modify linter configs to pass instead of fixing code. Three defense layers:
1. **PreToolUse hook**: Blocks edits to linter config files
2. **Stop hook**: Detects config changes via `git diff`
3. **Protected files**: `.ruff.toml`, `biome.json`, `.shellcheckrc`, etc.

### Package Manager Enforcement
PreToolUse hook blocks legacy package managers:
- `pip`, `pip3`, `poetry`, `pipenv` → Blocked (use `uv`)
- `npm`, `yarn`, `pnpm` → Blocked (use `bun`)

---

## Hookify Plugin

Conversational hook creation instead of manual JSON editing. Simplifies hook setup.

---

## Hook Design Best Practices

1. **Use Stop hooks for learning** (runs once) rather than UserPromptSubmit (runs every message)
2. **Hooks over prompts for reliability**: LLMs forget instructions ~20% of the time. PostToolUse hooks enforce at tool level
3. **Scripts for deterministic logic**: Calendar math, timezone handling → scripts, not LLM
4. **Keep hooks fast**: Slow hooks add latency to every tool call
5. **Exit codes matter**: Exit 0 = success, Exit 2 = report to agent
6. **Use matchers**: Only run hooks on relevant tool calls (Edit|Write, Bash, etc.)
