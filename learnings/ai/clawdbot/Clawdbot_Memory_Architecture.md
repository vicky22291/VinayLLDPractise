# Clawdbot Memory Architecture - Complete Guide with Mermaid Diagrams

This document explains the complete memory architecture of Clawdbot, an open-source personal AI assistant. All diagrams are provided in Mermaid format for easy visualization.

---

## 1. HIGH-LEVEL ARCHITECTURE OVERVIEW

```mermaid
graph TB
    subgraph "User Interface"
        CLI[CLI Interface]
        Apps[Desktop/Mobile Apps]
    end

    subgraph "Agent Layer"
        Agent[Clawdbot Agent]
        Tools[Memory Tools]
        Session[Session Manager]
    end

    subgraph "Memory Layer"
        MemoryManager[Memory Index Manager]
        EmbeddingProvider[Embedding Provider]
        HybridSearch[Hybrid Search Engine]
    end

    subgraph "Storage Layer"
        Markdown[Markdown Files]
        SQLite[(SQLite DB)]
        VectorIndex[(sqlite-vec)]
        FTS5[(FTS5 Index)]
    end

    CLI --> Agent
    Apps --> Agent
    Agent --> Tools
    Agent --> Session
    Tools --> MemoryManager
    MemoryManager --> EmbeddingProvider
    MemoryManager --> HybridSearch
    HybridSearch --> VectorIndex
    HybridSearch --> FTS5
    MemoryManager --> Markdown
    MemoryManager --> SQLite
```

---

## 2. CONTEXT vs MEMORY

Understanding the distinction between context and memory is fundamental:

```mermaid
graph LR
    subgraph "CONTEXT - Ephemeral"
        direction TB
        SP[System Prompt]
        CH[Conversation History]
        TR[Tool Results]
        AT[Attachments]
    end

    subgraph "MEMORY - Persistent"
        direction TB
        MEMORY_MD[MEMORY.md<br/>Long-term Knowledge]
        DAILY[memory/YYYY-MM-DD.md<br/>Daily Logs]
        TRANSCRIPTS[Session Transcripts]
    end

    SP -->|"Loaded per request"| MODEL[Model Context Window]
    CH --> MODEL
    TR --> MODEL
    AT --> MODEL

    MEMORY_MD -->|"Searched when needed"| Tools2[memory_search]
    DAILY --> Tools2
    TRANSCRIPTS --> Tools2
    Tools2 -->|"Results added to"| MODEL
```

**CONTEXT Properties:**

- Ephemeral: exists only for this request
- Bounded: limited by model's context window (200K tokens for Claude)
- Expensive: every token counts toward API costs

**MEMORY Properties:**

- Persistent: survives restarts, days, months
- Unbounded: can grow indefinitely
- Cheap: no API cost to store
- Searchable: indexed for semantic retrieval

---

## 3. TWO-LAYER MEMORY SYSTEM

```mermaid
graph TB
    subgraph "Workspace home/clawd/"
        subgraph "Layer 2: Long-term Memory"
            MEMORY[MEMORY.md<br/>Curated Knowledge<br/>Preferences, Decisions]
        end

        subgraph "Layer 1: Daily Logs"
            TODAY[memory/2026-01-27.md<br/>Today's Notes]
            YESTERDAY[memory/2026-01-26.md<br/>Yesterday's Notes]
            OLDER[memory/2026-01-25.md<br/>Older Logs...]
        end

        subgraph "Bootstrap Files"
            AGENTS[AGENTS.md<br/>Agent Instructions]
            SOUL[SOUL.md<br/>Agent Personality]
            USER[USER.md<br/>User Info]
        end
    end

    AGENTS -->|"Loaded at session start"| Agent[Agent]
    SOUL --> Agent
    USER --> Agent
    TODAY -->|"Read today + yesterday"| Agent
    YESTERDAY --> Agent
    MEMORY -->|"Loaded in private sessions"| Agent
```

**Layer 1 - Daily Logs (memory/YYYY-MM-DD.md):**

- Append-only daily notes
- Agent writes throughout the day
- Timestamp-based organization

**Layer 2 - Long-term Memory (MEMORY.md):**

