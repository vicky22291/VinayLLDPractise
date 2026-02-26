# Claude Cowork: A Visual Guide for PMs

*Based on Paweł Huryn's article — [@PawelHuryn](https://x.com/PawelHuryn/status/2025470280945041547)*

---

## The Big Picture

Everyone's talking about Claude Code. But unless you live in the terminal, **Claude Cowork** is probably the more practical tool for your day-to-day work. It runs on all platforms (Pro, Max, Team, Enterprise) and uses the exact same model as Claude Code — the difference is purely in how you interact with it.

Claude Code requires git worktrees, tmux, and CLI flags. Cowork gives you a visual interface to that same power. Think of them as two doors into the same room.

```mermaid
graph LR
    MODEL["Claude Model
(same underneath)"]
    CODE["Claude Code
git · tmux · CLI flags
For developers"]
    COWORK["Claude Cowork
Visual interface · Files · Plugins
For everyone else"]
    CHAT["Claude Chat
Conversation only
No real file output"]

    MODEL --> CODE
    MODEL --> COWORK
    MODEL --> CHAT

    style COWORK fill:#d4edda,stroke:#28a745,color:#000
    style CODE fill:#dce8ff,stroke:#4a6cf7,color:#000
    style CHAT fill:#f5f5f5,stroke:#999,color:#000
```

---

## What Cowork Actually Is

Cowork is not a reskinned chat window. When you open the Cowork tab in Claude Desktop, you are handing Claude access to a **sandboxed Linux VM** running on your machine. Inside that VM, Claude can write code, run scripts, and produce real, editable files — not chat artifacts.

You describe the task. Cowork plans it, breaks it into parallel sub-agents, executes the work, and delivers clickable output files directly to a folder you grant access to.

```mermaid
sequenceDiagram
    participant You
    participant Cowork
    participant VM as Sandboxed Linux VM
    participant Agents as Sub-Agents (parallel)
    participant Folder as Your Shared Folder

    You->>Cowork: Describe a task
    Cowork->>VM: Plan & decompose into subtasks
    VM->>Agents: Spin up independent Claude instances
    Agents-->>VM: Each works on its piece simultaneously
    VM-->>Folder: Deliver .docx / .pptx / .xlsx / .pdf
    Folder-->>You: Open file directly
```

The sandbox boundary is important: Cowork cannot touch your OS or any files outside the folder you shared. Inside that folder, however, it has full read, write, and delete access — so choose carefully what you expose.

---

## Chat vs. Cowork: Knowing When to Use Which

The simplest mental model: **Chat is for conversations, Cowork is for workflows.** The distinction matters when you're deciding which tab to open.

```mermaid
flowchart LR
    subgraph CHAT ["💬 Chat"]
        direction TB
        C1["Single-turn Q&A"]
        C2["Artifacts in the chat window"]
        C3["No task tracking"]
    end

    subgraph COWORK ["🤖 Cowork"]
        direction TB
        W1["Multi-step task planning
+ real-time progress"]
        W2["Real files in your folder"]
        W3["Parallel sub-agents"]
        W4["Plugin panel + MCP connectors"]
    end

    Q{"Do you need
a real deliverable?"}
    Q -- No --> CHAT
    Q -- Yes --> COWORK

    style COWORK fill:#e8f5e9,stroke:#28a745,color:#000
    style CHAT fill:#f5f5f5,stroke:#aaa,color:#000
```

---

## Skills and Plugins: Teaching Claude New Tricks

Plugins are arguably the most underappreciated part of Cowork. When Anthropic unveiled AI tools for legal and financial research in early 2026, legacy software stocks dropped $285 billion in a single day — investors saw agents moving into the application layer. The plugin sidebar in Cowork is exactly that layer.

### How Skills Work

A **skill** is a reusable instruction manual that tells Claude how to handle a specific, repeatable task. Say "create a Word doc" and the `docx` skill loads automatically. You can also trigger any skill manually by typing `/` in Cowork for autocomplete.

Claude doesn't load all skills at once. It reads a ~100-token description of each skill to decide relevance, then fetches full instructions only when needed — keeping your context window lean.

```mermaid
flowchart TD
    INPUT["User types a request"]
    SCAN["Claude scans skill descriptions
~100 tokens each"]
    MATCH{"Relevant skill found?"}
    LOAD["Load full skill instructions
into context"]
    EXEC["Execute task"]

    INPUT --> SCAN --> MATCH
    MATCH -- Yes --> LOAD --> EXEC
    MATCH -- No --> EXEC

    style LOAD fill:#fff3cd,stroke:#f0ad4e,color:#000
```

