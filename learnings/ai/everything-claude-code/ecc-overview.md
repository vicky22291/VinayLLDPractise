# Everything Claude Code (ECC) - Overview & Architecture

**Source**: https://github.com/affaan-m/everything-claude-code
**What it is**: A comprehensive performance optimization system for AI agent harnesses (Claude Code, Codex, Cursor, etc.). Production-ready skills, agents, hooks, commands, rules, and MCP configurations.

---

## Repository Structure

```
everything-claude-code/
├── agents/          # 14 specialized delegation agents
├── skills/          # 56+ reusable workflow definitions
├── commands/        # 32+ slash commands
├── rules/           # Language-agnostic + language-specific rules
│   ├── common/      # coding-style, git-workflow, testing, security, patterns, performance, hooks, agents
│   ├── typescript/
│   ├── python/
│   ├── golang/
│   └── swift/
├── hooks/           # Session lifecycle, memory persistence, strategic compaction
├── scripts/         # Cross-platform Node.js hook implementations
├── contexts/        # System prompt injection contexts (dev, review, research)
├── examples/        # Example CLAUDE.md files for different project types
├── mcp-configs/     # MCP server configurations
├── plugins/         # Plugin packaging
├── tests/           # 992 internal tests
├── the-shortform-guide.md   # Setup, foundations, philosophy (START HERE)
├── the-longform-guide.md    # Token optimization, memory, evals, parallelization
├── the-security-guide.md    # Agent security practices
└── the-openclaw-guide.md    # Security lessons from recursive agent infrastructure
```

## Core Philosophy

1. **Context window is the scarcest resource** - Everything is designed to minimize token usage
2. **Reusable patterns compound** - Invest early in skills/commands/agents that improve over time
3. **Security first** - Minimize attack surface, sandbox agents, audit everything
4. **Simplicity over complexity** - Avoid overcomplicating configurations
5. **Parallel execution** - Use forks and worktrees for concurrent work

## Six Pillars

| Component | Purpose | Storage |
|-----------|---------|---------|
| **Agents** | Specialized subagents with scoped tools/permissions | `agents/*.md` |
| **Skills** | Workflow rules with specific scopes and domain expertise | `skills/*/SKILL.md` |
| **Commands** | User-invoked slash commands | `commands/*.md` |
| **Rules** | Mandatory best practices loaded every session | `rules/**/*.md` |
| **Hooks** | Event-triggered automations (lifecycle events) | `hooks/`, `scripts/hooks/` |
| **MCPs** | External service connections via Model Context Protocol | `mcp-configs/` |

## Quick Start

```bash
# Install as plugin
/plugin marketplace add affaan-m/everything-claude-code
/plugin install everything-claude-code@everything-claude-code

# Or clone and use install script
git clone https://github.com/affaan-m/everything-claude-code.git
cd everything-claude-code
./install.sh typescript  # or python, golang
```

## Key Guides

1. **Shortform Guide** - Setup, foundations, skills, hooks, subagents, rules, MCPs, keyboard shortcuts, parallel workflows
2. **Longform Guide** - Token optimization, memory persistence, continuous learning, verification loops, parallelization, subagent orchestration
3. **Security Guide** - Attack vectors, sandboxing, sanitization, common attack types, OWASP agentic top 10
4. **OpenClaw Guide** - Security lessons from recursive agent infrastructure (cautionary tale)