- Curated, persistent knowledge
- Significant decisions, preferences
- Important facts and lessons learned

---

## 4. MEMORY TOOLS FLOW

```mermaid
sequenceDiagram
    participant User
    participant Agent
    participant memory_search
    participant memory_get
    participant MemoryIndex
    participant Files

    User->>Agent: "What did we decide about the API?"
    Agent->>memory_search: search("API decision")
    memory_search->>MemoryIndex: Hybrid Search
    MemoryIndex-->>memory_search: Results with file paths + lines
    memory_search-->>Agent: Snippets, scores, paths

    Agent->>memory_get: get("memory/2026-01-20.md", from=45, lines=10)
    memory_get->>Files: Read specific lines
    Files-->>memory_get: Full content
    memory_get-->>Agent: Detailed text

    Agent-->>User: "We decided to use REST over GraphQL…"
```

**Memory_search Tool:**

- Purpose: Find relevant memories semantically
- Parameters: query, maxResults, minScore
- Returns: snippets with path, line range, score

**Memory_get Tool:**

- Purpose: Read specific content after finding it
- Parameters: path, from (line), lines (count)
- Returns: Full text content from file

---

## 5. MEMORY INDEXING PIPELINE

```mermaid
flowchart TB
    subgraph "File Change Detection"
        FileWatcher[File System Watcher<br/>Debounced 1.5s]
        MemoryFiles[MEMORY.md + memory/*.md]
    end

    subgraph "Chunking"
        Chunker[Markdown Chunker<br/>400 tokens target<br/>80 token overlap]
    end

    subgraph "Embedding Generation"
        EmbedProvider{Provider?}
        OpenAI[OpenAI API<br/>text-embedding-3-small]
        Gemini[Gemini API<br/>gemini-embedding-001]
        Local[Local Model<br/>node-llama-cpp]
        EmbedCache[(Embedding Cache<br/>SQLite)]
    end

    subgraph "Storage"
        VectorTable[(sqlite-vec<br/>chunks_vec)]
        FTSTable[(FTS5<br/>chunks_fts)]
        MetaStore[(Metadata Store<br/>SQLite)]
    end

    MemoryFiles -->|"Change detected"| FileWatcher
    FileWatcher -->|"Mark dirty"| Chunker
    Chunker -->|"Chunks"| EmbedProvider
    EmbedProvider -->|"remote"| OpenAI
    EmbedProvider -->|"gemini"| Gemini
    EmbedProvider -->|"local"| Local

    OpenAI -->|"Check cache first"| EmbedCache
    Gemini --> EmbedCache
    Local --> EmbedCache

    EmbedCache -->|"Store vectors"| VectorTable
    Chunker -->|"Store text"| FTSTable
    Chunker -->|"Store metadata"| MetaStore
```

---

## 6. HYBRID SEARCH ALGORITHM

```mermaid
flowchart LR
    subgraph "Input"
        Query[Search Query<br/>API database thing]
    end

    subgraph "Parallel Search"
        direction TB
        VectorSearch[Vector Search<br/>Semantic Similarity]
        BM25Search[BM25 Search<br/>Keyword Matching]
    end

    subgraph "Scoring"
        direction TB
        VScore[Vector Score<br/>0.0 - 1.0]
        TScore["Text Score<br/>1 / (1 + bm25Rank)"]
    end

    subgraph "Merge"
        Formula["Final Score =<br/>0.7 x vectorScore +<br/>0.3 x textScore"]
    end

    subgraph "Output"
        Results[Ranked Results<br/>Filtered by minScore 0.35]
    end

    Query --> VectorSearch
    Query --> BM25Search
    VectorSearch --> VScore
    BM25Search --> TScore
    VScore --> Formula
    TScore --> Formula
    Formula --> Results
```

**Why 70/30 Weighting?**

- Semantic similarity (70%): Primary signal for memory recall
- BM25 keywords (30%): Catches exact matches like IDs, code symbols, env vars

**Example Searches:**

- Semantic: "Mac Studio gateway host" ≈ "the machine running the gateway"
- Keyword: "a828e60" (commit ID) needs exact match

