# Temporally-Aware Contextual Knowledge Graph

> **Navigation**: [Architecture Hub](./09-end-to-end-architecture.md) | [Prev: Ontological Structure](./02-ontological-structure-vs-flat-index.md) | **Temporal Graph** | [Next: Sliding Window Pipeline](./04-sliding-window-inference-pipeline.md) | [All References](./10-all-references.md)

## Section 2.2 of the Paper

---

## The State Confusion Problem

Standard RAG systems suffer when facts change over time:

```mermaid
sequenceDiagram
    participant User
    participant VectorStore as Vector Store (Traditional)
    participant HydraDB as Hydra DB

    User->>VectorStore: "I live in New York" (2022)
    User->>VectorStore: "I live in London" (2024)

    Note over VectorStore: Both chunks exist<br/>System retrieves BOTH<br/>or overwrites the old one

    rect rgb(255, 235, 238)
        VectorStore->>User: ❌ "NYC" (stale) OR<br/>❌ Loses timeline entirely
    end

    User->>HydraDB: "I live in New York" (2022)
    User->>HydraDB: "I live in London" (2024)

    Note over HydraDB: Appends new edge<br/>Both states preserved<br/>with temporal metadata

    rect rgb(200, 230, 201)
        HydraDB->>User: ✅ "London" (current)<br/>✅ Can also answer "Where in 2022?"
    end
```

---

## The Destructive Update Problem (Section 2.2.1)

Many memory systems use an **Iterative Resolution Loop**: find "similar" facts → ask LLM to update or delete.

Hydra DB **rejects** this for two reasons:

```mermaid
graph TD
    IRL["Iterative Resolution Loop"] --> P1["Instability via<br/>False Positives"]
    IRL --> P2["O(N) Latency Trap"]

    P1 --> P1D["'I love Python' ≈ 'I used to love Python'<br/>Semantically similar ≠ factually redundant<br/>LLM may delete historical context<br/>= False Positive Delete"]

    P2 --> P2D["N lookups for N inputs<br/>Retrieval + reasoning per chunk<br/>Prohibitive latency at scale"]

    style IRL fill:#ffcdd2
    style P1 fill:#ffebee
    style P2 fill:#ffebee
```

---

## The Solution: Git-Style Append-Only Log (Section 2.2.2)

Hydra DB treats memory as an **immutable ledger**, analogous to a Git repository's commit history.

### Temporal-State Graph Topology

```mermaid
graph LR
    subgraph "Subject: Alice"
        Alice["Alice"]
    end

    subgraph "current_housing_location"
        NYC["Value: 'NYC'<br/>temporal: 2022<br/>context: work at startup XYZ"]
        London["Value: 'London'<br/>temporal: 2024<br/>context: now at Meta,<br/>closer to parents"]
    end

    subgraph "dietary_preference"
        Omni["Value: 'Omnivore'<br/>temporal: 2021"]
        Vegan["Value: 'Vegan'<br/>temporal: 2025"]
    end

    Alice -->|"e₁ (older)"| NYC
    Alice -->|"e₂ (current)"| London
    Alice -->|"e₃ (older)"| Omni
    Alice -->|"e₄ (current)"| Vegan

    style Alice fill:#bbdefb
    style NYC fill:#e0e0e0
    style London fill:#c8e6c9
    style Omni fill:#e0e0e0
    style Vegan fill:#c8e6c9
```

### Formal Edge Definition

Each relationship `R` between entities `u` and `v` is not a single static edge, but a **time-ordered sequence of state changes** (versioned commits).

Let `E(u,v)` denote the set of all edges connecting entities `u` and `v`. Each edge:

```
e_k = (r_k, t_commit, t_valid, C_meta)
```

| Component | Description | Example |
|---|---|---|
| `r_k` | Semantic relation | `located_in`, `prefers` |
| `t_commit` | Ingestion timestamp | When the system recorded it |
| `t_valid` | Real-world temporal validity | "in 2022", "since 2024" |
| `C_meta` | Auxiliary metadata | Context, sentiment, reasoning |

### Append-Only Behavior

```mermaid
graph TD
    subgraph "Traditional System"
        T1["User.location = 'NYC'"] -->|"OVERWRITE"| T2["User.location = 'London'"]
        T1 -.->|"❌ LOST"| GONE["History destroyed"]
    end

    subgraph "Hydra DB (Git-Style)"
        H1["e₁: User →located_in→ NYC<br/>t_valid: 2022"]
        H2["e₂: User →located_in→ London<br/>t_valid: 2024"]
        H1 -->|"APPEND (no overwrite)"| H2
        H1 -.->|"✅ PRESERVED"| KEPT["Full history intact"]
    end

    style T1 fill:#ffcdd2
    style T2 fill:#ffcdd2
    style GONE fill:#ef9a9a
    style H1 fill:#c8e6c9
    style H2 fill:#c8e6c9
    style KEPT fill:#a5d6a7
```

### Differential Reasoning

The current relational state is formalized as:

```
ΔState(u, v) = SortByTime(E(u, v)) | t ≤ t_now
```

This strictly monotonic growth enables **Differential Reasoning** — the system can:
- Answer "Where do you live **now**?" → London (latest edge)
- Answer "Where did you live **in 2022**?" → NYC (historical edge)
- Answer "**Why** did you move?" → C_meta from the London edge (context: "switched to Meta, closer to parents")
- Answer "What places have you visited?" → Traverse all edges

---

## Key Guarantees

```mermaid
graph TD
    AOL["Append-Only Ledger"] --> G1["Zero Data Loss"]
    AOL --> G2["Historical Queries"]
    AOL --> G3["Decision Tree Preservation"]
    AOL --> G4["No False Positive Deletes"]
    AOL --> G5["Temporal Conflict Resolution"]

    G1 --> G1D["Every state change preserved"]
    G2 --> G2D["'What places did I visit last year?'"]
    G3 --> G3D["'Why did I make that career switch?'"]
    G4 --> G4D["No LLM-driven probabilistic purging"]
    G5 --> G5D["Latest edge = current truth<br/>Full history = complete timeline"]

    style AOL fill:#c8e6c9
```

---

## Analogy: Git vs. Hydra DB

| Git | Hydra DB |
|---|---|
| Repository | Knowledge Graph `G` |
| Commit | Edge `e_k` with timestamp |
| Commit message | `C_meta` (context, reasoning) |
| Branch history | `E(u,v)` — all edges between two entities |
| `HEAD` | Latest edge (current state) |
| `git log` | `ΔState(u,v)` — full temporal history |
| Never mutates past commits | Never mutates past edges |

---

## Related Components

- The [Ontological Structure](./02-ontological-structure-vs-flat-index.md) explains *why* a graph is used over a flat index
- The [Sliding Window Pipeline](./04-sliding-window-inference-pipeline.md) produces the enriched chunks that get stored as graph edges
- The [Recall Pipeline](./07-recall-pipeline.md) traverses this graph during retrieval (Stage 3: Graph-Augmented Retrieval)
- The [Bio-Mimetic Decay](./05-bio-mimetic-memory-decay.md) manages retention scores on these edges (experimental)

## References

- [\[8\] Lewis, P. et al. "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks"](./10-all-references.md#8-retrieval-augmented-generation-for-knowledge-intensive-nlp-tasks) (2021). arXiv:2005.11401

---

> **Navigation**: [Architecture Hub](./09-end-to-end-architecture.md) | [Prev: Ontological Structure](./02-ontological-structure-vs-flat-index.md) | **Temporal Graph** | [Next: Sliding Window Pipeline](./04-sliding-window-inference-pipeline.md) | [All References](./10-all-references.md)
