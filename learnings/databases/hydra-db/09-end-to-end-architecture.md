# End-to-End Architecture: How It All Fits Together

> **Start here.** This is the central hub linking all Hydra DB breakdown documents.

---

## The Big Picture

This document ties together all components described in the individual breakdowns. Use this as the **map** that connects everything.

```mermaid
graph TD
    subgraph "INGESTION (Write Path)"
        RAW["Raw Conversational<br/>Stream (sessions)"]
        RAW --> SWP["Sliding Window<br/>Inference Pipeline<br/>(Section 2.3)"]

        SWP --> TR["T_res: Entity Resolution<br/>'he' → 'John the manager'"]
        SWP --> PM["P_map: Preference Mapping<br/>Extract implicit preferences"]

        TR --> EC["Enriched, Self-Contained<br/>Chunks c'_i"]
        PM --> EC

        EC --> KG["Git-Style Temporal<br/>Knowledge Graph<br/>(Section 2.2)"]
        EC --> VS["Multi-Field Vector Store<br/>(Section 2.5)"]

        KG --> EDGES["Append-only edges<br/>e_k = (r_k, t_commit, t_valid, C_meta)"]
        VS --> VECS["Three vectors per chunk:<br/>v_content, v_sparse, v_latent"]
    end

    subgraph "RETRIEVAL (Read Path)"
        Q["User Query q"]
        Q --> AQE["1. Adaptive Query<br/>Expansion Φ(q)"]
        AQE --> WHS["2. Weighted Hybrid<br/>Search"]
        AQE --> GAR["3. Graph-Augmented<br/>Retrieval"]

        WHS --> CLGE["4. Chunk-Level<br/>Graph Expansion"]
        GAR --> CLGE

        CLGE --> FUSION["5. Triple-Tier Reranking<br/>+ Graph-Vector Fusion"]
        FUSION --> CFINAL["Final Context C_final"]
    end

    EDGES -.->|"feeds"| GAR
    VECS -.->|"feeds"| WHS

    subgraph "EXPERIMENTAL"
        DECAY["Bio-Mimetic Memory Decay<br/>(Section 2.4)"]
        DECAY --> TIERED["Tiered Storage:<br/>Active → Warm → Cold → Evicted"]
    end

    EDGES -.-> DECAY
    VECS -.-> DECAY

    style RAW fill:#fff3e0
    style SWP fill:#c8e6c9
    style KG fill:#bbdefb
    style VS fill:#e1bee7
    style Q fill:#fff3e0
    style CFINAL fill:#c8e6c9
    style DECAY fill:#f5f5f5
```

---

## Component Dependency Map

```mermaid
graph LR
    SWP["Sliding Window<br/>Inference Pipeline"]
    KG["Git-Style Temporal<br/>Knowledge Graph"]
    VS["Multi-Field<br/>Vector Substrate"]
    LSB["Latent Semantic<br/>Bridging"]
    RP["Multi-Stage<br/>Recall Pipeline"]
    BMD["Bio-Mimetic<br/>Decay (Experimental)"]

    SWP -->|"produces enriched chunks<br/>that feed into"| KG
    SWP -->|"produces enriched chunks<br/>that feed into"| VS
    SWP -->|"enriched context becomes<br/>v_latent via"| LSB

    LSB -->|"v_latent stored in"| VS

    KG -->|"graph paths used by"| RP
    VS -->|"vector candidates used by"| RP

    KG -.->|"retention scores<br/>managed by"| BMD
    VS -.->|"retention scores<br/>managed by"| BMD

    style SWP fill:#c8e6c9
    style KG fill:#bbdefb
    style VS fill:#e1bee7
    style LSB fill:#fff9c4
    style RP fill:#ffccbc
    style BMD fill:#f5f5f5
```

---

## File Index: How to Read the Breakdown

| # | File | Section | What You'll Learn |
|---|---|---|---|
| 1 | [Overview and Motivation](./01-overview-and-motivation.md) | 1 (Intro) | Why Hydra DB exists, what problems it solves, key results |
| 2 | [Ontological Structure vs. Flat Index](./02-ontological-structure-vs-flat-index.md) | 2.1 | Why knowledge graphs beat flat vector stores |
| 3 | [Temporal Knowledge Graph](./03-temporal-knowledge-graph.md) | 2.2 | Git-style append-only temporal graph design |
| 4 | [Sliding Window Inference Pipeline](./04-sliding-window-inference-pipeline.md) | 2.3 | How chunks become self-contained via entity resolution |
| 5 | [Bio-Mimetic Memory Decay](./05-bio-mimetic-memory-decay.md) | 2.4 | Experimental memory forgetting inspired by neuroscience |
| 6 | [Vector Substrate & Latent Bridging](./06-vector-substrate-and-latent-bridging.md) | 2.5 | Triple-vector schema and vocabulary mismatch fix |
| 7 | [Recall Pipeline](./07-recall-pipeline.md) | 2.6 | The 5-stage retrieval pipeline with all equations |
| 8 | [Results and Benchmarks](./08-results-and-benchmarks.md) | 3 | Benchmark results, cross-model generalization |
| 9 | **This file** (Architecture Hub) | All | How all components connect |
| 10 | [All References](./10-all-references.md) | Refs | Complete bibliography with descriptions |

