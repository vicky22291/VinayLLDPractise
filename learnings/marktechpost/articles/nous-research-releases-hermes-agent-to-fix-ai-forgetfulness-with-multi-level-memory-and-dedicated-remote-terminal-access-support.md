---
title: "Nous Research Releases ‘Hermes Agent’ to Fix AI Forgetfulness with Multi-Level Memory and Dedicated Remote Terminal Access Support"
date: "2026-02-26T00:01:12"
modified: "2026-02-26T00:01:20"
url: "https://www.marktechpost.com/2026/02/26/nous-research-releases-hermes-agent-to-fix-ai-forgetfulness-with-multi-level-memory-and-dedicated-remote-terminal-access-support/"
slug: "nous-research-releases-hermes-agent-to-fix-ai-forgetfulness-with-multi-level-memory-and-dedicated-remote-terminal-access-support"
---

![Nous Research Releases ‘Hermes Agent’ to Fix AI Forgetfulness with Multi-Level Memory and Dedicated Remote Terminal Access Support](../images/ec96b082b6f3d49a.png)

# Nous Research Releases ‘Hermes Agent’ to Fix AI Forgetfulness with Multi-Level Memory and Dedicated Remote Terminal Access Support

> In the current AI landscape, we’ve become accustomed to the ‘ephemeral agent’—a brilliant but forgetful assistant that restarts its cognitive clock with every new chat session. While LLMs have become master coders, they lack the persistent state required to function as true teammates. Nous Research team released Hermes Agent, an open-source autonomous system designed to […]

In the current AI landscape, we’ve become accustomed to the ‘ephemeral agent’—a brilliant but forgetful assistant that restarts its cognitive clock with every new chat session. While LLMs have become master coders, they lack the **persistent state** required to function as true teammates.

**Nous Research** team released **Hermes Agent**, an open-source autonomous system designed to solve the two biggest bottlenecks in agentic workflows: memory decay and environmental isolation.

Built on the high-steerability **Hermes-3** model family, Hermes Agent is billed as the assistant that ‘grows with you.’

### The Memory Hierarchy: Learning via Skill Documents

For an agent to ‘grow,’ it needs more than just a large context window. Hermes Agent utilizes a **multi-level memory system** that mimics procedural learning. While it handles short-term tasks through standard inference, its long-term utility is driven by **Skill Documents**.

When Hermes Agent completes a complex task—such as debugging a specific microservice or optimizing a data pipeline—it can synthesize that experience into a permanent record. These records are stored as searchable markdown files following the **agentskills.io** open standard.

- **Procedural Memory:** The next time you ask the agent to perform a similar task, it doesn’t start from scratch. It queries its own library of Skill Documents to ‘remember’ the successful steps it took previously.

- **Contextual Persistence:** Unlike standard RAG (Retrieval-Augmented Generation), which often pulls disjointed snippets, this system allows the agent to maintain a cohesive understanding of your specific codebase and preferences over weeks or months.

### Persistent Machine Access: Beyond the Sandbox

A major friction point for AI devs is the ‘execution gap.’ Most agents write code but cannot interact with the real world without heavy manual intervention. Hermes Agent closes this gap by providing **persistent dedicated machine access**.

**The agent is designed to live inside a functional environment, supporting five distinct backends:**

- **Local:** Direct interaction with the host machine.

- **Docker:** Isolated, reproducible containers for safe code execution.

- **SSH:** The ability to log into remote servers or cloud instances.

- **Singularity:** High-performance computing (HPC) container support.

- **Modal:** Serverless execution for scaling heavy workloads.

This persistence is critical for AI devs. You can initialize a long-running EDA (Exploratory Data Analysis) on a remote server via SSH, log-off, and return later. The agent maintains the terminal state, handles background processes, and tracks file system changes independently. It isn’t just simulating a conversation; it is managing a workspace.

### The Gateway: An Agent in Your Pocket

While most technical agents are confined to a CLI or a proprietary web dashboard, Nous Research has prioritized accessibility through the **Hermes Gateway**.

The system integrates directly with existing communication stacks, including **Telegram, Discord, Slack, and WhatsApp**. This allows for a continuous feedback loop: an engineer can start a task at their workstation and receive a ‘task completed’ notification via Telegram. Through the gateway, you can send follow-up instructions or even voice memos that the agent processes and executes within its persistent environment.

### Under the Hood: The ReAct Loop and Steerability

For the AI devs building on this, the architecture is a refined implementation of the **ReAct (Reasoning and Acting) loop**. **The agent follows a structured cycle:**

- **Observation:** Reading terminal output or file contents.

- **Reasoning:** Analyzing the current state against the goal.

- **Action:** Executing a command or calling a tool.

This is powered by **Hermes-3 (based on Llama 3.1)**, which was trained using a specialized reinforcement learning framework called **Atropos**. This training specifically targets tool-calling accuracy and long-range planning, ensuring the agent doesn’t get ‘lost’ during multi-step deployments.

### Key Takeaways

- **Persistent Machine Access:** Unlike stateless chatbots, it operates in real terminal environments (**Docker, SSH, Local, etc.**), allowing it to run long-term tasks and maintain file states across sessions.

- **Self-Evolving ‘Skill Documents’:** It uses a multi-level memory system to record successful workflows as searchable markdown files (via **agentskills.io**), meaning it literally gets smarter the more you use it.

- **Precision ‘Hermes-3’ Thinking:** Powered by the **Llama 3.1-based Hermes-3** model, it is fine-tuned with **Atropos RL** for high steerability and reliable tool-calling within complex reasoning loops.

- **Omnipresent Gateway:** You can interact with your agent via **Telegram, Discord, or Slack**, enabling you to manage heavy engineering tasks or receive status updates from your phone.

---

Check out the **[Technical details](https://nousresearch.com/hermes-agent/) **and** [GitHub Repo](https://github.com/NousResearch/hermes-agent). **Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[120k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**. Wait! are you on telegram? **[now you can join us on telegram as well.](https://t.me/machinelearningresearchnews)**
