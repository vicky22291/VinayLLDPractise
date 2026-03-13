---
title: "Paper accepted to CIKM 2022"
author: "SwiggyBytes"
date: "Aug 9, 2022"
url: "https://bytes.swiggy.com/paper-accepted-to-cikm-2022-7e77f62fde6d"
tags: ['Swiggy Research']
---

# Paper accepted to CIKM 2022

![image](../images/cfd2bb0776ed4596.png)

Our paper ‘Mining Entry Gates for Points of Interest’ has been accepted ifor presentation at CIKM’22 Short Paper track. October 17–21, 2022. The authors of the paper are Abhinav Ganesan, Tanya Khanna, Jose Mathew and Swiggy DS alum Kranthi Mitra.

Conference link: [https://www.cikm2022.org/](https://www.cikm2022.org/)

Abstract: In this paper, we propose two algorithms for identifying entry gates for Points of Interest (PoIs) using polygon representations of the PoIs (PoI polygons) and the Global Positioning System (GPS) trajectories of the Delivery Partners (DPs) obtained from their smartphones in the context of online food delivery platforms. PoIs include residential complexes, office complexes, and educational institutes where customers can order from. Identifying entry gates of PoIs helps avoid delivery hassles by routing the DPs to the nearest entry gates for customers within the PoIs. The DPs mark ‘reached’ on their smartphone applications when they reach the entry gate or the parking spot of the PoI. However, it is not possible to ensure compliance, and the ‘reached’ locations are dispersed throughout the PoI. The first algorithm is based on density-based clustering of GPS traces where the DPs mark ‘reached’. The clusters that overlap with the PoI polygon as measured by a metric that we propose, namely Cluster Fraction in Polygon (CFIP), are declared as entry gate clusters. The second algorithm obtains the entry gate clusters as density-based clustering of intersections of GPS trajectories of the DPs with the PoI polygon edges. The entry gates are obtained as median centroids of the entry gate clusters for both the algorithms which are then snapped to the nearest polygon edge in the case of the first algorithm. We evaluate the algorithms for a few thousand PoIs across 9 large cities in India using appropriately defined precision and recall metrics. For single-gate PoIs, we obtain a mean precision of 84%, a mean recall of 77%, and an average haversine distance error of 14.7 meters for the first algorithm. For the second algorithm, the mean precision is the same as the first algorithm while the recall obtained is 78% and the average haversine distance error is 14.3 meters. The algorithmically identified gates were evaluated by manual validation. To the best of our knowledge, this is the first published work with metrics that solves for a “last-last mile” entity of digital maps for India, i.e., the entry gates.

---
**Tags:** Swiggy Research
