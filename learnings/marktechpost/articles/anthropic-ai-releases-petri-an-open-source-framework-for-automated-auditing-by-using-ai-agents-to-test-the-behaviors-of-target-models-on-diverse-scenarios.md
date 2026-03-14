---
title: "Anthropic AI Releases Petri: An Open-Source Framework for Automated Auditing by Using AI Agents to Test the Behaviors of Target Models on Diverse Scenarios"
date: "2025-10-08T10:03:57"
modified: "2025-10-08T10:04:04"
url: "https://www.marktechpost.com/2025/10/08/anthropic-ai-releases-petri-an-open-source-framework-for-automated-auditing-by-using-ai-agents-to-test-the-behaviors-of-target-models-on-diverse-scenarios/"
slug: "anthropic-ai-releases-petri-an-open-source-framework-for-automated-auditing-by-using-ai-agents-to-test-the-behaviors-of-target-models-on-diverse-scenarios"
---

![Anthropic AI Releases Petri: An Open-Source Framework for Automated Auditing by Using AI Agents to Test the Behaviors of Target Models on Diverse Scenarios](../images/090adde183a5690c.png)

# Anthropic AI Releases Petri: An Open-Source Framework for Automated Auditing by Using AI Agents to Test the Behaviors of Target Models on Diverse Scenarios

> How do you audit frontier LLMs for misaligned behavior in realistic multi-turn, tool-use settings—at scale and beyond coarse aggregate scores? Anthropic released Petri (Parallel Exploration Tool for Risky Interactions), an open-source framework that automates alignment audits by orchestrating an auditor agent to probe a target model across multi-turn, tool-augmented interactions and a judge model to […]

**How do you audit frontier LLMs for misaligned behavior in realistic multi-turn, tool-use settings—at scale and beyond coarse aggregate scores?** Anthropic released **[Petri (Parallel Exploration Tool for Risky Interactions)](https://github.com/safety-research/petri)**, an open-source framework that automates alignment audits by orchestrating an _auditor_ agent to probe a _target_ model across multi-turn, tool-augmented interactions and a _judge_ model to score transcripts on safety-relevant dimensions. In a pilot, Petri was applied to **14 frontier models** using **111 seed instructions**, eliciting misaligned behaviors including **autonomous deception, oversight subversion, whistleblowing, and cooperation with human misuse**.

![](https://www.marktechpost.com/wp-content/uploads/2025/10/Screenshot-2025-10-08-at-9.52.05-AM-1-1024x670.png)![](https://www.marktechpost.com/wp-content/uploads/2025/10/Screenshot-2025-10-08-at-9.52.05-AM-1-1024x670.png)*https://alignment.anthropic.com/2025/petri/*

### What Petri does (at a systems level)?

Petri programmatically: (1) synthesizes realistic environments and tools; (2) drives multi-turn audits with an auditor that can send user messages, set system prompts, create synthetic tools, simulate tool outputs, **roll back** to explore branches, optionally **prefill** target responses (API-permitting), and early-terminate; and (3) scores outcomes via an LLM judge across a **default 36-dimension rubric** with an accompanying transcript viewer.

The stack is built on the UK AI Safety Institute’s **Inspect** evaluation framework, enabling role binding of `auditor`, `target`, and `judge` in the CLI and support for major model APIs.

![](https://www.marktechpost.com/wp-content/uploads/2025/10/Screenshot-2025-10-08-at-9.51.37-AM-1-1024x945.png)![](https://www.marktechpost.com/wp-content/uploads/2025/10/Screenshot-2025-10-08-at-9.51.37-AM-1-1024x945.png)*https://alignment.anthropic.com/2025/petri/*

### Pilot results

Anthropic characterizes the release as a **broad-coverage pilot**, not a definitive benchmark. In the technical report, **Claude Sonnet 4.5 and GPT-5 “roughly tie”** for strongest safety profile across most dimensions, with both rarely cooperating with misuse; the research overview page summarizes Sonnet 4.5 as _slightly_ ahead on the aggregate “misaligned behavior” score.

A case study on **whistleblowing** shows models sometimes escalate to external reporting when granted autonomy and broad access—even in scenarios framed as harmless (e.g., _dumping clean water_)—suggesting sensitivity to narrative cues rather than calibrated harm assessment.

![](https://www.marktechpost.com/wp-content/uploads/2025/10/Screenshot-2025-10-08-at-9.52.47-AM-1-1024x744.png)![](https://www.marktechpost.com/wp-content/uploads/2025/10/Screenshot-2025-10-08-at-9.52.47-AM-1-1024x744.png)*https://alignment.anthropic.com/2025/petri/*

### Key Takeaways

- **Scope & behaviors surfaced:** Petri was run on **14 frontier models** with **111 seed instructions**, eliciting autonomous deception, oversight subversion, whistleblowing, and cooperation with human misuse.

- **System design:** An _auditor_ agent probes a _target_ across multi-turn, tool-augmented scenarios (send messages, set system prompts, create/simulate tools, rollback, prefill, early-terminate), while a _judge_ scores transcripts across a default rubric; Petri automates environment setup through to initial analysis.

- **Results framing:** On pilot runs, **Claude Sonnet 4.5 and GPT-5 roughly tie** for the strongest safety profile across most dimensions; scores are **relative signals**, not absolute guarantees.

- **Whistleblowing case study:** Models sometimes escalated to external reporting even when the “wrongdoing” was explicitly benign (e.g., dumping clean water), indicating sensitivity to narrative cues and scenario framing.

- **Stack & limits:** Built atop the UK AISI **Inspect** framework; Petri ships open-source (MIT) with CLI/docs/viewer. Known gaps include no code-execution tooling and potential judge variance—manual review and customized dimensions are recommended.

![](https://www.marktechpost.com/wp-content/uploads/2025/10/Screenshot-2025-10-08-at-9.53.08-AM-1-1024x620.png)![](https://www.marktechpost.com/wp-content/uploads/2025/10/Screenshot-2025-10-08-at-9.53.08-AM-1-1024x620.png)*https://alignment.anthropic.com/2025/petri/*

### Editorial Comments

Petri is an MIT-licensed, Inspect-based auditing framework that coordinates an auditor–target–judge loop, ships 111 seed instructions, and scores transcripts on 36 dimensions. Anthropic’s pilot spans 14 models; results are preliminary, with Claude Sonnet 4.5 and GPT-5 roughly tied on safety. Known gaps include lack of code-execution tools and judge variance; transcripts remain the primary evidence.

---

Check out the **[Technical Paper](https://alignment.anthropic.com/2025/petri/), [GitHub Page](https://github.com/safety-research/petri) and [technical blog](https://www.anthropic.com/research/petri-open-source-auditing)**. Feel free to check out our **[GitHub Page for Tutorials, Codes and Notebooks](https://github.com/Marktechpost/AI-Tutorial-Codes-Included)**. Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[100k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**. Wait! are you on telegram? **[now you can join us on telegram as well.](https://t.me/machinelearningresearchnews)**
