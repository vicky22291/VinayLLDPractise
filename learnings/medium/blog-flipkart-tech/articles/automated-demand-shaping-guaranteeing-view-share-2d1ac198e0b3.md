---
title: "Automated Demand Shaping Guaranteeing View Share"
author: "Raj Kumar Yadav"
date: "Feb 22, 2022"
url: "https://blog.flipkart.tech/automated-demand-shaping-guaranteeing-view-share-2d1ac198e0b3"
tags: ['Mgd', 'Demand Shaping', 'Ecommerce', 'Merchandising']
---

# Automated Demand Shaping Guaranteeing View Share

**Table of Contents**

## Introduction

_Demand Shaping_ is a strategy in eCommerce companies that helps match the market demand with the business plan. As part of the Demand Shaping usually, companies use various merchandising techniques such as product selection, price incentives, value proposition callout, and product substitution.

Content views through web traffic on an eCommerce website contribute to a fair share of online purchases, making it an additional merchandising technique. Different business teams own various categories in the product catalog, in a typical e-commerce setup. Each team competes for a time-sliced share of the website traffic to merchandise their products and meet the business goals in the specific time window. This requires the following manual processes:

These processes become voluminous, mundane, tedious, and sub-optimal when applied to many of the categories in Flipkart.

We have devised a pioneering, novel, and automated approach called ‘Minimum Goals Delivery (MGD)’ to resolve this content merchandizing challenge in Demand Shaping. MGD looks at this problem as a multi-objective optimization problem, where user preferences and business goals are optimized together. Hence, the system can help meet the business goals of category teams consistently with no manual intervention. In this approach, the category teams feed goals (as View share or Click share of supply) into a system and the algorithms handle the workflow.

## Deducing the Content Merchandising Challenge

A typical HomePage (HP) setup for a high traffic eCommerce destination such as Flipkart, has configurable slots, and each slot can contain specific types of content/banner.

A page is configured as ‘pinned’ or ‘optimized’ based on the content drivers, such as customer experience, business goals, and Flipkart objectives.

The content for an optimized slot is driven by algorithms that decide on the content based on what is best for Flipkart customers (aid customers in shopping journey), Flipkart level goal (e.g. GMV), Business level goal (e.g. view), content personalization, etc.

Business teams use the pinned slot on a page as a lever to shape the demand (view share) and meet their objectives. Decisions to display different content in near real-time on the pinned slots is a manual activity which is clearly the content merchandizing challenge that requires automation.

Using MGD as an opportunity to transfer control from human to machine ensures an optimized homepage and enhanced content merchandising to shape the Demand.

## Unleashing Minimum Goals Delivery as a solution

Minimum Goals Delivery (MGD) is a system that enables business teams to enter their goals as a _view_ percentage share of the ‘supply’ on HP and perform the periodic computation to _boost_ or _delimit_ content as required for each of the teams. This computation is based on the SLA for the data feed availability.

The core component of MGD receives data from multiple channels:

It is imperative to have feedback data flow with as minimal lag as possible to shape demand in near real-time. Currently, at Flipkart, we leverage our robust data platforms and compute the demand every 30–60 mins.

In further sections, let’s dive deeper into the tech stack, core algorithms, and plans for MGD.

### MGD High-Level Architecture — A glance

The page configuration is stored in **Page Wireframe Configuration.** Whenever a customer opens HomePage, **StoreFront Federator service** calls Page Wireframe Configuration to get the HomePage configuration.

For each slot, the **StoreFront Federator service** either assigns pre-defined banners or chooses the ML way. The **Machine Controlled Content Promotion **module has Machine learning models that select content from the multiple banners competing for the ‘optimized’ slot on the homepage using a multi-objective ranking function.

> Banner Score = α*engagement + β*units + γ*GMV (α,β,γ are model coefficients)(Engagement, units and GMV are probabilistic values computed using the model)

We have added a new objective to the existing multi-objective function to shape the view demand on the HomePage:

> New Banner Score = α*engagement + β*units + γ*GMV+ δ*views

MGD algorithm in the **MGD Compute service** uses the category team goals and category level views from the **Goals Tracker service** to output the Model coefficients. The updated model coefficients boost or delimit a category on the HomePage to shape the demand.

Flipkart HomePage gets more than a few billion requests in a day. The MGD uses Apache Spark pipelines to:

The App also registers customer feedback data into the Flipkart Data Warehouse after multiple hops.

### The MGD Core Algorithm

The MGD algorithm uses data points such as Under-achievement per category and category goals to curate content. The strategy is to boost the categories which are behind their goal achievement and delimit the categories which have over-achieved until that hour of the day.

**Calculate Under-achievement**

Under-achievement or lag in the category goal is the difference between the numerical value of the category’s view goal and the achieved view count.

> CategoryViewAchieved = CategoryViewCount / TotalViewCount * 100UnderAchievement = (1 — (CategoryViewAchieved / CategoryViewTarget))

_CategoryViewCount_ is an actual view count calculated in real-time. As we have goals in percentage, we first calculate the percentage of _CategoryViewAchieved_ and then use it to calculate the underachievement.

When a category has achieved its goals, underachievement will be negative, underachievement will get marked as zero, whereas when category view count is zero, underachievement is one.

**Compute Score**

The algorithm computes a score for each category, which is equal to the category goal multiplied by the category’s under-achievement.

> Score = Goal* UnderAchievement

The score represents the effort required to achieve the goal. The higher the score, the greater is the effort needed to achieve the goals.

**Normalize Score**

