---
title: "Is There a Community Edition of Palantir? Meet OpenPlanter: An Open Source Recursive AI Agent for Your Micro Surveillance Use Cases"
date: "2026-02-21T13:10:24"
modified: "2026-02-21T13:10:34"
url: "https://www.marktechpost.com/2026/02/21/is-there-a-community-edition-of-palantir-meet-openplanter-an-open-source-recursive-ai-agent-for-your-micro-surveillance-use-cases/"
slug: "is-there-a-community-edition-of-palantir-meet-openplanter-an-open-source-recursive-ai-agent-for-your-micro-surveillance-use-cases"
---

![Is There a Community Edition of Palantir? Meet OpenPlanter: An Open Source Recursive AI Agent for Your Micro Surveillance Use Cases](../images/033382911a21df1a.png)

# Is There a Community Edition of Palantir? Meet OpenPlanter: An Open Source Recursive AI Agent for Your Micro Surveillance Use Cases

> The balance of power in the digital age is shifting. While governments and large corporations have long used data to track individuals, a new open-source project called OpenPlanter is giving that power back to the public. Created by a developer ‘Shin Megami Boson‘, OpenPlanter is a recursive-language-model investigation agent. Its goal is simple: help you […]

The balance of power in the digital age is shifting. While governments and large corporations have long used data to track individuals, a new open-source project called **OpenPlanter** is giving that power back to the public. Created by a developer ‘**[Shin Megami Boson](https://x.com/shinboson)**‘, OpenPlanter is a recursive-language-model investigation agent. Its goal is simple: **help you keep tabs on your government, since they are almost certainly keeping tabs on you.**

### Solving the ‘Heterogeneous Data’ Problem

Investigative work is difficult because data is messy. Public records are often spread across **100** different formats. You might have a **CSV** of campaign finance records, a **JSON** file of government contracts, and a **PDF** of lobbying disclosures.

OpenPlanter ingests these **disparate structured and unstructured data sources** effortlessly. It uses Large Language Models (LLMs) to perform **entity resolution**. This is the process of identifying when different records refer to the same person or company. Once it connects these dots, the agent **probabilistically looks for anomalies**. It searches for patterns that a human might miss, such as a sudden spike in contract wins following a specific lobbying event.

### The Architecture: Recursive Sub-Agent Delegation

What makes OpenPlanter unique is its **recursive engine**. Most AI agents handle **1** request at a time. OpenPlanter, however, breaks large objectives into smaller pieces. If you give it a massive task, it uses a **sub-agent delegation** strategy.

The agent has a default **max-depth of 4**. This means the main agent can spawn a sub-agent, which can spawn another, and so on. These agents work in parallel to:

- **Resolve entities** across massive datasets.

- **Link datasets** that have no common ID numbers.

- **Construct evidence chains** that back up every single finding.

This recursive approach allows the system to handle investigations that are too large for a single ‘context window.’

### The 2026 AI Stack

OpenPlanter is built for the high-performance requirements of **2026**. It is written in **Python 3.10+** and integrates with the most advanced models available today. **The technical documentation lists several supported providers:**

- **OpenAI**: It uses **gpt-5.2** as the default.

- **Anthropic**: It supports **claude-opus-4-6**.

- **OpenRouter**: It defaults to **anthropic/claude-sonnet-4-5**.

- **Cerebras**: It uses **qwen-3-235b-a22b-instruct-2507** for high-speed tasks.

The system also uses **Exa** for web searches and **Voyage** for high-accuracy embeddings. This multi-model strategy ensures that the agent uses the best ‘brain’ for each specific sub-task.

### 19 Tools for Digital Forensics

The agent is equipped with **19** specialized tools. These tools allow it to interact with the real world rather than just ‘chatting.’ **These are organized into 4 core areas:**

- **File I/O and Workspace**: Tools like `read_file`, `write_file`, and `hashline_edit` allow the agent to manage its own database of findings.

- **Shell Execution**: The agent can use `run_shell` to execute actual code. It can write a Python script to analyze a dataset and then run that script to get results.

- **Web Retrieval**: With `web_search` and `fetch_url`, it can pull live data from government registries or news sites.

- **Planning and Logic**: The `think` tool lets the agent pause and strategize. It uses **acceptance-criteria** to verify that a sub-task was completed correctly before moving to the next step.

### Deployment and Interface

OpenPlanter is designed to be accessible but powerful. It features a **Terminal User Interface (TUI)** built with `rich` and `prompt_toolkit`. The interface includes a splash art screen of ASCII potted plants, but the work it does is serious.

You can get started quickly using **Docker**. By running `docker compose up`, the agent starts in a container. This is a critical security feature because it isolates the agent’s `run_shell` commands from the user’s host operating system.

The command-line interface allows for ‘headless’ tasks. You can run a single command like:

Copy CodeCopiedUse a different Browser
```
openplanter-agent --task "Flag all vendor overlaps in lobbying data" --workspace ./data
```

The agent will then work autonomously until it produces a final report.

### Key Takeaways

- **Autonomous Recursive Logic:** Unlike standard agents, OpenPlanter uses a **recursive sub-agent delegation** strategy (default max-depth of **4**). It breaks complex investigative objectives into smaller sub-tasks, parallelizing work across multiple agents to build detailed evidence chains.

- **Heterogeneous Data Correlation:** The agent is built to ingest and resolve **disparate structured and unstructured data**. It can simultaneously process **CSV** files, **JSON** records, and **unstructured text** (like PDFs) to identify entities across fragmented datasets.

- **Probabilistic Anomaly Detection:** By performing **entity resolution**, OpenPlanter automatically connects records—such as matching a corporate alias to a lobbying disclosure—and looks for **probabilistic anomalies** to surface hidden connections between government spending and private interests.

- **High-End 2026 Model Stack:** The system is provider-agnostic and utilizes the latest frontier models, including **OpenAI gpt-5.2**, **Anthropic claude-opus-4-6**, and **Cerebras qwen-3-235b-a22b-instruct-2507** for high-speed inference.

- **Integrated Toolset for Forensics:** OpenPlanter features **19** distinct tools, including **shell execution (`run_shell`)**, **web search (Exa)**, and **file patching (`hashline_edit`)**. This allows it to write and run its own analysis scripts while verifying results against real-world acceptance criteria.

---

Check out the **[Repo here.](https://github.com/ShinMegamiBoson/OpenPlanter?tab=readme-ov-file) **Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[100k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**. Wait! are you on telegram? **[now you can join us on telegram as well.](https://t.me/machinelearningresearchnews)**

**Disclaimer:** MarkTechPost does not endorse the OpenPlanter project and provides this technical report for informational purposes only.
