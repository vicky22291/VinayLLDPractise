# Recall Pipeline (Multi-Stage Retrieval)

> **Navigation**: [Architecture Hub](./09-end-to-end-architecture.md) | [Prev: Vector Substrate](./06-vector-substrate-and-latent-bridging.md) | **Recall Pipeline** | [Next: Results](./08-results-and-benchmarks.md) | [All References](./10-all-references.md)

## Section 2.6 of the Paper

---

## Overview

Hydra DB employs a **Multi-Stage Pipeline** that combines hybrid semantic search with a versioned memory graph. The pipeline has five stages:

```mermaid
graph TD
    Q["User Query q"] --> S1["Stage 1:<br/>Adaptive Query Expansion"]
    S1 --> S2["Stage 2:<br/>Weighted Hybrid Search"]
    S2 --> S3["Stage 3:<br/>Graph-Augmented Retrieval<br/>(Entity-Based Search)"]
    S3 --> S4["Stage 4:<br/>Chunk-Level Graph Expansion"]
    S4 --> S5["Stage 5:<br/>Triple-Tier Reranking<br/>with Graph-Vector Fusion"]
    S5 --> OUT["Final Context Window C_final"]

    style Q fill:#bbdefb
    style S1 fill:#c8e6c9
    style S2 fill:#c8e6c9
    style S3 fill:#c8e6c9
    style S4 fill:#c8e6c9
    style S5 fill:#c8e6c9
    style OUT fill:#fff9c4
```

This pipeline retrieves from two data stores populated during ingestion:
- The [Git-Style Temporal Knowledge Graph](./03-temporal-knowledge-graph.md) (for graph-based retrieval in Stages 3-4)
- The [Multi-Field Vector Substrate](./06-vector-substrate-and-latent-bridging.md) (for hybrid search in Stage 2)

---

## Stage 1: Adaptive Query Expansion (Section 2.6.1)

Hydra DB treats the user query `q` as a **semantic seed**, not a fixed string. An LLM-based projection function `Φ(q)` generates `N` semantically diverse reformulations:

```
Q' = {q₁, q₂, ..., qₙ}
```

### Example

```mermaid
graph TD
    Q["'What did I do last week?'"]
    Q --> Q1["'Projects worked on<br/>in the last 7 days'"]
    Q --> Q2["'Commits pushed during<br/>the previous week'"]
    Q --> Q3["'Meetings or tasks<br/>completed last week'"]

    Q1 --> PAR["All executed in PARALLEL<br/>against retrieval backend"]
    Q2 --> PAR
    Q3 --> PAR

    style Q fill:#fff3e0
    style Q1 fill:#bbdefb
    style Q2 fill:#bbdefb
    style Q3 fill:#bbdefb
    style PAR fill:#c8e6c9
```

**Why**: Ensures high recall even when relevant memories differ significantly in surface phrasing from the original query.

---

## Stage 2: Weighted Hybrid Search (Section 2.6.2)

