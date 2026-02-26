# mem0 — Architecture & Claude Code Integration Guide

> **Repository:** [github.com/mem0ai/mem0](https://github.com/mem0ai/mem0)
> **Current Release:** v1.0.0 (API modernization milestone)
> **License:** Apache 2.0

---

## Table of Contents

1. [What Is mem0?](#1-what-is-mem0)
2. [High-Level Repository Layout](#2-high-level-repository-layout)
3. [Core Architecture Overview](#3-core-architecture-overview)
4. [Memory Layer Deep Dive](#4-memory-layer-deep-dive)
5. [Data Flow: `add()` — Storing a Memory](#5-data-flow-add--storing-a-memory)
6. [Data Flow: `search()` — Retrieving Memories](#6-data-flow-search--retrieving-memories)
7. [The Dual-Store Strategy: Vector + Graph](#7-the-dual-store-strategy-vector--graph)
8. [Configuration System](#8-configuration-system)
9. [LLM Integrations](#9-llm-integrations)
10. [Embedding Providers](#10-embedding-providers)
11. [Vector Store Backends](#11-vector-store-backends)
12. [Graph Store Backends](#12-graph-store-backends)
13. [Reranking Pipeline](#13-reranking-pipeline)
14. [Hosted Platform vs. Self-Hosted](#14-hosted-platform-vs-self-hosted)
15. [OpenMemory — Local MCP Server](#15-openmemory--local-mcp-server)
16. [Using mem0 with Claude Code](#16-using-mem0-with-claude-code)
17. [Key Design Patterns](#17-key-design-patterns)
18. [Performance Benchmarks (from Research Paper)](#18-performance-benchmarks-from-research-paper)

---

## 1. What Is mem0?

mem0 ("mem-zero") is an **intelligent, persistent memory layer** for AI agents and assistants. Instead of passing full conversation history into every prompt (expensive, slow, lossy), mem0 extracts, deduplicates, and stores discrete *memory facts* that can be semantically retrieved on-demand. The result is:

- **+26% accuracy** vs OpenAI Memory on the LOCOMO benchmark
- **91% faster** responses than full-context injection
- **90% fewer tokens** consumed vs full-context methods

It supports three memory scopes:

| Scope | Identifier | Use-case |
|---|---|---|
| **User memory** | `user_id` | Preferences, history, personal facts |
| **Session memory** | `run_id` | Ephemeral in-session context |
| **Agent memory** | `agent_id` | The agent's own behaviour and style |

---

## 2. High-Level Repository Layout

```
mem0ai/mem0/
├── mem0/                   ← Core Python library (pip install mem0ai)
│   ├── client/             ← Managed API client (MemoryClient)
│   ├── configs/            ← Pydantic config classes & prompts
│   ├── embeddings/         ← Embedding provider adapters (15)
│   ├── graphs/             ← Graph memory tools & configs
│   ├── llms/               ← LLM provider adapters (19)
│   ├── memory/             ← Core Memory class + graph/kuzu/memgraph
│   ├── proxy/              ← OpenAI-compatible proxy layer
│   ├── reranker/           ← Re-ranking backends (5)
│   ├── utils/              ← Factory classes, helpers
│   └── vector_stores/      ← Vector store adapters (25)
├── openmemory/             ← Self-hosted MCP server + UI
│   ├── api/                ← FastAPI backend + MCP server
│   └── ui/                 ← Next.js frontend
├── mem0-ts/                ← TypeScript/Node.js SDK
├── vercel-ai-sdk/          ← Vercel AI SDK integration
├── server/                 ← Standalone REST server
├── examples/               ← Demo applications
├── cookbooks/              ← Jupyter notebook guides
├── evaluation/             ← Benchmarking suite
└── tests/                  ← Test suite
```

---

## 3. Core Architecture Overview

```mermaid
graph TB
    subgraph "User / Application"
        APP[Your Application / Agent]
    end

    subgraph "mem0 Public API"
        OSS["Memory() — Self-Hosted OSS"]
        CLIENT["MemoryClient() — Managed Platform"]
    end

    subgraph "Core Memory Engine (memory/main.py)"
        PARSE[Message Parser]
        LLM_EXT[LLM: Fact Extraction]
        LLM_UPD[LLM: Memory Update Decision]
        EMBED[Embedder]
        DEDUP[Deduplication via Hash]
    end

    subgraph "Storage Layer"
        VS[(Vector Store)]
        GS[(Graph Store\nOptional)]
        SQL[(SQLite\nHistory DB)]
    end

    subgraph "Retrieval Pipeline"
        VSEARCH[Vector Search]
        GSEARCH[Graph Search\nOptional]
        RERANK[Reranker\nOptional]
        MERGE[Result Merger]
    end

    APP -->|add / search / get / delete| OSS
    APP -->|add / search / get / delete| CLIENT
    OSS --> PARSE
    PARSE --> LLM_EXT
    LLM_EXT --> LLM_UPD
    LLM_UPD --> EMBED
    EMBED --> DEDUP
    DEDUP --> VS
    DEDUP --> GS
    LLM_UPD --> SQL

    APP -->|search| VSEARCH
    VSEARCH --> VS
    GSEARCH --> GS
    VSEARCH --> RERANK
    GSEARCH --> RERANK
    RERANK --> MERGE
    MERGE --> APP
```

---

## 4. Memory Layer Deep Dive

The `Memory` class (`mem0/memory/main.py`) is the heart of the library. It exposes both a **synchronous** and an **asynchronous** (`AsyncMemory`) implementation with identical semantics.

### Class Hierarchy

```mermaid
classDiagram
    class MemoryBase {
        <<abstract>>
        +get(memory_id)
        +get_all()
        +update(memory_id, data)
        +delete(memory_id)
        +history(memory_id)
        +add(messages)
        +search(query)
    }

    class Memory {
        +config: MemoryConfig
        +llm: LLMBase
        +embedding_model: EmbedderBase
        +vector_store: VectorStoreBase
        +graph: MemoryGraph
        +db: SQLiteManager
        +add(messages, user_id, agent_id, run_id)
        +search(query, user_id, limit)
        +get(memory_id)
        +get_all(user_id, limit)
        +update(memory_id, data)
        +delete(memory_id)
        +delete_all(user_id)
        +history(memory_id)
        +reset()
    }

    class AsyncMemory {
        +add(...) async
        +search(...) async
        +get_all(...) async
    }

    class MemoryClient {
        +api_key: str
        +host: str
        +add(messages, user_id)
        +search(query, user_id)
        +get(memory_id)
        +get_all(user_id)
        +delete(memory_id)
    }

    MemoryBase <|-- Memory
    MemoryBase <|-- AsyncMemory
```

### MemoryConfig — The Central Configuration Object

```mermaid
classDiagram
    class MemoryConfig {
        +vector_store: VectorStoreConfig
        +llm: LlmConfig
        +embedder: EmbedderConfig
        +graph_store: GraphStoreConfig
        +reranker: RerankerConfig
        +history_db_path: str
        +version: str
        +custom_fact_extraction_prompt: str
        +custom_update_memory_prompt: str
    }

    class VectorStoreConfig {
        +provider: str
        +config: dict
    }

    class LlmConfig {
        +provider: str
        +config: dict
    }

    class EmbedderConfig {
        +provider: str
        +config: dict
    }

    class GraphStoreConfig {
        +provider: str
        +config: dict
        +custom_prompt: str
        +embedding_model_dims: int
        +similarity_threshold: float
    }

    class RerankerConfig {
        +provider: str
        +config: dict
    }

    MemoryConfig --> VectorStoreConfig
    MemoryConfig --> LlmConfig
    MemoryConfig --> EmbedderConfig
    MemoryConfig --> GraphStoreConfig
    MemoryConfig --> RerankerConfig
```

### MemoryItem — The Atom of Storage

```python
class MemoryItem(BaseModel):
    id: str          # UUID
    memory: str      # The extracted fact text
    hash: str        # SHA256 for deduplication
    metadata: dict   # user_id, agent_id, run_id, custom keys
    score: float     # Similarity score (on retrieval)
    created_at: str
    updated_at: str
```

---

## 5. Data Flow: `add()` — Storing a Memory

```mermaid
sequenceDiagram
    participant App
    participant Memory
    participant LLM
    participant Embedder
    participant VectorStore
    participant GraphStore
    participant SQLite

    App->>Memory: add(messages, user_id="alice")
    Memory->>Memory: parse_messages() → formatted string
    Memory->>LLM: Fact Extraction Prompt\n(FACT_RETRIEVAL_PROMPT)
    LLM-->>Memory: ["Alice likes pizza", "Alice is vegetarian"]

    loop For each extracted fact
        Memory->>Memory: Compute SHA256 hash
        Memory->>VectorStore: search existing memories\n(find near-duplicates)
        VectorStore-->>Memory: [existing memories + scores]
        Memory->>LLM: Memory Update Decision Prompt\n(ADD / UPDATE / DELETE / NOOP)
        LLM-->>Memory: {"event": "ADD", "text": "..."}\nor {"event": "UPDATE", "id": "..."}

        alt ADD
            Memory->>Embedder: embed(fact_text)
            Embedder-->>Memory: [float vector]
            Memory->>VectorStore: insert(vector, metadata)
            Memory->>SQLite: log history event
        else UPDATE
            Memory->>Embedder: embed(updated_text)
            Memory->>VectorStore: update(memory_id, new_vector)
            Memory->>SQLite: log history event
        else DELETE
            Memory->>VectorStore: delete(memory_id)
        else NOOP
            Memory->>Memory: skip
        end
    end

    opt Graph Store enabled
        Memory->>LLM: Entity Extraction + Relation Extraction
        LLM-->>Memory: {entities, relationships}
        Memory->>GraphStore: upsert nodes and edges (Cypher)
    end

    Memory-->>App: {"results": [{"id": "...", "memory": "...", "event": "ADD"}]}
```

---

## 6. Data Flow: `search()` — Retrieving Memories

```mermaid
sequenceDiagram
    participant App
    participant Memory
    participant Embedder
    participant VectorStore
    participant GraphStore
    participant Reranker

    App->>Memory: search(query="does alice like food?", user_id="alice")
    Memory->>Memory: validate filters (user_id/agent_id/run_id required)
    Memory->>Embedder: embed(query)
    Embedder-->>Memory: [query vector]

    par Vector Search
        Memory->>VectorStore: similarity_search(vector, filters, limit)
        VectorStore-->>Memory: [MemoryItem list with scores]
    and Graph Search (if enabled)
        Memory->>GraphStore: search(query, filters)
        GraphStore-->>Memory: [relations list]
    end

    opt Reranker configured
        Memory->>Reranker: rerank(query, candidates)
        Reranker-->>Memory: [re-scored candidates]
    end

    Memory->>Memory: merge vector + graph results
    Memory-->>App: {"results": [...], "relations": [...]}
```

---

## 7. The Dual-Store Strategy: Vector + Graph

```mermaid
graph LR
    subgraph "Input Conversation"
        MSG["User: I work at Google in NYC.\nAssistant: Cool, what team?"]
    end

    subgraph "Vector Store Path"
        FACTS["Extracted Facts:\n- Works at Google\n- Based in NYC"]
        VECS["Dense Vectors\n(1536-dim)"]
        VS[(Qdrant / Pinecone\n/ Chroma / etc.)]
    end

    subgraph "Graph Store Path (Optional)"
        ENTS["Entities:\nUser → Google, NYC"]
        RELS["Relations:\nUSER –[WORKS_AT]→ GOOGLE\nUSER –[LOCATED_IN]→ NYC"]
        GS[(Neo4j / Memgraph\n/ Kuzu)]
    end

    MSG --> FACTS
    FACTS --> VECS --> VS
    MSG --> ENTS --> RELS --> GS

    style GS fill:#f9f,stroke:#333
    style VS fill:#bbf,stroke:#333
```

The **vector store** handles semantic similarity search across facts. The **graph store** (optional) captures *structural relationships* between entities — enabling traversal queries like "what does Alice know about her colleagues?" — complementing the vector search with relational reasoning.

---

## 8. Configuration System

### Factory Pattern

```mermaid
graph TD
    CONFIG["MemoryConfig dict"]

    LF["LlmFactory.create(provider, config)"]
    EF["EmbedderFactory.create(provider, config)"]
    VF["VectorStoreFactory.create(provider, config)"]
    GF["GraphStoreFactory.create(provider, config)"]
    RF["RerankerFactory.create(provider, config)"]

    CONFIG --> LF
    CONFIG --> EF
    CONFIG --> VF
    CONFIG --> GF
    CONFIG --> RF

    LF --> |"'anthropic'"| ANTHROPIC["AnthropicLLM"]
    LF --> |"'openai'"| OPENAI["OpenAILLM"]
    LF --> |"'ollama'"| OLLAMA["OllamaLLM"]
    LF --> |"'litellm'"| LITELLM["LiteLLM (100+ providers)"]

    EF --> |"'openai'"| OE["OpenAIEmbedder"]
    EF --> |"'ollama'"| OLE["OllamaEmbedder"]
    EF --> |"'fastembed'"| FE["FastEmbed (local)"]

    VF --> |"'qdrant'"| QD["Qdrant"]
    VF --> |"'chroma'"| CH["Chroma"]
    VF --> |"'pinecone'"| PIN["Pinecone"]

    GF --> |"'default'"| NEO["Neo4j (via LangChain)"]
    GF --> |"'memgraph'"| MG["Memgraph"]
    GF --> |"'neptune'"| NEPT["AWS Neptune"]
```

All factories use **lazy dynamic imports** (`importlib.import_module`) so you only pay the dependency cost for the providers you actually configure.

---

## 9. LLM Integrations

The LLM is used for **two critical tasks**: extracting facts from conversations and deciding whether to ADD / UPDATE / DELETE / NOOP existing memories.

```mermaid
graph LR
    BASE["LLMBase\n(abstract)\n+generate_response()"]

    BASE --> ANTHROPIC["AnthropicLLM\nclaude-3-5-sonnet"]
    BASE --> OPENAI["OpenAILLM\ngpt-4.1-nano (default)"]
    BASE --> AZURE["AzureOpenAILLM"]
    BASE --> AZURE_S["AzureOpenAIStructuredLLM"]
    BASE --> OPENAI_S["OpenAIStructuredLLM"]
    BASE --> GEMINI["GeminiLLM"]
    BASE --> GROQ["GroqLLM"]
    BASE --> DEEPSEEK["DeepSeekLLM"]
    BASE --> OLLAMA["OllamaLLM\n(local)"]
    BASE --> LITELLM["LiteLLM\n100+ providers"]
    BASE --> VLLM["vLLMLLM\n(self-hosted)"]
    BASE --> LMSTUDIO["LMStudioLLM\n(local)"]
    BASE --> TOGETHER["TogetherLLM"]
    BASE --> LANGCHAIN["LangChainLLM"]
    BASE --> XAI["xAILLM (Grok)"]
    BASE --> BEDROCK["AWSBedrockLLM"]
    BASE --> SARVAM["SarvamLLM"]
```

The `AnthropicLLM` adapter handles the Anthropic-specific requirement of extracting `system` messages from the messages list and passing them via the dedicated `system=` parameter of `client.messages.create()`.

---

## 10. Embedding Providers

Embeddings convert memory text and search queries into vectors for similarity search.

| Provider | Class | Notes |
|---|---|---|
| `openai` | `OpenAIEmbedder` | Default — `text-embedding-3-small` |
| `azure_openai` | `AzureOpenAIEmbedder` | Azure Identity auth support |
| `ollama` | `OllamaEmbedder` | Local models |
| `fastembed` | `FastEmbedEmbedder` | Fully local, no API key needed |
| `huggingface` | `HuggingFaceEmbedder` | Custom HF models via base URL |
| `gemini` | `GeminiEmbedder` | Google models |
| `vertexai` | `VertexAIEmbedder` | GCP Vertex AI |
| `aws_bedrock` | `AWSBedrockEmbedder` | AWS Bedrock |
| `together` | `TogetherEmbedder` | Together AI |
| `lmstudio` | `LMStudioEmbedder` | Local LM Studio |
| `langchain` | `LangChainEmbedder` | Any LangChain embedder |

---

## 11. Vector Store Backends

25 backends are supported. The `VectorStoreBase` interface requires: `insert`, `search`, `delete`, `update`, `get`, `list`, `reset`.

```mermaid
mindmap
  root((Vector Stores))
    Cloud Managed
      Pinecone
      Qdrant Cloud
      Weaviate
      MongoDB Atlas
      Upstash Vector
      Azure AI Search
      AWS Neptune Analytics
      Databricks
      S3 Vectors
      Vertex AI Vector Search
    Open Source Self-Hosted
      Qdrant (local)
      Chroma
      Milvus
      Elasticsearch
      OpenSearch
      FAISS (in-memory)
      PGVector (Postgres)
      Redis
      Valkey
      Cassandra
    Database Extensions
      Supabase (PGVector)
      Azure MySQL
      Baidu VectorDB
      LangChain (adapter)
```

**Default:** Qdrant running locally, storing data in `~/.mem0/qdrant`.

---

## 12. Graph Store Backends

```mermaid
graph TD
    GF["GraphStoreFactory"]
    GF --> |"'default'"| NEO["Neo4j\n(via langchain-neo4j)"]
    GF --> |"'memgraph'"| MG["Memgraph\n(Cypher-compatible)"]
    GF --> |"'kuzu'"| KZ["Kuzu\n(embedded graph DB)"]
    GF --> |"'neptune'"| NEPT["AWS Neptune Analytics\n(Gremlin/openCypher)"]
    GF --> |"'neptunedb'"| NEPTDB["AWS Neptune DB"]

    NEO --> CYPHER["Cypher Queries\nvia LangChain Neo4j"]
    MG --> CYPHER
    KZ --> CYPHER
```

Graph memories store **entity-relationship triples** (e.g., `User -[WORKS_AT]-> Google`) extracted by the LLM. During search, both the vector store and the graph store are queried in parallel (via `asyncio.gather`) and results are merged.

---

## 13. Reranking Pipeline

After initial vector retrieval, an optional reranker rescores results for higher precision.

```mermaid
graph LR
    VS_RESULTS["Initial Vector\nSearch Results\n(top-k candidates)"]

    VS_RESULTS --> COHERE["CohereReranker\ncohere-rerank-v3"]
    VS_RESULTS --> ST["SentenceTransformerReranker\n(local cross-encoder)"]
    VS_RESULTS --> LLM_R["LLMReranker\n(any LLM provider)"]
    VS_RESULTS --> HF["HuggingFaceReranker\n(HF cross-encoders)"]
    VS_RESULTS --> ZE["ZeroEntropyReranker\n(entropy-based)"]

    COHERE --> FINAL["Re-ranked &\nFiltered Results"]
    ST --> FINAL
    LLM_R --> FINAL
    HF --> FINAL
    ZE --> FINAL
```

---

## 14. Hosted Platform vs. Self-Hosted

```mermaid
graph TB
    subgraph "Hosted (mem0.ai Platform)"
        MC["MemoryClient(api_key='...')"]
        API["mem0.ai REST API"]
        MANAGED["Managed Vector Store\nManaged LLM\nManaged Graph"]
        MC --> API --> MANAGED
    end

    subgraph "Self-Hosted OSS"
        MEM["Memory(config=MemoryConfig(...))"]
        LOCAL_LLM["Your LLM\n(Anthropic/OpenAI/Ollama/etc.)"]
        LOCAL_VS["Your Vector Store\n(Qdrant/Chroma/Pinecone/etc.)"]
        LOCAL_GS["Your Graph Store\n(Neo4j/Kuzu/etc.) [Optional]"]
        MEM --> LOCAL_LLM
        MEM --> LOCAL_VS
        MEM --> LOCAL_GS
    end

    subgraph "Shared Interface"
        IFACE["add() / search()\nget() / delete()\nget_all() / history()"]
    end

    MC -.->|"same method signatures"| IFACE
    MEM -.->|"same method signatures"| IFACE
```

Both implement the same method interface so you can switch between them without changing application code.

---

## 15. OpenMemory — Local MCP Server

OpenMemory is the self-hosted, Docker-based deployment that exposes mem0 as an **MCP (Model Context Protocol) server** — the protocol used by Claude Code and other AI tools.

```mermaid
graph TB
    subgraph "OpenMemory Stack"
        direction TB
        UI["Next.js UI\nlocalhost:3000"]
        API["FastAPI Backend\nlocalhost:8765"]
        MCP["MCP SSE Endpoint\n/mcp/{client}/{user_id}"]
        DB["SQLite / PostgreSQL"]
        VDB["Qdrant Vector Store"]
    end

    subgraph "MCP Clients"
        CC["Claude Code"]
        CLAUDE_APP["Claude Desktop"]
        CURSOR["Cursor"]
        OTHER["Any MCP Client"]
    end

    API --> DB
    API --> VDB
    MCP --> API

    CC -->|"SSE connection"| MCP
    CLAUDE_APP -->|"SSE connection"| MCP
    CURSOR -->|"SSE connection"| MCP
    OTHER -->|"SSE connection"| MCP

    UI --> API
```

**MCP Tools exposed by OpenMemory:**

| MCP Tool | Description |
|---|---|
| `add_memories` | Store new memories from the current session |
| `search_memories` | Semantic search over stored memories |
| `list_memories` | Get all memories for the user |
| `delete_memory` | Remove a specific memory |

**Quick-start command:**
```bash
curl -sL https://raw.githubusercontent.com/mem0ai/mem0/main/openmemory/run.sh | \
  OPENAI_API_KEY=your_key bash
```

**Register with a client (e.g., Claude Code):**
```bash
npx @openmemory/install local \
  http://localhost:8765/mcp/claude_code/sse/<user-id> \
  --client claude_code
```

---

## 16. Using mem0 with Claude Code

There are **two distinct integration patterns** for Claude Code.

### Pattern A: OpenMemory MCP Server (Zero-Code Integration)

This is the plug-and-play approach. Claude Code connects to OpenMemory via MCP and automatically gains persistent memory across all sessions — no code changes required.

```mermaid
sequenceDiagram
    participant User
    participant CC as Claude Code
    participant MCP as OpenMemory MCP Server
    participant mem0 as mem0 Engine
    participant VS as Vector Store

    User->>CC: "Remember that I prefer Python type hints"
    CC->>MCP: add_memories(text, user_id)
    MCP->>mem0: memory.add(...)
    mem0->>VS: store embedding

    User->>CC: "Start a new session: write a utility function"
    CC->>MCP: search_memories("coding preferences", user_id)
    MCP->>mem0: memory.search(...)
    mem0->>VS: similarity search
    VS-->>mem0: ["Prefers Python type hints", ...]
    MCP-->>CC: relevant memories
    CC-->>User: Generates code WITH type hints automatically
```

**Setup steps for Claude Code:**

1. Start OpenMemory locally:
```bash
export OPENAI_API_KEY=sk-...
curl -sL https://raw.githubusercontent.com/mem0ai/mem0/main/openmemory/run.sh | bash
```

2. Register the MCP server with Claude Code:
```bash
npx @openmemory/install local \
  "http://localhost:8765/mcp/claude_code/sse/my_user" \
  --client claude_code
```

3. Claude Code will now automatically call `add_memories` and `search_memories` tools when appropriate.

### Pattern B: Direct Library Integration in Code Generated by Claude Code

When Claude Code *writes code for you*, it can scaffold applications that directly use mem0. You can instruct Claude Code with a context like:

> "Build a customer support chatbot using Anthropic Claude and mem0 for persistent memory."

**Example scaffolded pattern (with Anthropic):**

```python
import anthropic
from mem0 import Memory

# Configure mem0 to use Claude for fact extraction too
config = {
    "llm": {
        "provider": "anthropic",
        "config": {
            "model": "claude-3-5-sonnet-20240620",
            "api_key": "ANTHROPIC_API_KEY"  # or via env var
        }
    },
    "embedder": {
        "provider": "openai",
        "config": {"model": "text-embedding-3-small"}
    },
    "vector_store": {
        "provider": "qdrant",
        "config": {"collection_name": "support_bot"}
    }
}

memory = Memory.from_config(config)
anthropic_client = anthropic.Anthropic()

def chat_with_memory(user_message: str, user_id: str) -> str:
    # 1. Retrieve relevant memories
    memories = memory.search(query=user_message, user_id=user_id, limit=5)
    memory_context = "\n".join(
        f"- {m['memory']}" for m in memories["results"]
    )

    # 2. Build prompt with memory context
    system = f"""You are a helpful support agent.
Known context about this user:
{memory_context}"""

    # 3. Call Claude
    response = anthropic_client.messages.create(
        model="claude-opus-4-5",
        max_tokens=1024,
        system=system,
        messages=[{"role": "user", "content": user_message}]
    )
    reply = response.content[0].text

    # 4. Store new memories from this exchange
    memory.add(
        [
            {"role": "user", "content": user_message},
            {"role": "assistant", "content": reply}
        ],
        user_id=user_id
    )
    return reply
```

### Why mem0 + Claude Code Is Particularly Powerful

```mermaid
graph LR
    subgraph "Without mem0"
        CC1["Claude Code Session 1\n(context limit)"]
        CC2["Claude Code Session 2\n(starts fresh)"]
        CC1 -. "❌ forgotten" .-> CC2
    end

    subgraph "With mem0"
        MC1["Claude Code Session 1"]
        MC2["Claude Code Session 2"]
        MEM_STORE[(mem0\nMemory Store)]
        MC1 -->|"add(): stores project prefs,\ncode style, decisions"| MEM_STORE
        MEM_STORE -->|"search(): retrieves\nrelevant context"| MC2
        MC1 -. "✅ remembered" .-> MC2
    end
```

Things mem0 can persist across Claude Code sessions:

- Your preferred code style (tabs vs spaces, type hint conventions, docstring format)
- Project architecture decisions and constraints
- Libraries and frameworks already in use
- Previous bugs encountered and their resolutions
- Team conventions and naming standards
- Personal workflow shortcuts and preferences

---

## 17. Key Design Patterns

### Provider Pattern (Factory + Lazy Imports)

Every subsystem (LLM, embedder, vector store, graph store, reranker) follows the same pattern:

```mermaid
graph TD
    CONFIG["provider: 'anthropic'\nconfig: {...}"]
    FACTORY["LlmFactory.create(provider, config)"]
    LAZY["importlib.import_module(\n'mem0.llms.anthropic'\n)"]
    INSTANCE["AnthropicLLM(config)"]

    CONFIG --> FACTORY --> LAZY --> INSTANCE
```

This means unused providers have zero import overhead — you don't pay for the `anthropic` package unless you configure `provider: 'anthropic'`.

### Dual Sync/Async API

```mermaid
graph LR
    SYNC["Memory()\n(synchronous)"]
    ASYNC["AsyncMemory()\n(async/await)"]

    SYNC -->|"Uses concurrent.futures\nThreadPoolExecutor"| VS[(Vector Store)]
    ASYNC -->|"Uses asyncio.gather\nfor parallel VS + Graph"| VS
    ASYNC -->|"Parallel search"| GS[(Graph Store)]
```

`AsyncMemory` uses `asyncio.gather` to query the vector store and graph store simultaneously during `search`, substantially reducing latency when both are enabled.

### Deduplication via Hashing

```mermaid
graph LR
    FACT["Extracted fact:\n'Alice likes pizza'"]
    HASH["SHA256 hash"]
    CHECK{Hash exists\nin vector store?}
    SKIP["NOOP — skip"]
    PROCESS["Continue to LLM\nupdate decision"]

    FACT --> HASH --> CHECK
    CHECK -->|"Yes"| SKIP
    CHECK -->|"No"| PROCESS
```

### Memory Scoping via Metadata Filters

All CRUD operations accept `user_id`, `agent_id`, and `run_id` as filter dimensions. These are stored as vector metadata and used for namespace isolation — so multiple users, agents, and sessions share one collection without cross-contamination.

---

## 18. Performance Benchmarks (from Research Paper)

From the arXiv paper *"Mem0: Building Production-Ready AI Agents with Scalable Long-Term Memory"* (arXiv:2504.19413):

| Metric | mem0 vs. OpenAI Memory | mem0 vs. Full-Context |
|---|---|---|
| Accuracy (LOCOMO benchmark) | +26% | — |
| Response Speed | — | 91% faster |
| Token Usage Reduction | — | 90% fewer tokens |

The key insight is that retrieving 3–5 targeted memory facts is orders of magnitude more token-efficient than passing 50+ turns of conversation history — while actually being *more* accurate because the LLM isn't overwhelmed with irrelevant context.

---

## Quick Reference: API Cheat Sheet

```python
from mem0 import Memory

m = Memory()  # uses OpenAI LLM + OpenAI embeddings + local Qdrant by default

# Store memories from a conversation
result = m.add(
    [{"role": "user", "content": "I love hiking and Python."}],
    user_id="alice"
)
# → {"results": [{"id": "...", "memory": "Loves hiking", "event": "ADD"}, ...]}

# Search for relevant memories
hits = m.search("what does alice like?", user_id="alice", limit=5)
# → {"results": [{"memory": "Loves hiking", "score": 0.92}, ...]}

# Retrieve all memories for a user
all_mem = m.get_all(user_id="alice")

# Update a specific memory
m.update(memory_id="abc123", data="Alice loves trail running and Python")

# Delete a memory
m.delete(memory_id="abc123")

# View the history of changes to a memory
m.history(memory_id="abc123")

# Wipe everything for a user
m.delete_all(user_id="alice")
```

**Using Claude as the LLM for mem0:**

```python
from mem0 import Memory

m = Memory.from_config({
    "llm": {
        "provider": "anthropic",
        "config": {"model": "claude-3-5-sonnet-20240620"}
        # Set ANTHROPIC_API_KEY env var
    }
})
```

**Fully local / private setup (no external API calls):**

```python
m = Memory.from_config({
    "llm": {"provider": "ollama", "config": {"model": "llama3.2"}},
    "embedder": {"provider": "fastembed", "config": {"model": "BAAI/bge-small-en-v1.5"}},
    "vector_store": {"provider": "chroma", "config": {"collection_name": "local_mem"}}
})
```

---

*Generated from a full read of the `mem0ai/mem0` repository at commit `a140829` (February 18, 2026).*
