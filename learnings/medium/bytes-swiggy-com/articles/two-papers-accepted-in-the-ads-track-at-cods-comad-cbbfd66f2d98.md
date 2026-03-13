---
title: "Two Papers accepted in the ADS track at CODS-COMAD"
author: "SwiggyBytes"
date: "Oct 8, 2021"
url: "https://bytes.swiggy.com/two-papers-accepted-in-the-ads-track-at-cods-comad-cbbfd66f2d98"
tags: ['Swiggy Research', 'Delivery', 'Hyperlocal', 'Data Science', 'Machine Learning']
---

# Two Papers accepted in the ADS track at CODS-COMAD

Our team’s paper, “FoodNet: Simplifying Online Food Ordering with Contextual Food Combos” has been accepted for [CODS-COMAD ’22](https://cods-comad.in/), 5th Joint International Conference on Data Science & Management of Data, India. Congratulations to Rutvik Vijjali, Deepesh Bhageria, Ashay Tamhane, Mithun TM and Jairaj Sathyanarayana.

The paper by Gaurav Pawar, Abhinav Ganesan, Ritwik Moghe, Bharath Nayak, Tanya Khanna, Kranthi Mitra Adusimilli titled “Learning to Predict Two-Wheeler Travel Distance” was also accepted.

![Photo by Rowan Freeman on Unsplash](../images/5ae220de19aab136.jpg)
*Photo by Rowan Freeman on Unsplash*

Abstracts are below:

1. Abstract for ‘FoodNet: Simplifying Online Food Ordering with Contextual Food Combos’: Bundling complementary dishes into easy-to-order food combos is vital to providing a seamless food ordering experience. Manually curating combos across several thousands of restaurants and millions of dishes is neither scalable nor can be personalized. We propose FoodNet, an attention-based deep learning architecture with a monotonically decreasing constraint of diversity, to recommend personalized two-item combos from across different restaurants. In a large-scale evaluation involving 200 million candidate combos, we show that FoodNet outperforms the Transformer based model by 1.3%, the Siamese network based model by 13.6%, and the traditional Apriori baseline by 18.8% in terms of NDCG, which are significant improvements at our scale. We also present qualitative results to show the importance of attention and lattice layers in the proposed architecture.
2. Abstract for ‘Learning to Predict Two-Wheeler Travel Distance’: Estimating travel distance between two geographical locations is one of the primary services sought after by retail users of digital maps. Distance between two locations is also a fundamental requirement for online food ordering and delivery platforms which operate in a hyperlocal setting. The distances are used at enterprise scale at decision points such as deciding the set of restaurants shown to a customer, assignment of delivery partners (DPs) to customers, payout to DPs, and delivery fee for customers. The distance service APIs hosted by third-party maps service providers are often an inaccurate estimate of two-wheeler travel distance in India. The historical GPS trajectories of DPs which can be used as alternate sources of distance estimates are also noisy due to inherent noise in Global Position System (GPS) signal reception. Distance estimates from OpenStreetMap (OSM) are also error-prone due to crowd-sourced nature of the map. In this paper, we adopt a machine learning (ML) based approach to predict distance between location pairs by de-noising the noisy distance sources, viz. the OSM distance, the trajectory distance, and the third party maps distance. The de-noising is achieved by averaging out the noise in the nonsingular equivalence classes of the set of noisy distance estimates, where the equivalence classes arise from defining a “match” relation between the distances. The de-noised distance estimates are used as the target variables and their historical versions are used as features in a random forest model. We further design a distance usability criterion based on OSM distance that offers a reasonable trade-off between the Mean Absolute Error (MAE) and the model coverage, i.e., the fraction of DP trips for which the model prediction is used in our downstream systems. The proposed system achieves a 21.88 % reduction in the MAE as compared to OSM distance and 47.40 % reduction in MAE as compared to a third-party maps distance with a 52.44 % trip-wise coverage as evaluated on our internal dataset

---
**Tags:** Swiggy Research · Delivery · Hyperlocal · Data Science · Machine Learning
