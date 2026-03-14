---
title: "OpenAI has Released the ‘circuit-sparsity’: A Set of Open Tools for Connecting Weight Sparse Models and Dense Baselines through Activation Bridges"
date: "2025-12-13T19:01:30"
modified: "2025-12-13T19:01:43"
url: "https://www.marktechpost.com/2025/12/13/openai-has-released-the-circuit-sparsity-a-set-of-open-tools-for-connecting-weight-sparse-models-and-dense-baselines-through-activation-bridges/"
slug: "openai-has-released-the-circuit-sparsity-a-set-of-open-tools-for-connecting-weight-sparse-models-and-dense-baselines-through-activation-bridges"
---

![OpenAI has Released the ‘circuit-sparsity’: A Set of Open Tools for Connecting Weight Sparse Models and Dense Baselines through Activation Bridges](../images/fdf6b184f7cf7e4c.png)

# OpenAI has Released the ‘circuit-sparsity’: A Set of Open Tools for Connecting Weight Sparse Models and Dense Baselines through Activation Bridges

> OpenAI team has released their openai/circuit-sparsity model on Hugging Face and the openai/circuit_sparsity toolkit on GitHub. The release packages the models and circuits from the paper ‘Weight-sparse transformers have interpretable circuits‘. What is a weight sparse transformer? The models are GPT-2 style decoder only transformers trained on Python code. Sparsity is not added after training, […]

