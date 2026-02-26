# Andrej Karpathy LLM Coding Guidelines Skill

## Overview

This is a Claude Code skill based on **Andrej Karpathy's observations** about common pitfalls when working with LLM coding assistants. The skill provides behavioral guidelines to reduce common mistakes that large language models make when writing code.

**Repository**: [forrestchang/andrej-karpathy-skills](https://github.com/forrestchang/andrej-karpathy-skills)

**License**: MIT

---

## The Problem

From Andrej Karpathy's [original post](https://x.com/karpathy/status/2015883857489522876):

> "The models make wrong assumptions on your behalf and just run along with them without checking. They don't manage their confusion, don't seek clarifications, don't surface inconsistencies, don't present tradeoffs, don't push back when they should."

> "They really like to overcomplicate code and APIs, bloat abstractions, don't clean up dead code... implement a bloated construction over 1000 lines when 100 would do."

> "They still sometimes change/remove comments and code they don't sufficiently understand as side effects, even if orthogonal to the task."

---

## The Four Core Principles

The skill addresses these issues through four key behavioral guidelines:

| Principle | Addresses |
|-----------|-----------|
| **Think Before Coding** | Wrong assumptions, hidden confusion, missing tradeoffs |
| **Simplicity First** | Overcomplication, bloated abstractions |
| **Surgical Changes** | Orthogonal edits, touching code you shouldn't |
| **Goal-Driven Execution** | Leverage through tests-first, verifiable success criteria |

---

## 1. Think Before Coding

**Core Principle:** Don't assume. Don't hide confusion. Surface tradeoffs.

### Guidelines

Before implementing:
- **State assumptions explicitly** - If uncertain, ask rather than guess
- **Present multiple interpretations** - Don't pick silently when ambiguity exists
- **Push back when warranted** - If a simpler approach exists, say so
- **Stop when confused** - Name what's unclear and ask for clarification

### Example: Hidden Assumptions

**User Request:** "Add a feature to export user data"

**❌ What LLMs Do (Wrong):**
```python
def export_users(format='json'):
    """Export all users to a file."""
    users = User.query.all()  # Assumes ALL users - privacy issue!

    if format == 'json':
        with open('users.json', 'w') as f:  # Assumes file location
            json.dump([u.to_dict() for u in users], f)
```

**Problems:**
- Assumed it should export ALL users (what about privacy?)
- Assumed file location without asking
- Assumed which fields to include

**✅ What Should Happen:**

```
Before implementing, I need to clarify:

1. **Scope**: Export all users or filtered subset? (privacy implications)
2. **Format**: You mentioned "export" - did you mean:
   - Download file in browser?
   - Background job with email notification?
   - API endpoint returning data?
3. **Fields**: Which user fields? (some might be sensitive)
4. **Volume**: How many users typically? (affects approach)

Simplest approach: Add an API endpoint that returns paginated JSON.
Would need more info for file-based exports.

What's your preference?
```

---

## 2. Simplicity First

**Core Principle:** Minimum code that solves the problem. Nothing speculative.

### Guidelines

- No features beyond what was asked
- No abstractions for single-use code
- No "flexibility" or "configurability" that wasn't requested
- No error handling for impossible scenarios
- If you write 200 lines and it could be 50, rewrite it

**The Test:** Would a senior engineer say this is overcomplicated? If yes, simplify.

### Example: Over-Abstraction

**User Request:** "Add a function to calculate discount"

**❌ What LLMs Do (Overengineered - 100+ lines):**
```python
from abc import ABC, abstractmethod

class DiscountStrategy(ABC):
    @abstractmethod
    def calculate(self, amount: float) -> float:
        pass

class PercentageDiscount(DiscountStrategy):
    def __init__(self, percentage: float):
        self.percentage = percentage

    def calculate(self, amount: float) -> float:
        return amount * (self.percentage / 100)

class DiscountCalculator:
    def __init__(self, config: DiscountConfig):
        self.config = config
    # ... 50+ more lines
```

**✅ What Should Happen (Simple - 4 lines):**
```python
def calculate_discount(amount: float, percent: float) -> float:
    """Calculate discount amount. percent should be 0-100."""
    return amount * (percent / 100)
```

**When to add complexity:** Only when you actually need multiple discount types. If that requirement comes later, refactor then.

### Key Insight

The "overcomplicated" examples aren't obviously wrong—they follow design patterns and best practices. The problem is **timing**: they add complexity before it's needed, which:

- Makes code harder to understand
- Introduces more bugs
- Takes longer to implement
- Harder to test

**Good code is code that solves today's problem simply, not tomorrow's problem prematurely.**

---

## 3. Surgical Changes

**Core Principle:** Touch only what you must. Clean up only your own mess.

### Guidelines

**When editing existing code:**
- Don't "improve" adjacent code, comments, or formatting
- Don't refactor things that aren't broken
- Match existing style, even if you'd do it differently
- If you notice unrelated dead code, mention it - don't delete it

**When your changes create orphans:**
- Remove imports/variables/functions that YOUR changes made unused
- Don't remove pre-existing dead code unless asked

**The Test:** Every changed line should trace directly to the user's request.

### Example: Drive-by Refactoring

**User Request:** "Fix the bug where empty emails crash the validator"

**❌ What LLMs Do (Too Much):**
```diff
  def validate_user(user_data):
-     # Check email format
-     if not user_data.get('email'):
+     """Validate user data."""  # Added docstring (not asked)
+     email = user_data.get('email', '').strip()
+
+     # Validate email  # Changed comment (not asked)
+     if not email:
          raise ValueError("Email required")
-     if '@' not in user_data['email']:
+     if '@' not in email or '.' not in email.split('@')[1]:  # Extra validation (not asked)
          raise ValueError("Invalid email")

-     # Check username
-     if not user_data.get('username'):
+     # Validate username
+     username = user_data.get('username', '').strip()
+     if not username:
          raise ValueError("Username required")
+     if len(username) < 3:  # New validation (not asked)
+         raise ValueError("Username too short")
```

**Problems:**
- "Improved" email validation beyond the bug fix
- Added username validation nobody asked for
- Changed comments and added docstring

**✅ What Should Happen (Surgical):**
```diff
  def validate_user(user_data):
      # Check email format
-     if not user_data.get('email'):
+     email = user_data.get('email', '')
+     if not email or not email.strip():
          raise ValueError("Email required")

      # Basic email validation
-     if '@' not in user_data['email']:
+     if '@' not in email:
          raise ValueError("Invalid email")

      # Check username (UNCHANGED)
      if not user_data.get('username'):
          raise ValueError("Username required")

      return True
```

**Only changed:** The specific lines that fix empty email handling.

---

## 4. Goal-Driven Execution

**Core Principle:** Define success criteria. Loop until verified.

### Guidelines

Transform imperative tasks into verifiable goals:

| Instead of... | Transform to... |
|--------------|-----------------|
| "Add validation" | "Write tests for invalid inputs, then make them pass" |
| "Fix the bug" | "Write a test that reproduces it, then make it pass" |
| "Refactor X" | "Ensure tests pass before and after" |

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

**Strong success criteria let the LLM loop independently. Weak criteria ("make it work") require constant clarification.**

### Example: Test-First Verification

**User Request:** "The sorting breaks when there are duplicate scores"

**❌ What LLMs Do (Fix Without Reproducing):**
```python
# Immediately changes sort logic without confirming the bug
def sort_scores(scores):
    return sorted(scores, key=lambda x: (-x['score'], x['name']))
```

**✅ What Should Happen (Reproduce First):**

```python
# 1. First, write a test that reproduces the issue
def test_sort_with_duplicate_scores():
    """Test sorting when multiple items have same score."""
    scores = [
        {'name': 'Alice', 'score': 100},
        {'name': 'Bob', 'score': 100},
        {'name': 'Charlie', 'score': 90},
    ]

    result = sort_scores(scores)

    # The bug: order is non-deterministic for duplicates
    assert result[0]['score'] == 100
    assert result[1]['score'] == 100
    assert result[2]['score'] == 90

# Verify: Run test 10 times → fails with inconsistent ordering

# 2. Now fix with stable sort
def sort_scores(scores):
    """Sort by score descending, then name ascending for ties."""
    return sorted(scores, key=lambda x: (-x['score'], x['name']))

# Verify: Test passes consistently
```

### Example: Multi-Step with Verification

**User Request:** "Add rate limiting to the API"

**❌ What LLMs Do (All at Once):**
```
Implements full rate limiting with Redis, multiple strategies,
configuration system, and monitoring in one 300-line commit
No clear verification steps
```

**✅ What Should Happen (Incremental with Verification):**
```
Plan for rate limiting:

1. Add basic in-memory rate limiting (single endpoint)
   Verify:
   - Test: 100 requests → first 10 succeed, rest get 429
   - Manual: curl endpoint 11 times, see rate limit error

2. Extract to middleware (apply to all endpoints)
   Verify:
   - Test: Rate limits apply to /users and /posts
   - Existing endpoint tests still pass

3. Add Redis backend (for multi-server)
   Verify:
   - Test: Rate limit persists across app restarts
   - Test: Two app instances share rate limit counter

4. Add configuration (rates per endpoint)
   Verify:
   - Test: /search allows 10/min, /users allows 100/min
   - Config file parsed correctly

Each step is independently verifiable and deployable.
Start with step 1?
```

---

## Installation

### Option A: Claude Code Plugin (Recommended)

This installs the guidelines as a Claude Code plugin, making the skill available across all your projects:

```bash
claude plugins add https://github.com/forrestchang/andrej-karpathy-skills
```

### Option B: CLAUDE.md (Per-Project)

**New project:**
```bash
curl -o CLAUDE.md https://raw.githubusercontent.com/forrestchang/andrej-karpathy-skills/main/CLAUDE.md
```

**Existing project (append):**
```bash
echo "" >> CLAUDE.md
curl https://raw.githubusercontent.com/forrestchang/andrej-karpathy-skills/main/CLAUDE.md >> CLAUDE.md
```

---

## How to Know It's Working

These guidelines are working if you see:

✅ **Fewer unnecessary changes in diffs** - Only requested changes appear

✅ **Fewer rewrites due to overcomplication** - Code is simple the first time

✅ **Clarifying questions come before implementation** - Not after mistakes

✅ **Clean, minimal PRs** - No drive-by refactoring or "improvements"

---

## Key Insight from Karpathy

From Andrej's observations:

> "LLMs are exceptionally good at looping until they meet specific goals... Don't tell it what to do, give it success criteria and watch it go."

The "Goal-Driven Execution" principle captures this: **transform imperative instructions into declarative goals with verification loops.**

---

## Tradeoff Note

**Important:** These guidelines bias toward **caution over speed**.

For trivial tasks (simple typo fixes, obvious one-liners), use judgment — not every change needs the full rigor.

**The goal is reducing costly mistakes on non-trivial work, not slowing down simple tasks.**

---

## Customization

These guidelines are designed to be merged with project-specific instructions. Add them to your existing `CLAUDE.md` or create a new one.

For project-specific rules, add sections like:

```markdown
## Project-Specific Guidelines

- Use TypeScript strict mode
- All API endpoints must have tests
- Follow the existing error handling patterns in `src/utils/errors.ts`
```

---

## Anti-Patterns Summary

| Principle | Anti-Pattern | Fix |
|-----------|-------------|-----|
| **Think Before Coding** | Silently assumes file format, fields, scope | List assumptions explicitly, ask for clarification |
| **Simplicity First** | Strategy pattern for single discount calculation | One function until complexity is actually needed |
| **Surgical Changes** | Reformats quotes, adds type hints while fixing bug | Only change lines that fix the reported issue |
| **Goal-Driven** | "I'll review and improve the code" | "Write test for bug X → make it pass → verify no regressions" |

---

## The CLAUDE.md File

Here's the complete guidelines file that gets added to your project:

```markdown
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

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
```

---

## Relation to OpenClaw/Clawdbot

While this skill was created for **Claude Code** (Anthropic's CLI tool), it represents the same philosophy that makes AI agents like OpenClaw more effective:

- **Explicit reasoning** before taking action
- **Minimal, focused changes** rather than overengineering
- **Verifiable goals** with clear success criteria
- **Asking for clarification** when uncertain

The skill can be used with:
- **Claude Code CLI** - As a plugin or CLAUDE.md file
- **OpenClaw/Clawdbot** - As a skill in the `.claude/skills/` directory
- **Any LLM coding assistant** - As guidelines in system prompts

---

## Sources

- [GitHub Repository - forrestchang/andrej-karpathy-skills](https://github.com/forrestchang/andrej-karpathy-skills)
- [Karpathy Guidelines Skill](https://skills.sh/forrestchang/andrej-karpathy-skills/karpathy-guidelines)
- [Original Andrej Karpathy Tweet](https://x.com/karpathy/status/2015883857489522876)
- [From Clawdbot to Moltbot to OpenClaw - CNBC](https://www.cnbc.com/2026/02/02/openclaw-open-source-ai-agent-rise-controversy-clawdbot-moltbot-moltbook.html)
- [OpenClaw's AI assistants social network - TechCrunch](https://techcrunch.com/2026/01/30/openclaws-ai-assistants-are-now-building-their-own-social-network/)

---

**Document Version**: 1.0
**Last Updated**: February 3, 2026
**Repository**: Successfully cloned to `clawdbot/andrej-karpathy-skills/`
