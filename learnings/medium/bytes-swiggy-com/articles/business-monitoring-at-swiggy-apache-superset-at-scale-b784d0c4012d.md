---
title: "Business Monitoring at Swiggy [Part 1]: Apache Superset at Scale"
author: "Sundaram Dubey"
date: "Mar 31, 2025"
url: "https://bytes.swiggy.com/business-monitoring-at-swiggy-apache-superset-at-scale-b784d0c4012d"
---

# Business Monitoring at Swiggy [Part 1]: Apache Superset at Scale

![image](../images/62f7649bd7ff3b48.png)

At Swiggy, data-driven decision-making is at the core of our Business and operations. From optimizing delivery routes to understanding customer preferences, new feature **performance**, and other scenarios like rainy days, the need for real-time insights **has never been greater**. As Swiggy scaled, so did the need for an efficient and scalable business monitoring solution. Enter **Apache Superset**, an open-source BI (Business Intelligence) platform that became a key component of our monitoring stack.

In this blog, we’ll walk through how we leveraged **Apache Superset** to monitor **Swiggy’s business metrics at scale**, customize it to meet our unique challenges, and actively use it to drive operational insights. We won’t dive into the technical specifics of Apache Superset itself, but instead, focus on how it serves as a critical tool for our monitoring needs and what we’ve built on top of it to make it even more powerful for our organization.

## Why Apache Superset?

- **Open Source and Community: **Apache Superset is open source and is one of the oldest BI tools with the largest communities. It has a very vibrant[ community](https://superset.apache.org/community), with ~65k stars on Github, and continuous development and releases.
- **Customization**: It’s open-source, allowing us to tweak and extend its…