For a query `q` and candidate chunk `c`, the initial retrieval score is a **linear combination of three signal paths** from the [Vector Substrate](./06-vector-substrate-and-latent-bridging.md#how-the-three-vectors-work-together):

```
S_retrieval(q, c) = x · sim(q, v_content) + y · sim(q, v_inferred) + α · BM25(q, v_sparse)
                     \________________/     \___________________/     \__________________/
                      Primary Dense          Secondary Dense           Sparse Signal
```

```mermaid
graph LR
    subgraph "Three Signal Paths"
        PD["Primary Dense<br/>x · sim(q, v_content)<br/>Direct semantic similarity"]
        SD["Secondary Dense<br/>y · sim(q, v_inferred)<br/>Implicit/latent meaning"]
        SS["Sparse Signal<br/>α · BM25(q, v_sparse)<br/>Exact lexical tokens"]
    end

    PD --> SCORE["S_retrieval(q, c)"]
    SD --> SCORE
    SS --> SCORE

    style PD fill:#c8e6c9
    style SD fill:#e1bee7
    style SS fill:#fff9c4
    style SCORE fill:#ffccbc
```

| Signal | Weight | Captures |
|---|---|---|
| Primary Dense | `x` | Direct semantic similarity to literal content |
| Secondary Dense | `y` | Implicit meaning via [Latent Semantic Bridging](./06-vector-substrate-and-latent-bridging.md#latent-semantic-bridging-section-252) |
| Sparse (BM25) | `α` | Rare but critical tokens (project IDs, usernames) |

---

## Stage 3: Graph-Augmented Retrieval (Section 2.6.3)

**In parallel** with hybrid vector retrieval, Hydra DB runs a **graph-based retrieval pass** over the [Temporal Knowledge Graph](./03-temporal-knowledge-graph.md).

```mermaid
graph TD
    Q["Query q"] --> EE["Extract entities E<br/>from query"]
    EE --> EM["Exact entity name<br/>matching"]
    EM --> PT["Bounded, variable-length<br/>path traversal"]

    PT --> P["P_graph = Path(E_start →*1..n→ E_end)"]
    P --> CTX["Construct graph context:<br/>concat(node_name, relation_context,<br/>temporal_details)"]
    CTX --> RERANK["Cross-encoder reranking:<br/>S_graph(p) = S_semantic(q, context_graph(p))"]

    style Q fill:#bbdefb
    style PT fill:#c8e6c9
    style RERANK fill:#fff9c4
```

The cross-encoder evaluates the combination of entity names, relational context, and temporal information against the query — capturing relational dependencies and temporal sequences not present in any single text chunk.

**Example**: "Project A is blocked by Issue B" — this relationship exists in the graph but may never appear in a single text chunk. This is the [structured relational index](./02-ontological-structure-vs-flat-index.md#211-structured-relational-index-over-flat-embedding-space) advantage.

---

## Stage 4: Chunk-Level Graph Expansion (Section 2.6.4)

A **second-stage expansion** that avoids post-hoc entity extraction from vector results.

```mermaid
graph TD
    subgraph "During Ingestion"
        CH["Chunk c"] -->|"Pre-linked to"| ENT["Referenced entities E(c)"]
    end

    subgraph "During Retrieval"
        VR["Vector-retrieved chunk c"] --> ACC["Access pre-indexed<br/>entity references E(c)"]
        ACC --> EXP["Explore adjacent graph<br/>neighborhoods up to depth n"]
        EXP --> NC["N(c) = ∪ Path(e →*1..n→)"]
    end

    NC --> CTX2["context_expansion(p) =<br/>concat(node_name,<br/>relation_context,<br/>temporal_details)"]
    CTX2 --> RR2["Rerank independently:<br/>S_expansion(p) = S_semantic(q,<br/>context_expansion(p))"]

    style CH fill:#e8eaf6
    style VR fill:#bbdefb
    style EXP fill:#c8e6c9
    style RR2 fill:#fff9c4
```

**Why this matters**: A retrieved meeting note may expand to include related tasks, blockers, or decisions recorded elsewhere. This expansion happens **before** context assembly — no entity extraction needed at query time.

The entity pre-linking at ingestion time is enabled by the [Sliding Window Pipeline](./04-sliding-window-inference-pipeline.md) which resolves entities during enrichment.

---

## Stage 5: Triple-Tier Reranking with Graph-Vector Fusion (Section 2.6.5)

The final context window fuses **three independently reranked candidate streams**.

### Vector Stream Reranking

```
S_rerank_vs(c) = γ · S_semantic(c) + (1 - γ) · S_lexical(c)         -- semantic + lexical balance
S_final_vs(c)  = β · S_vs(c) + (1 - β) · S_rerank_vs(c)             -- vector confidence bias
```

- `γ` controls balance between deep semantic understanding and exact lexical matching
- `β` preserves global structure learned by the vector index

### Graph Stream Fusion

```mermaid
graph TD
    subgraph "Three Candidate Streams"
        CVS["C_vs: Vector-retrieved chunks<br/>(reranked)"]
        CGRAPH["C_graph: Entity-matched<br/>graph paths (reranked)"]
        CEXP["C_expansion: Chunk-level<br/>graph expansion paths (reranked)"]
    end

    CVS --> MERGE["Merge: C_vs ⊕ C_expansion<br/>(attach expansion context<br/>to each vector chunk)"]
    CEXP --> MERGE

    MERGE --> TOPK1["TopK₁(merged, k₁)"]
    CGRAPH --> TOPK2["TopK₂(C_graph, k₂)"]

    TOPK1 --> FINAL["C_final = TopK₁ ∪ TopK₂"]
    TOPK2 --> FINAL

    style CVS fill:#c8e6c9
    style CGRAPH fill:#bbdefb
    style CEXP fill:#e1bee7
    style FINAL fill:#fff9c4
```

### Final Context Formula

```
C_final = TopK₁(C_vs_final ⊕ C_expansion, k₁) ∪ TopK₂(C_graph, k₂)
```

Where:
- `⊕` = merge operation that attaches expansion context `N(c)` to each vector chunk `c`
- `k₁` = number of merged vector-expansion pairs
- `k₂` = number of independent entity-based graph paths
- `k_total` determined dynamically based on relevance score distributions and context window constraints

### What the Final Context Includes

```mermaid
graph TD
    CF["Final Context C_final"]
    CF --> I1["High-confidence semantically<br/>similar chunks from vector store"]
    CF --> I2["Relational paths directly<br/>connected to query entities<br/>from the graph"]
    CF --> I3["Expanded relational neighborhoods<br/>of vector-retrieved chunks"]

    style CF fill:#c8e6c9
    style I1 fill:#bbdefb
    style I2 fill:#e1bee7
    style I3 fill:#fff9c4
```

This retrieval architecture directly underpins Hydra DB's **[90.79% overall accuracy](./08-results-and-benchmarks.md)** on LongMemEval-s.

---

## Complete Pipeline Summary

```mermaid
graph TD
    Q["User Query"] --> AQE["1. Adaptive Query<br/>Expansion Φ(q)"]
    AQE --> QS["{q₁, q₂, ..., qₙ}"]

    QS --> WHS["2. Weighted Hybrid<br/>Search (parallel)"]
    QS --> GAR["3. Graph-Augmented<br/>Retrieval (parallel)"]

    WHS --> VEC_RESULTS["Vector candidates C_vs"]
    GAR --> GRAPH_RESULTS["Graph candidates C_graph"]

    VEC_RESULTS --> CLGE["4. Chunk-Level<br/>Graph Expansion"]
    CLGE --> EXP_RESULTS["Expansion candidates<br/>C_expansion"]

    VEC_RESULTS --> FUSION["5. Triple-Tier<br/>Reranking & Fusion"]
    GRAPH_RESULTS --> FUSION
    EXP_RESULTS --> FUSION

    FUSION --> CFINAL["C_final:<br/>Correct factual AND<br/>relational state"]

    style Q fill:#fff3e0
    style FUSION fill:#ffccbc
    style CFINAL fill:#c8e6c9
```

---

> **Navigation**: [Architecture Hub](./09-end-to-end-architecture.md) | [Prev: Vector Substrate](./06-vector-substrate-and-latent-bridging.md) | **Recall Pipeline** | [Next: Results](./08-results-and-benchmarks.md) | [All References](./10-all-references.md)
