---
title: "Paper on multi-objective optimization accepted to the UDML workshop at the ICDM’20 conference"
author: "SwiggyBytes"
date: "Apr 27, 2021"
url: "https://bytes.swiggy.com/paper-on-multi-objective-optimization-accepted-to-the-udml-workshop-at-the-icdm20-conference-a77e8b260b38"
tags: ['Ranking', 'Swiggy Research', 'Recommendations', 'Data Mining', 'Data']
---

# Paper on multi-objective optimization accepted to the UDML workshop at the ICDM’20 conference

![image](../images/b6336c8850b61016.png)

Our team comprising Abhay Shukla, Dipyaman Bannerjee and Jairaj Sathyanarayana presented a paper titled “Sample-Rank: Weak Multi-Objective Recommendations Using Rejection Sampling” at the 3rd International Workshop on Utility-Driven Mining and Learning (UDML 2020) held in conjunction with the 20th IEEE International Conference on Data Mining (ICDM 2020). Below is the abstract of the paper which can be accessed [here](https://arxiv.org/pdf/2008.10277.pdf).

Online food ordering marketplaces are multi-stakeholder systems where recommendations impact the experience and growth of each participant in the system. A recommender system in this setting has to encapsulate the objectives and constraints of different stakeholders in order to find utility of an item for recommendation. Constrained optimization based approaches to this problem typically involve complex formulations and have high computational complexity in production settings involving millions of entities. Simplifications and relaxation techniques (for example, scalarization) help but introduce sub-optimality and can be time-consuming due to the amount of tuning needed. In this paper, we introduce a method involving multi-goal sampling followed by ranking for user relevance (Sample-Rank), to nudge recommendations towards multi-objective (MO) goals of the marketplace. The proposed method’s novelty is that it reduces the MO recommendation problem to sampling from a desired multi-goal distribution then using it to build a production-friendly learning-to-rank (LTR) model. In offline experiments we show that we are able to bias recommendations towards MO criteria with acceptable tradeoffs in metrics like AUC and NDCG. We also show results from a large-scale online A/B experiment where this approach gave a statistically significant lift of 2.64% in average revenue per order (RPO) (objective #1) with no drop in conversion rate (CR) (objective #2) while holding the average last-mile traversed flat  
(objective #3), vs. the baseline ranking method. This method also significantly reduces time to model development and deployment in MO settings and allows for trivial extensions to more objectives and other types of LTR models.

---
**Tags:** Ranking · Swiggy Research · Recommendations · Data Mining · Data
