# Bio-Mimetic Memory Consolidation (Experimental)

> **Navigation**: [Architecture Hub](./09-end-to-end-architecture.md) | [Prev: Sliding Window](./04-sliding-window-inference-pipeline.md) | **Memory Decay** | [Next: Vector Substrate](./06-vector-substrate-and-latent-bridging.md) | [All References](./10-all-references.md)

## Section 2.4 of the Paper

> **Note**: This is an **experimental framework** — the authors state they are "currently experimenting" with this approach.

---

## The Problem: Infinite Memory Retention

The assumption that "more data is better" leads to:

```mermaid
graph TD
    INF["Infinite Memory<br/>Retention"] --> RL["Retrieval Latency<br/>(too many memories to search)"]
    INF --> SD["Semantic Drift<br/>(obsolete trivium drowns<br/>out critical facts)"]

    RL --> FAIL["Agent performance<br/>degrades over time"]
    SD --> FAIL

    style INF fill:#ffcdd2
    style RL fill:#ffebee
    style SD fill:#ffebee
    style FAIL fill:#ef9a9a
```

## The Solution: Memory as a Living Lifecycle

Inspired by the human brain's process of **synaptic pruning** and **memory consolidation**.

**Hypothesis**: Memory should not be a static repository, but a living lifecycle managed by a **Retention Score** (R).

This operates on top of the [Temporal Knowledge Graph](./03-temporal-knowledge-graph.md) and [Vector Substrate](./06-vector-substrate-and-latent-bridging.md), managing which memories remain in active storage.

---

## The Time-Frequency Decay Function (Section 2.4.1)

Inspired by the **Ebbinghaus Forgetting Curve**, augmented with **reinforcement learning** principles.

### The Retention Score Formula

```
R(m, t) = I_salience · e^(-λΔt) + σ · Σᵢ₌₁ⁿ (1 / (t - t_access_i))
           \_________/   \________/       \______________________________/
          Initial       Temporal           Reinforcement Boost
          Importance    Decay
```

```mermaid
graph TD
    R["Retention Score R(m, t)"]
    R --> IS["I_salience<br/>(Initial Importance)"]
    R --> TD["e^(-λΔt)<br/>(Temporal Decay)"]
    R --> RB["σ · Σ 1/(t - t_access_i)<br/>(Reinforcement Boost)"]

    IS --> IS1["High-impact facts get higher scores<br/>e.g., medical allergies > coffee orders"]
    TD --> TD1["Δt = time since memory created<br/>λ = rate of exponential decay"]
    RB --> RB1["Each successful retrieval<br/>strengthens the memory trace<br/>= long-term potentiation"]

    style R fill:#c8e6c9
    style IS fill:#bbdefb
    style TD fill:#e1bee7
    style RB fill:#fff9c4
```

### Parameters

| Symbol | Meaning |
|---|---|
| `I_salience` | Initial importance assigned during preprocessing + retrieval frequency |
| `Δt` | Elapsed time since memory was first created |
| `λ` | Rate of exponential decay |
| `σ` | Reinforcement scaling factor |
| `t_access_i` | Timestamp of the i-th successful retrieval of memory `m` |

### Key Behavior: Long-Term Potentiation

```mermaid
graph LR
    subgraph "Memory A (frequently accessed)"
        MA["Medical allergy info<br/>Created: 6 months ago<br/>Accessed: 50 times"]
        MA --> MA_R["R(m,t) = HIGH<br/>(reinforcement offsets decay)"]
    end

    subgraph "Memory B (rarely accessed)"
        MB["Coffee order from 2022<br/>Created: 2 years ago<br/>Accessed: 1 time"]
        MB --> MB_R["R(m,t) = LOW<br/>(decay dominates)"]
    end

    style MA fill:#c8e6c9
    style MA_R fill:#a5d6a7
    style MB fill:#ffcdd2
    style MB_R fill:#ef9a9a
```

Each successful retrieval **resets and elevates** the decay curve — frequently accessed memories are prevented from fading regardless of chronological age. This retrieval happens via the [Multi-Stage Recall Pipeline](./07-recall-pipeline.md).

---

## Tiered Storage Architecture (Section 2.4.2)

Memories that are candidates for forgetting move through a **multi-stage relevance storage process**.

```mermaid
graph TD
    subgraph "Tier 1: Active Memory"
        T1["High R(m,t)<br/>Immediately retrievable<br/>Full priority"]
    end

    subgraph "Tier 2: Warm Storage"
        T2["Medium R(m,t)<br/>Lower priority<br/>Still retrievable"]
    end

    subgraph "Tier 3: Cold Storage"
        T3["Low R(m,t)<br/>Rarely retrieved<br/>Last chance"]
    end

    subgraph "Evicted"
        T4["R(m,t) below threshold<br/>Fully forgotten"]
    end

    T1 -->|"R falls below<br/>threshold"| T2
    T2 -->|"R falls below<br/>threshold"| T3
    T3 -->|"R falls below<br/>threshold"| T4

    T2 -->|"Retrieved!<br/>R increases"| T1
    T3 -->|"Retrieved!<br/>R increases"| T2

    style T1 fill:#c8e6c9
    style T2 fill:#fff9c4
    style T3 fill:#ffecb3
    style T4 fill:#ffcdd2
```

A memory is moved down one level when its Retention Score `R` falls below a specified threshold.

---

## Analogy to Human Memory

| Human Brain | Hydra DB |
|---|---|
| Ebbinghaus Forgetting Curve | Exponential decay `e^(-λΔt)` |
| Synaptic pruning | Tiered eviction |
| Long-term potentiation | Reinforcement boost from retrieval |
| Working memory → Long-term memory | Tier promotion on access |
| Forgetting trivial details | Low-salience facts decay faster |
| Medical allergies = always remembered | High `I_salience` = slow decay |

---

> **Navigation**: [Architecture Hub](./09-end-to-end-architecture.md) | [Prev: Sliding Window](./04-sliding-window-inference-pipeline.md) | **Memory Decay** | [Next: Vector Substrate](./06-vector-substrate-and-latent-bridging.md) | [All References](./10-all-references.md)
