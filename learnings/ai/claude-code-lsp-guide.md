# Claude Code LSP Tool — Complete Guide

> **Based on official Anthropic documentation and the Claude Code plugin system (as of 2026)**

---

## Table of Contents

1. [What is ENABLE_LSP_TOOL?](#what-is-enable_lsp_tool)
2. [How It Works Internally](#how-it-works-internally)
3. [LSP Indexing & Git Workflow](#lsp-indexing--git-workflow)
4. [Supported Languages](#supported-languages)
5. [Setup Instructions](#setup-instructions)
   - [TypeScript / JavaScript](#typescript--javascript)
   - [Python](#python)
   - [Go](#go)
   - [Java](#java)
   - [Rust](#rust)
   - [Other Languages](#other-languages)
6. [What Claude Gains](#what-claude-gains)
7. [Troubleshooting](#troubleshooting)
8. [Internal Setup Reference](#internal-setup-reference)

---

## What is ENABLE_LSP_TOOL?

`ENABLE_LSP_TOOL` is an environment variable (set to `"1"`) that activates Claude Code's built-in **LSP tool** — a first-class tool that connects Claude to Language Server Protocol (LSP) servers.

### Default behaviour (without it)

By default, Claude Code finds code using text-based tools:

- **Grep** — searches file contents via pattern matching
- **Glob** — finds files by name patterns

These tools are semantically blind. Claude does not understand the structure of your code — it only matches text. This is why searches can take 30–60 seconds, return the wrong file, or miss renamed symbols.

### With ENABLE_LSP_TOOL enabled

Claude gains a new tool called `LSP` that communicates with a running language server daemon — the same technology that powers VS Code's "Go to Definition" (Ctrl+Click). Claude now understands your code structurally, not just textually.

---

## How It Works Internally

### Architecture

```
Claude Code (LSP client)
       │
       │  JSON-RPC over stdio (or socket)
       ▼
Language Server (gopls / typescript-language-server / pyright / etc.)
       │
       │  Reads & indexes
       ▼
Your workspace files on disk
```

### Step-by-step internals

1. **Plugin provides configuration** — A plugin's `.lsp.json` file tells Claude Code which binary to run (e.g. `gopls`, `typescript-language-server`) and which file extensions map to which language.

2. **Language server is spawned** — Claude Code launches the language server as a child process when a session starts. The server does a full workspace scan and builds an in-memory index of all symbols, types, imports, and call graphs.

3. **Claude sends LSP requests** — Instead of grepping text, Claude sends standard LSP JSON-RPC messages to the server. These are the same protocol messages VS Code sends. Available operations include:
   - `textDocument/definition` — jump to where a function/type is declared
   - `textDocument/references` — find all call sites
   - `textDocument/hover` — get type signatures and documentation
   - `workspace/symbol` — search symbols across the whole project
   - `textDocument/implementation` — find all implementations of an interface
   - `textDocument/publishDiagnostics` — type errors and warnings after every edit

4. **File watching** — The LSP spec defines a `workspace/didChangeWatchedFiles` notification. Claude Code (as the LSP client) registers OS-level file system watchers. When any file changes on disk — including changes caused by `git pull` or `git merge` — the OS fires an event, Claude Code forwards it to the language server, and the server re-indexes the affected files.

5. **How ENABLE_LSP_TOOL is stored** — The flag is written into `~/.claude/settings.json` under the `env` key:
   ```json
   {
     "env": {
       "ENABLE_LSP_TOOL": "1"
     }
   }
   ```
   Installing the LSP plugin via `/plugin install` sets this automatically. You can also set it manually as a shell export:
   ```bash
   export ENABLE_LSP_TOOL=1
   claude
   ```

### Why it is faster

A language server keeps a persistent in-memory symbol index. When Claude queries "where is `processPayment` defined?", it is a direct index lookup — milliseconds. Grep has to scan every file on disk — seconds.

---

## LSP Indexing & Git Workflow

### Does the index survive between sessions?

**No.** The language server is a child process tied to Claude Code's lifetime. When you close Claude Code, the server process exits and the in-memory index is gone. The next session starts a fresh index.

### What happens with `git pull` / `git merge`?

| Scenario | What happens |
|---|---|
| **git pull → start Claude session** | ✅ Best case. Server indexes the current on-disk state (post-pull) from scratch. Full, coherent index. |
| **git pull while Claude session is running** | ✅ Works. File system watchers fire, `workspace/didChangeWatchedFiles` is sent to the server, affected files are re-indexed. May have brief partial staleness on very large merges. |
| **git pull while Claude Code is closed** | ✅ No problem. Server isn't running so there's nothing to update. Next session indexes the post-pull state fresh. |

### Recommended workflow

```bash
git pull origin main   # Pull latest changes
claude                 # Start Claude Code — LSP indexes post-pull state
```

This is the cleanest approach. The server has a complete, coherent view of your codebase from the very first query.

---

## Supported Languages

| Language | LSP Plugin | Binary Required |
|---|---|---|
| TypeScript / JavaScript | `typescript-lsp` | `typescript-language-server` |
| Python | `pyright-lsp` | `pyright-langserver` (via Pyright) |
| Go | `gopls-lsp` | `gopls` |
| Java | `jdtls-lsp` | `jdtls` + JDK |
| Rust | `rust-analyzer-lsp` | `rust-analyzer` |
| C / C++ | `clangd-lsp` | `clangd` |
| C# | `csharp-lsp` | `csharp-ls` |
| Kotlin | `kotlin-lsp` | `kotlin-language-server` |
| Lua | `lua-lsp` | `lua-language-server` |
| PHP | `php-lsp` | `intelephense` |
| Swift | `swift-lsp` | `sourcekit-lsp` |

> You can also create a custom LSP plugin for any language not listed above.

---

## Setup Instructions

### General Pattern

Every language follows the same two steps:
1. Install the language server binary on your system
2. Install the Claude Code LSP plugin (which sets `ENABLE_LSP_TOOL=1` automatically)
3. Restart Claude Code

---

### TypeScript / JavaScript

**Install the binary:**
```bash
npm install -g typescript-language-server typescript
```

**Install the plugin:**
```
/plugin install typescript-lsp@claude-plugins-official
```

**Restart Claude Code.**

---

### Python

**Install the binary:**
```bash
# Option A — via pip
pip install pyright

# Option B — via npm
npm install -g pyright
```

**Install the plugin:**
```
/plugin install pyright-lsp@claude-plugins-official
```

**Restart Claude Code.**

---

### Go

**Install the binary:**
```bash
go install golang.org/x/tools/gopls@latest
```
Make sure `$(go env GOPATH)/bin` is in your `$PATH`.

**Install the plugin:**
```
/plugin install gopls-lsp@claude-plugins-official
```

**Restart Claude Code.**

---

### Java

**Prerequisites:** A JDK must be installed (`java --version` should work).

**Install the binary:**

```bash
# macOS (Homebrew)
brew install jdtls

# Linux (manual) — download from GitHub releases
# https://github.com/eclipse-jdtls/eclipse.jdt.ls/releases
# Extract and ensure the jdtls binary is in your $PATH
```

**Install the plugin:**
```
/plugin install jdtls-lsp@claude-plugins-official
```

**Restart Claude Code.**

---

### Rust

**Install the binary:**
```bash
# Via rustup (recommended)
rustup component add rust-analyzer

# Or via package manager (macOS)
brew install rust-analyzer
```

**Install the plugin:**
```
/plugin install rust-analyzer-lsp@claude-plugins-official
```

**Restart Claude Code.**

---

### Other Languages

For any language not in the official marketplace, you can create a custom LSP plugin with a `.lsp.json` file:

```json
{
  "my-language": {
    "command": "my-language-server",
    "args": ["--stdio"],
    "extensionToLanguage": {
      ".ext": "my-language"
    }
  }
}
```

See the [Claude Code plugin docs](https://docs.anthropic.com/en/docs/claude-code/plugins) for full configuration options.

---

## What Claude Gains

Once installed, Claude automatically gets two new capabilities:

### 1. Automatic Diagnostics

After **every file edit**, the language server analyzes the changes and reports errors and warnings back to Claude — type errors, missing imports, syntax issues — without running a compiler or linter. If Claude introduces a bug, it notices and fixes it in the same turn.

You can view diagnostics inline by pressing **Ctrl+O** when the "diagnostics found" indicator appears.

### 2. Precise Code Navigation

Claude can now:
- Jump to definitions (instead of grep-searching for the function name)
- Find all references to a symbol
- Get type information on hover
- List all symbols in the project
- Find all implementations of an interface
- Trace full call hierarchies

This means when you ask "add a Stripe webhook to my payments page", Claude finds your existing payment logic in ~50ms instead of searching through hundreds of files.

### Token savings

Because Claude no longer wastes context window searching the wrong files, you also save tokens on every task.

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| `Executable not found in $PATH` in /plugin Errors | Language server binary not installed | Install the binary from the table above |
| LSP not activating after plugin install | Requires restart | Fully restart Claude Code |
| LSP not working after `/reload-plugins` | LSP changes need a full restart | Restart Claude Code (not just reload) |
| High memory usage | Some servers (rust-analyzer, pyright) use a lot of RAM on large projects | Disable with `/plugin disable <plugin-name>` if needed |
| False positive errors in monorepos | Language server misconfigured for internal packages | Configure workspace settings in the `.lsp.json` or server config |
| ENABLE_LSP_TOOL not set | Manual setup path | Add `{ "env": { "ENABLE_LSP_TOOL": "1" } }` to `~/.claude/settings.json` |

---

## Internal Setup Reference

### Manual settings.json configuration

If you prefer not to use the plugin system, you can configure LSP manually by editing `~/.claude/settings.json`:

```json
{
  "env": {
    "ENABLE_LSP_TOOL": "1"
  }
}
```

Then launch Claude Code with the plugin directory flag pointing to a directory containing your `.lsp.json`:

```bash
ENABLE_LSP_TOOL=1 claude --plugin-dir ./my-lsp-plugin
```

### Shell profile setup (alternative)

```bash
# Add to ~/.zshrc or ~/.bashrc
export ENABLE_LSP_TOOL=1
```

### Team / project-wide setup

To enable LSP for your entire team automatically, add the plugin to project scope:

```bash
/plugin install typescript-lsp@claude-plugins-official --scope project
```

This writes to `.claude/settings.json` in your repo, so every team member who clones the repo and runs Claude Code gets the LSP plugin automatically.

### Custom .lsp.json fields reference

| Field | Required | Description |
|---|---|---|
| `command` | ✅ | The LSP binary to execute (must be in PATH) |
| `extensionToLanguage` | ✅ | Maps file extensions to language identifiers |
| `args` | ❌ | Command-line arguments for the server |
| `transport` | ❌ | `stdio` (default) or `socket` |
| `env` | ❌ | Environment variables when starting the server |
| `initializationOptions` | ❌ | Options passed during initialization |
| `settings` | ❌ | Settings via `workspace/didChangeConfiguration` |
| `workspaceFolder` | ❌ | Workspace folder path for the server |
| `startupTimeout` | ❌ | Max ms to wait for server startup |
| `shutdownTimeout` | ❌ | Max ms to wait for graceful shutdown |
| `restartOnCrash` | ❌ | Auto-restart if server crashes |
| `maxRestarts` | ❌ | Max restart attempts before giving up |

---

*Generated from official Anthropic Claude Code documentation — code.claude.com/docs*
