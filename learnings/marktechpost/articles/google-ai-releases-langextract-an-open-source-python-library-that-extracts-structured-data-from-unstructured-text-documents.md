---
title: "Google AI Releases LangExtract: An Open Source Python Library that Extracts Structured Data from Unstructured Text Documents"
date: "2025-08-04T22:49:50"
modified: "2025-08-04T22:50:00"
url: "https://www.marktechpost.com/2025/08/04/google-ai-releases-langextract-an-open-source-python-library-that-extracts-structured-data-from-unstructured-text-documents/"
slug: "google-ai-releases-langextract-an-open-source-python-library-that-extracts-structured-data-from-unstructured-text-documents"
---

![Google AI Releases LangExtract: An Open Source Python Library that Extracts Structured Data from Unstructured Text Documents](../images/84bb9cb1607cddf7.png)

# Google AI Releases LangExtract: An Open Source Python Library that Extracts Structured Data from Unstructured Text Documents

> In today’s data-driven world, valuable insights are often buried in unstructured text—be it clinical notes, lengthy legal contracts, or customer feedback threads. Extracting meaningful, traceable information from these documents is both a technical and practical challenge. Google AI’s new open-source Python library, LangExtract, is designed to address this gap directly, using LLMs like Gemini to deliver […]

In today’s data-driven world, valuable insights are often buried in unstructured text—be it clinical notes, lengthy legal contracts, or customer feedback threads. Extracting meaningful, traceable information from these documents is both a technical and practical challenge. **Google AI’s new open-source Python library, [LangExtract](https://github.com/google/langextract), is designed to address this gap directly, using LLMs like Gemini to deliver powerful, automated extraction with traceability and transparency at its core.**

### Key Innovations of LangExtract

#### 1. Declarative and Traceable Extraction

LangExtract lets users define custom extraction tasks using natural language instructions and high-quality “few-shot” examples. This empowers developers and analysts to **specify exactly which entities, relationships, or facts to extract, and in what structure**. Crucially, every extracted piece of information is **tied directly back to its source text**—enabling validation, auditing, and end-to-end traceability.

#### 2. Domain Versatility

The library works not just in tech demos but in critical real-world domains—including health (clinical notes, medical reports), finance (summaries, risk documents), law (contracts), research literature, and even the arts (analyzing Shakespeare). Original use cases include automatic extraction of medications, dosages, and administration details from clinical documents, as well as relationships and emotions from plays or literature.

#### 3. Schema Enforcement with LLMs

Powered by Gemini and compatible with other LLMs, LangExtract enables **enforcement of custom output schemas** (like JSON), so results aren’t just accurate—they’re immediately usable in downstream databases, analytics, or AI pipelines. It solves traditional LLM weaknesses around hallucination and schema drift by grounding outputs to both user instructions and actual source text.

#### 4. Scalability and Visualization

- **Handles Large Volumes:** LangExtract efficiently processes long documents by chunking, parallelizing, and aggregating results.

- **Interactive Visualization:** Developers can generate interactive HTML reports, viewing each extracted entity with context by highlighting its location in the original document—making auditing and error analysis seamless.

- **Smooth Integration:** Works in Google Colab, Jupyter, or as standalone HTML files, supporting a rapid feedback loop for developers and researchers.

#### 5. Installation and Usage

**Install easily with pip:**

Copy CodeCopiedUse a different Browser
```
pip install langextract

```

**Example Workflow (Extracting Character Info from Shakespeare):**

Copy CodeCopiedUse a different Browser
```
import langextract as lx
import textwrap

# 1. Define your prompt
prompt = textwrap.dedent("""
Extract characters, emotions, and relationships in order of appearance.
Use exact text for extractions. Do not paraphrase or overlap entities.
Provide meaningful attributes for each entity to add context.
""")

# 2. Give a high-quality example
examples = [
    lx.data.ExampleData(
        text="ROMEO. But soft! What light through yonder window breaks? It is the east, and Juliet is the sun.",
        extractions=[
            lx.data.Extraction(extraction_class="character", extraction_text="ROMEO", attributes={"emotional_state": "wonder"}),
            lx.data.Extraction(extraction_class="emotion", extraction_text="But soft!", attributes={"feeling": "gentle awe"}),
            lx.data.Extraction(extraction_class="relationship", extraction_text="Juliet is the sun", attributes={"type": "metaphor"}),
        ],
    )
]

# 3. Extract from new text
input_text = "Lady Juliet gazed longingly at the stars, her heart aching for Romeo"

result = lx.extract(
    text_or_documents=input_text,
    prompt_description=prompt,
    examples=examples,
    model_id="gemini-2.5-pro"
)

# 4. Save and visualize results
lx.io.save_annotated_documents([result], output_name="extraction_results.jsonl")
html_content = lx.visualize("extraction_results.jsonl")
with open("visualization.html", "w") as f:
    f.write(html_content)

```

This results in structured, source-anchored JSON outputs, plus an interactive HTML visualization for easy review and demonstration.

### Specialized & Real-World Applications

- **Medicine**: Extracts medications, dosages, timing, and links them back to source sentences. Powered by insights from research conducted on accelerating medical information extraction, LangExtract’s approach is directly applicable to structuring clinical and radiology reports—improving clarity and supporting interoperability.

- **Finance & Law**: Automatically pulls relevant clauses, terms, or risks from dense legal or financial text, ensuring every output can be traced back to its context.

- **Research & Data Mining**: Streamlines high-throughput extraction from thousands of scientific papers.

The team even provides a demonstration called _RadExtract_ for structuring radiology reports—highlighting not just what was extracted, but exactly where the information appeared in the original input.

### How LangExtract Compares

FeatureTraditional ApproachesLangExtract Approach**Schema Consistency**Often manual/error-proneEnforced via instructions & few-shot examples**Result Traceability**MinimalAll output linked to input text**Scaling to Long Texts**Windowed, lossyChunked + parallel extraction, then aggregation**Visualization**Custom, usually absentBuilt-in, interactive HTML reports**Deployment**Rigid, model-specificGemini-first, open to other LLMs & on-premises

### In Summary

LangExtract presents a new era for extracting structured, actionable data from text—delivering:

- **Declarative, explainable extraction**

- **Traceable results backed by source context**

- **Instant visualization for rapid iteration**

- **Easy integration into any Python workflow**

---

Check out the **[GitHub Page](https://github.com/google/langextract)** and **[Technical Blog](https://developers.googleblog.com/en/introducing-langextract-a-gemini-powered-information-extraction-library/)_._** Feel free to check out our **[GitHub Page for Tutorials, Codes and Notebooks](https://github.com/Marktechpost/AI-Tutorial-Codes-Included)**. Also, feel free to follow us on **[Twitter](https://x.com/intent/follow?screen_name=marktechpost)** and don’t forget to join our **[100k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)** and Subscribe to **[our Newsletter](https://www.aidevsignals.com/)**.
