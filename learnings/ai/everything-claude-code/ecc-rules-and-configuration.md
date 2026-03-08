# ECC - Rules, Configuration & Examples

## Rules System

Rules are mandatory best practices in `.claude/rules/` that load automatically every session. The LLM cannot choose to ignore them (unlike prompt instructions).

### Organization: Modular by Concern
```
rules/
├── common/
│   ├── coding-style.md      # Naming, formatting, code structure
│   ├── git-workflow.md       # Branch naming, commit messages, PR process
│   ├── testing.md            # Test coverage, TDD, test organization
│   ├── security.md           # No hardcoded secrets, input validation, OWASP
│   ├── patterns.md           # Design patterns, immutability, modularity
│   ├── performance.md        # Optimization, caching, lazy loading
│   ├── hooks.md              # Hook architecture and conventions
│   ├── agents.md             # Agent delegation patterns
│   └── development-workflow.md  # Overall workflow conventions
├── typescript/
│   ├── coding-style.md
│   ├── patterns.md
│   ├── testing.md
│   ├── security.md
│   └── hooks.md
├── python/
│   ├── coding-style.md
│   ├── patterns.md
│   ├── testing.md
│   ├── security.md
│   └── hooks.md
├── golang/
│   └── (same structure)
└── swift/
    └── (same structure)
```

### Common Rule Examples

**Security Rules:**
- No hardcoded secrets, API keys, or passwords in source
- All user input must be validated
- Use parameterized queries (never string concatenation for SQL)
- Never commit .env files

**Coding Style Rules:**
- Prefer immutability (const, readonly, final)
- Modular code structure (files in hundreds, not thousands of lines)
- Early returns over deep nesting
- No console.logs in committed code

**Testing Rules:**
- Write tests before or alongside implementation
- Minimum 80% coverage target
- Table-driven tests (Go) / parametrized tests (Python)
- Mock external dependencies, test actual logic

**Git Workflow Rules:**
- Descriptive branch names: `feature/`, `fix/`, `refactor/`
- Conventional commit messages
- PRs require review before merge
- Never force-push to main/master

---

## CLAUDE.md Examples

### Generic Project CLAUDE.md
```markdown
# Project: My SaaS App

## Tech Stack
- Frontend: Next.js 14, TypeScript, Tailwind CSS
- Backend: Node.js, Express, PostgreSQL
- Testing: Jest, Playwright
- Deploy: Vercel (frontend), Railway (backend)

## Commands
- `npm run dev` — Start development server
- `npm test` — Run tests
- `npm run build` — Production build
- `npm run lint` — Run ESLint

## Architecture
- `/src/app` — Next.js app router pages
- `/src/components` — React components
- `/src/lib` — Shared utilities
- `/src/server` — API routes and server logic
- `/prisma` — Database schema and migrations

## Conventions
- Use server components by default, client only when needed
- API routes in `/api/` follow RESTful conventions
- All database queries go through Prisma
- Environment variables: never hardcode, always use process.env
```

### User-Level CLAUDE.md (~/.claude/CLAUDE.md)
```markdown
# User Preferences

## Style
- I prefer concise responses
- Use TypeScript over JavaScript always
- Prefer functional programming patterns
- Use early returns, avoid deep nesting

## Workflow
- Always run tests after changes
- Use git worktrees for parallel work
- Commit frequently with descriptive messages
- Use tmux for long-running commands

## Tools
- Package manager: pnpm
- Formatter: Prettier
- Linter: ESLint with strict config
- Editor: Zed with vim mode
```

### SaaS Next.js CLAUDE.md
Detailed project-specific instructions including database schema conventions, API patterns, auth flow, deployment process.

### Go Microservice CLAUDE.md
Go-specific patterns, error handling conventions, testing patterns, Docker deployment.

### Django API CLAUDE.md
Django REST Framework patterns, model conventions, serializer patterns, migration workflow.

### Rust API CLAUDE.md
Cargo conventions, error handling with thiserror/anyhow, async patterns with Tokio.

---

## Configuration Best Practices

### Status Line Customization
```
/statusline
```
Display: user, directory, git branch + dirty indicator, context percentage, model, time, todo count.

### Keyboard Shortcuts
| Key | Action |
|-----|--------|
| `Ctrl+U` | Delete entire line |
| `!` | Bash command prefix |
| `@` | File search |
| `/` | Slash commands |
| `Shift+Enter` | Multi-line input |
| `Tab` | Toggle thinking display |
| `Esc Esc` | Interrupt / restore code |

### Terminal Aliases
```bash
alias c='claude'
alias claude-dev='claude --system-prompt "$(cat ~/.claude/contexts/dev.md)"'
alias claude-review='claude --system-prompt "$(cat ~/.claude/contexts/review.md)"'
alias claude-research='claude --system-prompt "$(cat ~/.claude/contexts/research.md)"'
```

### Voice Input
Tools like superwhisper/MacWhisper on macOS for talking to Claude Code instead of typing. Claude interprets transcription errors contextually.

### Editor Recommendations
**Zed**: Rust-based, fast, instant opening, minimal resources, vim mode, agent panel integration.
**VS Code/Cursor**: Native extension, integrated interface, LSP support.

**Key practice**: Split screen terminal/editor, enable autosave, use file watchers for auto-reload.

---

## llms.txt Pattern

Many documentation sites provide `/llms.txt` endpoints with LLM-optimized content. Access these for better context when reaching docs.

---

## Long-Running Commands

Use tmux for streaming and monitoring:
```bash
tmux new -s dev
# Claude runs commands here
tmux attach -t dev
```

---

## Useful Built-in Commands

| Command | Purpose |
|---------|---------|
| `/rewind` | Return to previous state |
| `/statusline` | Customize display |
| `/checkpoints` | File-level undo points |
| `/compact` | Manual context compaction |
| `/fork` | Fork conversation for parallel work |
| `/rename` | Label chats for organization |
| `/clear` | Clear context between agent phases |
| `/model opus` | Switch to Opus model |
