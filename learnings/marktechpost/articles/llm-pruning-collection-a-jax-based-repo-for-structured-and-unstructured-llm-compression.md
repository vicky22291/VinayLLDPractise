---
title: "LLM-Pruning Collection: A JAX Based Repo For Structured And Unstructured LLM Compression"
date: "2026-01-04T23:21:37"
modified: "2026-01-04T23:21:45"
url: "https://www.marktechpost.com/2026/01/04/llm-pruning-collection-a-jax-based-repo-for-structured-and-unstructured-llm-compression/"
slug: "llm-pruning-collection-a-jax-based-repo-for-structured-and-unstructured-llm-compression"
---

![LLM-Pruning Collection: A JAX Based Repo For Structured And Unstructured LLM Compression](../images/4a8f1f3646c68501.png)

# LLM-Pruning Collection: A JAX Based Repo For Structured And Unstructured LLM Compression

> Zlab Princeton researchers have released LLM-Pruning Collection, a JAX based repository that consolidates major pruning algorithms for large language models into a single, reproducible framework. It targets one concrete goal, make it easy to compare block level, layer level and weight level pruning methods under a consistent training and evaluation stack on both GPUs and […]

Zlab Princeton researchers have released **[LLM-Pruning Collection](https://github.com/zlab-princeton/llm-pruning-collection)**, a JAX based repository that consolidates major pruning algorithms for large language models into a single, reproducible framework. It targets one concrete goal, make it easy to compare block level, layer level and weight level pruning methods under a consistent training and evaluation stack on both GPUs and TPUs.

### What LLM-Pruning Collection Contains?

It is described as a JAX based repo for LLM pruning. **It is organized into three main directories:**

- `pruning` holds implementations for several pruning methods: Minitron, ShortGPT, Wanda, SparseGPT, Magnitude, Sheared Llama and LLM-Pruner.

- `training` provides integration with FMS-FSDP for GPU training and MaxText for TPU training.

- `eval` exposes JAX compatible evaluation scripts built around lm-eval-harness, with accelerate based support for MaxText that gives about 2 to 4 times speedup.

### Pruning Methods Covered

LLM-Pruning Collection spans several families of pruning algorithms with different granularity levels:

#### Minitron

Minitron is a practical pruning and distillation recipe developed by NVIDIA that compresses Llama 3.1 8B and Mistral NeMo 12B to 4B and 8B while preserving performance. It explores depth pruning and joint width pruning of hidden sizes, attention and MLP, followed by distillation.

In LLM-Pruning Collection, the `pruning/minitron` folder provides scripts such as `prune_llama3.1-8b.sh` which run Minitron style pruning on Llama 3.1 8B.

#### ShortGPT

ShortGPT is based on the observation that many Transformer layers are redundant. The method defines Block Influence, a metric that measures the contribution of each layer and then removes low influence layers by direct layer deletion. Experiments show that ShortGPT outperforms previous pruning methods for multiple choice and generative tasks.

In the collection, ShortGPT is implemented through the Minitron folder with a dedicated script `prune_llama2-7b.sh`.

#### Wanda, SparseGPT, Magnitude

Wanda is a post training pruning method that scores weights by the product of weight magnitude and corresponding input activation on a per output basis. It prunes the smallest scores, requires no retraining and induces sparsity that works well even at billion parameter scale.

SparseGPT is another post training method that uses a second order inspired reconstruction step to prune large GPT style models at high sparsity ratios. Magnitude pruning is the classical baseline that removes weights with small absolute value.

In LLM-Pruning Collection, all three live under `pruning/wanda` with a shared installation path. The README includes a dense table of Llama 2 7B results that compares Wanda, SparseGPT and Magnitude across BoolQ, RTE, HellaSwag, Winogrande, ARC E, ARC C and OBQA, under unstructured and structured sparsity patterns such as 4:8 and 2:4.

#### Sheared Llama

Sheared LLaMA is a structured pruning method that learns masks for layers, attention heads and hidden dimensions and then retrains the pruned architecture. The original release provides models at multiple scales including 2.7B and 1.3B.

The `pruning/llmshearing` directory in LLM-Pruning Collection integrates this recipe. It uses a RedPajama subset for calibration, accessed through Hugging Face, and helper scripts to convert between Hugging Face and MosaicML Composer formats.

#### LLM-Pruner

LLM-Pruner is a framework for structural pruning of large language models. It removes non critical coupled structures, such as attention heads or MLP channels, using gradient based importance scores and then recovers performance with a short LoRA tuning stage that uses about 50K samples. The collection includes LLM-Pruner under `pruning/LLM-Pruner` with scripts for LLaMA, LLaMA 2 and Llama 3.1 8B.

### Key Takeaways

- LLM-Pruning Collection is a JAX based, Apache-2.0 repo from zlab-princeton that unifies modern LLM pruning methods with shared pruning, training and evaluation pipelines for GPUs and TPUs.

- The codebase implements block, layer and weight level pruning approaches, including Minitron, ShortGPT, Wanda, SparseGPT, Sheared LLaMA, Magnitude pruning and LLM-Pruner, with method specific scripts for Llama family models.

- Training integrates FMS-FSDP on GPU and MaxText on TPU with JAX compatible evaluation scripts built on lm-eval-harness, giving roughly 2 to 4 times faster eval for MaxText checkpoints via accelerate.

- The repository reproduces key results from prior pruning work, publishing side by side “paper vs reproduced” tables for methods like Wanda, SparseGPT, Sheared LLaMA and LLM-Pruner so engineers can verify their runs against known baselines.

---

Check out the **[GitHub Repo](https://github.com/zlab-princeton/llm-pruning-collection)**. Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[100k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**. Wait! are you on telegram? **[now you can join us on telegram as well.](https://t.me/machinelearningresearchnews)**
