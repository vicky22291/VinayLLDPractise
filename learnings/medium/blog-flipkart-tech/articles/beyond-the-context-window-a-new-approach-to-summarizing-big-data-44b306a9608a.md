---
title: "Beyond the Context Window: A New Approach to Summarizing Big Data"
author: "Sri Raghava Muddu"
date: "Dec 4, 2025"
url: "https://blog.flipkart.tech/beyond-the-context-window-a-new-approach-to-summarizing-big-data-44b306a9608a"
tags: ['Text Summarization', 'Deep Learning', 'Large Language Models', 'Ecommerce', 'Data Science']
---

# Beyond the Context Window: A New Approach to Summarizing Big Data

## TL;DR

LLMs struggle to summarize the thousands of reviews for a single product because of context window limits and conflicting opinions.

The proposed solution, XL-OPSUMM, is a scalable framework that summarizes reviews incrementally (in chunks) to overcome these limits.

Its key innovation is the Aspect Dictionary — a numerical scorecard that tracks the exact count of Positive, Negative, and Neutral opinions for every product feature (e.g., “Battery Life”). This dictionary acts as the objective source of truth to resolve conflicts when updating the summary.

Evaluated on the new, large-scale **XL-FLIPKART** dataset (avg. 3,680 reviews/product), XL-OPSUMM significantly **outperformed all baselines**, achieving a state-of-the-art [**BooookScore**](https://arxiv.org/pdf/2310.00785)** of 85.60** for summary quality.

## The Problem: Drowning in Data

Ever found yourself twenty pages deep in product reviews, more confused than when you started? You’re not alone. In the world of e-commerce, more information isn’t always better. While Large Language Models (LLMs) are great at summarizing, they often choke on the sheer volume of opinions for a single product.

But what if a framework could intelligently read and distill thousands of reviews on the fly, giving you a clear, concise summary that’s always up-to-date? In our paper, “[Distilling Opinions at Scale: Incremental Opinion Summarization using XL-OPSUMM](https://arxiv.org/pdf/2406.10886)”, we introduced a framework called XL-OPSUMM to do just that.

## The Solution: Inside the XL-OPSUMM Framework 🧠

To handle an ever-growing stream of reviews, we designed XL-OPSUMM as a sophisticated and scalable framework. Instead of simply feeding all the reviews to an LLM, our approach uses a structured, multi-step process to intelligently integrate new information and resolve conflicts. We believe this helps the summary remain accurate and comprehensive over time.

The heart of our system is a component we call the **Aspect Dictionary**. You can think of it as a quantitative scorecard or a statistical tracker for consumer sentiment. Its function is to maintain an objective count of positive and negative opinions for every product feature (which we call an “aspect”) mentioned in the reviews.

The structure we used is a simple key-value map:

Here is a simplified example of what the dictionary might look like:

```
{
  "Battery Life": {
    "positive_count": 58,
    "negative_count": 5,
    "neutral_count": 12
  },
  "Camera Quality": {
    "positive_count": 32,
    "negative_count": 12
    "neutral_count": 10
  }
}
```

We found this data-driven approach acts as the framework’s source of truth. When opinions in the text seem to conflict, this dictionary provides the numbers needed to determine the consensus.

## The Incremental Process: A Four-Step Reconciliation

The core of XL-OPSUMM is a methodical, two-step process for updating the main summary with each new batch of reviews. We guided this process with a highly detailed prompt. Here’s how we designed it to work:

**Step 1: Initialization**

**Step 2: Incremental Update Loop**

For each of the remaining chunks, the following two-part process is repeated:

**Step 2a: Update the Aspect Dictionary**

**Step 2b: Reconcile and Update the Global Summary**

We believe this evidence-based approach allows XL-OPSUMM to scale reliably and helps prevent the LLM from getting “distracted” by a few outlier opinions

## Putting Models to the Test: The Datasets 📊

To properly evaluate our methods, we used two key datasets. The first dataset was the [AMASUM](https://arxiv.org/pdf/2109.04325) dataset, derived from Amazon, which averages over 560 reviews per product.

However, we identified a limitation in existing benchmarks for this kind of work. In a live e-commerce environment, popular products can accumulate thousands of reviews, a scenario not represented by the AMASUM data. To address this gap and evaluate our framework in a context closer to a real-world scenario, we created the **XL-FLIPKART** dataset.

## The Gauntlet: Experimental Setup and Results ⚔️

To validate the XL-OpSUMM framework, we conducted a series of experiments, pitting our method against a range of existing models and evaluation techniques on the AMASUM and the highly challenging XL-FLIPKART datasets.

### The Setup

We evaluated our framework using two powerful, open-source large language models (LLMs) as the backbone:

The performance of the XL-OpSUMM framework was compared against several baselines, including non-LLM methods and two standard LLM-based approaches: a straightforward Incremental method and a Hierarchical method that summarizes chunks of text before merging them. All experiments were conducted on Nvidia DGX A100 GPUs.

### The Results 🏆

The results from the experiments are clear: the XL-OpSUMM framework consistently and significantly outperforms the baseline models, especially on the more challenging, large-scale dataset.

### Reference-Based Metrics (ROUGE & BERT-F1)

These metrics measure how well the generated summary matches a reference summary.

This shows the effectiveness of the XL-OpSUMM framework across the evaluated datasets.

### Reference-Free Metrics (Fluency, Coherence & BooookScore)

The framework’s capability to produce high-quality output is particularly evident in its reference-free evaluation. This assessment judges the summary purely on its own merits, without requiring a comparison to a reference text. The reference-free metrics employed include **Fluency (FL)** and **Coherence (CO) (**[Siledar et al., ACL 2024)](https://aclanthology.org/2024.acl-long.655/), which were evaluated by models such as[ GPT-3.5-TURBO](https://chatgpt.com/) and[ MISTRAL-7B-32K,](https://arxiv.org/abs/2310.06825) along with the specialized[ **BooookScore**](https://arxiv.org/pdf/2310.00785) for an independent measure of summary quality.

The data consistently shows that the XL-OpSUMM framework is not just an incremental improvement but a significant leap forward in generating high-quality, scalable opinion summaries.

## Final Thoughts

In a world with ever-increasing amounts of data, we believe the XL-OPSUMM framework provides a useful and scalable solution. Its superiority was demonstrated on the challenging XL-FLIPKART dataset, where it achieved a state-of-the-art BooookScore of 85.60, significantly outperforming all baseline methods. We believe this research marks a significant step towards creating more practical AI that can turn data overload into clear, actionable insight for both consumers and businesses. This framework offers a cost-effective alternative for practitioners concerned about the API costs associated with using closed-source models that have large context windows, enabling effective summary generation at a lower price.

## References

---
**Tags:** Text Summarization · Deep Learning · Large Language Models · Ecommerce · Data Science
