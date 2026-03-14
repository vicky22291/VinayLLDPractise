---
title: "AMD Releases AMD-135M: AMD’s First Small Language Model Series Trained from Scratch on AMD Instinct™ MI250 Accelerators Utilizing 670B Tokens "
date: "2024-09-28T11:00:07"
modified: "2024-09-28T11:00:16"
url: "https://www.marktechpost.com/2024/09/28/amd-releases-amd-135m-amds-first-small-language-model-series-trained-from-scratch-on-amd-instinct-mi250-accelerators-utilizing-670b-tokens/"
slug: "amd-releases-amd-135m-amds-first-small-language-model-series-trained-from-scratch-on-amd-instinct-mi250-accelerators-utilizing-670b-tokens"
---

![AMD Releases AMD-135M: AMD’s First Small Language Model Series Trained from Scratch on AMD Instinct™ MI250 Accelerators Utilizing 670B Tokens ](https://www.marktechpost.com/wp-content/uploads/2024/09/3BExb8VVT3yQNA_9FwM8-g.webp)

# AMD Releases AMD-135M: AMD’s First Small Language Model Series Trained from Scratch on AMD Instinct™ MI250 Accelerators Utilizing 670B Tokens 

> AMD has recently introduced its new language model, AMD-135M or AMD-Llama-135M, which is a significant addition to the landscape of AI models. Based on the LLaMA2 model architecture, this language model boasts a robust structure with 135 million parameters and is optimized for performance on AMD’s latest GPUs, specifically the MI250. This release marks a […]

AMD has recently introduced its new language model, [**AMD-135M**](https://huggingface.co/amd/AMD-Llama-135m) or [**AMD-Llama-135M**](https://huggingface.co/amd/AMD-Llama-135m), which is a significant addition to the landscape of AI models. Based on the LLaMA2 model architecture, this language model boasts a robust structure with 135 million parameters and is optimized for performance on AMD’s latest GPUs, specifically the MI250. This release marks a crucial milestone for AMD in its endeavor to establish a strong foothold in the competitive AI industry.

**Background and Technical Specifications**

The AMD-135M is built on the LLaMA2 model architecture and is integrated with advanced features to support various applications, particularly in text generation and language comprehension. The model is designed to work seamlessly with the Hugging Face Transformers library, making it accessible for developers and researchers. The model can handle complex tasks with a hidden size of 768, 12 layers (blocks), and 12 attention heads while maintaining high efficiency. The activation function used is the Swiglu function, and the layer normalization is based on RMSNorm. Its positional embedding is designed using the RoPE method, enhancing its ability to understand and generate contextual information accurately.

The release of this model is not just about the hardware specifications but also about the software and datasets that power it. AMD-135M has been pretrained on two key datasets: the SlimPajama and Project Gutenberg datasets. SlimPajama is a deduplicated version of RedPajama, which includes sources such as Commoncrawl, C4, GitHub, Books, ArXiv, Wikipedia, and StackExchange. The Project Gutenberg dataset provides access to a vast repository of classical texts, enabling the model to grasp various language structures and vocabularies.

**Key Features of AMD-135M**

AMD-135M has remarkable features that set it apart from other models in the market. Some of these key features include:

- **Parameter Size:** 135 million parameters, allowing for efficient processing and generation of text.

- **Number of Layers:** 12 layers with 12 attention heads for in-depth analysis and contextual understanding.

- **Hidden Size:** 768, offering the capability to handle various language modeling tasks.

- Attention Type: Multi-Head Attention, enabling the model to focus on different aspects of the input data simultaneously.

- **Context Window Size:** 2048, ensuring the model can effectively manage larger input data sequences.

- **Pretraining and Finetuning Datasets:** The SlimPajama and Project Gutenberg datasets are utilized for pretraining, and the StarCoder dataset is used for finetuning, ensuring comprehensive language understanding.

- **Training Configuration: **The model employs a learning rate 6e-4 with a cosine learning rate schedule, and it has undergone multiple epochs for effective training and finetuning.

**Deployment and Usage**

The AMD-135M can be easily deployed and used through the Hugging Face Transformers library. For deployment, users can load the model using the `LlamaForCausalLM` and the `AutoTokenizer` modules. This ease of integration makes it a favorable option for developers looking to incorporate language modeling capabilities into their applications. Additionally, the model is compatible with speculative decoding for AMD’s CodeLlama, further extending its usability for code generation tasks. This feature makes AMD-135M particularly useful for developers working on programming-related text generation or other NLP applications.

**Performance Evaluation**

The performance of AMD-135M has been evaluated using the lm-evaluation-harness on various NLP benchmarks, such as SciQ, WinoGrande, and PIQA. The results indicate the model is highly competitive, offering comparable performance to other models in its parameter range. For instance, it achieved a pass rate of approximately 32.31% on the Humaneval dataset using MI250 GPUs, a strong performance indicator for a model of this size. This shows that AMD-135M can be a reliable model for research and commercial applications in natural language processing.

![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXelCHcLb59eZZzfJ-DVeQ0mq5j4DAXMW0EMCCYWpXoa9LFW7h_D9aW0kTdjVmRgKMFpBwsk5Az-XeES0O6YCbf3lEH_BqUvLSI4pWeBfVO4YKkbCw7Rftee3Y1HAgP_OCJOu4GfPr8bcCwWl1OM9l7UaZM?key=evHpc682G3XTQySu12s5VQ)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXelCHcLb59eZZzfJ-DVeQ0mq5j4DAXMW0EMCCYWpXoa9LFW7h_D9aW0kTdjVmRgKMFpBwsk5Az-XeES0O6YCbf3lEH_BqUvLSI4pWeBfVO4YKkbCw7Rftee3Y1HAgP_OCJOu4GfPr8bcCwWl1OM9l7UaZM?key=evHpc682G3XTQySu12s5VQ)

In conclusion, the release of AMD-135M underscores AMD’s commitment to advancing AI technologies and providing accessible, high-performance models for the research community. Its robust architecture and advanced training techniques position AMD-135M as a formidable competitor in the rapidly evolving landscape of AI models.

---

Check out the **[Model on Hugging Face](https://www.amd.com/en/developer/resources/technical-articles/introducing-amd-first-slm-135m-model-fuels-ai-advancements.html?utm_source=organic&utm_medium=community&utm_campaign=blog&utm_id=amd35)** and **[Details](https://www.amd.com/en/developer/resources/technical-articles/introducing-amd-first-slm-135m-model-fuels-ai-advancements.html?utm_source=organic&utm_medium=community&utm_campaign=blog&utm_id=amd35)**. All credit for this research goes to the researchers of this project. Also, don’t forget to follow us on **[Twitter](https://twitter.com/Marktechpost)** and join our **[Telegram Channel](https://pxl.to/at72b5j)** and [**LinkedIn Gr**](https://www.linkedin.com/groups/13668564/)[**oup**](https://www.linkedin.com/groups/13668564/). **If you like our work, you will love our**[** newsletter..**](https://marktechpost-newsletter.beehiiv.com/subscribe)

Don’t Forget to join our **[50k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)**
