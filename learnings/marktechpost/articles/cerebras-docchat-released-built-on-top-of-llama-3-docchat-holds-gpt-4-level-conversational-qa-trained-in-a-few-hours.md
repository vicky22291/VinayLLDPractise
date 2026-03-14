---
title: "Cerebras DocChat Released: Built on Top of Llama 3, DocChat holds GPT-4 Level Conversational QA Trained in a Few Hours"
date: "2024-08-24T20:14:44"
modified: "2024-08-24T20:14:52"
url: "https://www.marktechpost.com/2024/08/24/cerebras-docchat-released-built-on-top-of-llama-3-docchat-holds-gpt-4-level-conversational-qa-trained-in-a-few-hours/"
slug: "cerebras-docchat-released-built-on-top-of-llama-3-docchat-holds-gpt-4-level-conversational-qa-trained-in-a-few-hours"
---

![Cerebras DocChat Released: Built on Top of Llama 3, DocChat holds GPT-4 Level Conversational QA Trained in a Few Hours](../images/3cc672bf425bd3ad.png)

# Cerebras DocChat Released: Built on Top of Llama 3, DocChat holds GPT-4 Level Conversational QA Trained in a Few Hours

> The release of DocChat by Cerebras marks a major milestone in document-based conversational question-answering systems. Cerebras, known for its deep expertise in machine learning (ML) and large language models (LLMs), has introduced two new models under the DocChat series: Cerebras Llama3-DocChat and Cerebras Dragon-DocChat. These models are designed to deliver high-performance conversational AI, specifically tailored […]

The release of DocChat by Cerebras marks a major milestone in document-based conversational question-answering systems. Cerebras, known for its deep expertise in machine learning (ML) and large language models (LLMs), has introduced two new models under the DocChat series: **Cerebras Llama3-DocChat** and **Cerebras Dragon-DocChat**. These models are designed to deliver high-performance conversational AI, specifically tailored for document-based question-answering tasks, and were developed with unprecedented speed using Cerebras’ cutting-edge technology.

**Overview of the DocChat Models**

Cerebras Llama3-DocChat is built on the foundation of Llama 3 and incorporates advanced insights from recent research in the field, particularly Nvidia’s ChatQA model series. The development of this model involved leveraging extensive experience in LLM training and dataset curation alongside innovative techniques like synthetic data generation. This approach enabled Cerebras to address limitations that could not be fully resolved using available real-world data.

Cerebras Dragon-DocChat is a multi-turn retriever model that is fine-tuned to improve recall rates. The model was trained on the ChatQA conversational Q&A dataset and enhanced using contrastive loss with hard negatives, leading to significant improvements in recall rates compared to its predecessors and competitors.