### The Plugin Ecosystem

Cowork ships with **11 built-in plugins** from Anthropic's knowledge-work repo covering productivity, product management, legal, finance, marketing, and data. Each plugin bundles a set of skills with slash commands. Code Tab has its own separate defaults focused on developer workflows.

Crucially, the two plugin panels are **isolated** — installing a plugin in Cowork doesn't make it available in Code Tab, and vice versa. But the skill format is fully cross-compatible: you can load Code's developer plugins into Cowork, or Cowork's business plugins into Code.

```mermaid
graph TD
    subgraph COWORK_PANEL ["Cowork Plugin Panel (isolated)"]
        CP["11 business plugins
productivity · legal · finance
marketing · data · PM"]
    end

    subgraph CODE_PANEL ["Code Tab Plugin Panel (isolated)"]
        DP["Developer plugins
agent-sdk · feature-dev
code-review · frontend"]
    end

    COMPAT["Same skill format
Cross-compatible
Add either set to the other tool"]

    CP <--> COMPAT
    DP <--> COMPAT

    SHARED["Skills uploaded via Desktop Settings
Shared across Chat + Cowork + Code Tab"]

    COMPAT --> SHARED

    style COMPAT fill:#e3f2fd,stroke:#1976d2,color:#000
    style SHARED fill:#e8f5e9,stroke:#28a745,color:#000
```

**Where to find more skills and plugins:**

| Source | What's there |
|---|---|
| `github.com/anthropics/skills` | Official doc skills: docx, xlsx, pptx, pdf + creative & enterprise examples |
| `github.com/anthropics/knowledge-work-plugins` | Cowork's 11 default business-role plugins |
| `claudemarketplaces.com` | A marketplace of marketplaces |
| `github.com/travisvn/awesome-claude-skills` | Community-curated, battle-tested skills |
| `github.com/sickn33/antigravity-awesome-skills` | 868+ agentic skills, role-based bundles |
| `skills.sh` | PM frameworks, PRD generators, launch playbooks |

---

## MCPs: Connecting Cowork to the Outside World

Skills teach Claude *how* to work. **MCPs (Model Context Protocol)** teach it *where* to reach. Each MCP server exposes a set of callable tools — a Gmail MCP might give Claude `search_emails`, `send_email`, and `read_email`. The GitHub MCP gives it `create_pull_request` and `list_issues`. You compose the toolset you need.

There are three ways to connect MCP servers to Claude Desktop (Chat, Cowork, and Code Tab all share the same config):

```mermaid
graph TD
    subgraph REMOTE ["🌐 Remote Connectors"]
        R["Hosted by third parties
Works in Claude Desktop AND claude.ai browser
Add via: Connectors → Manage connectors"]
    end

    subgraph EXT ["🔌 Extensions"]
        E["Anthropic-packaged local MCP servers
One-click install
Manage via: Settings → Extensions"]
    end

    subgraph CUSTOM ["⚙️ Custom (JSON config)"]
        C["Your own local or remote servers
Edit via: Menu → Developer → App Config File
Full control over args, env vars, paths"]
    end

    ALL["All three surfaces as unified
Connectors with on/off toggles"]

    REMOTE --> ALL
    EXT --> ALL
    CUSTOM --> ALL

    style ALL fill:#f3e5f5,stroke:#7b1fa2,color:#000
```

### Fine-grained Permissions

For every connector, each individual tool can be set to **Allow** (auto-run), **Ask** (confirm first), or **Block** (never run). For example: allow Claude to search your emails, but block it from sending them. Configure this at Settings → Connectors.

### A Note on Scope

Adding an MCP server through Claude Desktop's config makes it available in Chat, Cowork, and Code Tab — but **not** in the Code CLI. The CLI is a separate environment with its own config.

