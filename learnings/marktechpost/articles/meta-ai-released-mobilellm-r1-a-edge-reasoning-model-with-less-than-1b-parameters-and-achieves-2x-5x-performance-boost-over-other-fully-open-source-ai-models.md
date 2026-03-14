---
title: "Meta AI Released MobileLLM-R1: A Edge Reasoning Model with less than 1B Parameters and Achieves 2x–5x Performance Boost Over Other Fully Open-Source AI Models"
date: "2025-09-14T23:34:36"
modified: "2025-09-14T23:51:39"
url: "https://www.marktechpost.com/2025/09/14/meta-ai-released-mobilellm-r1-a-edge-reasoning-model-with-less-than-1b-parameters-and-achieves-2x-5x-performance-boost-over-other-fully-open-source-ai-models/"
slug: "meta-ai-released-mobilellm-r1-a-edge-reasoning-model-with-less-than-1b-parameters-and-achieves-2x-5x-performance-boost-over-other-fully-open-source-ai-models"
---

![Meta AI Released MobileLLM-R1: A Edge Reasoning Model with less than 1B Parameters and Achieves 2x–5x Performance Boost Over Other Fully Open-Source AI Models](../images/487f4db67f9b20b4.png)

# Meta AI Released MobileLLM-R1: A Edge Reasoning Model with less than 1B Parameters and Achieves 2x–5x Performance Boost Over Other Fully Open-Source AI Models

> Meta has released MobileLLM-R1, a family of lightweight edge reasoning models now available on Hugging Face. The release includes models ranging from 140M to 950M parameters, with a focus on efficient mathematical, coding, and scientific reasoning at sub-billion scale. Unlike general-purpose chat models, MobileLLM-R1 is designed for edge deployment, aiming to deliver state-of-the-art reasoning accuracy […]

### Table of contents

- [What architecture powers MobileLLM-R1?](#h-what-architecture-powers-mobilellm-r1)
- [How efficient is the training?](#h-how-efficient-is-the-training)
- [How does it perform against other open models?](#h-how-does-it-perform-against-other-open-models)
- [Where does MobileLLM-R1 fall short?](#h-where-does-mobilellm-r1-fall-short)
- [How does MobileLLM-R1 compare to Qwen3, SmolLM2, and OLMo?](#h-how-does-mobilellm-r1-compare-to-qwen3-smollm2-and-olmo)
- [Summary](#h-summary)

Meta has released **MobileLLM-R1**, a family of lightweight edge reasoning models now available on [Hugging Face](https://huggingface.co/facebook/MobileLLM-R1-950M). The release includes models ranging from 140M to 950M parameters, with a focus on efficient mathematical, coding, and scientific reasoning at sub-billion scale.

Unlike general-purpose chat models, MobileLLM-R1 is designed for edge deployment, aiming to deliver state-of-the-art reasoning accuracy while remaining computationally efficient.

[](https://www.marktechpost.com/wp-content/uploads/2025/09/1000x800-1-scaled.png)

### What architecture powers MobileLLM-R1?

The largest model, **MobileLLM-R1-950M**, integrates several architectural optimizations:

- **22 Transformer layers** with 24 attention heads and 6 grouped KV heads.

- **Embedding dimension: 1536**; **hidden dimension: 6144**.

- **Grouped-Query Attention (GQA)** reduces compute and memory.

- **Block-wise weight sharing** cuts parameter count without heavy latency penalties.

- **SwiGLU activations** improve small-model representation.

- **Context length:** 4K for base, 32K for post-trained models.

- **128K vocabulary** with shared input/output embeddings.

The emphasis is on reducing compute and memory requirements, making it suitable for deployment on constrained devices.

### How efficient is the training?

**MobileLLM-R1 is notable for data efficiency:**

- Trained on **~4.2T tokens** in total.

- By comparison, **Qwen3’s 0.6B** model was trained on **36T tokens**.

- This means MobileLLM-R1 uses only **≈11.7%** of the data to reach or surpass Qwen3’s accuracy.

- Post-training applies supervised fine-tuning on math, coding, and reasoning datasets.

This efficiency translates directly into lower training costs and resource demands.

### How does it perform against other open models?

On benchmarks, MobileLLM-R1-950M shows significant gains:

- **MATH (MATH500 dataset):** ~**5× higher accuracy** than **Olmo-1.24B** and ~**2× higher accuracy** than **SmolLM2-1.7B**.

- **Reasoning and coding (GSM8K, AIME, LiveCodeBench):** Matches or surpasses **Qwen3-0.6B**, despite using far fewer tokens.

The model delivers results typically associated with larger architectures while maintaining a smaller footprint.

### Where does MobileLLM-R1 fall short?

The model’s focus creates limitations:

- Strong in **math, code, and structured reasoning**.

- Weaker in **general conversation, commonsense, and creative tasks** compared to larger LLMs.

- Distributed under **FAIR NC (non-commercial) license**, which restricts usage in production settings.

- Longer contexts (32K) raise **KV-cache and memory demands** at inference.

### How does MobileLLM-R1 compare to Qwen3, SmolLM2, and OLMo?

**Performance snapshot (post-trained models):**

ModelParamsTrain tokens (T)MATH500GSM8KAIME’24AIME’25LiveCodeBench**MobileLLM-R1-950M**0.949B**4.2****74.0**67.515.516.319.9**Qwen3-0.6B**0.596B**36.0**73.0**79.2**11.317.014.9**SmolLM2-1.7B-Instruct**1.71B**~11.0**19.241.80.30.14.4**OLMo-2-1B-Instruct**1.48B**~3.95**19.269.70.60.10.0

**Key observations:**

- R1-950M matches **Qwen3-0.6B** in math (74.0 vs 73.0) while requiring ~**8.6× fewer tokens**.

- Performance gaps vs **SmolLM2** and **OLMo** are substantial across reasoning tasks.

- Qwen3 maintains an edge in GSM8K, but the difference is small compared to the training efficiency advantage.

### Summary

Meta’s MobileLLM-R1 underscores a trend toward smaller, domain-optimized models that deliver competitive reasoning without massive training budgets. By achieving 2×–5× performance gains over larger open models while training on a fraction of the data, it demonstrates that efficiency—not just scale—will define the next phase of LLM deployment, especially for math, coding, and scientific use cases on edge devices.

---

Check out the **[Model on Hugging Face](https://huggingface.co/facebook/MobileLLM-R1-950M)_._** Feel free to check out our **[GitHub Page for Tutorials, Codes and Notebooks](https://github.com/Marktechpost/AI-Tutorial-Codes-Included)**. Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[100k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**.