![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXcTHMQMyh08UBEkPIctZC_zqr8bbxP0AlFRG2Wnrk5YnMnR7kK8cg4jWJg1aA9F9mhECAFeamWZIpkVKdp1wOYaC4Vx-GvJOrp8B2zpc4V9V7jkYRMZ5ZVm__DMsEBEClk_EAn5QkjmbifsYprR6wDalut9?key=itfJxql3pLyNVx5whHQiTQ)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXcTHMQMyh08UBEkPIctZC_zqr8bbxP0AlFRG2Wnrk5YnMnR7kK8cg4jWJg1aA9F9mhECAFeamWZIpkVKdp1wOYaC4Vx-GvJOrp8B2zpc4V9V7jkYRMZ5ZVm__DMsEBEClk_EAn5QkjmbifsYprR6wDalut9?key=itfJxql3pLyNVx5whHQiTQ)*[**Image Source**](https://www.cerebras.net/blog/train-a-gpt-4-level-conversational-qa-in-a-few-hours)*

**Training Efficiency and Performance**

One of the standout features of the DocChat models is the speed at which they were trained. The Cerebras Llama3-DocChat model was trained in just a few hours using a single Cerebras System, while the Dragon-DocChat model was fine-tuned in minutes. This remarkable efficiency is a testament to Cerebras’ advanced hardware and software capabilities, setting a new benchmark in the AI industry.

![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXekn7K8OhUpE_XE8epUXJ8QEe_DfZ9Xk3noVjEqYC1FOPbjnLQ0MLa8ds9fraWu8QnpesR1wqMgwc21CKcXnHmh9_Lcqu21GrRfMQld17g9FCd_WDqL1l8n5dtfJDP16YXfm2XO88wpCMENnulzSlTKlCwC?key=itfJxql3pLyNVx5whHQiTQ)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXekn7K8OhUpE_XE8epUXJ8QEe_DfZ9Xk3noVjEqYC1FOPbjnLQ0MLa8ds9fraWu8QnpesR1wqMgwc21CKcXnHmh9_Lcqu21GrRfMQld17g9FCd_WDqL1l8n5dtfJDP16YXfm2XO88wpCMENnulzSlTKlCwC?key=itfJxql3pLyNVx5whHQiTQ)*[**Image Source**](https://www.cerebras.net/blog/train-a-gpt-4-level-conversational-qa-in-a-few-hours)*

The performance of these models has been rigorously evaluated across various benchmarks. Both models achieved top-tier results for their respective sizes, outperforming many existing solutions. For instance, on benchmarks like ConvFinQA and SQA, Cerebras Llama3-DocChat showed significant improvements, demonstrating its superior capability in handling complex conversational Q&A tasks.

**Open Source Commitment**

Cerebras has also reaffirmed its commitment to the open-source community by releasing DocChat. The company has made the model weights, the complete training recipes, and associated datasets available to the public. This level of transparency allows other AI researchers and developers to replicate, build upon, and innovate with Cerebras’ work, potentially leading to further advancements in the field.

**Benchmark Comparisons**

Cerebras’ DocChat models have shown impressive results in head-to-head comparisons with other models. For example, in the ChatRAG Benchmark, Cerebras Llama3-DocChat scored higher than Nvidia’s Llama3-ChatQA and GPT-4 Turbo in several key metrics. Similarly, Cerebras Dragon-DocChat outperformed Facebook’s Dragon+ and Nvidia’s Dragon Multiturn in recall rates, particularly in multi-turn conversational settings.

The development of DocChat had its challenges. One of the key issues addressed during training was the model’s ability to handle unanswerable questions. Initial tests showed that the model struggled with these questions, often failing to respond appropriately. Through experimentation, Cerebras found that upsampling samples corresponding to unanswerable questions improved the model’s performance. However, the company acknowledges that there is still room for improvement in this area, particularly when benchmarked against state-of-the-art models like QuAC and DoQA.

Another challenge was improving the model’s arithmetic performance, which was initially prone to errors. By incorporating techniques inspired by the Chain of Thought (CoT) method, Cerebras significantly boosted the model’s accuracy in arithmetic tasks. Entity extraction posed difficulties due to a need for more high-quality training data. This issue was mitigated by integrating a subset of SKGInstruct, an instruction-tuning dataset that improved the model’s performance on entity extraction tasks.

Cerebras has ambitious plans for the future development of the DocChat series. The company is exploring several exciting directions, including support for longer contexts, improved mathematical reasoning, and larger model sizes. These enhancements are expected to solidify further Cerebras’ position as a leader in conversational AI.

In conclusion, the release of DocChat by Cerebras, the speed and efficiency with which these models were trained, and their top-tier performance highlight Cerebras’ technological prowess. Also, the company’s commitment to open source and continuous innovation ensures that DocChat will benefit its users and contribute to the broader AI community. As Cerebras continues to refine and expand its offerings, the impact of DocChat on the future of AI-driven communication will likely be profound.

---

Check out the **[Model on HF](https://huggingface.co/cerebras/Llama3-DocChat-1.0-8B) and [Details](https://cerebras.ai/blog/train-a-gpt-4-level-conversational-qa-in-a-few-hours).** All credit for this research goes to the researchers of this project. Also, don’t forget to follow us on **[Twitter](https://twitter.com/Marktechpost)** and join our **[Telegram Channel](https://arxiv.org/abs/2408.08231)** and [**LinkedIn Gr**](https://www.linkedin.com/groups/13668564/)[**oup**](https://www.linkedin.com/groups/13668564/). **If you like our work, you will love our**[** newsletter..**](https://marktechpost-newsletter.beehiiv.com/subscribe)

Don’t Forget to join our **[49k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)**

**Find Upcoming [AI Webinars here](https://www.marktechpost.com/ai-webinars-list-llms-rag-generative-ai-ml-vector-database/)**
