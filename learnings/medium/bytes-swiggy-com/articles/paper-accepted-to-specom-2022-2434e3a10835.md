---
title: "Paper accepted to SPECOM 2022"
author: "SwiggyBytes"
date: "Sep 26, 2022"
url: "https://bytes.swiggy.com/paper-accepted-to-specom-2022-2434e3a10835"
tags: ['Swiggy Research', 'Machine Learning', 'Speech Technology', 'Hyperlocal Delivery', 'Indian Languages']
---

# Paper accepted to SPECOM 2022

Our paper ‘A multi-modal approach to mining intent from code-mixed Hindi-English calls in the hyperlocal-delivery domain’ has been accepted to the 24TH INTERNATIONAL CONFERENCE ON SPEECH AND COMPUTER (SPECOM’22), November 14–16, 2022. The authors of the paper are Jose Mathew, Pranjal Sahu*, Bhavuk Singhal*, Aniket Joshi*, Krishna Medikonda, and Jairaj Sathyanarayana.

Conference link: [https://specom.nw.ru/2022/](https://specom.nw.ru/2022/)

![image](../images/57b8bfd2e554352a.png)

Abstract: In this work we outline an approach to mine insights from calls between delivery partners (DP) and customers involved in hyperlocal food delivery in India. Incorrect addresses/ locations or other impediments prompt the DPs to call customers leading to suboptimal experiences like breaches in the promised arrival-time, cancellation, fraud, etc. We demonstrate an end-to-end system that utilizes a multi- modal approach where we combine data across speech, text and geospatial domains to extract the intent behind these calls. To transcribe calls to text, we develop an Automatic Speech Recognition (ASR) engine that works in the Indian context where the calls are typically highly code-mixed (in our case Hindi and English) along with variations in dialects and pronunciations. Additionally in the hyperlocal delivery space, the calls are also corrupted by high levels of background noise due to the nature of the business. Starting with Wav2Vec2.0 as the base we carried out a series of data and model based experiments to progressively reduce the WER from 85.30% to 31.17%. The transcripts from the ASR engine are encoded into embeddings by adapting an IndicBERT based model. Features extracted from the geospatial markers of calls are concatenated with the embeddings and passed through an XGBoost classification head to classify calls into one of three intents. Through ablation studies we show incremental improvements attributable to signals from different modalities. The winning multi-modal model has a macro average precision of 68.33% which is a 29.3pp lift over the baseline not utilizing all the modalities.

* As interns

---
**Tags:** Swiggy Research · Machine Learning · Speech Technology · Hyperlocal Delivery · Indian Languages
