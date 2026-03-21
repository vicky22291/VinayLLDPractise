# Superpowers — Complete Architecture & Deep-Dive

> **Repository:** [github.com/obra/superpowers](https://github.com/obra/superpowers)
> **Version:** 5.0.5 | **Author:** Jesse Vincent | **License:** MIT
> **Generated:** 2026-03-19

---

## 1. Project Overview

Superpowers is a **composable skills framework** that transforms AI coding agents (Claude Code, Cursor, Gemini CLI, Codex, OpenCode) into disciplined software engineers. Rather than letting agents jump straight into writing code, Superpowers enforces a structured workflow: brainstorm a design, write a plan, execute via subagents with two-stage review, and finish with proper branch management — all driven by automatically-triggered "skills."

The core philosophy rests on four pillars: **Test-Driven Development** (write tests first, always), **Systematic over ad-hoc** (process over guessing), **Complexity reduction** (simplicity as the primary goal), and **Evidence over claims** (verify before declaring success).

> *Source: [`README.md`](https://github.com/obra/superpowers/blob/main/README.md)*

---

## 2. High-Level System Architecture

```mermaid
graph TB
    subgraph "Host Environment"
        CC["Claude Code"]
        CUR["Cursor"]
        GEM["Gemini CLI"]
        CDX["Codex"]
        OC["OpenCode"]
    end

    subgraph "Plugin Bootstrap Layer"
        PJ["plugin.json<br/>(v5.0.5 metadata)"]
        HK["hooks.json<br/>(SessionStart hook)"]
        SS["session-start<br/>(bash script)"]
        GJ["gemini-extension.json"]
        OP[".opencode/plugins/<br/>superpowers.js"]
    end

    subgraph "Skills Engine"
        US["using-superpowers<br/>(Gateway Skill)"]
        SK["14 Composable Skills"]
        AG["agents/<br/>code-reviewer.md"]
        CMD["commands/<br/>(deprecated)"]
    end

    subgraph "Supporting Infrastructure"
        BS["Brainstorm Server<br/>(WebSocket + HTTP)"]
        WT["Git Worktree<br/>Management"]
        DR["Document Review<br/>System (Subagents)"]
    end

    CC -->|"plugin install"| PJ
    CUR -->|"add-plugin"| PJ
    GEM -->|"extensions install"| GJ
    CDX -->|"fetch INSTALL.md"| PJ
    OC -->|"fetch INSTALL.md"| OP

    PJ --> HK
    HK -->|"SessionStart"| SS
    SS -->|"Injects using-superpowers<br/>into context"| US
    GJ -->|"activates via<br/>GEMINI.md"| US

    US -->|"Skill tool<br/>invocation"| SK
    SK --> BS
    SK --> WT
    SK --> DR
    AG -->|"Used by<br/>requesting-code-review"| SK
```

> *Sources: [`.claude-plugin/plugin.json`](https://github.com/obra/superpowers/blob/main/.claude-plugin/plugin.json), [`hooks/hooks.json`](https://github.com/obra/superpowers/blob/main/hooks/hooks.json), [`hooks/session-start`](https://github.com/obra/superpowers/blob/main/hooks/session-start)*

---

## 3. Bootstrap & Initialization Flow

When a session starts, the plugin injects the gateway skill into the agent's context so that all subsequent actions are routed through the skills system.

```mermaid
sequenceDiagram
    participant User
    participant Platform as Claude Code / Cursor / Gemini
    participant Hook as SessionStart Hook
    participant Script as session-start (bash)
    participant Agent as AI Agent

    User->>Platform: Start new session
    Platform->>Hook: Trigger SessionStart event
    Hook->>Script: Execute via run-hook.cmd
    Script->>Script: Read using-superpowers/SKILL.md
    Script->>Script: Check for legacy ~/.config/superpowers/skills
    alt Legacy directory exists
        Script->>Script: Build migration warning
    end
    Script->>Platform: Return JSON with additionalContext
    Note over Script,Platform: Claude Code uses hookSpecificOutput<br/>Cursor uses additional_context
    Platform->>Agent: Context injected:<br/>"You have superpowers."
    Agent->>Agent: using-superpowers skill is active
    User->>Agent: First message
    Agent->>Agent: Check: does any skill apply?
    alt Skill applies (even 1% chance)
        Agent->>Platform: Invoke Skill tool
        Platform->>Agent: Skill content loaded
        Agent->>Agent: Follow skill instructions
    else No skill applies
        Agent->>User: Respond normally
    end
```

The `session-start` script handles platform detection: if `CURSOR_PLUGIN_ROOT` is set, it emits `additional_context`; if `CLAUDE_PLUGIN_ROOT` is set, it emits `hookSpecificOutput.additionalContext`. This prevents double-injection when both environment variables exist.

> *Sources: [`hooks/session-start`](https://github.com/obra/superpowers/blob/main/hooks/session-start), [`hooks/hooks.json`](https://github.com/obra/superpowers/blob/main/hooks/hooks.json), [`hooks/run-hook.cmd`](https://github.com/obra/superpowers/blob/main/hooks/run-hook.cmd)*

---

## 4. The Complete Development Workflow

This is the core value proposition of Superpowers — an end-to-end workflow from idea to merged code.

```mermaid
flowchart TD
    A["User has an idea"] --> B{"brainstorming<br/>skill"}
    B -->|"Explore context,<br/>ask questions,<br/>propose approaches"| C["Design Document<br/>(spec)"]
    C --> D{"Spec Review Loop<br/>(subagent reviewer)"}
    D -->|"Issues found"| C
    D -->|"Approved"| E["User reviews spec"]
    E -->|"Changes requested"| C
    E -->|"Approved"| F{"writing-plans<br/>skill"}
    F -->|"Break into<br/>bite-sized tasks<br/>(2-5 min each)"| G["Implementation Plan"]
    G --> H{"Plan Review Loop<br/>(subagent reviewer)"}
    H -->|"Issues found"| G
    H -->|"Approved"| I{"Execution Choice"}

    I -->|"Subagent-Driven<br/>(recommended)"| J{"subagent-driven-<br/>development"}
    I -->|"Inline Execution"| K{"executing-plans"}

    J --> L["Per-Task Loop"]
    K --> L

    subgraph "Per-Task Execution"
        L --> M["using-git-worktrees<br/>(isolated workspace)"]
        M --> N["test-driven-development<br/>(RED-GREEN-REFACTOR)"]
        N --> O["requesting-code-review<br/>(subagent reviewer)"]
        O -->|"Issues"| N
        O -->|"Approved"| P["Next task"]
    end

    P --> Q{"All tasks<br/>complete?"}
    Q -->|"No"| L
    Q -->|"Yes"| R["Final code review<br/>(full implementation)"]
    R --> S{"finishing-a-<br/>development-branch"}
    S --> T["Merge / PR / Keep / Discard"]

    style B fill:#f9e79f
    style F fill:#f9e79f
    style J fill:#aed6f1
    style K fill:#aed6f1
    style N fill:#a9dfbf
    style S fill:#f5b7b1
```

> *Sources: [`README.md`](https://github.com/obra/superpowers/blob/main/README.md), [`skills/brainstorming/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/brainstorming/SKILL.md), [`skills/writing-plans/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/writing-plans/SKILL.md), [`skills/subagent-driven-development/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/subagent-driven-development/SKILL.md)*

---

## 5. Skills Taxonomy & Dependency Map

The 14 skills fall into four categories and form a directed dependency graph.

```mermaid
graph LR
    subgraph "Meta Skills"
        US["using-superpowers<br/>(Gateway)"]
        WS["writing-skills<br/>(Skill Authoring)"]
    end

    subgraph "Design & Planning"
        BR["brainstorming"]
        WP["writing-plans"]
    end

    subgraph "Execution & Implementation"
        SDD["subagent-driven-<br/>development"]
        EP["executing-plans"]
        DPA["dispatching-<br/>parallel-agents"]
        TDD["test-driven-<br/>development"]
        UGW["using-git-<br/>worktrees"]
        FDB["finishing-a-<br/>development-branch"]
    end

    subgraph "Quality & Review"
        RCR["requesting-<br/>code-review"]
        RECV["receiving-<br/>code-review"]
        VBC["verification-<br/>before-completion"]
        SD["systematic-<br/>debugging"]
    end

    US -.->|"triggers all"| BR
    US -.->|"triggers all"| SD
    BR -->|"invokes"| WP
    WP -->|"hands off to"| SDD
    WP -->|"hands off to"| EP
    SDD -->|"requires"| UGW
    SDD -->|"requires"| TDD
    SDD -->|"requires"| RCR
    SDD -->|"requires"| FDB
    EP -->|"requires"| UGW
    EP -->|"requires"| FDB
    RCR -->|"feedback via"| RECV
    FDB -->|"cleans up"| UGW
    SD -->|"fix verified by"| VBC
    TDD -->|"completion via"| VBC
```

### Skills Reference Table

| Skill | Category | Trigger | Key Behavior |
|-------|----------|---------|--------------|
| `using-superpowers` | Meta | Every session start | Gateway; checks if any skill applies before every response |
| `writing-skills` | Meta | Creating new skills | TDD applied to documentation; skill authoring guide |
| `brainstorming` | Design | Before any creative/feature work | Hard gate: no code until design approved |
| `writing-plans` | Planning | After approved spec | Bite-sized tasks (2-5 min), exact file paths, complete code |
| `subagent-driven-development` | Execution | Executing plans (same session) | Fresh subagent per task + two-stage review |
| `executing-plans` | Execution | Executing plans (parallel session) | Batch execution with human checkpoints |
| `dispatching-parallel-agents` | Execution | Multiple independent tasks | Concurrent subagent dispatch pattern |
| `test-driven-development` | Execution | Any feature or bugfix | Iron law: no production code without failing test first |
| `using-git-worktrees` | Execution | Feature work needing isolation | Smart directory selection + safety verification |
| `finishing-a-development-branch` | Execution | Work complete, tests pass | 4 options: merge, PR, keep, discard |
| `requesting-code-review` | Quality | After each task, before merge | Dispatches code-reviewer subagent |
| `receiving-code-review` | Quality | When review feedback arrives | Technical evaluation over performative agreement |
| `verification-before-completion` | Quality | Any completion claim | Iron law: no completion without fresh verification evidence |
| `systematic-debugging` | Quality | Bug investigation | 4-phase root cause process; no fixes without root cause |

> *Sources: All 14 `SKILL.md` files in [`skills/`](https://github.com/obra/superpowers/tree/main/skills)*

---

## 6. Subagent-Driven Development — Deep Dive

This is the flagship execution model. A coordinator agent dispatches fresh subagents per task, with a two-stage review pipeline (spec compliance, then code quality).

```mermaid
stateDiagram-v2
    [*] --> ReadPlan: Load plan file

    ReadPlan --> ExtractTasks: Extract all tasks with full text

    state "Per-Task Loop" as TaskLoop {
        ExtractTasks --> DispatchImpl: Dispatch implementer subagent

        state DispatchImpl <<choice>>
        DispatchImpl --> AnswerQuestions: NEEDS_CONTEXT
        DispatchImpl --> Implementation: DONE / proceeds
        DispatchImpl --> HandleBlocked: BLOCKED

        AnswerQuestions --> DispatchImpl: Re-dispatch with context

        HandleBlocked --> ProvideMoreContext: Context problem
        HandleBlocked --> UpgradeModel: Needs more reasoning
        HandleBlocked --> SplitTask: Task too large
        HandleBlocked --> EscalateHuman: Plan is wrong

        ProvideMoreContext --> DispatchImpl
        UpgradeModel --> DispatchImpl
        SplitTask --> DispatchImpl

        Implementation --> SpecReview: Dispatch spec reviewer

        state SpecReview <<choice>>
        SpecReview --> FixSpecGaps: Issues found
        SpecReview --> QualityReview: Approved

        FixSpecGaps --> SpecReview: Re-review

        QualityReview --> FixQuality: Issues found
        QualityReview --> TaskComplete: Approved

        state QualityReview <<choice>>
        FixQuality --> QualityReview: Re-review
    end

    TaskComplete --> MoreTasks: Check remaining tasks

    state MoreTasks <<choice>>
    MoreTasks --> DispatchImpl: More tasks
    MoreTasks --> FinalReview: All done

    FinalReview --> FinishBranch: finishing-a-development-branch
    FinishBranch --> [*]
```

### Subagent Roles & Prompt Templates

```mermaid
graph TB
    COORD["Coordinator<br/>(Main Agent)"]

    subgraph "Implementer Subagent"
        IMP["implementer-prompt.md"]
        IMP_DO["Implements code<br/>Writes tests (TDD)<br/>Commits work<br/>Self-reviews"]
    end

    subgraph "Spec Reviewer Subagent"
        SPEC["spec-reviewer-prompt.md"]
        SPEC_DO["Verifies code matches spec<br/>Checks nothing extra added<br/>Checks nothing missing"]
    end

    subgraph "Quality Reviewer Subagent"
        QUAL["code-quality-reviewer-prompt.md"]
        QUAL_DO["Architecture review<br/>Code quality check<br/>Issue categorization<br/>by severity"]
    end

    COORD -->|"Task text +<br/>project context"| IMP
    IMP --> IMP_DO
    IMP_DO -->|"Status:<br/>DONE / DONE_WITH_CONCERNS /<br/>NEEDS_CONTEXT / BLOCKED"| COORD
    COORD -->|"Changed files +<br/>spec section"| SPEC
    SPEC --> SPEC_DO
    SPEC_DO -->|"Approved / Issues"| COORD
    COORD -->|"Git SHAs +<br/>plan context"| QUAL
    QUAL --> QUAL_DO
    QUAL_DO -->|"Approved / Issues<br/>(Critical/Important/Suggestion)"| COORD
```

Model selection follows a cost-optimization strategy: mechanical tasks (1-2 files, clear spec) use cheap/fast models, integration tasks use standard models, and architecture/review tasks use the most capable model.

> *Sources: [`skills/subagent-driven-development/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/subagent-driven-development/SKILL.md), [`skills/subagent-driven-development/implementer-prompt.md`](https://github.com/obra/superpowers/blob/main/skills/subagent-driven-development/implementer-prompt.md), [`skills/subagent-driven-development/spec-reviewer-prompt.md`](https://github.com/obra/superpowers/blob/main/skills/subagent-driven-development/spec-reviewer-prompt.md), [`skills/subagent-driven-development/code-quality-reviewer-prompt.md`](https://github.com/obra/superpowers/blob/main/skills/subagent-driven-development/code-quality-reviewer-prompt.md)*

---

## 7. Test-Driven Development Cycle

The TDD skill enforces the strictest discipline in the entire system, with an absolute iron law.

```mermaid
flowchart LR
    RED["RED<br/>Write failing test"]
    VRED{"Verify:<br/>fails correctly?"}
    GREEN["GREEN<br/>Minimal code to pass"]
    VGREEN{"Verify:<br/>all tests pass?"}
    REFACTOR["REFACTOR<br/>Clean up code"]
    NEXT["Next behavior"]

    RED --> VRED
    VRED -->|"Yes, fails<br/>as expected"| GREEN
    VRED -->|"Wrong failure<br/>or passes"| RED
    GREEN --> VGREEN
    VGREEN -->|"All pass"| REFACTOR
    VGREEN -->|"Fails"| GREEN
    REFACTOR --> VGREEN
    VGREEN -->|"Still passing"| NEXT
    NEXT --> RED

    style RED fill:#ffcccc
    style GREEN fill:#ccffcc
    style REFACTOR fill:#ccccff
```

The iron law is enforced through rationalization prevention — the skill explicitly lists common excuses and marks them as red flags that mean "delete code, start over."

> *Source: [`skills/test-driven-development/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/test-driven-development/SKILL.md)*

---

## 8. Systematic Debugging — 4-Phase Process

```mermaid
flowchart TD
    BUG["Bug Reported"] --> P1

    subgraph P1["Phase 1: Root Cause Investigation"]
        RC1["Reproduce the bug"]
        RC2["Trace data flow backward"]
        RC3["Identify exact failure point"]
        RC1 --> RC2 --> RC3
    end

    P1 --> P2

    subgraph P2["Phase 2: Pattern Analysis"]
        PA1["Check for similar issues"]
        PA2["Identify environmental factors"]
        PA3["Map affected components"]
        PA1 --> PA2 --> PA3
    end

    P2 --> P3

    subgraph P3["Phase 3: Hypothesis & Testing"]
        HT1["Form hypothesis"]
        HT2["Design targeted test"]
        HT3["Validate or reject"]
        HT1 --> HT2 --> HT3
        HT3 -->|"Rejected"| HT1
    end

    P3 --> P4

    subgraph P4["Phase 4: Implementation"]
        IM1["Write failing test<br/>(TDD integration)"]
        IM2["Implement minimal fix"]
        IM3["Verify fix + no regressions"]
        IM4["verification-before-completion"]
        IM1 --> IM2 --> IM3 --> IM4
    end

    style P1 fill:#fff3cd
    style P2 fill:#d1ecf1
    style P3 fill:#d4edda
    style P4 fill:#f8d7da
```

Supporting techniques include root-cause tracing (tracing bugs backward through stack traces), defense-in-depth (four-layer validation pattern), and condition-based waiting (replacing arbitrary timeouts with polling for flaky tests).

> *Sources: [`skills/systematic-debugging/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/systematic-debugging/SKILL.md), [`skills/systematic-debugging/root-cause-tracing.md`](https://github.com/obra/superpowers/blob/main/skills/systematic-debugging/root-cause-tracing.md), [`skills/systematic-debugging/defense-in-depth.md`](https://github.com/obra/superpowers/blob/main/skills/systematic-debugging/defense-in-depth.md), [`skills/systematic-debugging/condition-based-waiting.md`](https://github.com/obra/superpowers/blob/main/skills/systematic-debugging/condition-based-waiting.md)*

---

## 9. Brainstorming & Visual Companion Architecture

The brainstorming skill includes a browser-based visual companion powered by a zero-dependency WebSocket server.

```mermaid
graph TB
    subgraph "Terminal (Agent)"
        AG["AI Agent"]
        AG_Q["Asks questions<br/>one at a time"]
        AG_D["Presents design<br/>in sections"]
    end

    subgraph "Browser (Visual Companion)"
        SRV["server.cjs<br/>(Node.js, RFC 6455)"]
        WS["WebSocket Protocol"]
        FT["frame-template.html<br/>(Light/Dark themes)"]
        HLP["helper.js<br/>(Client-side events)"]
    end

    subgraph "Content Types"
        OPT["Options / Cards"]
        MCK["Mockups / Wireframes"]
        DIA["Architecture Diagrams"]
        CMP["Side-by-Side Comparisons"]
    end

    AG -->|"Text questions"| User
    AG -->|"Visual questions"| SRV
    SRV -->|"WebSocket"| WS
    WS --> FT
    FT --> HLP
    HLP -->|"Selection events"| SRV
    SRV -->|"User choice"| AG

    SRV --> OPT
    SRV --> MCK
    SRV --> DIA
    SRV --> CMP

    subgraph "Spec Review Pipeline"
        SD["spec-document-<br/>reviewer-prompt.md"]
        SR["Subagent reviewer"]
        SD --> SR
        SR -->|"Approved / Issues"| AG
    end
```

The server implements RFC 6455 WebSocket handshake with zero external dependencies (no `node_modules`). It supports file watching for live updates and persistence of brainstorming state.

> *Sources: [`skills/brainstorming/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/brainstorming/SKILL.md), [`skills/brainstorming/visual-companion.md`](https://github.com/obra/superpowers/blob/main/skills/brainstorming/visual-companion.md), [`skills/brainstorming/scripts/server.cjs`](https://github.com/obra/superpowers/blob/main/skills/brainstorming/scripts/server.cjs)*

---

## 10. Document Review System

A subagent-based review pipeline ensures quality at every stage of the workflow.

```mermaid
flowchart TD
    subgraph "Brainstorming Phase"
        SPEC_DOC["Spec Document Written"]
        SPEC_REV["spec-document-reviewer<br/>(subagent)"]
        SPEC_DOC --> SPEC_REV
        SPEC_REV -->|"Issues"| SPEC_DOC
        SPEC_REV -->|"Approved<br/>(max 3 iterations)"| USER_REV["User Review Gate"]
    end

    subgraph "Planning Phase"
        PLAN_DOC["Plan Document Written"]
        PLAN_REV["plan-document-reviewer<br/>(subagent)"]
        PLAN_DOC --> PLAN_REV
        PLAN_REV -->|"Issues"| PLAN_DOC
        PLAN_REV -->|"Approved<br/>(max 3 iterations)"| EXEC["Execution Handoff"]
    end

    subgraph "Execution Phase"
        TASK["Task Implemented"]
        SPEC_CR["Spec Compliance<br/>Reviewer (subagent)"]
        QUAL_CR["Code Quality<br/>Reviewer (subagent)"]
        TASK --> SPEC_CR
        SPEC_CR -->|"Issues"| TASK
        SPEC_CR -->|"Approved"| QUAL_CR
        QUAL_CR -->|"Issues"| TASK
        QUAL_CR -->|"Approved"| DONE["Task Complete"]
    end

    subgraph "Completion Phase"
        ALL["All Tasks Done"]
        FINAL["Final Code Review<br/>(full implementation)"]
        ALL --> FINAL
        FINAL --> FINISH["finishing-a-development-branch"]
    end
```

Each review loop has a maximum of 3 iterations before escalating to the human. Reviewers are dispatched with precisely crafted context (never session history) to keep them focused.

> *Sources: [`skills/brainstorming/spec-document-reviewer-prompt.md`](https://github.com/obra/superpowers/blob/main/skills/brainstorming/spec-document-reviewer-prompt.md), [`skills/writing-plans/plan-document-reviewer-prompt.md`](https://github.com/obra/superpowers/blob/main/skills/writing-plans/plan-document-reviewer-prompt.md), [`docs/superpowers/specs/2026-01-22-document-review-system-design.md`](https://github.com/obra/superpowers/blob/main/docs/superpowers/specs/2026-01-22-document-review-system-design.md)*

---

## 11. Multi-Platform Integration Architecture

Superpowers supports five different AI coding platforms with platform-specific adapters.

```mermaid
graph TB
    subgraph "Core"
        SKILLS["14 Skills<br/>(SKILL.md files)"]
        AGENTS["agents/<br/>code-reviewer.md"]
        PROMPTS["Subagent Prompt<br/>Templates"]
    end

    subgraph "Claude Code"
        CC_PLUGIN[".claude-plugin/<br/>plugin.json"]
        CC_HOOKS["hooks/hooks.json<br/>(SessionStart)"]
        CC_SCRIPT["hooks/session-start"]
        CC_MARKET[".claude-plugin/<br/>marketplace.json"]
        CC_PLUGIN --> CC_HOOKS --> CC_SCRIPT
    end

    subgraph "Cursor"
        CUR_PLUGIN[".cursor-plugin/<br/>plugin.json"]
        CUR_HOOKS["hooks/<br/>hooks-cursor.json"]
        CUR_SCRIPT["hooks/session-start<br/>(CURSOR_PLUGIN_ROOT)"]
        CUR_PLUGIN --> CUR_HOOKS --> CUR_SCRIPT
    end

    subgraph "Gemini CLI"
        GEM_EXT["gemini-extension.json"]
        GEM_MD["GEMINI.md"]
        GEM_TOOLS["references/<br/>codex-tools.md"]
        GEM_EXT --> GEM_MD
    end

    subgraph "Codex"
        CDX_INST[".codex/INSTALL.md"]
        CDX_SETUP["Symlinks to<br/>skills directory"]
    end

    subgraph "OpenCode"
        OC_INST[".opencode/INSTALL.md"]
        OC_PLUG[".opencode/plugins/<br/>superpowers.js"]
        OC_INST --> OC_PLUG
    end

    CC_SCRIPT --> SKILLS
    CUR_SCRIPT --> SKILLS
    GEM_MD --> SKILLS
    CDX_SETUP --> SKILLS
    OC_PLUG --> SKILLS
    SKILLS --> AGENTS
    SKILLS --> PROMPTS
```

The `run-hook.cmd` file is a polyglot script (both Windows batch and Unix shell) enabling cross-platform hook execution. The OpenCode plugin (`superpowers.js`) is a native ESM module that injects the bootstrap content and auto-registers skills.

> *Sources: [`.claude-plugin/plugin.json`](https://github.com/obra/superpowers/blob/main/.claude-plugin/plugin.json), [`.cursor-plugin/plugin.json`](https://github.com/obra/superpowers/blob/main/.cursor-plugin/plugin.json), [`gemini-extension.json`](https://github.com/obra/superpowers/blob/main/gemini-extension.json), [`.codex/INSTALL.md`](https://github.com/obra/superpowers/blob/main/.codex/INSTALL.md), [`.opencode/plugins/superpowers.js`](https://github.com/obra/superpowers/blob/main/.opencode/plugins/superpowers.js), [`docs/windows/polyglot-hooks.md`](https://github.com/obra/superpowers/blob/main/docs/windows/polyglot-hooks.md)*

---

## 12. Repository File Structure

```mermaid
graph TD
    ROOT["superpowers/"]

    ROOT --> SKILLS_DIR["skills/ (14 skills)"]
    ROOT --> AGENTS_DIR["agents/"]
    ROOT --> COMMANDS["commands/ (deprecated)"]
    ROOT --> HOOKS["hooks/"]
    ROOT --> DOCS["docs/"]
    ROOT --> TESTS["tests/"]
    ROOT --> PLUGIN_CC[".claude-plugin/"]
    ROOT --> PLUGIN_CUR[".cursor-plugin/"]
    ROOT --> OPENCODE[".opencode/"]
    ROOT --> CODEX[".codex/"]
    ROOT --> CONFIG["Config files"]

    SKILLS_DIR --> SK_META["Meta: using-superpowers,<br/>writing-skills"]
    SKILLS_DIR --> SK_DESIGN["Design: brainstorming,<br/>writing-plans"]
    SKILLS_DIR --> SK_EXEC["Execution: subagent-driven-dev,<br/>executing-plans, dispatching-<br/>parallel-agents, TDD,<br/>git-worktrees, finishing-branch"]
    SKILLS_DIR --> SK_QUALITY["Quality: requesting-code-review,<br/>receiving-code-review,<br/>verification-before-completion,<br/>systematic-debugging"]

    AGENTS_DIR --> CR["code-reviewer.md"]

    HOOKS --> HJ["hooks.json"]
    HOOKS --> HCJ["hooks-cursor.json"]
    HOOKS --> HSS["session-start"]
    HOOKS --> HRC["run-hook.cmd"]

    DOCS --> SPECS["superpowers/specs/<br/>(3 design specs)"]
    DOCS --> PLANS["superpowers/plans/<br/>(3 impl plans)"]
    DOCS --> LEGACY["plans/<br/>(4 legacy plans)"]

    TESTS --> T_BS["brainstorm-server/"]
    TESTS --> T_OC["opencode/"]
    TESTS --> T_SK["skill-triggering/"]
    TESTS --> T_EX["explicit-skill-requests/"]
    TESTS --> T_SDD["subagent-driven-dev/"]
    TESTS --> T_CC["claude-code/"]

    CONFIG --> PKG["package.json"]
    CONFIG --> GEM["gemini-extension.json + GEMINI.md"]
    CONFIG --> LIC["LICENSE (MIT)"]
```

> *Source: Repository file listing*

---

## 13. Skill Authoring & Testing (Meta-Skill)

The `writing-skills` skill applies TDD principles to documentation itself.

```mermaid
flowchart TD
    IDEA["Skill idea"] --> FAIL_TEST["Write failing test<br/>(prompt that current skills handle poorly)"]
    FAIL_TEST --> VERIFY_FAIL["Verify test fails<br/>(run against agent, confirm poor result)"]
    VERIFY_FAIL --> WRITE_SKILL["Write SKILL.md<br/>(minimal content to pass)"]
    WRITE_SKILL --> VERIFY_PASS["Verify test passes<br/>(run again, confirm good result)"]
    VERIFY_PASS --> REFACTOR["Refactor skill<br/>(improve clarity, add examples)"]
    REFACTOR --> PRESSURE["Pressure test<br/>(adversarial prompts,<br/>rationalization scenarios)"]
    PRESSURE -->|"Fails"| WRITE_SKILL
    PRESSURE -->|"Passes"| DONE["Skill complete"]

    style FAIL_TEST fill:#ffcccc
    style WRITE_SKILL fill:#ccffcc
    style REFACTOR fill:#ccccff
```

Skills use Claude Search Optimization (CSO) — structuring content so the agent will find and follow instructions reliably. The `writing-skills` skill includes guidance on persuasion principles (authority, commitment, scarcity), Graphviz conventions for flow diagrams, and a render-graphs.js utility.

> *Sources: [`skills/writing-skills/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/writing-skills/SKILL.md), [`skills/writing-skills/anthropic-best-practices.md`](https://github.com/obra/superpowers/blob/main/skills/writing-skills/anthropic-best-practices.md), [`skills/writing-skills/persuasion-principles.md`](https://github.com/obra/superpowers/blob/main/skills/writing-skills/persuasion-principles.md)*

---

## 14. Git Worktree Lifecycle

```mermaid
stateDiagram-v2
    [*] --> CheckExisting: Start feature work

    CheckExisting --> UseExisting: .worktrees/ or worktrees/ found
    CheckExisting --> CheckClaudeMD: Neither found

    CheckClaudeMD --> UsePreference: Preference specified
    CheckClaudeMD --> AskUser: No preference

    AskUser --> ProjectLocal: User picks .worktrees/
    AskUser --> GlobalDir: User picks ~/.config/superpowers/worktrees/

    UseExisting --> VerifyIgnored: Project-local directory
    UsePreference --> VerifyIgnored: Project-local directory
    ProjectLocal --> VerifyIgnored

    VerifyIgnored --> AddGitignore: NOT ignored
    VerifyIgnored --> CreateWorktree: Already ignored
    AddGitignore --> CommitFix: Add to .gitignore
    CommitFix --> CreateWorktree

    GlobalDir --> CreateWorktree: No gitignore needed

    CreateWorktree --> RunSetup: git worktree add
    RunSetup --> RunTests: npm install / cargo build / etc.
    RunTests --> Ready: All tests pass
    RunTests --> ReportFailures: Tests fail
    ReportFailures --> AskProceed: Ask user

    Ready --> DoWork: Implement feature

    DoWork --> FinishBranch: All tasks complete

    state FinishBranch {
        VerifyTests --> PresentOptions: Tests pass
        PresentOptions --> MergeLocal: Option 1
        PresentOptions --> CreatePR: Option 2
        PresentOptions --> KeepAsIs: Option 3
        PresentOptions --> Discard: Option 4
    }

    MergeLocal --> CleanupWorktree
    CreatePR --> CleanupWorktree
    Discard --> CleanupWorktree
    KeepAsIs --> [*]: Worktree preserved

    CleanupWorktree --> [*]: git worktree remove
```

> *Sources: [`skills/using-git-worktrees/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/using-git-worktrees/SKILL.md), [`skills/finishing-a-development-branch/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/finishing-a-development-branch/SKILL.md)*

---

## 15. Instruction Priority & Rationalization Prevention

A key design pattern throughout Superpowers is explicit rationalization prevention. Multiple skills define "iron laws" and then list common excuses agents use to skip them.

```mermaid
graph TD
    subgraph "Priority Hierarchy"
        P1["1. User instructions<br/>(CLAUDE.md, direct requests)"]
        P2["2. Superpowers skills"]
        P3["3. Default system prompt"]
        P1 --> P2 --> P3
    end

    subgraph "Iron Laws (Non-Negotiable)"
        IL1["TDD: No production code<br/>without failing test first"]
        IL2["Debugging: No fixes<br/>without root cause"]
        IL3["Verification: No completion<br/>claims without evidence"]
        IL4["Brainstorming: No code<br/>until design approved"]
        IL5["Skills: Invoke skill if even<br/>1% chance it applies"]
    end

    subgraph "Rationalization Red Flags"
        RF1["'Too simple to need X'"]
        RF2["'I'll do X after'"]
        RF3["'Just this once'"]
        RF4["'I already manually tested'"]
        RF5["'This is different because...'"]
    end

    IL1 --- RF1
    IL1 --- RF2
    IL2 --- RF3
    IL3 --- RF4
    IL4 --- RF5

    style IL1 fill:#f8d7da
    style IL2 fill:#f8d7da
    style IL3 fill:#f8d7da
    style IL4 fill:#f8d7da
    style IL5 fill:#f8d7da
    style RF1 fill:#fff3cd
    style RF2 fill:#fff3cd
    style RF3 fill:#fff3cd
    style RF4 fill:#fff3cd
    style RF5 fill:#fff3cd
```

> *Sources: [`skills/using-superpowers/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/using-superpowers/SKILL.md), [`skills/test-driven-development/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/test-driven-development/SKILL.md), [`skills/systematic-debugging/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/systematic-debugging/SKILL.md), [`skills/verification-before-completion/SKILL.md`](https://github.com/obra/superpowers/blob/main/skills/verification-before-completion/SKILL.md)*

---

## 16. Testing Infrastructure

```mermaid
graph LR
    subgraph "Test Suites"
        T1["brainstorm-server/<br/>Unit tests (Jest)<br/>+ lifecycle tests"]
        T2["skill-triggering/<br/>Prompt-based tests"]
        T3["explicit-skill-requests/<br/>Multi-turn conversation tests"]
        T4["subagent-driven-dev/<br/>Integration tests<br/>(Svelte, Go projects)"]
        T5["claude-code/<br/>Full integration tests"]
        T6["opencode/<br/>Plugin loading tests"]
    end

    subgraph "Test Tools"
        TT1["analyze-token-usage.py<br/>(Token cost analysis)"]
        TT2["find-polluter.sh<br/>(Test isolation bisection)"]
        TT3["test-helpers.sh<br/>(Common utilities)"]
    end

    T1 --> TT1
    T4 --> TT2
    T5 --> TT3
```

The testing approach includes prompt-based tests (running prompts through agents and checking for correct skill triggering), integration tests with real scaffolded projects, and token usage analysis for cost optimization.

> *Sources: [`docs/testing.md`](https://github.com/obra/superpowers/blob/main/docs/testing.md), [`tests/`](https://github.com/obra/superpowers/tree/main/tests)*

---

## 17. Evolution & Design History

The project has evolved through several major architectural decisions, documented in its specs and plans.

```mermaid
timeline
    title Superpowers Evolution
    section Early Development
        2025-11 : OpenCode support design & implementation
        2025-11 : Skills improvements from user feedback (8 real problems fixed)
    section Maturation
        2026-01 : Visual brainstorming feature added
        2026-01 : Document review system (subagent reviewers for specs & plans)
    section Refinement
        2026-02 : Visual brainstorming refactored (blocking TUI → browser + terminal)
        2026-03 : Zero-dependency brainstorm server (RFC 6455, no node_modules)
    section Current
        2026-03 : v5.0.5 — ESM fix, Windows PID handling, stop-server reliability
```

> *Sources: [`CHANGELOG.md`](https://github.com/obra/superpowers/blob/main/CHANGELOG.md), [`RELEASE-NOTES.md`](https://github.com/obra/superpowers/blob/main/RELEASE-NOTES.md), all files in [`docs/superpowers/specs/`](https://github.com/obra/superpowers/tree/main/docs/superpowers/specs) and [`docs/plans/`](https://github.com/obra/superpowers/tree/main/docs/plans)*

---

## 18. Key Design Decisions & Trade-offs

| Decision | Rationale | Trade-off |
|----------|-----------|-----------|
| Fresh subagent per task | Prevents context pollution; each task gets clean slate | More subagent invocations = higher token cost |
| Two-stage review (spec then quality) | Spec compliance prevents over/under-building; quality ensures good code | Adds latency per task |
| Iron laws with rationalization lists | Agents tend to rationalize skipping discipline; explicit lists counter this | Verbose skill files; feels rigid |
| Zero-dependency brainstorm server | Eliminates vendored `node_modules`; simpler distribution | Must implement RFC 6455 from scratch |
| Polyglot hook script | Single file works on Windows and Unix | Complex bash/batch interleaving |
| Skills trigger automatically | User doesn't need to remember workflows | May feel intrusive for simple tasks |
| Max 3 review iterations then escalate | Prevents infinite loops | May surface to human prematurely |
| Plan tasks are 2-5 minutes each | Small enough for reliable subagent execution | Requires very granular planning |

---

## Summary

Superpowers transforms AI coding agents from eager-but-undisciplined code generators into structured software engineers. Its architecture is built around composable skills that enforce a linear workflow (brainstorm → plan → execute → review → finish), with multiple quality gates (spec review, code review, verification-before-completion) and explicit rationalization prevention at every stage. The system supports five different AI platforms through platform-specific adapters, all sharing the same core skill files.
