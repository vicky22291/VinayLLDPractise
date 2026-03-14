---
title: "Microsoft AI Releases Fara-7B: An Efficient Agentic Model for Computer Use"
date: "2025-11-24T14:57:29"
modified: "2025-11-24T14:57:35"
url: "https://www.marktechpost.com/2025/11/24/microsoft-ai-releases-fara-7b-an-efficient-agentic-model-for-computer-use/"
slug: "microsoft-ai-releases-fara-7b-an-efficient-agentic-model-for-computer-use"
---

![Microsoft AI Releases Fara-7B: An Efficient Agentic Model for Computer Use](../images/79dafe7d4de49200.png)

# Microsoft AI Releases Fara-7B: An Efficient Agentic Model for Computer Use

> How do we safely let an AI agent handle real web tasks like booking, searching, and form filling directly on our own devices without sending everything to the cloud? Microsoft Research has released Fara-7B, a 7 billion parameter agentic small language model designed specifically for computer use. It is an open weight Computer Use Agent […]

How do we safely let an AI agent handle real web tasks like booking, searching, and form filling directly on our own devices without sending everything to the cloud? Microsoft Research has released **Fara-7B**, a 7 billion parameter agentic [small language model](https://www.marktechpost.com/2025/01/12/what-are-small-language-models-slms/) designed specifically for computer use. It is an open weight Computer Use Agent that runs from screenshots, predicts mouse and keyboard actions, and is small enough to execute on a single user device, which reduces latency and keeps browsing data local.

![](https://www.marktechpost.com/wp-content/uploads/2025/11/Screenshot-2025-11-24-at-2.45.36-PM.png)![](https://www.marktechpost.com/wp-content/uploads/2025/11/Screenshot-2025-11-24-at-2.45.36-PM.png)*https://www.microsoft.com/en-us/research/blog/fara-7b-an-efficient-agentic-model-for-computer-use/*

### From Chatbots to Computer Use Agents

Conventional chat oriented LLMs return text. Computer Use Agents such as Fara-7B instead control the browser or desktop user interface to complete tasks like filling forms, booking travel, or comparing prices. They perceive the screen, reason about the page layout, then emit low level actions such as click, scroll, type, web_search, or visit_url.

Many existing systems rely on large multimodal models wrapped in complex scaffolding that parses accessibility trees and orchestrates multiple tools. This increases latency and often requires [server](https://www.marktechpost.com/2025/08/08/proxy-servers-explained-types-use-cases-trends-in-2025-technical-deep-dive/) side deployment. Fara-7B compresses the behavior of such multi agent systems into a single multimodal decoder only model built on Qwen2.5-VL-7B. It consumes browser screenshots and text context, then directly outputs thought text followed by a tool call with grounded arguments such as coordinates, text, or URLs.

### FaraGen, Synthetic Trajectories for Web Interaction

The key bottleneck for Computer Use Agents is data. High quality logs of human web interaction with multi step actions are rare and expensive to collect. The Fara project introduces **FaraGen**, a synthetic data engine that generates and filters web trajectories on live sites.

**FaraGen **uses a three stage pipeline. Task Proposal starts from seed URLs drawn from public corpora such as ClueWeb22 and Tranco, which are categorized into domains like e commerce, travel, entertainment, or forums. Large language models convert each URL into realistic tasks that users might attempt on that page, for example booking specific movie tickets or creating a shopping list with constraints on reviews and materials. Tasks must be achievable without login or paywall, fully specified, useful, and automatically verifiable.

![](https://www.marktechpost.com/wp-content/uploads/2025/11/Screenshot-2025-11-24-at-2.46.37-PM-1-1024x576.png)![](https://www.marktechpost.com/wp-content/uploads/2025/11/Screenshot-2025-11-24-at-2.46.37-PM-1-1024x576.png)*https://www.microsoft.com/en-us/research/blog/fara-7b-an-efficient-agentic-model-for-computer-use/*

Task Solving runs a multi agent system based on Magentic-One and Magentic-UI. An Orchestrator agent plans the high level strategy and keeps a ledger of task state. A WebSurfer agent receives accessibility trees and Set-of-Marks screenshots, then emits browser actions through Playwright, such as click, type, scroll, visit_url, or web_search. A UserSimulator agent supplies follow up instructions when the task needs clarification.

Trajectory Verification uses **three LLM based verifiers**. An Alignment Verifier checks that the actions and final answer match the task intent. A Rubric Verifier generates a rubric of subgoals and scores partial completion. A Multimodal Verifier inspects screenshots plus the final answer to catch hallucinations and confirm that visible evidence supports success. These verifiers agree with human labels on 83.3 percent of cases, with reported false positive and false negative rates around 17 to 18 percent.

After filtering, **FaraGen** yields 145,603 trajectories with 1,010,797 steps over 70,117 unique domains. The trajectories range from 3 to 84 steps, with an average of 6.9 steps and about 0.5 unique domains per trajectory, which indicates that many tasks involve sites not seen elsewhere in the dataset. Generating data with premium models such as GPT-5 and o3 costs roughly 1 dollar per verified trajectory.

![](https://www.marktechpost.com/wp-content/uploads/2025/11/Screenshot-2025-11-24-at-2.47.26-PM-1.png)![](https://www.marktechpost.com/wp-content/uploads/2025/11/Screenshot-2025-11-24-at-2.47.26-PM-1.png)*https://www.microsoft.com/en-us/research/wp-content/uploads/2025/11/Fara-7B-An-Efficient-Agentic-Model-for-Computer-Use.pdf*

### Model Architecture

Fara-7B is a multimodal decoder only model that uses Qwen2.5-VL-7B as the base. It takes as input a user goal, the latest screenshots from the browser, and the full history of previous thoughts and actions. The context window is 128,000 tokens. At each step the model first generates a chain of thought describing the current state and the plan, then outputs a tool call that specifies the next action and its arguments.

The tool space matches the Magentic-UI computer_use interface. It includes key, type, mouse_move, left_click, scroll, visit_url, web_search, history_back, pause_and_memorize_fact, wait, and terminate. Coordinates are predicted directly as pixel positions on the screenshot, which allows the model to operate without access to the accessibility tree at inference time.

Training uses supervised finetuning over approximately 1.8 million samples that mix multiple data sources. These include the FaraGen trajectories broken into observe think act steps, grounding and UI localization tasks, screenshot based visual question answering and captioning, and safety and refusal datasets.

![](https://www.marktechpost.com/wp-content/uploads/2025/11/Screenshot-2025-11-24-at-2.48.09-PM-2-1024x628.png)![](https://www.marktechpost.com/wp-content/uploads/2025/11/Screenshot-2025-11-24-at-2.48.09-PM-2-1024x628.png)*https://www.microsoft.com/en-us/research/wp-content/uploads/2025/11/Fara-7B-An-Efficient-Agentic-Model-for-Computer-Use.pdf*

### Benchmarks and Efficiency

Microsoft evaluates Fara-7B on **four live web benchmarks**: WebVoyager, Online-Mind2Web, DeepShop, and the new WebTailBench, which focuses on under represented segments such as restaurant reservations, job applications, real estate search, comparison shopping, and multi site compositional tasks.

On these benchmarks, Fara-7B achieves 73.5 percent success on WebVoyager, 34.1 percent on Online-Mind2Web, 26.2 percent on DeepShop, and 38.4 percent on WebTailBench. This outperforms the 7B Computer Use Agent baseline UI-TARS-1.5-7B, which scores 66.4, 31.3, 11.6, and 19.5 respectively, and compares favorably to larger systems like OpenAI computer-use-preview and SoM Agent configurations built on GPT-4o.

On WebVoyager, Fara-7B uses on average 124,000 input tokens and 1,100 output tokens per task, with about 16.5 actions. Using market token prices, the research team estimate an average cost of 0.025 dollars per task, versus around 0.30 dollars for SoM agents backed by proprietary reasoning models such as GPT-5 and o3. Fara-7B uses a similar number of input tokens but about one tenth the output tokens of these SoM agents.

### Key Takeaways

- Fara-7B is a 7B parameter, open weight Computer Use Agent built on Qwen2.5-VL-7B that operates directly from screenshots and text, then outputs grounded actions such as clicks, typing and navigation, without relying on accessibility trees at inference time.

- The model is trained with 145,603 verified browser trajectories and 1,010,797 steps generated by the FaraGen pipeline, which uses multi agent task proposal, solving, and LLM based verification on live websites across 70,117 domains.

- Fara-7B achieves 73.5 percent success on WebVoyager, 34.1 percent on Online-Mind2Web, 26.2 percent on DeepShop, and 38.4 percent on WebTailBench, improving substantially over the 7B UI-TARS-1.5 baseline on all four benchmarks.

- On WebVoyager, Fara-7B uses about 124,000 input tokens and 1,100 output tokens per task, with an average of 16.5 actions, yielding an estimated cost of around 0.025 dollars per task, which is around an order of magnitude cheaper in output token usage than SoM agents backed by GPT 5 class models.

### Editorial Notes

Fara-7B is a useful step toward practical Computer Use Agents that can run on local hardware with lower inference cost while preserving privacy. The combination of Qwen2.5 VL 7B, FaraGen synthetic trajectories and WebTailBench gives a clear and well instrumented path from multi agent data generation to a single compact model that matches or exceeds larger systems on key benchmarks while enforcing Critical Point and refusal safeguards.

---

Check out the **[Paper](https://www.microsoft.com/en-us/research/wp-content/uploads/2025/11/Fara-7B-An-Efficient-Agentic-Model-for-Computer-Use.pdf), [Model weights](https://huggingface.co/microsoft/Fara-7B)** and [technical details](https://www.microsoft.com/en-us/research/blog/fara-7b-an-efficient-agentic-model-for-computer-use/). Feel free to check out our **[GitHub Page for Tutorials, Codes and Notebooks](https://github.com/Marktechpost/AI-Tutorial-Codes-Included)**. Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[100k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**. Wait! are you on telegram? **[now you can join us on telegram as well.](https://t.me/machinelearningresearchnews)**
