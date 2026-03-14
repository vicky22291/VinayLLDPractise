---
title: "Nvidia AI Released Llama-Minitron 3.1 4B: A New Language Model Built by Pruning and Distilling Llama 3.1 8B"
date: "2024-08-16T12:36:44"
modified: "2025-01-12T11:59:18"
url: "https://www.marktechpost.com/2024/08/16/nvidia-ai-released-llama-minitron-3-1-4b-a-new-language-model-built-by-pruning-and-distilling-llama-3-1-8b/"
slug: "nvidia-ai-released-llama-minitron-3-1-4b-a-new-language-model-built-by-pruning-and-distilling-llama-3-1-8b"
---

![Nvidia AI Released Llama-Minitron 3.1 4B: A New Language Model Built by Pruning and Distilling Llama 3.1 8B](../images/02e76d28ff9ee6b8.png)

# Nvidia AI Released Llama-Minitron 3.1 4B: A New Language Model Built by Pruning and Distilling Llama 3.1 8B

> Nvidia has just announced a new release in language models, but this time, a small language model: the Llama-3.1-Minitron 4B model. This means it is one of the major steps in the continuous evolution of language models, combining the efficiency of large-scale models with smaller models through cutting-edge techniques such as pruning and knowledge distillation. […]

Nvidia has just announced a new release in language models, but this time, a small language model: the [**Llama-3.1-Minitron 4B**](https://developer.nvidia.com/blog/how-to-prune-and-distill-llama-3-1-8b-to-an-nvidia-llama-3-1-minitron-4b-model/) model. This means it is one of the major steps in the continuous evolution of language models, combining the efficiency of large-scale models with smaller models through cutting-edge techniques such as pruning and knowledge distillation.

The Llama-3.1-Minitron 4B model is the distilled and pruned version of the bigger Llama-3.1 8B sister model. To create this smaller model from the original 8B model, Nvidia used structured pruning in the depth and width directions. Pruning is a technique that deletes less important layers or neurons of the network to reduce model size and complexity while retaining its performance. In this case, Nvidia performed the depth pruning by removing 16 layers from the model and downsizing it from an 8B to a 4B model. Another technique applied is width pruning through trimming embedding dimensions and MLP intermediate.

Besides pruning, Nvidia also applied classical distillation to enhance the efficiency of Llama-3.1-Minitron 4B. Knowledge distillation is a process whereby a smaller model, the student, is trained to mimic the behavior of a larger and more complex one, the teacher. In this way, much of the predictive power of the original model is preserved in the smaller model, but it is faster and more frugal in terms of resources. Nvidia has combined this with the distillation technique and pruning, making sure that the retrained model of 4B is high-performing and is well-spent in larger models.

![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXekbi-AFyba53WXI5LUsvy5OAzNLK074rEFWcPbEVQaA_ptS6PCnT4FGbU2cF8Wr4rppQ5F-BLhOOJMhVRRpr9bO-LasCMaZLvjY_Rdx6xe3kqcc48ske3tGBneaFLqu5TZzcKhhloem9nNObVlaiLdWfg?key=26kW3SCxZOHCUL-b5TW6aQ)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXekbi-AFyba53WXI5LUsvy5OAzNLK074rEFWcPbEVQaA_ptS6PCnT4FGbU2cF8Wr4rppQ5F-BLhOOJMhVRRpr9bO-LasCMaZLvjY_Rdx6xe3kqcc48ske3tGBneaFLqu5TZzcKhhloem9nNObVlaiLdWfg?key=26kW3SCxZOHCUL-b5TW6aQ)*[**Image Source**](https://developer.nvidia.com/blog/how-to-prune-and-distill-llama-3-1-8b-to-an-nvidia-llama-3-1-minitron-4b-model/)*

The Llama-3.1-Minitron 4B model excels in various benchmarks, producing competitive performance against larger state-of-the-art open-source models. It highly outperforms many other [small language models](https://www.marktechpost.com/2025/01/12/what-are-small-language-models-slms/) in most domains, like Minitron 4B, Phi-2 2.7B, Gemma2 2.6B, and Qwen2-1.5B. Extensive benchmarking has proven this model’s effectiveness in terms of better accuracy and efficiency for reasoning, coding, and math.

One of the biggest advantages of the Llama-3.1-Minitron 4B model lies in its ability to compete equally well, yet it’s resource-efficient. It uses a fraction of the number of training tokens required by training from scratch, up to 40 times smaller. This translates to considerable compute cost savings. It makes this a very appealing option to deploy in scenarios where there might be limits to computational resources to deploy large-scale language models.

![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXea_sEmcjxvtGlYoNkx96zoTKb4xgqM2qAwQwRJKqhkeLriOWxkaHVAbsPG7F8NC25IGAZAWRmG1Ppyvicfq_--q3LsXjpCTSnrD-Vldc9EuVm6Em3wEJsDXrzVC5fgLsQjsMWH4UcrCp1kYwGje_9lx2w?key=26kW3SCxZOHCUL-b5TW6aQ)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXea_sEmcjxvtGlYoNkx96zoTKb4xgqM2qAwQwRJKqhkeLriOWxkaHVAbsPG7F8NC25IGAZAWRmG1Ppyvicfq_--q3LsXjpCTSnrD-Vldc9EuVm6Em3wEJsDXrzVC5fgLsQjsMWH4UcrCp1kYwGje_9lx2w?key=26kW3SCxZOHCUL-b5TW6aQ)*[**Image Source**](https://developer.nvidia.com/blog/how-to-prune-and-distill-llama-3-1-8b-to-an-nvidia-llama-3-1-minitron-4b-model/)*

Nvidia has further optimized the Llama-3.1-Minitron 4B model to deploy it using its TensorRT-LLM toolkit, which enhances its inference performance. For instance, the model’s throughput in FP8 precision for various cases increased to 2.7x higher than the original Llama 3.1 8B model. The additional optimization performed on Llama-3.1-Minitron 4B renders this model extremely powerful and efficient, easily applicable in many domains.

![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXf-botkLUUMINR9DZr5T0TPDDdS5VF07i2euJPEnZOs7FZTmuai1PNuwpjGbvD3Pi2LCuLt6SgIRrcjO8mXXOkPGT0EQnpOwfawvVLHIODLNkc4sXcc4Q_ifpaL77c4eOySI8tzwF0nUuNo3pSmVXtvVKVI?key=26kW3SCxZOHCUL-b5TW6aQ)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXf-botkLUUMINR9DZr5T0TPDDdS5VF07i2euJPEnZOs7FZTmuai1PNuwpjGbvD3Pi2LCuLt6SgIRrcjO8mXXOkPGT0EQnpOwfawvVLHIODLNkc4sXcc4Q_ifpaL77c4eOySI8tzwF0nUuNo3pSmVXtvVKVI?key=26kW3SCxZOHCUL-b5TW6aQ)*[**Image Source**](https://developer.nvidia.com/blog/how-to-prune-and-distill-llama-3-1-8b-to-an-nvidia-llama-3-1-minitron-4b-model/)*

In conclusion, Nvidia’s release of the Llama-3.1-Minitron 4B model is a huge leap in the creation of [LLMs](https://www.marktechpost.com/2025/01/11/what-are-large-language-model-llms/). Thus, the model designed by Nvidia has achieved good performance while being resource-efficient; hence, it is very useful in many NLP tasks. The Llama-3.1-Minitron 4B model will become part of Nvidia’s Hugging Face collection and add to the shifting landscape of powerful, freely available AI models.

---

Check out the **[Model Card](https://huggingface.co/nvidia/Minitron-4B-Base) and [Details](https://developer.nvidia.com/blog/how-to-prune-and-distill-llama-3-1-8b-to-an-nvidia-llama-3-1-minitron-4b-model/).** All credit for this research goes to the researchers of this project. Also, don’t forget to follow us on **[Twitter](https://twitter.com/Marktechpost)** and join our **[Telegram Channel](https://pxl.to/at72b5j)** and [**LinkedIn Gr**](https://www.linkedin.com/groups/13668564/)[**oup**](https://www.linkedin.com/groups/13668564/). **If you like our work, you will love our**[** newsletter..**](https://marktechpost-newsletter.beehiiv.com/subscribe)

Don’t Forget to join our **[48k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)**

**Find Upcoming [AI Webinars here](https://www.marktechpost.com/ai-webinars-list-llms-rag-generative-ai-ml-vector-database/)**

---

> [Arcee AI Introduces Arcee Swarm: A Groundbreaking Mixture of Agents MoA Architecture Inspired by the Cooperative Intelligence Found in Nature Itself](https://www.marktechpost.com/2024/08/15/arcee-ai-introduces-arcee-swarm-a-groundbreaking-mixture-of-agents-moa-architecture-inspired-by-the-cooperative-intelligence-found-in-nature-itself/)
