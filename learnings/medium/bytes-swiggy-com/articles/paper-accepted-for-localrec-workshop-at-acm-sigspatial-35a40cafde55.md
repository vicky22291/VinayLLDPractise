---
title: "Paper accepted for LocalRec workshop at ACM SIGSPATIAL"
author: "SwiggyBytes"
date: "Oct 8, 2021"
url: "https://bytes.swiggy.com/paper-accepted-for-localrec-workshop-at-acm-sigspatial-35a40cafde55"
tags: ['Swiggy Research', 'Maps', 'Points Of Interest', 'Location', 'Algorithms']
---

# Paper accepted for LocalRec workshop at ACM SIGSPATIAL

![Photo by GeoJango Maps on Unsplash](../images/30e4c58de91bc071.jpg)
*Photo by GeoJango Maps on Unsplash*

Abhinav Ganesan, Anubhav Gupta, Jose Mathew are the authors of the paper “Mining Points of Interest via Address Embeddings: An Unsupervised Approach” which has been accepted in the proceedings of the 5th ACM SIGSPATIAL Workshop on Location-Based Recommendations, Geosocial Networks, and Geoadvertising.

The online workshop is on Nov 2 and details are here [https://localrec.github.io/2021/](https://localrec.github.io/2021/)

Sharing an abstract of the paper below :

Digital maps are commonly used across the globe for exploring places that users are interested in, commonly referred to as points of interest (PoI). In online food delivery platforms, PoIs could represent any major private compounds where customers could order from such as hospitals, residential complexes, office complexes, educational institutes and hostels. In this work, we propose an endto-end unsupervised system design for obtaining polygon representations of PoIs (PoI polygons) from address locations and address texts. We preprocess the address texts using locality names and generate embeddings for the address texts using a deep learning-based architecture, viz. RoBERTa, trained on our internal address dataset. The PoI candidates are identified by jointly clustering the anonymised customer phone GPS locations (obtained during address onboarding) and the embeddings of the address texts. The final list of PoI polygons is obtained from these PoI candidates using novel post-processing steps that involve density-based cluster refinement and graph-based technique for cluster merging. This algorithm identified 74.8 % more PoIs than those obtained using the Mummidi-Krumm baseline algorithm run on our internal dataset. We define area-based precision and recall metrics to evaluate the performance of the algorithm. The proposed algorithm achieves a median area precision of 98 %, a median recall of 8 %, and a median F-score of 0.15. In order to improve the recall of the algorithmic polygons, we post-process them using building footprint polygons from the OpenStreetMap (OSM) database. The post-processing algorithm involves reshaping the algorithmic polygon using intersecting polygons and closed private roads from the OSM database, and accounting for intersection with public roads on the OSM database. We achieve a median area recall of 70 %, a median area precision of 69 %, and a median F-score of 0.69 on these post-processed polygons. The ground truth polygons for the evaluation of the metrics were obtained using manual validation of the algorithmic polygons obtained from the Mummidi-Krumm baseline approach. These polygons are not used to train the proposed algorithm pipeline, and hence, the algorithm is unsupervised.

---
**Tags:** Swiggy Research · Maps · Points Of Interest · Location · Algorithms
