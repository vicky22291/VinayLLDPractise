# The Self-Improving AI System That Built Itself
> **Author:** Prateek ([@agent_wrapper](https://x.com/agent_wrapper)) — Building Agent Orchestrator and the developer tooling layer at [Composio](https://composio.io)  
> **Published:** February 23, 2026  
> **Repository:** [github.com/ComposioHQ/agent-orchestrator](https://github.com/ComposioHQ/agent-orchestrator)  
> **Metrics Report:** [metrics-v1 release](https://github.com/ComposioHQ/agent-orchestrator/releases/tag/metrics-v1)  
> **Interactive Visualizations:** [pkarnal.com/ao-labs](https://pkarnal.com/ao-labs/)
---
## TL;DR
A solo developer at Composio started running AI coding agents in parallel to ship faster, hit the bottleneck of *managing* those agents himself, then wrote a system to automate coordination. He pointed the agents at that coordination system — and they built a full replacement in 8 days: **40,000 lines of TypeScript, 17 plugins, 3,288 tests**. The resulting system then started improving itself. It is now open-sourced as **Agent Orchestrator**.
---
## 1. The Origin: Scaling Past the Human Bottleneck
### The Starting Problem
Prateek had a large codebase, a long backlog, and limited time. The natural solution was to run multiple AI coding agents in parallel — give each one a task, let them write code, open PRs, and merge. He started with 2–3 agents, then scaled to 5, then 10.
**The unexpected bottleneck:** The agents were fast. *He* wasn't. He became the coordinator:
- Checking whether CI (Continuous Integration) passed
- Reading review comments
- Copy-pasting error logs back into agent sessions
- Switching between terminal tabs
He had traded coding for **bad project management**.
> "I'd gone from writing code to babysitting the things that write code. That doesn't scale."
### The First Fix: 2,500 Lines of Bash
To automate coordination, he wrote ~2,500 lines of bash scripts that managed:
- **tmux sessions** — a terminal multiplexer allowing multiple isolated terminal sessions within one window
- **Git worktrees** — a Git feature allowing multiple working trees from a single repository (each agent gets its own branch and directory without cloning the whole repo)
- **Tab switching** via natural language (e.g., "take me to the tab for PR #1121")
- **CI failure forwarding** — the orchestrator would detect a failed CI run and pipe the logs back to the relevant agent session
This worked — "barely." The bash scripts were a brittle scaffold, not a proper system.
### The Recursive Leap
He then pointed the agents *at the bash scripts* to rebuild them properly. The agents produced **v1 of a TypeScript orchestrator**. v1 then managed the agents that built v2. And v2 has been improving itself ever since.
This recursive bootstrapping — where the tool being built is also managing its own construction — is the core "inception" of the project.
---
## 2. What Was Built: By the Numbers
| Metric | Value |
|---|---|
| Total lines of code | ~40,000–43,000 (TypeScript) |
| Plugins | 17 across 8 architecture slots |
| Test cases | 3,288 (49 unit + 25 integration test files) |
| Time to build | 8 days (Feb 13–20, 2026) |
| Actual focused work | ~3 days (agents filled the gaps) |
| Total commits (all branches) | 722 |
| Main branch commits | 91 |
| PRs merged | 61 (27 in a single day — Feb 14) |
| Peak concurrent agents | 30 |
| AI-authored PRs | 86 of 102 (84%) |
| AI co-author trailers | 1,013 |
| CI overall success rate | 84.6% |
| Automated code review comments | ~700 |
| CI failures self-corrected | 41 out of 41 |
Every commit carries a **git trailer** identifying which AI model wrote it, creating a fully auditable, non-ambiguous record of human vs. AI contributions.
---
## 3. The Real Bottleneck in AI-Assisted Coding
Most developers run AI agents serially or in small batches and assume the ceiling is *agent quality*. The article argues this is the wrong frame.
**The actual ceiling is attention routing.**
When you run 5 agents in parallel:
1. They finish their tasks in ~20 minutes.
2. You return to find 5 open PRs, CI statuses to check, and review comments to read.
3. You've automated the engineering and replaced it with **manual project management**.
The orchestrator replaces the developer *in that feedback loop* — not with a dumb script, but with an agent that has full context on every active session, every open PR, and every CI run. It doesn't just display data; it synthesizes decisions:
> "This PR is blocking three other tasks, this CI failure is a flaky test, and this review comment is the one that actually matters."
The developer only gets pinged when **genuine human judgment** is required. Everything else resolves automatically.
---
## 4. Architecture: The Plugin System
The orchestrator is structured around **8 swappable plugin slots**. Every abstraction is a TypeScript interface — any conforming implementation can be dropped in.
| Slot | Default | Alternatives |
|---|---|---|
| **Runtime** | `tmux` | Docker, Kubernetes, process |
| **Agent** | `claude-code` | Codex, Aider, OpenCode |
| **Workspace** | `worktree` | clone |
| **Tracker** | `github` | Linear |
| **SCM** | `github` | — |
| **Notifier** | `desktop` | Slack, Composio, webhook |
| **Terminal** | `iterm2` | web (xterm.js) |
| **Lifecycle** | `core` | — |
All interfaces are defined in `packages/core/src/types.ts`. A plugin implements one interface and exports a `PluginModule`.
### Session Lifecycle (From Issue to Merged PR)
```
1. Tracker pulls an issue (GitHub issue or Linear ticket)
2. Workspace creates an isolated git worktree + feature branch
3. Runtime starts a tmux session (or Docker container)
4. Agent (Claude Code, Aider, etc.) works autonomously — reads code, writes tests, creates PR
5. Terminal lets you observe live (iTerm2 or browser via xterm.js)
6. SCM creates the PR and enriches it with context
7. Reactions auto-handle CI failures and review comments
8. Notifier pings you only when human judgment is needed
```
---
## 5. Self-Healing CI: The Reactions System
One of the most practically powerful features. The system defines automated **reactions** to GitHub events using a YAML config:
```yaml
reactions:
  ci_failed:
    action: spawn_agent
    prompt: "CI failed on this PR. Read the failure logs and fix the issues."
  changes_requested:
    action: spawn_agent
    prompt: "Review comments have been posted. Address each comment and push fixes."
  approved:
    action: notify
    channel: slack
    message: "PR approved and ready to merge."
```
When CI fails, a new agent session is spawned automatically with the failure logs. When a reviewer requests changes, an agent reads the comments and pushes fixes. When a PR is approved, a Slack notification fires.
**The result:** All 41 CI failures across 9 branches were self-corrected. No human touched any of them.
---
## 6. Autonomous Code Review Pipeline
Agents don't just open PRs and wait. There is a full automated review loop:
```
Agent creates PR → pushes code
      ↓
Cursor Bugbot automatically reviews → posts inline comments
      ↓
Agent reads comments → fixes code → pushes again
      ↓
Bugbot re-reviews
      ↓
(repeat until clean)
```
**Stats from the 700 automated review comments:**
- ~68% of issues fixed immediately by the agent
- ~7% explained away as intentional design decisions
- ~4% deferred to future PRs
- ~1% required human intervention
**What Bugbot caught (real bugs, not false positives):**
- Shell injection via `exec()`
- Path traversal vulnerabilities
- Unclosed intervals (memory leaks)
- Missing null checks
> **Deep Dive — Cursor Bugbot:** Bugbot is an AI-powered automated code reviewer integrated into GitHub. It analyzes diffs on every PR and posts inline comments flagging bugs, security issues, and code quality problems — similar in spirit to static analysis tools (ESLint, SonarQube) but powered by an LLM for semantic understanding. It re-runs on every new push, making it ideal for the agent feedback loop described here.
---
## 7. The ao-58 Story: 12 Rounds, Zero Humans
The most dramatic individual example in the project was **PR #125** — a full dashboard redesign.
It went through **12 CI failure → fix cycles**:
- Each cycle: agent receives CI failure output → diagnoses the issue (type errors, lint failures, test regressions) → pushes a fix
- No human ever touched it
- Final PR: shipped clean
This is the reaction system working at full depth. The agent wasn't just retrying blindly — it was reading structured error output and reasoning about the fix at each step.
---
## 8. Activity Detection: How the Orchestrator Knows What Agents Are Doing
One underappreciated engineering problem: **how do you know if an agent is working, stuck, or done** without interrupting it?
Naive approaches (polling agent stdout, asking the agent to self-report) are noisy. Agents can get confused about their own state.
**The solution:** Claude Code writes structured **JSONL event files** during every session. The orchestrator reads these files directly to determine agent state:
| State | Signal |
|---|---|
| Actively generating tokens | Token event in JSONL stream |
| Waiting for tool execution | Tool call event pending |
| Idle | No recent events |
| Finished | Session-end event |
This out-of-band observation means the orchestrator doesn't need to interfere with the agent's actual work to know what it's doing. A future `agent-aider` plugin would read Aider's equivalent event files.
> **Deep Dive — JSONL event files:** Claude Code (Anthropic's terminal coding agent) emits a structured stream of JSON Lines (JSONL) — one JSON object per line — that records every action: file reads, file writes, shell commands, token generation events, and tool calls. This is a form of **structured observability**, similar to OpenTelemetry traces for distributed systems, but scoped to a single AI agent session.
---
## 9. Model Breakdown: Which AI Did What
Every commit is tagged with the model that wrote it. Models used: **Claude Opus 4.6**, **Sonnet 4.5**, and **Sonnet 4.6**. (Commit totals exceed 722 because some commits were written by one model and reviewed/fixed by another.)
| Model | Role |
|---|---|
| **Claude Opus 4.6** | Hard problems — complex architecture, cross-package integrations |
| **Claude Sonnet 4.6 / 4.5** | Volume work — plugin implementations, tests, documentation |
> **Deep Dive — Model tiering strategy:** This mirrors the emerging best practice in multi-agent systems of routing tasks by complexity. Frontier/expensive models handle reasoning-heavy work (architecture decisions, cross-cutting concerns); faster/cheaper models handle high-volume, well-defined work (boilerplate, tests, docs). This is analogous to how human engineering teams tier work between senior engineers and junior contributors.
---
## 10. The Self-Improvement Loop
The orchestrator includes a self-improvement subsystem (**ao-52** — itself built by an agent) that:
1. Logs performance data from every agent session
2. Tracks session outcomes (clean PR vs. multiple CI failure cycles)
3. Runs retrospectives to identify patterns
The feedback loop is:
```
Agents build features
       ↓
Orchestrator observes what worked (which prompts led to clean PRs, which spiraled)
       ↓
Orchestrator adjusts how it manages future sessions (tighter guardrails, better prompts)
       ↓
Agents build better features
       ↓
(repeat)
```
Since the agents built the orchestrator, and the orchestrator makes the agents more effective, and those agents improve the orchestrator further — the loop is **recursive and compounding**.
> "I think this is why orchestration matters more than any individual agent improvement. The ceiling isn't 'how good is Claude Code at TypeScript.' It's 'how good can a system get at deploying, observing, and improving dozens of agents working in parallel.' That ceiling is much higher. And it rises every time the loop runs."
> **Deep Dive — Why self-improving systems matter:** This architecture is a practical implementation of ideas from the AI safety/alignment literature around **recursive self-improvement** — systems that get better at getting better. The key safety insight here is that the improvement loop is *bounded*: it optimizes for cleaner PRs, faster CI cycles, and better code review outcomes — not open-ended capability gain. The humans remain in the loop for architecture decisions and final merges.
---
## 11. The Web Dashboard
Built with **Next.js 15** and **Server-Sent Events (SSE)** for real-time updates (no polling).
| Feature | Detail |
|---|---|
| **Attention zones** | Sessions grouped by urgency: failing CI, awaiting review, running fine |
| **Live terminal** | `xterm.js` in the browser showing the agent's actual terminal output in real time |
| **Session detail** | Current file being edited, recent commits, PR status, CI status |
| **Config discovery** | Automatically finds `ao.config.yaml` and shows available sessions |
> **Deep Dive — xterm.js:** xterm.js is an open-source terminal emulator written in TypeScript that runs in the browser. It supports full VT100/VT220 escape codes, making it possible to render a genuine tmux session in a browser tab — not a screenshot or a log stream, but an interactive, live terminal. It's used in tools like VS Code's integrated terminal and GitHub Codespaces.
> **Deep Dive — Server-Sent Events vs. WebSockets:** SSE is a unidirectional HTTP-based push mechanism (server → client). For a dashboard that primarily *reads* live status and doesn't need bidirectional messaging, SSE is simpler and more robust than WebSockets — it automatically reconnects, works through HTTP/2 multiplexing, and doesn't require a separate protocol upgrade.
---
## 12. The Inception: Human vs. Agent Contributions
At peak, **30 concurrent agents** were working on Agent Orchestrator simultaneously — building the TypeScript replacement while the bash-script version managed their construction.
### What Humans Did
- Architecture decisions (plugin slots, config schema, session lifecycle)
- Spawning sessions and assigning issues
- High-level PR reviews (architecture, not line-by-line)
- Resolving cross-agent conflicts (two agents editing the same file)
- Judgment calls (reject this approach, try that one)
### What Agents Did
- All 40,000+ lines of implementation code
- All 3,288 test cases
- 86 of 102 PR creations
- All review comment fixes
- All CI failure resolutions
> "I never committed directly to a feature branch. Every line of code went through a PR."
---
## 13. Standout Day: Saturday, February 14, 2026
- **27 PRs merged in a single day**
- The entire platform shipped: core services, CLI, web dashboard, all 17 plugins, npm publishing
- The developer was reviewing and merging PRs faster than he could read them
- Every PR had already passed CI and automated code review before it reached him
This is the compounding effect of the system at full operation — the orchestrator had handled all the coordination, leaving the human only with merge approvals.
---
## 14. Getting Started
### Installation
```bash
git clone https://github.com/ComposioHQ/agent-orchestrator.git
cd agent-orchestrator
pnpm install && pnpm build
```
### Prerequisites
- Node.js 20+
- Git 2.25+
- tmux (for default runtime)
- `gh` CLI (for GitHub integration)
### Basic Usage
```bash
ao init --tracker github --agent claude-code --runtime tmux
ao start
ao spawn my-project 123   # GitHub issue number or Linear ticket
```
The dashboard opens at `http://localhost:3000`. The orchestrator agent reads your config, spawns coding agents for each issue, and manages the full lifecycle from there.
### CLI Commands
| Command | Action |
|---|---|
| `ao status` | Overview of all sessions |
| `ao spawn <project> [issue]` | Spawn an agent for an issue |
| `ao send <session> "message"` | Send instructions to an agent |
| `ao session ls` | List all sessions |
| `ao session kill <session>` | Kill a session |
| `ao session restore <session>` | Revive a crashed agent |
| `ao dashboard` | Open the web dashboard |
---
## 15. What's Next: Towards Fully Autonomous Software Engineering
The article outlines several planned capabilities:
### Near-Term
- **Mobile/remote control** — message the orchestrator from Telegram or Slack while away from your desk (check status, approve a merge, redirect an agent)
- **Mid-session drift correction** — agents sometimes go down rabbit holes or over-engineer simple fixes; the orchestrator needs to compare agent work against the original intent and inject course corrections before too much time is wasted
- **Automatic escalation ladder** — Agent can't solve it → escalate to orchestrator → orchestrator can't decide → escalate to human. Only genuine human decisions reach the developer.
### Longer-Term
- **Reconciler** — automatic conflict resolution between parallel agents editing the same files
- **Auto-rebase** — automated handling of long-running branches that have drifted from `main`
- **Docker/Kubernetes runtimes** — cloud-native agent deployment (currently tmux-based, local)
- **Plugin marketplace** — community-contributed plugins for agent runtimes, trackers, and notification channels
---
## 16. Why This Matters: The Broader Implication
The article makes a quietly significant claim:
> The ceiling of AI-assisted development isn't the capability of any single model. It's the capability of the *system* that coordinates, observes, and improves a fleet of agents.
This reframes the question from "how good is GPT-5 / Claude Opus 5 at coding?" to "how good is the orchestration layer at deploying, routing, and learning from many agents simultaneously?"
Individual model improvements are incremental. Orchestration improvements are multiplicative — each better feedback loop makes every future agent session more effective.
The Agent Orchestrator demonstrates this concretely: **Claude Opus 4.6 and Sonnet models available today**, combined with a well-designed orchestration layer, produced 40,000 lines of production TypeScript with 84.6% CI success and 99% autonomous code review in 8 days. No future model release was needed.
---
## 17. Open Source & Contributing
The repo is **MIT-licensed** and actively seeking contributors:
- New plugins (agent runtimes, trackers, notifiers)
- Docker/Kubernetes runtime
- A reconciler for automatic conflict detection
- Better escalation rules
**Repository:** [github.com/ComposioHQ/agent-orchestrator](https://github.com/ComposioHQ/agent-orchestrator)  
**Full metrics report:** [metrics-v1 release](https://github.com/ComposioHQ/agent-orchestrator/releases/tag/metrics-v1)  
**Interactive build data:** [pkarnal.com/ao-labs](https://pkarnal.com/ao-labs/)  
**Hiring (SF & Bangalore):** [jobs.ashbyhq.com/composio](https://jobs.ashbyhq.com/composio)