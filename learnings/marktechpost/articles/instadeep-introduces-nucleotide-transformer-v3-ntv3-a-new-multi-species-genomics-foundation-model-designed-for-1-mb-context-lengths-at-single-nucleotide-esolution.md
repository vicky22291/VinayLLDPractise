---
title: "InstaDeep Introduces Nucleotide Transformer v3 (NTv3): A New Multi-Species Genomics Foundation Model, Designed for 1 Mb Context Lengths at Single-Nucleotide Resolution"
date: "2025-12-23T22:53:14"
modified: "2025-12-23T23:04:33"
url: "https://www.marktechpost.com/2025/12/23/instadeep-introduces-nucleotide-transformer-v3-ntv3-a-new-multi-species-genomics-foundation-model-designed-for-1-mb-context-lengths-at-single-nucleotide-esolution/"
slug: "instadeep-introduces-nucleotide-transformer-v3-ntv3-a-new-multi-species-genomics-foundation-model-designed-for-1-mb-context-lengths-at-single-nucleotide-esolution"
---

![InstaDeep Introduces Nucleotide Transformer v3 (NTv3): A New Multi-Species Genomics Foundation Model, Designed for 1 Mb Context Lengths at Single-Nucleotide Resolution](../images/36c03360e0d58d5a.png)

# InstaDeep Introduces Nucleotide Transformer v3 (NTv3): A New Multi-Species Genomics Foundation Model, Designed for 1 Mb Context Lengths at Single-Nucleotide Resolution

> Genomic prediction and design now require models that connect local motifs with megabase scale regulatory context and that operate across many organisms. Nucleotide Transformer v3, or NTv3, is InstaDeep’s new multi species genomics foundation model for this setting. It unifies representation learning, functional track and genome annotation prediction, and controllable sequence generation in a single […]

Genomic prediction and design now require models that connect local motifs with megabase scale regulatory context and that operate across many organisms. Nucleotide Transformer v3, or NTv3, is InstaDeep’s new multi species genomics foundation model for this setting. It unifies representation learning, functional track and genome annotation prediction, and controllable sequence generation in a single backbone that runs on 1 Mb contexts at single nucleotide resolution.

Earlier Nucleotide Transformer models already showed that self supervised pretraining on thousands of genomes yields strong features for molecular phenotype prediction. The original series included models from 50M to 2.5B parameters trained on 3,200 human genomes and 850 additional genomes from diverse species. NTv3 keeps this sequence only pretraining idea but extends it to longer contexts and adds explicit functional supervision and a generative mode.

