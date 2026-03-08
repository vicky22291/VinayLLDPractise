# ECC - Advanced Patterns & Lessons

## Instinct System (Continuous Learning v2)

The instinct system is ECC's approach to persistent agent learning across sessions.

### What is an Instinct?
An atomic unit of learned behavior — one trigger, one action, stored as YAML:
```yaml
id: instinct-abc123
trigger: "Python import error with relative paths"
action: "Use absolute imports from project root, not relative"
confidence: 0.7
evidence_count: 3
scope: project  # or global
```

### Confidence Scoring
- 0.3 = tentative (just observed)
- 0.5 = moderate (seen twice)
- 0.7 = reliable (multiple confirmations)
- 0.9 = near-certain (extensively validated)

Confidence increases with successful application, decreases when corrections are needed.

### Project Scoping (v2.1)
Instincts are isolated per project using git remote URL hashing (12-char hash). Prevents cross-project contamination.

**Scope decision guide:**
- **Project scope**: Language idioms, framework patterns, project style preferences
- **Global scope**: Security best practices, tool workflows, universal patterns

### Promotion Pipeline
Project instincts auto-promote to global when:
- Seen in 2+ different projects
- Average confidence >= 0.8

### Evolution Path
Instinct clusters can evolve via `/evolve` into:
- **Commands** — user-invoked actions (frequent manual triggers)
- **Skills** — auto-triggered behaviors (complex domain knowledge)
- **Agents** — multi-step processes (requires orchestration)

### 100% Reliable Observation
v2 uses PreToolUse/PostToolUse **hooks** for observation (not skills). Skills fire 50-80% of the time; hooks fire 100%.

Background Haiku agent analyzes observations in parallel — no latency added to main session.

---

## The De-Sloppify Pattern

**Key insight**: Negative instructions ("don't add extra comments", "don't over-engineer") degrade quality because they consume reasoning budget on what NOT to do.

**Solution**: Two separate focused agents:
1. **Implementation agent** — writes code freely, focuses on functionality
2. **Cleanup agent** — separate context window, focused solely on quality fixes

Two focused agents outperform one agent with a long list of constraints.

---

## The /orchestrate Command (Agent Chains)

Defines 4 preset agent chains with structured HANDOFF documents:

| Chain | Agents | Use Case |
|-------|--------|----------|
| **feature** | planner -> tdd-guide -> code-reviewer -> security-reviewer | New features |
| **bugfix** | planner -> tdd-guide -> code-reviewer | Bug fixes |
| **refactor** | architect -> code-reviewer -> tdd-guide | Refactoring |
| **security** | security-reviewer -> code-reviewer -> architect | Security audit |

Each agent passes a structured HANDOFF document to the next agent in the chain.

---

## OpenClaw Security Lessons (Cautionary Tale)

The OpenClaw guide is a detailed security analysis of a multi-channel AI agent platform. Key lessons applicable to any agent setup:

### The Phishing Chain Pattern
1. Bot monitors a channel (Telegram, Discord, etc.)
2. Processes a link that contains prompt injection
3. Follows injected instructions (exfiltrate data, modify config)
4. Uses other connected channels to spread

**Every channel is an injection point that can pivot to every other channel.**

### The "Who Is This For?" Paradox
- Technical users who understand risks don't need multi-channel orchestration
- Non-technical users who benefit from the GUI can't evaluate the risks

### Real-World Failures Referenced
- **Moltbook breach**: 1.5M records, 32K API keys (plaintext), caused by "vibe-coded" Supabase with no RLS
- **ClawdHub marketplace**: 20% malicious skills, 800+ with hidden payloads
- **CVE-2026-25253** (CVSS 8.8): One-click RCE via workspace escape, 42K+ exposed instances

### The MiniClaw Alternative
"90% of the productivity, 5% of the attack surface":
- One access point (SSH only)
- Containerized execution
- Headless terminal (tmux)
- Manually audited local skills only
- Tailscale mesh (no exposed ports)
- Scoped permissions (read-only by default)

---

## Hookify Plugin

Instead of manually writing JSON hook configurations, use the `hookify` plugin for conversational hook creation:
```
/hookify
```
Tell it what you want (e.g., "run prettier after every edit") and it generates the JSON config.

---

## The Skeleton Project Pattern

Before building from scratch, search for battle-tested starters:
1. Search GitHub, npm, package registries for existing starters
2. Evaluate candidates with parallel agents (security, extensibility, relevance)
3. Clone best match
4. Iterate on top of proven foundation

---

## Session Management

### Session Files
Each session creates a summary file in `.claude/sessions/`:
- User messages (last 10)
- Tools used
- Files modified
- Patterns extracted

### Session Start
Loads last 7 days of session files, injects latest summary into context.

### Session Aliases
Name sessions for easy recall: `/rename feature-auth-flow`

---

## Context Authority Hierarchy

When Claude receives conflicting instructions:
1. **System prompt** (highest authority) — `--system-prompt` flag
2. **User messages** — what you type
3. **Tool results** (lowest authority) — content from file reads, web fetches, etc.

This is why dynamic system prompt injection is powerful for overriding defaults.

---

## Cross-Cutting Themes

1. **Skills fire 50-80%; hooks fire 100%** — use hooks for enforcement, skills for knowledge
2. **Negative instructions degrade quality** — use separate cleanup passes instead
3. **Two focused agents > one constrained agent** — split concerns across context windows
4. **Every integration is an attack vector** — minimize access points
5. **Config becomes an immune system** — every correction encodes into rules/hooks/skills
6. **Research before coding** — GitHub search, npm/PyPI, MCP tools, /llms.txt endpoints
7. **Immutability everywhere** — code, state tracking, cost tracking, all use immutable patterns
