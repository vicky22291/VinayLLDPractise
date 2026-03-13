---
title: "Paper on large scale weak supervision for fraud detection accepted to the Applied DS track at ECML PKDD’21"
author: "SwiggyBytes"
date: "Jul 2, 2021"
url: "https://bytes.swiggy.com/paper-on-large-scale-weak-supervision-for-fraud-detection-accepted-to-the-applied-ds-track-at-ecml-6e8cb7b677e8"
tags: ['Swiggy Research', 'Data Science', 'Fraud Detection', 'Fraud', 'Machine Learning']
---

# Paper on large scale weak supervision for fraud detection accepted to the Applied DS track at ECML PKDD’21

Jose Mathew, Meghana Negi, Rutvik Vijjali and Jairaj Sathyanarayana will present their paper titled “DeFraudNet: An End-to-End Weak Supervision Framework to Detect Fraud in Online Food Delivery” in the Applied Data Science track at the ECML PKDD’21 conference to be held Sept 13–17. Below is an abstract of the paper. A blog post version of the paper appears [here](./defraudnet-an-end-to-end-weak-supervision-framework-to-detect-fraud-in-online-food-delivery-22366ddce461.md).

![image](../images/fab34f044778bf7e.png)

Detecting abusive and fraudulent claims is one of the key challenges in online food delivery. This is further aggravated by the fact that it is not practical to do reverse-logistics on food unlike in e-commerce. This makes the already-hard problem of harvesting labels for fraud even harder because we cannot confirm if the claim was legitimate by inspecting the item(s). Using manual effort to analyze transactions to generate labels is often expensive and time-consuming. On the other hand, typically, there is a wealth of ‘noisy’ information about what constitutes fraud, in the form of customer service interactions, weak and hard rules derived from data analytics, business intuition and domain understanding.

In this paper, we present a novel end-to-end framework for detecting fraudulent transactions based on large-scale label generation using weak supervision. We directly use Stanford AI Lab’s (SAIL) Snorkel and tree based methods to do manual and automated discovery of labeling functions, to generate weak labels. We follow this up with an auto-encoder reconstruction-error based method to reduce label noise. The final step is a discriminator model which is an ensemble of an MLP and an LSTM. In addition to cross-sectional and longitudinal features around customer history, transactions, we also harvest customer embeddings from a Graph Convolution Network (GCN) on a customer-customer relationship graph, to capture collusive behavior. The final score is thresholded and used in decision making.

This solution is currently deployed for real-time serving and has yielded a 16 percentage points’ improvement in recall at a given precision level. These results are against a baseline MLP model based on manually labeled data and are highly significant at our scale. Our approach can easily scale to additional fraud scenarios or to use-cases where ‘strong’ labels are hard to get but weak labels are prevalent.

---
**Tags:** Swiggy Research · Data Science · Fraud Detection · Fraud · Machine Learning