![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-23-at-10.38.20-PM-1.png)![](https://www.marktechpost.com/wp-content/uploads/2025/12/Screenshot-2025-12-23-at-10.38.20-PM-1.png)*https://huggingface.co/spaces/InstaDeepAI/ntv3*

### Architecture for 1 Mb genomic windows

NTv3 uses a U-Net style architecture that targets very long genomic windows. A convolutional downsampling tower compresses the input sequence, a transformer stack models long range dependencies in that compressed space, and a deconvolution tower restores base level resolution for prediction and generation. Inputs are tokenized at the character level over A, T, C, G, N with special tokens such as `<unk>`, `<pad>`, `<mask>`, `<cls>`, `<eos>`, and `<bos>`. Sequence length must be a multiple of 128 tokens, and the reference implementation uses padding to enforce this constraint. All public checkpoints use single base tokenization with a vocabulary size of 11 tokens.

The smallest public model, NTv3 8M pre, has about 7.69M parameters with hidden dimension 256, FFN dimension 1,024, 2 transformer layers, 8 attention heads, and 7 downsample stages. At the high end, NTv3 650M uses hidden dimension 1,536, FFN dimension 6,144, 12 transformer layers, 24 attention heads, and 7 downsample stages, and adds conditioning layers for species specific prediction heads.

### Training data 

The NTv3 model is pretrained on 9 trillion base pairs from the OpenGenome2 resource using base resolution masked language modeling. After this stage, the model is post trained with a joint objective that integrates continued self supervision with supervised learning on approximately 16,000 functional tracks and annotation labels from 24 animal and plant species.

### Performance and Ntv3 Benchmark

After post training NTv3 achieves state of the art accuracy for functional track prediction and genome annotation across species. It outperforms strong sequence to function models and previous genomic foundation models on existing public benchmarks and on the new Ntv3 Benchmark, which is defined as a controlled downstream fine tuning suite with standardized 32 kb input windows and base resolution outputs.

The Ntv3 Benchmark currently consists of 106 long range, single nucleotide, cross assay, cross species tasks. Because NTv3 sees thousands of tracks across 24 species during post training, the model learns a shared regulatory grammar that transfers between organisms and assays and supports coherent long range genome to function inference.

### From prediction to controllable sequence generation

Beyond prediction, NTv3 can be fine tuned into a controllable generative model via masked diffusion language modeling. In this mode the model receives conditioning signals that encode desired enhancer activity levels and promoter selectivity, and it fills masked spans in the DNA sequence in a way that is consistent with those conditions.

In experiments described in the launch materials, the team designs 1,000 enhancer sequences with specified activity and promoter specificity and validates them in vitro using STARR seq assays in collaboration with the Stark Lab. The results show that these generated enhancers recover the intended ordering of activity levels and reach more than 2 times improved promoter specificity compared with baselines.

### Comparison Table

DimensionNTv3 (Nucleotide Transformer v3)GENA-LMPrimary goalUnified multi species genomics foundation model for representation learning, sequence to function prediction and controllable sequence generationFamily of DNA language models for long sequences focused on transfer learning for many supervised genomic prediction tasksArchitectureU-Net style convolutional tower, transformer stack, deconvolutional tower, single base resolution language model, post trained versions add multi species conditioning and task specific heads BERT based encoder models with 12 or 24 layers and BigBird variants with sparse attention, extended further with recurrent memory transformer for long contexts Parameter scaleFamily spans 8M, 100M and 650M parametersBase models have 110M parameters and large models have 336M parameters, including BigBird variants at 110M Native context lengthUp to 1 Mb input at single nucleotide resolution for both pre trained and post trained modelsUp to about 4500 bp with 512 BPE tokens for BERT models and up to 36000 bp with 4096 tokens for BigBird models Extended context mechanismUses U-Net style convolutional tower to aggregate long range context before transformer layers while keeping single base resolution; context length is fixed at 1 Mb in the released checkpoints Uses sparse attention in BigBird variants plus recurrent memory transformer to extend effective context to hundreds of thousands of base pairs TokenizationCharacter level tokenizer over A, T, C, G, N and special tokens; each nucleotide is a tokenBPE tokenizer on DNA that maps to about 4500 bp for 512 tokens; two tokenizers are used, one on T2T only and one on T2T plus 1000G SNPs plus multispecies data Pretraining corpus sizeFirst stage pre training on OpenGenome2 with about 9 trillion base pairs from more than 128000 speciesHuman only models trained on pre processed human T2T v2 plus 1000 Genomes SNPs, about 480 × 10^9 base pairs, multispecies models trained on combined human and multispecies data, about 1072 × 10^9 base pairsSpecies coverageMore than 128000 species in OpenGenome2 pretraining and post training supervision from 24 animal and plant speciesHuman focused models plus taxon specific models for yeast, Arabidopsis and Drosophila and multispecies models from ENSEMBL genomes Supervised post training signalsAbout 16000 functional tracks across about 10 assay types and about 2700 tissues in 24 species, used to condition the backbone with discrete labels and to train functional heads Fine tuned on multiple supervised tasks, including promoters, splice sites, Drosophila enhancers, chromatin profiles and polyadenylation sites, with task specific heads on top of the LMGenerative capabilitiesCan be fine tuned into a controllable generative model using masked diffusion language modeling, used to design 1000 promoter specific enhancers that achieved more than 2× increased specificity in STARR seq assaysPrimarily used as a masked language model and feature extractor, supports sequence completion through MLM but the main publication focuses on predictive tasks rather than explicit controllable sequence design 

### Key Takeaways

- **NTv3 is a long range, multi species genomics foundation model**: It unifies representation learning, functional track prediction, genome annotation, and controllable sequence generation in a single U Net style architecture that supports 1 Mb nucleotide resolution context across 24 animal and plant species.

- **The model is trained on 9 trillion base pairs with joint self supervised and supervised objectives**: NTv3 is pretrained on 9 trillion base pairs from OpenGenome2 with base resolution masked language modeling, then post trained on more than 16,000 functional tracks and annotation labels from 24 species using a joint objective that mixes continued self supervision with supervised learning.

- **NTv3 achieves state of the art performance on the Ntv3 Benchmark**: After post training, NTv3 reaches state of the art accuracy for functional track prediction and genome annotation across species and outperforms previous sequence to function models and genomics foundation models on public benchmarks and on the Ntv3 Benchmark, which contains 106 standardized long range downstream tasks with 32 kb input and base resolution outputs.

- **The same backbone supports controllable enhancer design validated with STARR seq**: NTv3 can be fine tuned as a controllable generative model using masked diffusion language modeling to design enhancer sequences with specified activity levels and promoter selectivity, and these designs are validated experimentally with STARR seq assays that confirm the intended activity ordering and improved promoter specificity.

---

Check out the **[Repo](https://github.com/instadeepai/nucleotide-transformer?), [Model on HF](https://huggingface.co/spaces/InstaDeepAI/ntv3) and [Technical details](https://instadeep.com/research/paper/a-foundational-model-for-joint-sequence-function-multi-species-modeling-at-scale-for-long-range-genomic-prediction/?utm_source=Social&utm_medium=LinkedIn&utm_campaign=NTv3&utm_id=NTv3&utm_content=Paper)**. Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[100k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**. Wait! are you on telegram? **[now you can join us on telegram as well.](https://t.me/machinelearningresearchnews)**