---

## The Three Pillars

The entire system rests on **three architectural pillars** that address the three failures of traditional RAG:

```mermaid
graph TD
    subgraph "Pillar 1: Contextual Self-Containment"
        P1["Sliding Window Inference Pipeline"]
        P1 --> P1F["Fixes: Semantic Fragmentation"]
        P1 --> P1H["How: Entity resolution + preference mapping<br/>at ingestion time"]
    end

    subgraph "Pillar 2: Immutable Temporal State"
        P2["Git-Style Versioned Temporal Graph"]
        P2 --> P2F["Fixes: Temporal Stagnation"]
        P2 --> P2H["How: Append-only edges with temporal<br/>metadata, never overwrite"]
    end

    subgraph "Pillar 3: Multi-Signal Retrieval"
        P3["Multi-Stage Recall Pipeline"]
        P3 --> P3F["Fixes: Flat retrieval limitations"]
        P3 --> P3H["How: Graph traversal + hybrid vector search<br/>+ latent semantic bridging"]
    end

    style P1 fill:#c8e6c9
    style P2 fill:#bbdefb
    style P3 fill:#e1bee7
```

**Deep dives:**
- Pillar 1: [Sliding Window Inference Pipeline](./04-sliding-window-inference-pipeline.md)
- Pillar 2: [Temporal Knowledge Graph](./03-temporal-knowledge-graph.md) + [Ontological Structure](./02-ontological-structure-vs-flat-index.md)
- Pillar 3: [Recall Pipeline](./07-recall-pipeline.md) + [Vector Substrate](./06-vector-substrate-and-latent-bridging.md)

---

## Ingestion vs. Retrieval Complexity

| Aspect | Ingestion (Write) | Retrieval (Read) |
|---|---|---|
| **Primary cost** | [Sliding window](./04-sliding-window-inference-pipeline.md) LLM enrichment | [Multi-query expansion](./07-recall-pipeline.md#stage-1-adaptive-query-expansion-section-261) + reranking |
| **Graph ops** | Append [edges](./03-temporal-knowledge-graph.md#formal-edge-definition) (O(1) per edge) | [Path traversal](./07-recall-pipeline.md#stage-3-graph-augmented-retrieval-section-263) (bounded depth n) |
| **Vector ops** | Embed [3 vectors per chunk](./06-vector-substrate-and-latent-bridging.md#three-representations-per-memory-block) | [Hybrid search](./07-recall-pipeline.md#stage-2-weighted-hybrid-search-section-262) + cross-encoder rerank |
| **Key property** | Pre-computes context (expensive once) | Fast retrieval (cheap many times) |
| **Philosophy** | "Do the hard work at write time" | "Reap the benefits at read time" |

---

## What Makes Hydra DB Different — In One Diagram

```mermaid
graph TD
    subgraph "Traditional RAG"
        TRAW["Raw text"] --> TCHUNK["Naive chunking"]
        TCHUNK --> TVEC["Single vector<br/>per chunk"]
        TVEC --> TCOS["Cosine similarity<br/>retrieval"]
        TCOS --> TOUT["Retrieved chunks<br/>(may be fragmented,<br/>stale, disconnected)"]
    end

    subgraph "Hydra DB"
        HRAW["Raw text"] --> HSWP["Sliding Window<br/>enrichment"]
        HSWP --> HKG["Temporal Knowledge<br/>Graph (append-only)"]
        HSWP --> HVS["Triple-vector store<br/>(content + latent + sparse)"]
        HKG --> HRECALL["5-stage recall pipeline<br/>(graph + vector fusion)"]
        HVS --> HRECALL
        HRECALL --> HOUT["Retrieved context<br/>(self-contained,<br/>temporally correct,<br/>relationally enriched)"]
    end

    style TOUT fill:#ffcdd2
    style HOUT fill:#c8e6c9
```

See [benchmark results](./08-results-and-benchmarks.md) for how this architecture translates to **90.79% overall accuracy**.