---

## 7. COMPACTION LIFECYCLE

```mermaid
flowchart TB
    subgraph "Normal Operation"
        Running[Session Running<br/>Messages Accumulating]
        TokenCount[Token Count<br/>e.g., 180,000 / 200,000]
    end

    subgraph "Pre-Compaction"
        Threshold{Near Limit?<br/>contextWindow - reserve - soft}
        MemFlush[Memory Flush<br/>Silent Turn]
        WriteMemory[Write to memory/*.md<br/>Store durable notes]
    end

    subgraph "Compaction"
        Compact[Compaction Process<br/>Summarize older messages]
        Summary[Compaction Summary<br/>Stored in JSONL]
    end

    subgraph "Post-Compaction"
        NewContext[New Context<br/>Summary + Recent Messages]
        Continue[Continue Session<br/>50,000 tokens]
    end

    Running --> TokenCount
    TokenCount --> Threshold
    Threshold -->|Yes| MemFlush
    MemFlush --> WriteMemory
    WriteMemory --> Compact
    Compact --> Summary
    Summary --> NewContext
    NewContext --> Continue
    Threshold -->|No| Running
```

**Compaction Flow:**

1. Context approaches limit (e.g., 180K/200K tokens)
2. Memory flush: silent turn to save durable memories
3. Compaction: older conversation summarized
4. Summary persisted to JSONL file
5. Session continues with compact context

**Manual Compaction:** `/compact [instructions]`

---

## 8. SESSION LIFECYCLE

```mermaid
stateDiagram-v2
    [*] --> SessionStart: User starts session

    SessionStart --> LoadContext: Load bootstrap files
    LoadContext --> ReadMemory: Read AGENTS.md, SOUL.md, USER.md
    ReadMemory --> ReadDailyLogs: Read today + yesterday logs
    ReadDailyLogs --> Active: Ready

    Active --> MemorySearch: User asks about past
    MemorySearch --> Active: Return relevant snippets

    Active --> WriteMemory: Agent decides to remember
    WriteMemory --> Active: Written to daily log

    Active --> ApproachingLimit: Token count high
    ApproachingLimit --> MemoryFlush: Silent pre-compaction
    MemoryFlush --> Compaction: Summarize history
    Compaction --> Active: Continue with summary

    Active --> NewSession: /new command
    NewSession --> SessionEndHook: Save context before reset
    SessionEndHook --> [*]: Session ends

    Active --> IdleTimeout: No activity
    IdleTimeout --> [*]: Session expires
```

**Session Reset Rules:**

- Configurable: calendar day boundary
- Manual: `/new` or `/reset` commands
- Timeout: after idle period

---

## 9. MULTI-AGENT MEMORY ISOLATION

```mermaid
graph TB
    subgraph "State Directory home/.clawdbot/memory/"
        MainDB[(main.sqlite<br/>Personal Agent)]
        WorkDB[(work.sqlite<br/>Work Agent)]
    end

    subgraph "Workspace: home/clawd/"
        MainWorkspace[Main Agent Files<br/>MEMORY.md, memory/]
    end

    subgraph "Workspace: home/work/"
        WorkWorkspace[Work Agent Files<br/>MEMORY.md, memory/]
    end

    subgraph "Agent Instances"
        MainAgent[Main Agent<br/>Personal Context]
        WorkAgent[Work Agent<br/>Work Context]
    end

    MainAgent --> MainWorkspace
    MainAgent --> MainDB
    WorkAgent --> WorkWorkspace
    WorkAgent --> WorkDB

    MainDB -.->|"No cross-agent search"| WorkDB
```

**Key Points:**

- Each agent has complete memory isolation
- Keyed by agentId + workspaceDir
- Markdown files (source) in each workspace
- SQLite indexes (derived) in state directory
- Agents cannot read each other's memories by default
- Workspace is a soft sandbox (can be escaped with file tools)

---

## 10. PRUNING vs COMPACTION

