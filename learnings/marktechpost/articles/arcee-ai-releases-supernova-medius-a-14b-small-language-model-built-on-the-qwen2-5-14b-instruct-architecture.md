---
title: "Arcee AI Releases SuperNova-Medius: A 14B Small Language Model Built on the Qwen2.5-14B-Instruct Architecture"
date: "2024-10-12T19:30:01"
modified: "2025-01-12T11:58:38"
url: "https://www.marktechpost.com/2024/10/12/arcee-ai-releases-supernova-medius-a-14b-small-language-model-built-on-the-qwen2-5-14b-instruct-architecture/"
slug: "arcee-ai-releases-supernova-medius-a-14b-small-language-model-built-on-the-qwen2-5-14b-instruct-architecture"
---

![Arcee AI Releases SuperNova-Medius: A 14B Small Language Model Built on the Qwen2.5-14B-Instruct Architecture](../images/e781e4d2a778c1b1.png)

# Arcee AI Releases SuperNova-Medius: A 14B Small Language Model Built on the Qwen2.5-14B-Instruct Architecture

> In the ever-evolving world of artificial intelligence (AI), large language models have proven instrumental in addressing a wide array of challenges, from automating complex tasks to enhancing decision-making processes. However, scaling these models has also introduced considerable complexities, such as high computational costs, reduced accessibility, and the environmental impact of extensive resource requirements. The enormous […]

In the ever-evolving world of artificial intelligence (AI), large language models have proven instrumental in addressing a wide array of challenges, from automating complex tasks to enhancing decision-making processes. However, scaling these models has also introduced considerable complexities, such as high computational costs, reduced accessibility, and the environmental impact of extensive resource requirements. The enormous size of conventional language models like GPTs or LLaMA-70B makes them challenging for many institutions to adopt due to constraints in computational infrastructure. Arcee AI has acknowledged these challenges and sought to bridge the gap between model capability and accessibility with the introduction of SuperNova-Medius—a [small language model](https://www.marktechpost.com/2025/01/12/what-are-small-language-models-slms/) that aims to maintain the high-quality output of larger counterparts without their limitations.

SuperNova-Medius: A 14B Small Language Model that seeks to disrupt the traditional notions of size versus performance in AI models. 70B SuperNova-Medius comes after the Arcee AI’s release of SuperNova-70B, followed by the 8B SuperNova-Lite. SuperNova-Medius is designed to match the prowess of significantly larger models, rivaling those with up to 70 billion parameters. It does so while retaining a relatively manageable size of 14 billion parameters, making it highly suitable for various use cases without the massive computational burden. By integrating groundbreaking optimization techniques and innovative architectural designs, SuperNova-Medius presents a fresh perspective on how effective language models can be designed for real-world usability while ensuring that smaller organizations can leverage the potential.

SuperNova-Medius is built on an optimized Transformer architecture, coupled with advanced quantization methods that allow it to maintain impressive accuracy and efficiency. The development of SuperNova-Medius involved a sophisticated multi-teacher, cross-architecture distillation process with the following key steps:

- **Logit Distillation from Llama 3.1 405B**: The logits of Llama 3.1 405B were distilled using an offline approach. The top K logits for each token were stored to capture most of the probability mass while managing storage requirements.

- **Cross-Architecture Adaptation**: Using mergekit-tokensurgeon, a version of Qwen2.5-14B was created that uses the vocabulary of Llama 3.1 405B. This allowed for the use of Llama 3.1 405B logits in training the Qwen-based model.

- **Distillation to Qwen Architecture**: The adapted Qwen2.5-14B model was trained using the stored 405B logits as the target.

- **Parallel Qwen Distillation**: In a separate process, Qwen2-72B was distilled into a 14B model.

- **Final Fusion and Fine-Tuning**: The Llama-distilled Qwen model’s vocabulary was reverted to the Qwen vocabulary. After re-aligning the vocabularies, a final fusion and fine-tuning step was conducted using a specialized dataset from EvolKit to ensure that SuperNova-Medius maintained coherence, fluency, and context understanding across a broad range of tasks.

Despite being smaller compared to the largest models, SuperNova-Medius has been extensively fine-tuned using a diverse and expansive dataset, covering multiple domains and languages. This extensive training allows SuperNova-Medius to exhibit a strong understanding of context, generate coherent responses, and perform complex reasoning tasks effectively. Furthermore, by employing innovations in parameter sharing and utilizing sparsity strategies, the model delivers results that are comparable to models with substantially higher parameter counts. The key benefits of SuperNova-Medius lie in its balanced capability—it provides high-quality language generation while being cost-effective to deploy, making it a perfect fit for applications needing reliable but resource-efficient solutions.

![](https://www.marktechpost.com/wp-content/uploads/2024/10/Screenshot-2024-10-12-at-7.21.38-PM-1-1024x364.png)![](https://www.marktechpost.com/wp-content/uploads/2024/10/Screenshot-2024-10-12-at-7.21.38-PM-1-1024x364.png)

SuperNova-Medius excels in instruction-following (IFEval) and complex reasoning tasks (BBH), outperforming Qwen2.5-14B and SuperNova-Lite across multiple benchmarks. This makes it a powerful, efficient solution for high-quality generative AI applications.

In conclusion, SuperNova-Medius stands as a testament to Arcee AI’s commitment to pushing the boundaries of what’s possible with language models while making advanced AI more inclusive and sustainable. By successfully reducing the model size without compromising on performance, Arcee AI has provided a solution that caters to the needs of various sectors, from startups and small businesses to educational institutions and beyond. As AI continues to shape our future, innovations like SuperNova-Medius are essential in ensuring that the benefits of advanced machine learning technology are accessible to all, paving the way for more equitable and impactful applications of AI across the globe.

---

Check out the **[Model on Hugging Face](https://huggingface.co/arcee-ai/SuperNova-Medius)**. All credit for this research goes to the researchers of this project. Also, don’t forget to follow us on **[Twitter](https://twitter.com/Marktechpost)** and join our **[Telegram Channel](https://pxl.to/at72b5j)** and [**LinkedIn Gr**](https://www.linkedin.com/groups/13668564/)[**oup**](https://www.linkedin.com/groups/13668564/). **If you like our work, you will love our**[** newsletter..**](https://marktechpost-newsletter.beehiiv.com/subscribe) Don’t Forget to join our **[50k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)**

**[[Upcoming Event- Oct 17 202] RetrieveX – The GenAI Data Retrieval Conference (Promoted)](https://www.retrievex.co/application?utm_source=print&utm_medium=markettechpost&utm_campaign=retrievex&utm_term=speakers&utm_content=SIZE)**