We used _Min-Max normalization_ to normalize the score.

> Normalized Score = (Score-Scoreₘ**ᵢ**ₙ / (Scoreₘ**ₐₓ**-Scoreₘ**ᵢ**ₙ)

Where:

> Scoreₘ**ₐₓ**= maximum score among all categories’ scoreScoreₘ**ᵢ**ₙ = minimum score among all categories’ score

**Calculate Category View Weight**

The score is then normalized between **minimum view weight** and **maximum view weight** to give **category view weight**. The category view weight is used in the ranking function to boost the category.

> Category View Weight = ViewWeightₘ**ᵢ**ₙ + (ViewWeightₘ**ₐₓ-**ViewWeightₘ**ᵢ**ₙ)*Normalized Score

Where:

> ViewWeightₘ**ₐₓ**= maximum value of view weightViewWeightₘ**ᵢ**ₙ = minimum value of view weight

At the start of the day, since under-achievement is 1 for all the categories, the category with the maximum goal will have the highest view weight. As the view values of achievements get closer to the goals, view weight gets close to 0.

### Experimentation and Results

MGD has been deployed in FK production for quite some time and we are tuning it further. In this section, we briefly talk about some of the initial results. We have onboarded over 50 category teams and tracked data for several consecutive BAU days.

On average, ~45% of the teams have achieved over 100% of their targets and ~33% of the teams have achieved below 70% of their targets. We have identified several tracks where we can work on pushing more teams towards achieving their view goals daily more consistently.

We have also implemented the Machine Optimized Demand Shaping on the Homepage of Flipkart during Sale Events, whose primary objective is to meet the GMV goals at the Flipkart level. The secondary objective is to optimize the category-level GMV to achieve individual team goals. Here, the flow remains similar to that of view share optimization.

We deployed MGD for events in the major sales of Flipkart 2021, and we found that ~90% of the teams are achieving over 70% of their goals and only ~11% of the teams are achieving less than 70% of their targets.

One of the major goals of MGD is to provide more content predictability than fully machine optimized content targeting, at the same time providing better click prediction than fully targeted content. We have found that CTR in MGD is at least 3 times higher than fully targeted content.

Upon deeper analysis, we call out the MGD algorithm as one area which can develop. The current algorithm is slightly biased towards teams with bigger targets, which negatively affects the achievement of the teams with smaller targets. We are working on addressing the bias to increase the achievement of teams with small targets. Also, It doesn’t account for goals met outside of MGD managed sections in the calculation which can be further improved upon.

## All well with MGD?

Certainly not. MGD has its fair share of issues and engineers have lessons we talk about in this section.

### Feedback delay

MGD needs to make near-real-time decisions, just like a human does with real-time view reports. Incorrect decisions during peak traffic hours can lead to one team over-achieving at the expense of others.

The view data is a typical clickstream-like system that goes through multiple hops. Each hop involves steps such as joining and transforming, which have their own SLA. This leads to an inherited delay of a few hours, which is beyond the portal’s SLA need to compute boost factors every 30–60 mins.

### Fallback Approach

We have introduced a fallback approach that triggers whenever there is a feedback data delay. In this approach, we use the content served to the page as a proxy source to get the data in real-time. Heuristic-based calculations help approximate the view information, such as expected views, for the served data.

To maintain the accuracy of the system the approximated views for the current feedback window are combined with the existing accurate view data of past windows to calculate the boosting factor.

Once the view data is available, the heuristic calculation factors can be further improved to provide view information with better accuracy.

### Disparate category objectives

Each category has different content types on the HomePage with varied objectives. For example, video content only focuses on views and not on conversions. Similarly, there are categories with good conversion probability, while others do not have conversion factors at all. It is a challenge to model these disparate objectives in a view-based common solution such as MGD. Presently, we are experimenting with ideas to keep page sections separate for these different objectives.

### The imbalance between organic engagement and business goals

MGD seems to promote chosen content for certain categories over others having organic objectives (engagement, conversion, etc.). This imposes the challenge of balancing the organic engagement and business goals of a few categories.

We are planning to display content with a higher probability of getting converted, without losing sight of business goals. Our solution is to implement guardrails in the lower limit parameters on the ranking function, which can discourage MGD from achieving business goals at the expense of user engagement.

## Way forward with MGD

At Flipkart, we have just started using MGD to optimize and control view and GMV share-based content-boost on a small section of Homepage (HP) for Demand Shaping.

While this works well for BAU and Sale days, to attain our goal of multi-objective optimization, we still need to implement the use case of units in the current flow. GMV and units take the front seat during sale events. Sale events cause a tremendous level of traffic and the behavior of users during sale events is considerably different.

Unlike view or click, where feedback is instantaneous, units are realized over a larger window with the amount of traffic that we are likely to see at an hourly rate. There could be multiple customer interactions over multiple user sessions before a customer purchases an item. The merchandised content card can itself contain products from multiple category teams. Similar to GMV, we track and aggregate the units achieved by the category every hour. We periodically calculate and tweak the weights based on how we are doing at Flipkart and the Category level.

The ultimate aim is to have multi-objective optimization for different business goals and let the system choose to optimize for views, GMV, units, etc. in a combined way.

The more we can learn from our findings and apply the same into machine-controlled automated business shaping, the better we can use content merchandising for Demand Shaping. Solving for units, we believe MGD can replace any human judgment not only on the HomePage but on any page, including category landing pages on the portal.

---
**Tags:** Mgd · Demand Shaping · Ecommerce · Merchandising