```mermaid
flowchart LR
    subgraph "Context Management"
        direction TB

        subgraph "Pruning - In Memory Only"
            ToolResults[Large Tool Results<br/>50K+ chars]
            Trim[Trim to head + tail<br/>Keep 1500 chars each]
            Placeholder["[Old tool result cleared]"]
        end

        subgraph "Compaction - Persistent"
            OldMessages[Older Messages]
            Summarize[LLM Summarization]
            JSONL[Stored in JSONL]
        end
    end

    ToolResults --> Trim
    Trim --> Placeholder

    OldMessages --> Summarize
    Summarize --> JSONL
```

**Pruning:**

- Trims large tool outputs
- In-memory only (per request)
- JSONL on disk unchanged
- Cache-TTL mode: prune after cache expires

**Compaction:**

- Summarizes conversation history
- Persists summary to disk
- Lossy process (info may be lost)
- Memory flush saves important bits first

---

## 11. COMPLETE DATA FLOW DIAGRAM

```mermaid
flowchart TB
    subgraph "User Input"
        UserQuery[User Query]
    end

    subgraph "Context Building"
        SystemPrompt[System Prompt]
        Bootstrap[Bootstrap Files<br/>AGENTS.md, SOUL.md]
        History[Conversation History]
        MemoryResults[Memory Search Results]
    end

    subgraph "Model Execution"
        Model[Claude / GPT Model]
        ToolCall[Tool Calls]
    end

    subgraph "Memory Operations"
        Search[memory_search]
        Get[memory_get]
        Write[Standard write/edit tools]
    end

    subgraph "Persistence"
        DailyLog[memory/YYYY-MM-DD.md]
        LongTerm[MEMORY.md]
        Index[(Vector + FTS Index)]
    end

    UserQuery --> SystemPrompt
    Bootstrap --> SystemPrompt
    History --> SystemPrompt
    MemoryResults --> SystemPrompt
    SystemPrompt --> Model

    Model --> ToolCall
    ToolCall --> Search
    ToolCall --> Get
    ToolCall --> Write

    Search --> Index
    Get --> DailyLog
    Get --> LongTerm
    Write --> DailyLog
    Write --> LongTerm

    DailyLog --> Index
    LongTerm --> Index
```

---

## 12. CONFIGURATION REFERENCE

### Key Configuration Paths

**Memory Search:**

```json
{
  "Agents": {
    "Defaults": {
      "memorySearch": {
        "Provider": "openai",
        "Model": "text-embedding-3-small",
        "Fallback": "openai",
        "Query": {
          "maxResults": 10,
          "minScore": 0.35,
          "Hybrid": {
            "Enabled": true,
            "vectorWeight": 0.7,
            "textWeight": 0.3
          }
        }
      }
    }
  }
}
```

**Compaction:**

```json
{
  "Agents": {
    "Defaults": {
      "Compaction": {
        "reserveTokensFloor": 20000,
        "memoryFlush": {
          "Enabled": true,
          "softThresholdTokens": 4000,
          "systemPrompt": "Session nearing compaction...",
          "Prompt": "Write lasting notes to memory/YYYY-MM-DD.md..."
        }
      }
    }
  }
}
```

**Context Pruning:**

```json
{
  "Agent": {
    "contextPruning": {
      "Mode": "cache-ttl",
      "Ttl": 600,
      "keepLastAssistants": 3,
      "softTrim": {
        "maxChars": 4000,
        "headChars": 1500,
        "tailChars": 1500
      }
    }
  }
}
```

---

## 13. KEY DESIGN PRINCIPLES

1. **TRANSPARENCY OVER BLACK BOXES**
   - Memory is plain Markdown
   - You can read, edit, version control
   - No opaque databases or proprietary formats

2. **SEARCH OVER INJECTION**
   - Agent searches for relevant memories
   - Not everything stuffed into context
   - Keeps context lean and focused

3. **PERSISTENCE OVER SESSION**
   - Important info survives in files
   - Compaction can't delete disk memories
   - Long-term knowledge preserved

4. **HYBRID OVER PURE**
   - Vector search catches semantic matches
   - BM25 catches exact keywords
   - Best of both worlds

---

## END OF DOCUMENT

For more information, visit:

- GitHub: https://github.com/clawdbot/clawdbot
- Documentation: https://docs.clawd.bot/
