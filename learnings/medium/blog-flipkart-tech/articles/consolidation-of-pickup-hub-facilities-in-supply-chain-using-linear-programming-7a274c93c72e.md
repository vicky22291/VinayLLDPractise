---
title: "Pickup Hubs — Optimization"
subtitle: "Consolidation of Pickup Hub facilities in supply chain using Linear Programming"
author: "Samir Kumar Pradhan"
url: "https://blog.flipkart.tech/consolidation-of-pickup-hub-facilities-in-supply-chain-using-linear-programming-7a274c93c72e"
tags: ['Linear Programming', 'Optimization', 'Supply Chain', 'Network Design', 'Flipkart Ph Optimisation']
---

# Pickup Hubs — Optimization

> Consolidation of Pickup Hub facilities in supply chain using Linear Programming

## Flipkart Pickup Hubs — a quick look!

Flipkart’s Ekart handles the supply chain operations end-to-end for both Business to Business (B2B) and Business to Customer (B2C). When you purchase a product from[ flipkart.com](http://flipkart.com/), it is stored in Flipkart’s warehouse (FBF — Fulfilled by Flipkart) or the Seller’s warehouse ( (NFBF — Non-Fulfilled by Flipkart).

Accordingly, Ekart’s supply chain has two components in its supply chain: — FBF and NFBF.

For NFBF, Pickup Hubs (PH) are the operational centers for the shipments. These PHs are setup in multiple locations across the city.

In the forward shipment process, the wish masters:

For all forward shipments, rarely the loads enter the PH, as most cross-dock sorting happens outside the facility. In the return process, the return shipments arrive unsorted from the Mother Hubs (MH) and enter the PH facility for sorting per seller. The wish masters take them to the Sellers during their forward shipments pickup trips.

As the space requirement at PH is primarily to sort the return loads, the PHs are under-utilised for the forward processes. In a move to optimize the space utilization, Flipkart set up a new facility with reduced rental cost, called Return Sortation Center (RSC) in the outskirts of the city. This move reduces the required area in the PHs across all the facilities and provides scope for shutting down a few PHs. In this article, we explain how we analytically solve for this optimized space utilization requirement.

## How do we solve our analytical problem?

The area required in the PHs across the facilities is lesser than the total space. We need to determine the optimal number of PHs needed, with a guardrail for distance between seller locations and PHs.

We convert this problem statement into a pincode-PH allocation problem, leveraging integer linear programming in which we minimize:

This method of choosing the PHs to be shut down has a potential to save fixed costs to the tune of a few crores for Delhi and Surat, per our calculations. We also save labor costs because of fewer sorters.

## Calculating Area usage

In the non-large NFBF supply chain, most of the first mile PHs are co-located (share the same physical asset/location) with the last mile(LM) Delivery Hubs (DHs). When planning for infra, out of the total area, we first allocate area for PHs based on FM area requirement, then use the remaining area for DHs based on LM area requirement. If there is a deficit, we buy or rent facilities according to temporary or long-term requirements. These facilities incur high rental costs as they are usually in central locations of the city, where they are closer to customers for faster delivery.

The area requirement is calculated as below:

**Note**: The SPI is a number specific to every facility based on its sorting efficiency.

## Reducing Area Requirement with a new Returns Sortation Center

The following levers reduce the area requirement:

A new facility has been introduced in the supply chain called Returns Sortation Center (RSC) which is a large facility on the outskirts of the city near or co-located with the Mother Hub (MH). All return shipments from MH land at the RSC, and seller level sortation happens with separate bagging for high-volume sellers (daily average > 60 return shipments).

The bags for high-volume sellers are directly sent to the sellers or through the PH, but the former move reduces the area required for the PHs much more.

## Realizing the goodness of reduced area

We can harness the benefit of the reduced area in the following ways:

We selected Approach 2 because of the potential problems associated with Approach 1. It was solved using Integer Linear Programming where we can change the allocation of seller pincode to PH and minimize the used area.

## Minimizing the distance between the Pincode-PH pairs

Within this limited set of PHs, we change the allocation again to minimize the total distance between each of the pincode-PH pairs.

## Mathematical representation of the optimization

## Objective function 1

Min. ∑i pi Ai (Minimize total sqft occupied by PHs)

**Variables**

i: Corresponds to PHs

j: Corresponds to pincodes

pi: Variable for PH active (recommendation to open or close)

Ai: Total area of the PH in sqft

Aij: Allocation variable (between pincode — PH)

Dij: Distance between the pincode — PH

Tj: Distance threshold for the pincode

Rj: Required area for the pincode in sqft

**Constraints**

2. Each pin code should be allocated to 1 PH only.

3. Distance should be within the threshold for allocation.

4. There should be sufficient area available at each PH for the required area by allocating pincodes.

5. The PH in use variable for a PH is max of allocation for that PH i.e., a PH will be active if at least 1 pincode is allocated to it.

The max in above makes this a non-linear problem, so we convert it into linear using following constraint:

## Objective function 2

Min. ∑i ∑j aij Dij (Minimize total distance between the pincode-PH combinations)

**Variables**

i: corresponds to PHs

j: corresponds to pincodes

pi: variable for PH active (recommendation to open or close)

Ai: Total area of the PH in sqft

aij: Allocation variable (between pincode — PH)

Dij: Distance between the pincode — PH

Tj: Distance threshold for the pincode

Rj: Required area for the pincode in sqft

**Constraints**

2. Each pin code should be allocated to 1 PH only.

3. Distance should be within the threshold for allocation.

4. There should be sufficient area available at each PH for the required area by allocating pincodes.

We have these data points:

Based on above we calculate the following data points:

## Results

The model was run for Delhi NCR and yielded below results:

We need only 35% of the existing PHs for the reduced load which occupies only 43% of the existing space. The metric is without modifying the average seller pincode to PH distance.

The existing and recommended PHs are shown on the map below.

## Conclusion

In this paper, we considered the problem of selecting optimal PHs to maintain among a set of pre-existing ones based on a reduction in demand for space.

Most existing network planning tools in the industry can only create a fresh network based on demand size and locations which is difficult to implement on ground. Our method is uniquely suited for use cases where required space has a drop within an existing network.

At present, decisions regarding the network are made at the pincode level, so we cannot assign different sellers to different PHs within a single pincode. If the constraint allows changing the mapping at a seller or a seller group level, the model can be run at the corresponding granularity to benefit from the additional goodness in terms of choice of space/distance.

Also, the model currently does not take into consideration the space required outside the PH for cross-docking vehicles. It is expected to increase when more sellers direct their forward load to the PHs (because of shifting out of the closed PHs). In future iterations of the model, when this data would be available, we plan to add it as an additional constraint.

**_Acknowledgements_**_: Thanks Shailendra Kumar from Analytics and Chetan Kotyalkar, Krati Sharma, & Sourabh Sawant from the Supply Chain Design team for their valuable contributions, which have led to the success of this project._

---
**Tags:** Linear Programming · Optimization · Supply Chain · Network Design · Flipkart Ph Optimisation
