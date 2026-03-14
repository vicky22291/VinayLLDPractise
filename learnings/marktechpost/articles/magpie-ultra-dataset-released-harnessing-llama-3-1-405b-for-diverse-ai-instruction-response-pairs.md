---
title: "Magpie-Ultra Dataset Released: Harnessing Llama 3.1 405B for Diverse AI Instruction-Response Pairs"
date: "2024-08-04T08:09:56"
modified: "2025-01-11T17:12:24"
url: "https://www.marktechpost.com/2024/08/04/magpie-ultra-dataset-released-harnessing-llama-3-1-405b-for-diverse-ai-instruction-response-pairs/"
slug: "magpie-ultra-dataset-released-harnessing-llama-3-1-405b-for-diverse-ai-instruction-response-pairs"
---

![Magpie-Ultra Dataset Released: Harnessing Llama 3.1 405B for Diverse AI Instruction-Response Pairs](../images/257df3cddf701dc8.png)

# Magpie-Ultra Dataset Released: Harnessing Llama 3.1 405B for Diverse AI Instruction-Response Pairs

> Magpie-ultra, a new dataset by the Argilla team for supervised fine-tuning, has been released, featuring 50,000 instruction-response pairs. This synthetically generated dataset utilizes the advanced Llama 3.1 405B-Instruct model and other Llama models like Llama-Guard-3-8B and Meta-Llama-3.1-8B-Instruct. The dataset covers various tasks, including coding, mathematics, data analysis, creative writing, advice-seeking, and brainstorming, offering challenging instructions […]

**_Magpie-ultra_**, a new dataset by the Argilla team for supervised fine-tuning, has been released, featuring 50,000 instruction-response pairs. This synthetically generated dataset utilizes the advanced Llama 3.1 405B-Instruct model and other Llama models like Llama-Guard-3-8B and Meta-Llama-3.1-8B-Instruct. The dataset covers various tasks, including coding, mathematics, data analysis, creative writing, advice-seeking, and brainstorming, offering challenging instructions and responses to enhance AI model training.

This dataset is created with distilabel, and the dataset’s creation follows the Magpie recipe, as outlined in the paper “Magpie: Alignment Data Synthesis from Scratch by Prompting Aligned [LLMs](https://www.marktechpost.com/2025/01/11/what-are-large-language-model-llms/) with Nothing.” This iteration differs from the original Magpie release by employing the new Llama 3.1 family of models and generating a more focused set of 50,000 instruction-response pairs, compared to the previous 1 million. The pipeline utilizes various models for instruction generation, response creation, quality assessment, and safety classification.

The generation process involved a single 8xH100 machine, with the instruction-response pair creation taking approximately 60 hours. Additional steps, such as generating responses with the base model, computing embeddings, assessing quality and difficulty, and classifying instructions, required about 51 hours combined. This efficient process resulted in a comprehensive dataset with multiple data points for each entry.

The dataset’s structure includes various columns providing rich information about each instruction-response pair. Key columns include the instruction itself, responses from both instruct and base models, intent, required knowledge, difficulty level, quality assessment, and category classification. Also, the dataset incorporates safety checks using Llama-Guard-3-8B and provides embedding information for each instruction.

One of the dataset’s strengths lies in its potential applications. It can be used for Supervised Fine-Tuning (SFT) or Direct Preference Optimization (DPO), depending on the score difference between instruct and base model responses. This flexibility allows researchers and developers to tailor the dataset to their specific needs in AI model training and optimization.

While this release marks a significant step forward in AI training data, it’s important to note its limitations. This version is unfiltered, with a filtered version planned for future release. Also, the dataset may need to be more balanced, an issue that will be addressed in upcoming iterations. Despite these limitations, Magpie-ultra represents a valuable resource for advancing AI capabilities across various domains.

---

Check out the **[Pipeline](https://huggingface.co/datasets/argilla/magpie-ultra-v0.1/blob/main/pipeline.py) and [Dataset](https://huggingface.co/datasets/argilla/magpie-ultra-v0.1).** All credit for this research goes to the researchers of this project. Also, don’t forget to follow us on **[Twitter](https://twitter.com/Marktechpost)** and join our **[Telegram Channel](https://pxl.to/at72b5j)** and [**LinkedIn Gr**](https://www.linkedin.com/groups/13668564/)[**oup**](https://www.linkedin.com/groups/13668564/). **If you like our work, you will love our**[** newsletter..**](https://marktechpost-newsletter.beehiiv.com/subscribe)

Don’t Forget to join our **[47k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)**

**Find Upcoming [AI Webinars here](https://www.marktechpost.com/ai-webinars-list-llms-rag-generative-ai-ml-vector-database/)**

---

> [Arcee AI Released DistillKit: An Open Source, Easy-to-Use Tool Transforming Model Distillation for Creating Efficient, High-Performance Small Language Models](https://www.marktechpost.com/2024/08/01/arcee-ai-released-distillkit-an-open-source-easy-to-use-tool-transforming-model-distillation-for-creating-efficient-high-performance-small-language-models/)