> **Windows gotcha:** If you installed Claude Desktop via the Microsoft Store (MSIX), the "Edit Config" button may open the wrong file. The app reads from the MSIX virtualized path, not `%APPDATA%\Claude\`. See GitHub issue #26073 if your MCP servers silently fail to load.

**Where to find MCP servers:**

| Source | What's there |
|---|---|
| `github.com/modelcontextprotocol/servers` | Official servers: filesystem, GitHub, Google Drive, Slack |
| `modelcontextprotocol.io/examples` | Reference implementations |
| `github.com/punkpeye/awesome-mcp-servers` | Community-curated, hundreds by category |
| `mcp.so` | Registry with search and install instructions |

---

## Scheduled Tasks: Useful but Not Yet Reliable

Cowork has a scheduled tasks feature, but in practice it's unreliable. It's worth knowing it exists. For serious automation pipelines, use **n8n** or build an MCP-based approach instead.

---

## Desktop Commander: The 1-Minute Power-Up

Once you've set up Cowork, the single highest-ROI move is installing **Desktop Commander**. It takes under a minute and gives Chat, Cowork, and Code Tab the ability to do virtually anything on your machine — including installing new MCP servers or reorganizing files.

```mermaid
flowchart LR
    A["Open Claude Desktop"] --> B["+ → Connectors
→ Manage connectors"]
    B --> C["Browse connectors
→ All → Desktop Commander"]
    C --> D["Select which tools
don't need your approval"]
    D --> E["✅ Full laptop access
for Chat, Cowork & Code Tab"]

    style E fill:#d4edda,stroke:#28a745,color:#000
```

**Two tips once it's installed:**
- Disable the Claude Chrome extension when not in use — otherwise Claude sometimes defaults to browser-based actions even when a local MCP would be faster.
- Review which individual tools you want to auto-approve vs. confirm. You can always watch what's happening and disable the connector temporarily.

---

## Cross-Session Memory: Making Claude Remember You

By default, every Cowork session starts blank. With Desktop Commander already installed, you can give Cowork persistent memory in one more step: paste a memory instruction into **Settings → Cowork → Global Instructions**.

The simplest version:

```
## Memory Management
When you discover something valuable for future sessions — architectural decisions,
bug fixes, gotchas, environment quirks — immediately append it to {your_folder}/memory.md.
Don't wait to be asked. Don't wait for session end.
Keep entries short: date, what, why.
Read this file at the start of every session.
```

This costs nearly zero tokens and survives crashes, context compaction, and new sessions.

### Scaling Up: Structured Memory

As your memory file grows, a single flat file becomes unwieldy. The structured approach splits memory into topic files so Claude loads only what's relevant:

```mermaid
graph TD
    ROOT[".claude/memory/"]
    ROOT --> IDX["memory.md
Index — updated whenever a file changes"]
    ROOT --> GEN["general.md
Cross-project facts, preferences, environment"]
    ROOT --> DOM["domain/{topic}.md
One file per knowledge domain"]
    ROOT --> TOOLS["tools/{tool}.md
Tool configs, CLI quirks, workarounds"]

    IDX -. "Claude reads this first,
then loads only relevant files" .-> GEN
    IDX -. "..." .-> DOM
    IDX -. "..." .-> TOOLS

    style ROOT fill:#dce8ff,stroke:#4a6cf7,color:#000
    style IDX fill:#fff3cd,stroke:#f0ad4e,color:#000
```

**The five memory rules:**
1. Write new knowledge to the right file immediately — not at session end
2. Keep `memory.md` as a one-line-per-entry index
3. Format every entry as: *date · what · why*
4. At session start, read the index; load topic files only when relevant
5. Periodically ask Claude to "reorganize memory" — it will deduplicate, merge, and resync the index

---

## Putting It All Together

Cowork's value compounds as you layer each piece on top of the last:

```mermaid
flowchart TB
    BASE["Cowork Tab
Autonomous agent · sandboxed VM · real file output"]
    SKILLS["+ Skills & Plugins
Teach Claude how to handle your specific work types"]
    MCP["+ MCP Connectors
Connect Claude to Gmail, GitHub, Slack, and custom tools"]
    DC["+ Desktop Commander
Full laptop access — files, installs, OS tasks"]
    MEM["+ Cross-Session Memory
Claude learns your context and never forgets it"]

    BASE --> SKILLS --> MCP --> DC --> MEM

    style BASE fill:#e3f2fd,stroke:#1976d2,color:#000
    style SKILLS fill:#e8f5e9,stroke:#388e3c,color:#000
    style MCP fill:#fff8e1,stroke:#f57f17,color:#000
    style DC fill:#fce4ec,stroke:#c62828,color:#000
    style MEM fill:#f3e5f5,stroke:#7b1fa2,color:#000
```

Start with the base and add one layer at a time. Each one is independently useful, and together they turn Cowork into a genuinely autonomous work partner.

---

*Written by Paweł Huryn · [The Product Compass Newsletter](https://productcompass.pm)*