OpenAI team has released their `openai/circuit-sparsity` model on Hugging Face and the `openai/circuit_sparsity` toolkit on GitHub. The release packages the models and circuits from the paper **‘[Weight-sparse transformers have interpretable circuits](https://arxiv.org/pdf/2511.13653)**‘.

![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.27.51-PM-1.png)![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.27.51-PM-1.png)*https://arxiv.org/pdf/2511.13653*

### What is a weight sparse transformer?

The models are GPT-2 style decoder only transformers trained on Python code. Sparsity is not added after training, it is enforced during optimization. After each AdamW step, the training loop keeps only the largest magnitude entries in every weight matrix and bias, including token embeddings, and zeros the rest. All matrices maintain the same fraction of nonzero elements.

The sparsest models have approximately **1 in 1000** nonzero weights. In addition, the OpenAI team enforced mild activation sparsity so that about **1 in 4** node activations are nonzero, covering residual reads, residual writes, attention channels and MLP neurons.

Sparsity is annealed during training. Models start dense, then the allowed nonzero budget gradually moves toward the target value. This design lets the research team scale width while holding the number of nonzero parameters fixed, and then study the capability interpretability tradeoff as they vary sparsity and model size. The research team show that, for a given pretraining loss, circuits recovered from sparse models are roughly **16 times** smaller than those from dense models.

![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.30.02-PM-1.png)![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.30.02-PM-1.png)*https://arxiv.org/pdf/2511.13653*

### So, what is a sparse circuit?

The central object in this research work is a **sparse circuit**. The research team defines nodes at a very fine granularity, each node is a single neuron, attention channel, residual read channel or residual write channel. An edge is a single nonzero entry in a weight matrix that connects two nodes. Circuit size is measured by the geometric mean number of edges across tasks.

To probe the models, the research team built **20 simple Python next token binary tasks**. Each task forces the model to choose between 2 completions that differ in one token. **Examples include:**

- `single_double_quote`, predict whether to close a string with a single or double quote

- `bracket_counting`, decide between `]` and `]]` based on list nesting depth

- `set_or_string`, track whether a variable was initialized as a set or a string

For each task, they prune the model to find the smallest circuit that still achieves a target loss of **0.15** on that task distribution. Pruning operates at the node level. Deleted nodes are **mean ablated**, their activations are frozen to the mean over the pretraining distribution. A learned binary mask per node is optimized with a straight through style surrogate so that the objective trades off task loss and circuit size.

![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.27.51-PM-1.png)![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.27.51-PM-1.png)*https://arxiv.org/pdf/2511.13653*

---

### Example circuits, quote closing and counting brackets

The most compact example is the circuit for `single_double_quote`. Here the model must emit the correct closing quote type given an opening quote. The pruned circuit has **12 nodes and 9 edges**.

The mechanism is two step. In layer `0.mlp`, 2 neurons specialize:

- a **quote detector** neuron that activates on both `"` and `'`

- a **quote type classifier** neuron that is positive on `"` and negative on `'`

A later attention head in layer `10.attn` uses the quote detector channel as a key and the quote type classifier channel as a value. The final token has a constant positive query, so the attention output copies the correct quote type into the last position and the model closes the string correctly.

![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.33.15-PM-1.png)![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.33.15-PM-1.png)*https://arxiv.org/pdf/2511.13653*

`bracket_counting` yields a slightly larger circuit but with a clear algorithm. The embedding of `[` writes into several residual channels that act as **bracket detectors**. A value channel in a layer 2 attention head averages this detector activation over the context, effectively computing nesting depth and storing it in a residual channel. A later attention head thresholds this depth and activates a **nested list close** channel only when the list is nested, which leads the model to output `]]`.

A third circuit, for `set_or_string_fixedvarname`, shows how the model tracks the type of a variable called `current`. One head copies the embedding of `current` into the `set()` or `""` token. A later head uses that embedding as query and key to copy the relevant information back when the model must choose between `.add` and `+=`.

![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.34.14-PM-1.png)![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.34.14-PM-1.png)*https://arxiv.org/pdf/2511.13653*

![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.34.38-PM-1.png)![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.34.38-PM-1.png)*https://arxiv.org/pdf/2511.13653*

### Bridges, connecting sparse models to dense models

The research team also introduces **bridges** that connect a sparse model to an already trained dense model. Each bridge is an encoder decoder pair that maps dense activations into sparse activations and back once per sublayer. The encoder uses a linear map with an AbsTopK activation, the decoder is linear.

Training adds losses that encourage hybrid sparse dense forward passes to match the original dense model. This lets the research team perturb interpretable sparse features such as the quote type classifier channel and then map that perturbation into the dense model, changing its behavior in a controlled way.

![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.35.58-PM-1-998x1024.png)![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-13-at-6.35.58-PM-1-998x1024.png)*https://arxiv.org/pdf/2511.13653*

### What Exactly has OpenAI Team released?

The OpenAI team as released **`openai/circuit-sparsity`** model on **Hugging Face. **This is a **0.4B parameter** model tagged with `custom_code`, corresponding to `csp_yolo2` in the [research paper](https://arxiv.org/pdf/2511.13653). The model is used for the qualitative results on bracket counting and variable binding. It is licensed under Apache 2.0.

Copy CodeCopiedUse a different Browser
```
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer

if __name__ == "__main__":
    PROMPT = "def square_sum(xs):\n    return sum(x * x for x in xs)\n\nsquare_sum([1, 2, 3])\n"
    tok = AutoTokenizer.from_pretrained("openai/circuit-sparsity", trust_remote_code=True)
    model = AutoModelForCausalLM.from_pretrained(
        "openai/circuit-sparsity",
        trust_remote_code=True,
        torch_dtype="auto",
    )
    model.to("cuda" if torch.cuda.is_available() else "cpu")

    inputs = tok(PROMPT, return_tensors="pt", add_special_tokens=False)["input_ids"].to(
        model.device
    )
    with torch.no_grad():
        out = model.generate(
            inputs,
            max_new_tokens=64,
            do_sample=True,
            temperature=0.8,
            top_p=0.95,
            return_dict_in_generate=False,
        )

    print(tok.decode(out[0], skip_special_tokens=True))
``` :contentReference[oaicite:14]{index=14}  

```

### Key Takeaways

- **Weight sparse training, not post hoc pruning**: Circuit sparsity trains GPT-2 style decoder models with extreme weight sparsity enforced during optimization, most weights are zero so each neuron has only a few connections.

- **Small, task specific circuits with explicit nodes and edges**: The research team defines circuits at the level of individual neurons, attention channels and residual channels, and recovers circuits that often have tens of nodes and few edges for 20 binary Python next token tasks.

- **Quote closing and type tracking are fully instantiated circuits**: For tasks like `single_double_quote`, `bracket_counting` and `set_or_string_fixedvarname`, the research team isolate circuits that implement concrete algorithms for quote detection, bracket depth and variable type tracking, with the string closing circuit using 12 nodes and 9 edges.

- **Models and tooling on Hugging Face and GitHub**: OpenAI released the 0.4B parameter `openai/circuit-sparsity` model on Hugging Face and the full `openai/circuit_sparsity` codebase on GitHub under Apache 2.0, including model checkpoints, task definitions and a circuit visualization UI.

- **Bridge mechanism to relate sparse and dense models**: The work introduces encoder-decoder bridges that map between sparse and dense activations, which lets researchers transfer sparse feature interventions into standard dense transformers and study how interpretable circuits relate to real production scale models.

---

Check out the **[Paper](https://arxiv.org/abs/2511.13653) and [Model Weights](https://huggingface.co/openai/circuit-sparsity)**. Feel free to check out our **[GitHub Page for Tutorials, Codes and Notebooks](https://github.com/Marktechpost/AI-Tutorial-Codes-Included)**. Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[100k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**. Wait! are you on telegram? **[now you can join us on telegram as well.](https://t.me/machinelearningresearchnews)**
