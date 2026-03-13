# Sliding Window Inference Pipeline

> **Navigation**: [Architecture Hub](./09-end-to-end-architecture.md) | [Prev: Temporal Graph](./03-temporal-knowledge-graph.md) | **Sliding Window** | [Next: Bio-Mimetic Decay](./05-bio-mimetic-memory-decay.md) | [All References](./10-all-references.md)

## Section 2.3 of the Paper

---

## The Orphaned Pronoun Paradox (Section 2.3.1)

Standard document chunking (e.g., recursive character splitting) rendered **nearly 40% of chunks semantically invisible**. This is the [Semantic Fragmentation problem](./01-overview-and-motivation.md#why-standard-rag-fails) discussed by [\[4\] Anthropic](./10-all-references.md#4-introducing-contextual-retrieval).

```mermaid
graph TD
    subgraph "Conversation Segments"
        S1["Segment s_{i-5}:<br/>'I'm a Marine Biologist'"]
        S2["..."]
        S3["Segment s_i:<br/>'I hate that framework'"]
        S4["Segment s_{i+2}:<br/>'React has too many bugs'"]
    end

    subgraph "Standard Chunking Result"
        C1["Chunk: 'I hate that framework'"]
        C1 -->|"❌ 'that framework' = ???"| FAIL["Vector search can never<br/>map 'that framework' → 'React'"]
    end

    subgraph "Hydra DB Enriched Chunk"
        E1["'The user (Marine Biologist)<br/>hates React framework'"]
        E1 -->|"✅ Self-contained"| SUCCESS["Searchable, meaningful,<br/>entity-resolved"]
    end

    S3 --> C1
    S3 --> E1

    style S1 fill:#e8eaf6
    style S3 fill:#fff3e0
    style S4 fill:#e8eaf6
    style C1 fill:#ffcdd2
    style FAIL fill:#ef9a9a
    style E1 fill:#c8e6c9
    style SUCCESS fill:#a5d6a7
```

**Initial attempt**: Larger overlap windows → just increased downstream token costs without solving the core problem.

---

## The Solution (Section 2.3.2)

### Step 1: Partition into Base Segments

Let `D` be a conversational session (sequence of tokens). Partition into base segments:

```
S = {s₁, s₂, ..., sₙ}
```

### Step 2: Construct Context Windows

For each segment `sᵢ`, construct a context window `Wᵢ` with a **lookback horizon** `h_prev` and a **lookahead horizon** `h_next`:

```
Wᵢ = [s_{i-h_prev}, ..., sᵢ, ..., s_{i+h_next}]
```

### Step 3: Enrichment Transformation

Apply transformation function `f_θ` (a lightweight LLM) that maps raw segment `sᵢ` and its context window `Wᵢ` to an enriched chunk `c'ᵢ`:

```
c'ᵢ = f_θ(sᵢ | Wᵢ) = {T_res, P_map, sᵢ}
```

```mermaid
graph TD
    subgraph "Input"
        SI5["s_{i-5}<br/>User: Marine Biologist"]
        DOTS1["..."]
        SI["s_i<br/>'I moved to the office.'"]
        DOTS2["..."]
    end

    subgraph "Context Window W_i"
        WI["lookback h_prev + lookahead h_next"]
    end

    SI5 --> WI
    SI --> WI

    WI --> FT["f_θ: Sliding-window enrichment<br/>Entity resolution (T_res)<br/>+ Preference map (P_map)"]

    FT --> OUT["c'_i (self-contained chunk)<br/>'The user (Marine Biologist)<br/>moved to the office.'"]

    style SI5 fill:#e8eaf6
    style SI fill:#fff3e0
    style WI fill:#e1bee7
    style FT fill:#bbdefb
    style OUT fill:#c8e6c9
```

---

## Two Key Operations

### T_res: Entity Resolution

Replaces implicit references with explicit, uniquely identifiable entities inferred from the context window.

| Before (Raw) | After (Resolved) |
|---|---|
| "he" | "John (the user's manager)" |
| "that project" | "Project Atlas" |
| "I moved to the office" | "The user (Marine Biologist) moved to the office" |
| "that framework" | "React" |

### P_map: Preference Mapping

Extracts persistent user constraints and conclusions from sequences that may influence future interactions. These preferences feed into the [Ontological Structure](./02-ontological-structure-vs-flat-index.md#213-preference-and-outcome-accumulation-across-sessions) as typed graph relationships.

```mermaid
graph LR
    subgraph "Conversation Sequence"
        U1["'I tried Tailwind'"]
        U2["'It was too verbose'"]
        U3["'I went back to plain CSS'"]
    end

    subgraph "Preference Map Extracted"
        P1["User AVOIDS Tailwind<br/>Reason: verbosity<br/>Current choice: plain CSS"]
    end

    U1 --> P1
    U2 --> P1
    U3 --> P1

    style P1 fill:#c8e6c9
```

---

## End-to-End Pipeline Flow

```mermaid
graph TD
    D["Raw Conversation D<br/>(sequence of tokens)"] --> PART["Partition into<br/>segments S = {s₁...sₙ}"]

    PART --> W1["W₁: context window<br/>for s₁"]
    PART --> W2["W₂: context window<br/>for s₂"]
    PART --> WN["Wₙ: context window<br/>for sₙ"]

    W1 --> F1["f_θ enrichment"]
    W2 --> F2["f_θ enrichment"]
    WN --> FN["f_θ enrichment"]

    F1 --> C1["c'₁: enriched chunk<br/>(entity-resolved,<br/>preference-mapped)"]
    F2 --> C2["c'₂: enriched chunk"]
    FN --> CN["c'ₙ: enriched chunk"]

    C1 --> KG["Knowledge Graph<br/>+ Vector Store"]
    C2 --> KG
    CN --> KG

    style D fill:#fff3e0
    style KG fill:#c8e6c9
    style C1 fill:#bbdefb
    style C2 fill:#bbdefb
    style CN fill:#bbdefb
```

Enriched chunks flow into two destinations:
- The [Git-Style Temporal Knowledge Graph](./03-temporal-knowledge-graph.md) as append-only edges
- The [High-Dimensional Vector Substrate](./06-vector-substrate-and-latent-bridging.md) as triple-vector embeddings (including `v_latent` from the enriched context)

---

## Why This Matters

| Standard Chunking | Sliding Window Inference |
|---|---|
| Chunks are "blind segments" | Every chunk is self-contained |
| Pronouns unresolved | Entity resolution via `T_res` |
| Preferences lost | Preference mapping via `P_map` |
| ~40% of chunks semantically useless | All chunks are semantically rich |
| Context lost at chunk boundaries | Context preserved via overlapping windows |
| Requires LLM to reconstruct context at query time | Context pre-computed at ingestion time |

This pipeline directly enables:
- **100% single-session accuracy** — see [benchmark results](./08-results-and-benchmarks.md#performance-gemini-30-pro-section-321)
- **96.67% preference extraction** — via `P_map` + [Latent Semantic Bridging](./06-vector-substrate-and-latent-bridging.md#latent-semantic-bridging-section-252)

---

> **Navigation**: [Architecture Hub](./09-end-to-end-architecture.md) | [Prev: Temporal Graph](./03-temporal-knowledge-graph.md) | **Sliding Window** | [Next: Bio-Mimetic Decay](./05-bio-mimetic-memory-decay.md) | [All References](./10-all-references.md)
