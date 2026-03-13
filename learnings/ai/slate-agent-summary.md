# Slate — Swarm-Native Coding Agent by Random Labs

**Source:** [X post by @realmcore_](https://x.com/realmcore_/status/2032146316730778004)
**Date:** March 12, 2026
**Author:** akira (@realmcore_) — [Random Labs](https://www.ycombinator.com/companies/random-labs) (@0xrandomlabs)
**Technical Report:** randomlabs.ai/blog/slate

---

## 1. What is Slate?

Slate is a coding agent that uses a code environment directly for swarm orchestration. Instead of relying on message-passing between isolated subagents (the typical multi-agent pattern), Slate programmatically orchestrates tasks by spinning up parallel "threads" — subagents that share context with a central orchestrator.

The core architectural bet: give an LLM a TypeScript REPL so it can reason at a **strategic level** about what needs to happen, while delegating the **tactical execution** to specialized subthreads. This separation is what makes it "swarm-native" — it's a hive mind, not a relay chain.

```mermaid
graph TB
    subgraph Slate["Slate Agent Architecture"]
        direction TB
        O["Orchestrator Thread<br/><i>Strategic reasoning via TypeScript DSL</i>"]

        subgraph Swarm["Parallel Thread Swarm"]
            T1["Thread 1<br/>Code Generation"]
            T2["Thread 2<br/>Agentic Search"]
            T3["Thread 3<br/>Testing"]
            T4["Thread N<br/>..."]
        end

        O -->|"Delegate task"| T1
        O -->|"Delegate task"| T2
        O -->|"Delegate task"| T3
        O -->|"Delegate task"| T4

        T1 -->|"Shared context"| O
        T2 -->|"Shared context"| O
        T3 -->|"Shared context"| O
        T4 -->|"Shared context"| O
    end

    style Slate fill:#1a1a2e,stroke:#e94560,color:#fff
    style Swarm fill:#16213e,stroke:#0f3460,color:#fff
    style O fill:#e94560,stroke:#fff,color:#fff
    style T1 fill:#0f3460,stroke:#e94560,color:#fff
    style T2 fill:#0f3460,stroke:#e94560,color:#fff
    style T3 fill:#0f3460,stroke:#e94560,color:#fff
    style T4 fill:#0f3460,stroke:#e94560,color:#fff
```

---

## 2. Multi-Model Orchestration

Slate automatically selects the optimal LLM for each subtask. A powerful model handles high-level planning; cheaper/faster models handle routine work; specialized models handle domain-specific tasks like agentic search.

```mermaid
flowchart LR
    subgraph Input["User Task"]
        Task["Complex Coding<br/>Task"]
    end

    subgraph Orchestrator["Orchestrator<br/>(Opus / Sonnet / GPT 5.4)"]
        Plan["Strategic<br/>Planning"]
    end

    subgraph Workers["Worker Threads — Auto-Selected"]
        direction TB
        W1["Codex 5.3<br/><i>Heavy code generation</i>"]
        W2["GLM 5<br/><i>Agentic search</i>"]
        W3["Haiku<br/><i>Simple edits</i>"]
        W4["Sonnet<br/><i>Moderate reasoning</i>"]
    end

    subgraph Output["Result"]
        R["Merged &<br/>Validated Output"]
    end

    Task --> Plan
    Plan --> W1
    Plan --> W2
    Plan --> W3
    Plan --> W4
    W1 --> R
    W2 --> R
    W3 --> R
    W4 --> R

    style Orchestrator fill:#6c5ce7,stroke:#a29bfe,color:#fff
    style Workers fill:#00b894,stroke:#55efc4,color:#fff
    style Plan fill:#6c5ce7,stroke:#fff,color:#fff
    style W1 fill:#00b894,stroke:#fff,color:#fff
    style W2 fill:#00b894,stroke:#fff,color:#fff
    style W3 fill:#00b894,stroke:#fff,color:#fff
    style W4 fill:#00b894,stroke:#fff,color:#fff
    style R fill:#fdcb6e,stroke:#fff,color:#2d3436
```

---

## 3. Core Concepts

### 3.1 Threads — Shared-Context Subagents

In traditional multi-agent systems, each subagent has isolated context and communicates through explicit messages. Slate's threads are fundamentally different: they **share context** with the orchestrator. Each thread has its own working memory ("RAM"), but the orchestrator can read and compose results across all threads.

This maps directly to Karpathy's "LLM OS" vision, where he proposed treating the LLM as a kernel process with its own CPU (reasoning), RAM (context window), and file system (retrieval). In Karpathy's model, each thread would have its own RAM and the main thread delegates to other threads directly — which is exactly what Slate implements.

The threading architecture is also inspired by the **BEAM VM** (Erlang/Elixir's runtime). In the BEAM, processes are extremely lightweight, fully isolated (each has its own heap, stack, and garbage collector), and communicate through non-blocking message passing via mailboxes. BEAM achieves concurrency by interleaving process execution with preemptive scheduling — one scheduler per core, picking processes from a ready queue. Slate adapts these concurrency semantics for LLM orchestration: each thread is lightweight, has its own context, and can be composed by the orchestrator.

The team originally called threads "actors" (matching BEAM terminology) but found that LLMs understand the concept of "threads" more naturally.

```mermaid
flowchart TB
    subgraph Traditional["Traditional Multi-Agent<br/>(Message Passing)"]
        direction LR
        A1["Agent A"] -- "Serialize → Message → Deserialize" --> A2["Agent B"]
        A2 -- "Serialize → Message → Deserialize" --> A3["Agent C"]
        A3 -- "Serialize → Message → Deserialize" --> A1
    end

    subgraph SlateModel["Slate Threads<br/>(Shared Context Hive Mind)"]
        direction TB
        Main["Main Orchestration Thread"]
        Sub1["Thread 1<br/>Own RAM"]
        Sub2["Thread 2<br/>Own RAM"]
        Sub3["Thread 3<br/>Own RAM"]

        Main <-. "Shared context" .-> Sub1
        Main <-. "Shared context" .-> Sub2
        Main <-. "Shared context" .-> Sub3
        Sub1 <-. "Reusable subthread" .-> Sub2
    end

    style Traditional fill:#c0392b,stroke:#e74c3c,color:#fff
    style SlateModel fill:#27ae60,stroke:#2ecc71,color:#fff
    style Main fill:#2ecc71,stroke:#fff,color:#fff
```

### 3.2 Knowledge Overhang

LLMs possess extensive knowledge about how to perform tasks strategically — but during typical execution, they only use a fraction of it because they're bogged down in tactical details. Slate calls this gap **knowledge overhang**.

By giving the model a TypeScript DSL to orchestrate at the strategic level, Slate separates the model's **strategic knowledge** (how to plan and decompose) from its **tactical knowledge** (how to execute individual steps). The orchestrator accesses strategic knowledge directly while threads handle tactics.

### 3.3 Expressivity

Expressivity describes the relationship between how powerful an interface is and how much of that power the model actually leverages. A natural language chat interface is low-expressivity (the model can only describe actions). A full programming language is high-expressivity (the model can compose, loop, branch, and parallelize). Slate's TypeScript DSL is designed to be expressive enough that the model can "program in action space."

```mermaid
quadrantChart
    title Expressivity vs Model Utilization
    x-axis "Low Expressivity" --> "High Expressivity"
    y-axis "Low Utilization" --> "High Utilization"
    "Natural Language Chat": [0.2, 0.7]
    "Tool Calling (JSON)": [0.4, 0.5]
    "ReAct Loop": [0.5, 0.45]
    "Python REPL": [0.7, 0.6]
    "Slate TS DSL": [0.8, 0.85]
```

---

## 4. Technical Foundations — The REPL Principle

Slate and RLM independently converged on the same insight: giving an agent a REPL fundamentally changes task decomposition. The CodeAct paper (ICML 2024) and Voyager (2023) also explored related ideas. Three independent teams arriving at the same primitives suggests these are foundational.

### 4.1 How RLM Works

RLM (Recursive Language Models) was created by Alex L. Zhang, Tim Kraska, and Omar Khattab at MIT CSAIL. Published as arXiv:2512.24601 in late 2025.

The key mechanism: the full input is loaded as a variable in a Python REPL notebook, **not** directly into the model's context window. The model then generates code to inspect, search, or slice the input. It can recursively call itself on subsections, storing results symbolically in variables. The root model aggregates outcomes without ever loading the entire input at once.

This enables handling of extremely long contexts (10M+ tokens) with better accuracy and lower cost than direct context feeding or context compaction.

```mermaid
flowchart TB
    subgraph RLM["RLM — Recursive Language Model"]
        direction TB
        Input["Full Input<br/>(loaded as REPL variable, NOT in context)"]
        Model["LLM generates code<br/>to inspect/slice input"]
        Recurse["Recursively calls itself<br/>on subsections"]
        Vars["Stores intermediate results<br/>in named variables"]
        Root["Root model aggregates<br/>without full input"]

        Input --> Model
        Model --> Recurse
        Recurse --> Vars
        Vars --> Root
        Recurse -->|"Recursive call"| Model
    end

    style RLM fill:#2d3436,stroke:#636e72,color:#fff
    style Input fill:#e17055,stroke:#fff,color:#fff
    style Model fill:#0984e3,stroke:#fff,color:#fff
    style Recurse fill:#6c5ce7,stroke:#fff,color:#fff
    style Vars fill:#fdcb6e,stroke:#fff,color:#2d3436
    style Root fill:#00b894,stroke:#fff,color:#fff
```

### 4.2 CodeAct — Code as Unified Action Space

The CodeAct paper (Wang et al., ICML 2024) proposed using executable Python code as a unified action space for LLM agents. Instead of generating JSON or text in a predefined format (which constrains flexibility), CodeAct agents generate Python code that is directly executed.

Integrated with a Python interpreter, CodeAct agents can dynamically revise prior actions or emit new actions based on execution feedback. Across 17 LLMs, CodeAct achieved up to 20% higher success rates than alternatives.

### 4.3 Voyager — Lifelong Learning Through Code

Voyager (Wang, Xie, Jiang, Fan et al., 2023) is an embodied lifelong learning agent in Minecraft. Its key innovation: an ever-growing **skill library of executable code** for storing and retrieving complex behaviors. Skills are temporally extended, interpretable, and compositional — they compound over time and avoid catastrophic forgetting.

Voyager achieved 3.3x more unique items and unlocked milestones up to 15.3x faster than prior methods.

### 4.4 Shared Primitive: REPL Reference Semantics

All these systems share a common primitive — using a code environment to:

1. **Decompose work** into operations that store values in named references
2. **Reason about the execution graph** rather than performing each operation inline
3. **Compose results** from stored references in non-linear ways

```mermaid
flowchart TB
    subgraph Without["Without REPL — Linear Execution"]
        direction TB
        S1["Step 1: Read file"] --> S2["Step 2: Parse"]
        S2 --> S3["Step 3: Analyze"]
        S3 --> S4["Step 4: Write"]
        S4 --> S5["Step 5: Validate"]
    end

    subgraph With["With REPL — Graph Execution"]
        direction TB
        R1["fileData = readFile()"]
        R2["parsed = parse(fileData)"]
        R3["analysis = analyze(parsed)"]
        R4["output = write(analysis)"]
        R5["check(output, fileData)"]

        R1 --> R2
        R2 --> R3
        R3 --> R4
        R4 --> R5
        R1 -. "ref accessible" .-> R5
        R2 -. "ref accessible" .-> R4
    end

    style Without fill:#636e72,stroke:#b2bec3,color:#fff
    style With fill:#0984e3,stroke:#74b9ff,color:#fff
```

---

## 5. Context Engineering & Memory

Context engineering is a central challenge for long-running agents. Slate employs multiple strategies that align with techniques used by other leading agents.

### 5.1 How Other Agents Handle Context

**Cognition (Devin)** favors a single-agent approach with long-context compression. They argue this delivers greater stability and lower costs than multi-agent setups. Over 2025, Devin became 4x faster at problem-solving and 2x more efficient in resource consumption.

**Manus AI** uses a multi-agent architecture with a planner (assigns tasks), a knowledge manager (determines what to save to filesystem), and an executor (performs tasks). Manus externalizes data to the filesystem — deleting web page content but retaining URLs, clearing documents but keeping file paths. It continuously updates a `todo.md` file, reciting incomplete goals at the end of context to avoid "lost-in-the-middle" problems.

**Fundamental (formerly Altera)** and **Cognition** both operate on the principle of thinking at a high level and delegating at a lower level, compressing lower-level context into something understandable by the strategizing agent.

### 5.2 Slate's Context Pipeline

Slate creates a natural compression boundary at the thread delegation point. Because it delegates simple tactical actions to threads one at a time, each completed thread can be compressed without losing critical information. This leads to tractable **episodic memory**: the system retains only tool calls that contributed to success.

```mermaid
flowchart TB
    subgraph Pipeline["Slate Context Engineering Pipeline"]
        direction TB
        Raw["Raw Thread Output<br/><i>All tool calls, responses, logs</i>"]
        Boundary["Thread Boundary Compression<br/><i>Natural boundary at delegation point</i>"]
        Episodic["Episodic Memory Filter<br/><i>Retain only successful tool calls</i>"]
        Cache["Subthread Cache & Reuse<br/><i>Maximizes API-level caching</i>"]
        Rolling["Rolling Compression<br/><i>Enables sessions up to 2 days</i>"]

        Raw --> Boundary
        Boundary --> Episodic
        Episodic --> Cache
        Cache --> Rolling
    end

    subgraph Outcomes["Outcomes"]
        B1["Lower Cost"]
        B2["Maximized Cache Hits"]
        B3["Long-Horizon Stability"]
    end

    Rolling --> B1
    Rolling --> B2
    Rolling --> B3

    style Pipeline fill:#2d3436,stroke:#636e72,color:#fff
    style Outcomes fill:#00b894,stroke:#55efc4,color:#fff
    style Raw fill:#e17055,stroke:#fff,color:#fff
    style Boundary fill:#fdcb6e,stroke:#fff,color:#2d3436
    style Episodic fill:#74b9ff,stroke:#fff,color:#2d3436
    style Cache fill:#a29bfe,stroke:#fff,color:#fff
    style Rolling fill:#55efc4,stroke:#fff,color:#2d3436
```

### 5.3 Context Technique Comparison

```mermaid
flowchart LR
    subgraph Approaches["Context Engineering Approaches"]
        direction TB
        subgraph Devin["Cognition / Devin"]
            D1["Single agent"]
            D2["Long-context compression"]
            D3["Stability-first"]
        end
        subgraph Manus["Manus AI"]
            M1["Multi-agent: Planner + Knowledge Mgr + Executor"]
            M2["Filesystem externalization"]
            M3["todo.md goal recitation"]
        end
        subgraph SlateCtx["Slate"]
            SL1["Swarm-native threads"]
            SL2["Thread-boundary compression"]
            SL3["Episodic memory + rolling compression"]
            SL4["Subthread caching & reuse"]
        end
    end

    style Devin fill:#e17055,stroke:#fff,color:#fff
    style Manus fill:#6c5ce7,stroke:#fff,color:#fff
    style SlateCtx fill:#00b894,stroke:#fff,color:#fff
```

---

## 6. System Architecture

Slate's client-server architecture is built on top of [OpenCode](https://opencode.ai/), an open-source coding agent framework. OpenCode uses event-driven design for orchestration without tight coupling, structured memory for context management, permission systems for balancing autonomy with user control, and snapshot systems for safety nets during autonomous actions.

```mermaid
flowchart TB
    subgraph Client["Client Layer"]
        UI["User Interface"]
    end

    subgraph Server["Slate Server<br/>(OpenCode-based architecture)"]
        direction TB
        Orch["Orchestrator Engine<br/><i>TypeScript DSL Runtime</i>"]
        Thread["Threading Engine<br/><i>BEAM-inspired concurrency</i>"]
        Model["Model Router<br/><i>Auto model selection</i>"]
        Mem["Memory System<br/><i>Episodic + Rolling</i>"]
        Ctx["Context Cache<br/><i>Subthread reuse</i>"]
    end

    subgraph Models["LLM Providers"]
        direction LR
        Anthropic["Anthropic<br/>Opus / Sonnet / Haiku"]
        OpenAI["OpenAI<br/>GPT 5.4 / Codex 5.3"]
        Other["Others<br/>GLM 5 / etc."]
    end

    subgraph Env["Code Environment"]
        REPL["TypeScript REPL"]
        FS["File System"]
        Tools["Dev Tools & LSP"]
    end

    UI <--> Orch
    Orch <--> Thread
    Orch <--> Model
    Orch <--> Mem
    Thread <--> Ctx
    Model <--> Anthropic
    Model <--> OpenAI
    Model <--> Other
    Orch <--> REPL
    REPL <--> FS
    REPL <--> Tools

    style Client fill:#2d3436,stroke:#636e72,color:#fff
    style Server fill:#6c5ce7,stroke:#a29bfe,color:#fff
    style Models fill:#e17055,stroke:#fab1a0,color:#fff
    style Env fill:#00b894,stroke:#55efc4,color:#fff
```

---

## 7. Intellectual Lineage

```mermaid
timeline
    title Evolution of Code-Based Agent Orchestration
    section 2023 — Foundations
        Voyager (Jim Fan et al.) : Executable skill library for lifelong learning
        : arXiv 2305.16291
    section 2024 — Code as Action
        CodeAct (Wang et al., ICML 2024) : Python code as unified action space
        : arXiv 2402.01030
        Karpathy LLM OS Vision : LLM as kernel with threads, RAM, filesystem
        Cognition launches Devin : Pioneering context engineering for agents
    section 2025 — Recursive Models
        RLM (Zhang, Kraska, Khattab) : REPL reference semantics for recursive decomposition
        : arXiv 2512.24601
        Manus AI : Planner + knowledge manager + executor pattern
        OpenCode reaches 44.6K stars : Open-source agent infrastructure
    section 2026 — Convergence
        Slate (Random Labs) : Swarm-native threads with shared context
        : Independent convergence on REPL primitives
        OpenCode passes 117K stars : Foundation for Slate's architecture
```

---

## 8. Early Results

An earlier, less flexible version of Slate's architecture passed 2 out of 3 tests on the `make-mips-interpreter` task from Terminal Bench 2.0 — a task that Opus 4.5 and 4.6 solve at most 1 in 5 times. Full benchmarks are planned for the coming weeks.

---

## 9. What's Next

- **Codex & Claude Code integration** — Direct support planned for the week following the announcement
- **Long-term memory** — The next major challenge beyond episodic memory
- **Benchmarking** — Hiring a research role for comprehensive evaluation

---

## 10. Citations & References

| Reference | Authors | Year | Link |
|-----------|---------|------|------|
| **RLM: Recursive Language Models** | Alex L. Zhang, Tim Kraska, Omar Khattab (MIT CSAIL) | 2025 | [arXiv:2512.24601](https://arxiv.org/abs/2512.24601), [Blog](https://alexzhang13.github.io/blog/2025/rlm/), [GitHub](https://github.com/alexzhang13/rlm) |
| **CodeAct: Executable Code Actions Elicit Better LLM Agents** | Xingyao Wang, Yangyi Chen, Lifan Yuan et al. | 2024 (ICML) | [arXiv:2402.01030](https://arxiv.org/abs/2402.01030), [GitHub](https://github.com/xingyaoww/code-act) |
| **Voyager: Open-Ended Embodied Agent with LLMs** | Guanzhi Wang, Yuqi Xie, Yunfan Jiang, Jim Fan et al. | 2023 | [arXiv:2305.16291](https://arxiv.org/abs/2305.16291), [Project](https://voyager.minedojo.org/) |
| **Karpathy LLM OS Vision** | Andrej Karpathy | 2024 | [X Post](https://x.com/karpathy/status/1723140519554105733) |
| **Context Engineering for AI Agents (Manus)** | Manus AI Team | 2025 | [Blog](https://manus.im/blog/Context-Engineering-for-AI-Agents-Lessons-from-Building-Manus) |
| **Context Engineering in Manus (Analysis)** | Lance Martin | 2025 | [Blog](https://rlancemartin.github.io/2025/10/15/manus/) |
| **Devin Performance Review** | Cognition | 2025 | [Blog](https://cognition.ai/blog/devin-annual-performance-review-2025) |
| **OpenCode** | thdxr (Dax) et al. | 2025–2026 | [opencode.ai](https://opencode.ai/), [GitHub](https://github.com/sst/opencode) |
| **BEAM VM / Erlang Concurrency** | Ericsson | 1986– | [BEAM Book](https://blog.stenmans.org/theBeamBook/) |
| **Random Labs (YC)** | — | 2026 | [YC Profile](https://www.ycombinator.com/companies/random-labs) |
| **Slate Announcement** | akira (@realmcore_) | Mar 2026 | [X Post](https://x.com/realmcore_/status/2032146316730778004) |
| **Prime Intellect RLM Analysis** | Prime Intellect | 2026 | [Blog](https://www.primeintellect.ai/blog/rlm) |
