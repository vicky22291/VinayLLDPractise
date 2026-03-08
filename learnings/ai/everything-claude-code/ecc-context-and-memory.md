# ECC - Context Window Optimization & Memory Management

## The Problem

Claude Code has a 200K context window, but with excessive MCP tools enabled, it can shrink to ~70K usable tokens. Every token matters.

---

## MCP Context Optimization

### Replace MCPs with Skills/Commands
Many MCPs (GitHub, Supabase, deployment) just wrap CLI tools. Replace them with skills/commands to save context:

```bash
# Instead of GitHub MCP consuming context, create a /gh-pr command wrapping:
gh pr create --title "..." --body "..."

# Instead of Supabase MCP, use Supabase CLI directly in skills
```

### MCP Limits
- Keep 20-30 MCPs configured but **enable only 10 or fewer**
- Keep under **80 active tools** total
- Disable unused MCPs via `/plugins` or `/mcp` in `~/.claude.json`

---

## Memory Persistence Patterns

### Session Storage Pattern
Create skills that save progress to `.tmp` files in `.claude/`:
- Create new file per session (don't pollute old context)
- Content: what worked (with evidence), what failed, what's left to try

### Strategic Context Clearing
1. Establish a plan
2. Clear context (default in plan mode)
3. Work from the plan — this removes accumulated exploration context

### Disable Auto-Compact
Manually compact at logical intervals instead of letting auto-compact trigger randomly. Use the `strategic-compact` skill.

---

## Dynamic System Prompt Injection

### CLI Flags for Surgical Context Loading
```bash
claude --system-prompt "$(cat memory.md)"
```

**Authority hierarchy**: System prompt > User messages > Tool results

### Context-Specific Aliases
```bash
alias claude-dev='claude --system-prompt "$(cat ~/.claude/contexts/dev.md)"'
alias claude-review='claude --system-prompt "$(cat ~/.claude/contexts/review.md)"'
alias claude-research='claude --system-prompt "$(cat ~/.claude/contexts/research.md)"'
```

### Context Files (from repo)

**dev.md** - Development context with coding standards, test requirements
**review.md** - Code review context with checklist focus
**research.md** - Research context with web search, documentation focus

---

## Memory Persistence Hooks

### PreCompact Hook
Saves important state before context compaction:
```javascript
// scripts/hooks/pre-compact.js
// Extracts key decisions, patterns, and progress before compaction
// Saves to .claude/session-memory.md
```

### Stop Hook (Session End)
Persists learnings when session ends:
```javascript
// scripts/hooks/session-end.js
// Extracts reusable patterns from the session
// Saves to persistent memory files
```

### Session Start Hook
Loads previous context automatically:
```javascript
// scripts/hooks/session-start.js
// Loads .claude/session-memory.md
// Injects previous context into new session
```

---

## Continuous Learning System

### Problem
Claude repeatedly encounters the same problems or generates familiar patterns. Wasted tokens + time.

### Solution: Auto-Extract Skills
When discoveries emerge (debugging techniques, workarounds, project patterns), automatically save them as new skills for future sessions.

### Implementation Choice: Stop Hooks (Not UserPromptSubmit)
- **Stop hooks** run once at session end — captures learnings without adding latency
- **UserPromptSubmit** runs on every message — adds latency to each prompt
- Use Stop hooks for learning extraction

### Continuous Learning v2
Adds confidence scoring to extracted patterns:
- Patterns start with low confidence
- Repeated successful use increases confidence
- High-confidence patterns can evolve into full skills/commands/agents

---

## Token Optimization Strategies

### 1. Subagent Architecture (Primary Strategy)
Delegate to cheapest sufficient model:

| Task Type | Model | Why |
|-----------|-------|-----|
| Exploration/search | Haiku | Fast, cheap, adequate for file discovery |
| Simple edits | Haiku | Single-file changes with clear instructions |
| Multi-file implementation | Sonnet | Optimal coding balance |
| Complex architecture | Opus | Deep reasoning needed |
| PR reviews | Sonnet | Context + nuance |
| Security analysis | Opus | Can't miss vulnerabilities |
| Documentation | Haiku | Simple structure |
| Complex debugging | Opus | Must hold entire system in memory |

**Default to Sonnet for 90% of coding tasks.** Upgrade to Opus when:
- Initial attempts fail
- Task spans 5+ files
- Requires architectural decisions
- Security-critical code

### 2. Use mgrep Instead of grep
~50% token reduction compared to grep/ripgrep. In 50-task benchmark, mgrep + Claude Code used ~2x fewer tokens.

### 3. Modular Codebase
Files in hundreds (not thousands) of lines:
- Reduces token costs
- Increases first-attempt success rate

### 4. Prompt Caching
Cache long system prompts:
```python
{
    "type": "text",
    "text": system_prompt,
    "cache_control": {"type": "ephemeral"}  # Cache this
}
```

---

## Cost-Aware LLM Pipeline Pattern

### Model Routing by Complexity
```python
def select_model(text_length, item_count, force_model=None):
    if force_model: return force_model
    if text_length >= 10_000 or item_count >= 30:
        return "claude-sonnet-4-6"  # Complex
    return "claude-haiku-4-5-20251001"  # Simple (3-4x cheaper)
```

### Budget Tracking (Immutable)
```python
@dataclass(frozen=True, slots=True)
class CostTracker:
    budget_limit: float = 1.00
    records: tuple[CostRecord, ...] = ()

    def add(self, record):
        return CostTracker(budget_limit=self.budget_limit, records=(*self.records, record))

    @property
    def over_budget(self): return sum(r.cost_usd for r in self.records) > self.budget_limit
```

### Pricing Reference (2025-2026)
| Model | Input ($/1M) | Output ($/1M) | Relative |
|-------|-------------|---------------|----------|
| Haiku 4.5 | $0.80 | $4.00 | 1x |
| Sonnet 4.6 | $3.00 | $15.00 | ~4x |
| Opus 4.5 | $15.00 | $75.00 | ~19x |
