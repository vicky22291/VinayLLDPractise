# Claude-Supermemory Architecture Document

## Overview

Claude-Supermemory is a plugin that enables persistent AI memory across coding sessions. It integrates with Claude Code and the Supermemory platform to retain knowledge about user projects, preferences, and workflows.

**Repository**: [supermemoryai/claude-supermemory](https://github.com/supermemoryai/claude-supermemory)

### Key Capabilities

- **Persistent Memory**: Remembers context across sessions and projects
- **Context Injection**: Automatically loads relevant memories at session start
- **Automatic Capture**: Stores conversation turns for future reference
- **Codebase Indexing**: Indexes project architecture, patterns, and conventions
- **Semantic Search**: Retrieves relevant memories using the `super-search` skill

---

## High-Level Architecture

### System Context Diagram

```mermaid
graph TB
    subgraph User Environment
        U[Developer] --> CC[Claude Code CLI]
        CC --> CSP[Claude-Supermemory Plugin]
    end

    subgraph Supermemory Platform
        CSP <--> API[Supermemory API]
        API --> ME[Memory Engine]
        ME --> VS[Vector Store]
        ME --> GDB[Graph Database]
        ME --> PG[(PostgreSQL)]
    end

    subgraph External Integrations
        API <--> GD[Google Drive]
        API <--> NT[Notion]
        API <--> OD[OneDrive]
    end

    subgraph AI Providers
        API --> OAI[OpenAI]
        API --> ANT[Anthropic]
        API --> GGL[Google AI]
    end
```

### Component Overview

| Component | Description |
|-----------|-------------|
| Claude Code CLI | Anthropic's official CLI tool for AI-assisted coding |
| Claude-Supermemory Plugin | JavaScript plugin that hooks into Claude Code lifecycle |
| Supermemory API | Unified HTTP API for memory operations |
| Memory Engine | Core processing pipeline for ingestion, embedding, and retrieval |
| Vector Store | Semantic similarity search using vector embeddings |
| Graph Database | Knowledge graph for relationship-based queries |
| PostgreSQL | Persistent storage via Drizzle ORM |

---

## Deep Dive: How Supermemory Works

### Core Philosophy

Supermemory is not a simple document storage system. It's designed to mirror how human memory works:
- **Forming connections** between related pieces of information
- **Evolving over time** as new information arrives
- **Generating insights** from accumulated knowledge
- **Smart forgetting** where less relevant information fades

When you upload a document, Supermemory doesn't just store it. It breaks it into hundreds of interconnected memories, each understanding its context and relationships to other knowledge.

### Why Dual Storage (Vector + Graph)?

**The Problem with Vector-Only Approaches:**
- Vector embeddings capture semantic similarity but relationships are **implicit**
- No understanding that "Supplier A ships to Germany" or "Product X requires temperature control"
- Cannot track how facts evolve over time

**The Solution - Knowledge Graph:**
- Relationships are **explicit** and queryable
- Enables sophisticated queries beyond simple similarity search
- Tracks memory evolution and contradictions

```mermaid
graph LR
    subgraph Vector Store
        VS1[Semantic Similarity]
        VS2[Fast Retrieval]
        VS3[Implicit Relations]
    end

    subgraph Knowledge Graph
        KG1[Explicit Relations]
        KG2[Fact Evolution]
        KG3[Multi-hop Reasoning]
    end

    VS1 & VS2 & VS3 --> HYBRID[Hybrid Query Engine]
    KG1 & KG2 & KG3 --> HYBRID
    HYBRID --> RESULT[Precise + Contextual Results]
```

---

## Plugin Architecture

### Claude Code Integration Flow

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant CC as Claude Code
    participant Plugin as Supermemory Plugin
    participant API as Supermemory API
    participant ME as Memory Engine

    Note over Dev, ME: Session Initialization
    Dev->>CC: Start session
    CC->>Plugin: Initialize plugin
    Plugin->>API: Fetch relevant memories
    API->>ME: Query vector store + graph
    ME-->>API: Return memories
    API-->>Plugin: Context payload
    Plugin->>CC: Inject context into Claude

    Note over Dev, ME: Active Session
    Dev->>CC: Execute commands
    CC->>Plugin: Capture conversation turn
    Plugin->>API: Store memory
    API->>ME: Process & embed
    ME-->>API: Confirmation

    Note over Dev, ME: Memory Retrieval (super-search)
    Dev->>CC: "What did I work on last week?"
    CC->>Plugin: Trigger super-search skill
    Plugin->>API: Semantic search query
    API->>ME: Vector + graph search
    ME-->>API: Ranked results
    API-->>Plugin: Memory results
    Plugin-->>CC: Formatted response
    CC-->>Dev: Display results
```

### Claude Code Hooks System

The plugin leverages Claude Code's hook system for memory capture:

| Hook Event | When It Fires | Plugin Action |
|------------|---------------|---------------|
| `Stop` | After Claude finishes responding | Capture conversation turn |
| `PreCompact` | Before context compaction | Archive full transcript |
| `PostToolUse` | After tool execution | Capture tool results |
| `SessionEnd` | When session terminates | Final sync to Supermemory |

```mermaid
graph TB
    subgraph Claude Code Hooks
        STOP[Stop Hook] --> |After each response| CAPTURE[Capture Turn]
        PRECOMPACT[PreCompact Hook] --> |Before summarization| ARCHIVE[Archive Transcript]
        POSTTOOL[PostToolUse Hook] --> |After tool runs| TOOLCAP[Capture Tool Result]
        SESSEND[SessionEnd Hook] --> |Session closes| SYNC[Final Sync]
    end

    subgraph Memory Processing
        CAPTURE --> CHUNK_TURN[Chunk if > 6000 chars]
        ARCHIVE --> FULL_EXPORT[Full Conversation Export]
        TOOLCAP --> FILTER[Filter by SKIP_TOOLS]
        SYNC --> PERSIST[Persist to Supermemory]
    end

    CHUNK_TURN --> PERSIST
    FULL_EXPORT --> PERSIST
    FILTER --> PERSIST
```

### Conversation Capture Detail

```
When a hook fires:
1. Extractor reads conversation transcript from last cursor position
2. IF new content > 6,000 characters:
   - Split into manageable chunks
   - Each chunk sent for embedding
3. Each chunk includes existing memories for context
4. Cursor file updated to track position
```

---

## Memory Engine Architecture

### Brain-Inspired Memory Layers

Like the human brain, Supermemory uses tiered memory with different access speeds:

```mermaid
graph TB
    subgraph Memory Tiers
        WM[Working Memory<br/>Cloudflare KV<br/>Hot, Recent Data<br/>< 100ms access]
        STM[Short-Term Memory<br/>Durable Objects<br/>Session Context<br/>< 300ms access]
        LTM[Long-Term Memory<br/>PostgreSQL + Vector Store<br/>Persistent Knowledge<br/>Full semantic search]
    end

    WM --> STM
    STM --> LTM

    subgraph Intelligent Decay
        LTM --> DEC{Relevance + Recency Check}
        DEC -->|High Access Frequency| STRENGTHEN[Strengthen Memory]
        DEC -->|Low Access + Old| DECAY[Apply Decay Factor]
        DEC -->|Contradicted| ARCHIVE[Archive as Historical]
    end
```

**Memory Characteristics:**
- **Recency Bias**: More recent information prioritized in retrieval
- **Access Frequency**: Frequently retrieved memories stay "sharp"
- **Smart Forgetting**: Like forgetting where you parked 3 weeks ago but remembering yesterday's meeting

### Data Ingestion Pipeline (Detailed)

```mermaid
flowchart TB
    subgraph Input Sources
        TXT[Plain Text]
        CHAT[Conversations]
        DOCS[Documents<br/>PDF, CSV, MD]
        CODE[Source Code]
        URL[Web Pages]
    end

    subgraph Preprocessing
        TXT & CHAT & DOCS & CODE & URL --> CLEAN[Data Cleaning<br/>Remove noise, normalize]
        CLEAN --> DETECT[Content Type Detection]
    end

    subgraph Chunking Strategy
        DETECT --> |Text/Docs| SEMANTIC[Semantic Chunking<br/>Boundary by meaning]
        DETECT --> |Code| AST[AST-Aware Chunking<br/>Preserve code structure]
        DETECT --> |Conversations| SESSION[Session Decomposition<br/>Turn-by-turn]
    end

    subgraph Memory Generation
        SEMANTIC & AST & SESSION --> ATOMIC[Atomic Memory Extraction<br/>Single facts per memory]
        ATOMIC --> CONTEXT[Contextual Enrichment<br/>Add surrounding context]
        CONTEXT --> EMBED[Vector Embedding<br/>Semantic representation]
    end

    subgraph Dual Indexing
        EMBED --> VS[(Vector Store<br/>Similarity Search)]
        EMBED --> GRAPH[Graph Construction<br/>Relationship Detection]
        GRAPH --> GDB[(Knowledge Graph<br/>Explicit Relations)]
    end

    VS & GDB --> PG[(PostgreSQL<br/>Persistent Storage)]
```

---

## Deep Dive: Conversation Indexing

### How Conversations Become Searchable Memories

```mermaid
flowchart TB
    subgraph Conversation Input
        CONV[Raw Conversation<br/>User + Assistant turns]
    end

    subgraph Session Decomposition
        CONV --> TURNS[Split into Turns<br/>Preserve temporal order]
        TURNS --> WINDOW[Sliding Window<br/>Maintain context overlap]
    end

    subgraph Memory Generation
        WINDOW --> EXTRACT[Extract Atomic Facts<br/>Single pieces of information]
        EXTRACT --> RESOLVE[Resolve Ambiguity<br/>He/She/It -> actual entities]
        RESOLVE --> TIMESTAMP[Add Temporal Metadata<br/>When did this happen?]
    end

    subgraph Embedding
        TIMESTAMP --> EMBED[Generate Vector Embedding<br/>Capture semantic meaning]
        EMBED --> ENRICH[Contextual Enrichment<br/>Add source chunk reference]
    end

    subgraph Storage
        ENRICH --> DUAL[Dual Index<br/>Vector + Graph]
        DUAL --> RELATE[Detect Relationships<br/>UPDATES/EXTENDS/DERIVES]
    end
```

### The Two-Layer Retrieval Strategy

**Problem**: Balancing precision vs. context in retrieval

**Solution**: Supermemory's unique approach:

```
1. SEARCH on atomic memories (high signal, low noise)
   - Memories = single facts, very precise
   - Example: "User prefers TypeScript over JavaScript"

2. RETRIEVE the original source chunk
   - Contains "finer details" and nuance
   - Example: Full conversation where preference was expressed

3. INJECT both into LLM prompt
   - Atomic memory for precision
   - Source chunk for context
```

```mermaid
sequenceDiagram
    participant Q as Query
    participant MEM as Memory Index
    participant CHUNK as Chunk Store
    participant LLM as LLM

    Q->>MEM: Semantic search on atomic memories
    MEM-->>Q: Top-K memory hits (precise)

    loop For each memory hit
        Q->>CHUNK: Fetch original source chunk
        CHUNK-->>Q: Full context chunk
    end

    Q->>LLM: Inject memories + source chunks
    Note over LLM: High precision (memories)<br/>+ Rich context (chunks)
```

### Temporal Metadata & Dual-Timestamping

A key differentiator: every memory has two timestamps:

| Timestamp | Purpose |
|-----------|---------|
| `created_at` | When the memory was stored |
| `event_time` | When the event actually occurred |

This enables:
- **Temporal reasoning**: "What was I working on last Tuesday?"
- **Knowledge update tracking**: "What changed about X over time?"
- **Multi-session reasoning**: Connect events across different sessions

---

## Knowledge Graph Relationships

### The Three Relationship Types

```mermaid
graph TB
    subgraph UPDATES Relationship
        OLD1[My favorite color is Blue] -->|UPDATES| NEW1[My favorite color is Green]
        NEW1 -.->|isLatest: true| CURRENT1[Current Value]
    end

    subgraph EXTENDS Relationship
        BASE2[Works at TechCorp] -->|EXTENDS| DETAIL2[Senior Engineer at TechCorp]
        DETAIL2 -->|EXTENDS| MORE2[Senior Engineer, AI Team at TechCorp]
    end

    subgraph DERIVES Relationship
        FACT3A[Morning workout routine]
        FACT3B[High protein diet]
        FACT3A & FACT3B -->|DERIVES| INSIGHT3[Correlation: Morning workouts<br/>increase protein intake]
    end
```

**1. UPDATES (State Mutation)**
- New information contradicts existing knowledge
- System tracks `isLatest` field
- Searches return current information by default
- Example: Address changes, preference updates

**2. EXTENDS (Refinement)**
- New information enriches existing knowledge
- No contradiction, just more detail
- Example: Adding job title to employment record

**3. DERIVES (Inference)**
- System infers connections from patterns
- Second-order logic from combining memories
- Surfaces insights user didn't explicitly state

---

## Chunking Strategies

### Semantic Chunking (for Text/Documents)

```
Algorithm:
1. Embed each sentence individually
2. Compute cosine distance between consecutive sentences
3. Start new chunk when distance > threshold (e.g., 95th percentile)
4. Result: Chunks aligned with semantic boundaries
```

**Why not fixed-size chunking?**
- Fixed chunks split meaningful units arbitrarily
- Semantic boundaries preserve coherent ideas
- Better retrieval precision

### AST-Aware Code Chunking (for Source Code)

Supermemory uses tree-sitter for structure-preserving code chunking:

```mermaid
flowchart TB
    subgraph Traditional Chunking
        CODE1[Source File] --> FIXED[Fixed-size Split<br/>Every 500 chars]
        FIXED --> BROKEN[Broken Functions<br/>Split Classes<br/>Lost Context]
    end

    subgraph AST-Aware Chunking
        CODE2[Source File] --> PARSE[Tree-sitter Parse<br/>Build AST]
        PARSE --> TRAVERSE[Traverse AST Nodes<br/>Functions, Classes, Methods]
        TRAVERSE --> MERGE[Merge Siblings<br/>Respect size limits]
        MERGE --> ENRICH2[Add Context<br/>Scope chain, imports, signatures]
    end

    BROKEN --> BAD[Poor Retrieval]
    ENRICH2 --> GOOD[Semantically Rich Chunks]
```

**What each code chunk contains:**
- The code itself (function, class, method)
- **Scope chain**: What class/module it belongs to
- **Imports**: Dependencies used
- **Siblings**: Related functions
- **Signatures**: Types and parameters

**Why this matters for embeddings:**
Embedding models are trained on natural language. When you embed `async getUser(id: string)`, the model doesn't know it's inside a `UserService` class or uses a `Database`. By prepending context, the embedding captures semantic relationships that pure code misses.

---

## Retrieval Architecture

### Hybrid Search (Vector + Graph)

```mermaid
flowchart LR
    Q[User Query] --> EMBED_Q[Embed Query]

    EMBED_Q --> VS_SEARCH[Vector Search<br/>Semantic Similarity]
    EMBED_Q --> KW_SEARCH[Keyword Search<br/>BM25 Lexical Match]

    subgraph Graph Traversal
        VS_SEARCH --> GRAPH_EXP[Expand via Relations<br/>Follow EXTENDS/DERIVES]
        KW_SEARCH --> GRAPH_EXP
    end

    GRAPH_EXP --> RANK[Hybrid Ranking<br/>Combine scores]
    RANK --> RERANK[Optional Reranking<br/>LLM-based]
    RERANK --> TOP_K[Top-K Results]
```

**Hybrid Search Benefits:**
- **Semantic**: Catches conceptually similar content
- **Lexical (BM25)**: Catches exact term matches
- **Graph Expansion**: Finds related memories not directly matching

### Context Injection Process

```mermaid
sequenceDiagram
    participant SESSION as New Session
    participant PLUGIN as Plugin
    participant API as Supermemory API
    participant CLAUDE as Claude

    SESSION->>PLUGIN: Session starts
    PLUGIN->>API: GET /memories/search<br/>containerTag: current_project

    Note over API: 1. Search recent memories<br/>2. Get user profile<br/>3. Rank by relevance + recency

    API-->>PLUGIN: Structured payload:<br/>- User profile (prefs)<br/>- Recent context<br/>- Project memories

    PLUGIN->>CLAUDE: Inject as system context:<br/>"User prefers TypeScript..."<br/>"Recently worked on auth flow..."

    Note over CLAUDE: Now has personalized context<br/>without user re-explaining
```

---

## Performance & Benchmarks

### LongMemEval Results

Supermemory achieves state-of-the-art on LongMemEval_s benchmark:

| Category | Supermemory Score | Notes |
|----------|-------------------|-------|
| Multi-Session | 71.43% | Connecting events across sessions |
| Temporal Reasoning | 76.69% | "What happened last week?" queries |
| Knowledge Update | High | Tracking fact changes over time |
| Information Extraction | High | Pulling specific facts |
| Abstention | High | Knowing when NOT to answer |

**Why Supermemory excels:**
- Dual-layer timestamping
- Atomic memory generation (high signal)
- Knowledge graph relationships
- Source chunk injection for context

### Performance Targets

| Metric | Target |
|--------|--------|
| Memory Recall | < 300ms |
| Scale | 50M tokens per user |
| Daily Throughput | 5B+ tokens globally |

---

## Data Model

### Memory Entity

```mermaid
erDiagram
    MEMORY {
        uuid id PK
        string content
        vector embedding
        timestamp created_at
        timestamp event_time
        timestamp updated_at
        string source_url
        json metadata
        float relevance_score
        int access_count
        boolean is_latest
        string source_chunk_id FK
    }

    SOURCE_CHUNK {
        uuid id PK
        text full_content
        string content_type
        json ast_context
    }

    CONTAINER_TAG {
        uuid id PK
        string name
        string project_id
    }

    USER {
        uuid id PK
        string api_key
        json preferences
        json profile
    }

    RELATIONSHIP {
        uuid id PK
        uuid source_memory_id FK
        uuid target_memory_id FK
        enum type
        timestamp created_at
    }

    USER ||--o{ MEMORY : owns
    MEMORY }o--|| CONTAINER_TAG : belongs_to
    MEMORY ||--o{ RELATIONSHIP : has_source
    MEMORY ||--o{ RELATIONSHIP : has_target
    MEMORY }o--|| SOURCE_CHUNK : references
```

### Relationship Types Enum

```
UPDATES  - New info replaces old (contradictions)
EXTENDS  - New info enriches existing (additions)
DERIVES  - Inferred from combining memories
CHUNK_SEQUENCE - Sequential chunks from same source
```

---

## API Architecture

### Memory API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/v3/documents` | POST | Add new memory/document |
| `/memories/search` | GET | Semantic + keyword search |
| `/memories/:id` | GET | Retrieve specific memory |
| `/memories/:id` | PUT | Update memory |
| `/memories/:id` | DELETE | Remove memory |

### Key Parameters

**Adding Memories:**
```
content: string          - The actual content
containerTags: string[]  - Project/workspace identifiers
metadata: object         - Custom key-value pairs
customId: string         - For upsert functionality
```

**Searching:**
```
informationToGet: string  - Search terms
includeFullDocs: boolean  - Include source chunks (default: true)
limit: number             - Max results (default: 10)
containerTag: string      - Scope to project
```

### Memory Router (Drop-in Proxy)

For automatic memory without code changes:

```mermaid
sequenceDiagram
    participant APP as Your App
    participant ROUTER as Supermemory Router
    participant LLM as LLM Provider

    APP->>ROUTER: Standard LLM API call<br/>(with Router URL prefix)

    Note over ROUTER: 1. Intercept request<br/>2. Search relevant memories<br/>3. Chunk & manage tokens<br/>4. Inject context

    ROUTER->>LLM: Enriched prompt
    LLM-->>ROUTER: Response

    Note over ROUTER: 5. Extract memories<br/>6. Store for future

    ROUTER-->>APP: Response (unchanged)
```

---

## Technology Stack

### Infrastructure

```mermaid
graph TB
    subgraph Cloudflare Edge
        CF_W[Workers<br/>Serverless Compute]
        CF_KV[KV Store<br/>Hot Memory Layer]
        CF_DO[Durable Objects<br/>Custom Vector Engine]
        CF_R2[R2 Storage<br/>Large Objects]
        CF_P[Pages<br/>Web Hosting]
    end

    subgraph Backend
        HONO[Hono API Framework]
        PG[(PostgreSQL)]
        DRIZZLE[Drizzle ORM]
    end

    subgraph AI/ML
        EMBED[Embedding Models<br/>Multi-provider]
        LLM_PROC[LLM Processing<br/>Memory extraction]
    end

    CF_W --> HONO
    HONO --> DRIZZLE
    DRIZZLE --> PG
    CF_DO --> EMBED
```

### Why Cloudflare Durable Objects?

Traditional approach problems:
- Database call after every message = expensive
- No DB call = inconsistencies if user opens multiple tabs
- WebSocket state management complexity

Durable Objects solution:
- Stateful serverless at the edge
- Built-in WebSocket support
- Consistent state across connections
- Scales automatically

---

## Configuration

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `SUPERMEMORY_CC_API_KEY` | Yes | API key from console.supermemory.ai |
| `SUPERMEMORY_SKIP_TOOLS` | No | Comma-separated tools to exclude from capture |
| `SUPERMEMORY_DEBUG` | No | Enable diagnostic logging |

### Settings File

**Location**: `~/.supermemory-claude/settings.json`

```
Settings include:
- Tool filtering preferences
- Capture preferences (what to store)
- Profile item limits
- Project-specific configurations
```

---

## Plugin Commands

| Command | Description |
|---------|-------------|
| `/claude-supermemory:index` | Index current project structure, architecture, and key files |
| `/claude-supermemory:logout` | Terminate session and remove stored credentials |

---

## Installation

1. Add from marketplace:
   ```
   /plugin marketplace add supermemoryai/claude-supermemory
   ```

2. Install plugin:
   ```
   /plugin install claude-supermemory
   ```

3. Configure API key:
   ```bash
   export SUPERMEMORY_CC_API_KEY="sm_..."
   ```

4. Obtain API key from [console.supermemory.ai](https://console.supermemory.ai)

**Note**: Requires Supermemory Pro subscription or higher.

---

## Sources

- [GitHub: supermemoryai/claude-supermemory](https://github.com/supermemoryai/claude-supermemory)
- [GitHub: supermemoryai/supermemory](https://github.com/supermemoryai/supermemory)
- [GitHub: supermemoryai/code-chunk](https://github.com/supermemoryai/code-chunk)
- [Supermemory Documentation](https://supermemory.ai/docs)
- [Supermemory: How It Works](https://supermemory.ai/docs/how-it-works)
- [Supermemory Research](https://supermemory.ai/research)
- [LongMemEval Benchmark](https://github.com/xiaowu0162/LongMemEval)
- [Claude Code Hooks Reference](https://code.claude.com/docs/en/hooks)
- [cAST: AST-based Code Chunking (CMU)](https://arxiv.org/abs/2506.15655)