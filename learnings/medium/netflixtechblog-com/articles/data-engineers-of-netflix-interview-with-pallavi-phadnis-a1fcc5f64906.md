---
title: "Data Engineers of Netflix — Interview with Pallavi Phadnis"
author: "Netflix Technology Blog"
date: "Oct 28, 2021"
url: "https://netflixtechblog.com/data-engineers-of-netflix-interview-with-pallavi-phadnis-a1fcc5f64906"
tags: ['Data Engineering', 'Big Data', 'Culture', 'Technology', 'Data']
---

# Data Engineers of Netflix — Interview with Pallavi Phadnis

This post is part of our “**_Data Engineers of Netflix_**” series, where our very own data engineers talk about their journeys to [**Data Engineering @ Netflix**](https://netflixtechblog.com/tagged/data-engineering).

![Pallavi Phadnis is a Senior Software Engineer at Netflix.](../images/125f69135e438464.jpg)
*Pallavi Phadnis is a Senior Software Engineer at Netflix.*

[**Pallavi Phadnis**](https://www.linkedin.com/in/pallavi-phadnis-75280b20/) is a Senior Software Engineer on the** Product Data Science and Engineering team**. In this post, Pallavi talks about her journey to Netflix and the challenges that keep the work interesting.

Pallavi received her master’s degree from Carnegie Mellon. Before joining Netflix, she worked in the advertising and e-commerce industries as a backend software engineer. In her free time, she enjoys watching Netflix and traveling.

**Her favorite shows: **Stranger Things, Gilmore Girls, and Breaking Bad.

## Pallavi, what’s your journey to data engineering at Netflix?

Netflix’s unique work culture and petabyte-scale data problems are what drew me to Netflix.

During earlier years of my career, I primarily worked as a backend software engineer, designing and building the backend systems that enable big data analytics. I developed many batch and real-time data pipelines using open source technologies for AOL Advertising and eBay. I also built online serving systems and microservices powering Walmart’s e-commerce.

> Those years of experience solving technical problems for various businesses have taught me that data has the power to maximize the potential of any product.

Before I joined Netflix, I was always fascinated by my experience as a Netflix member which left a great impression of Netflix engineering teams on me. **When I read Netflix’s **[**culture memo**](https://jobs.netflix.com/culture)** for the first time, I was impressed by how candid, direct and transparent the work culture sounded.** These cultural points resonated with me most: freedom and responsibility, high bar for performance, and no hiring of brilliant jerks.

Over the years, I followed the big data open-source community and Netflix tech blogs closely, and learned a lot about Netflix’s innovative engineering solutions and active contributions to the open-source ecosystem. In 2017, I attended the [Women in Big Data conference](https://www.youtube.com/watch?v=nscHk5H4xTY) at Netflix and met with several amazing women in data engineering, including our VP. At this conference, I also got to know my current team: Consolidated Logging (CL).

CL provides an end-to-end solution for logging, processing, and analyzing user interactions on Netflix apps from all devices. It is critical for fast-paced product innovation at Netflix since CL provides foundational data for personalization, A/B experimentation, and performance analytics. Moreover, its petabyte scale also brings unique engineering challenges. The scope of work, business impact, and engineering challenges of CL were very exciting to me. Plus, the roles on the CL team require a blend of data engineering, software engineering, and distributed systems skills, which align really well with my interests and expertise.

## What is your favorite project?

The project I am most proud of is building the Consolidated Logging V2 platform which processes and enriches data at a massive scale (5 million+ events per sec at peak) in real-time. I re-architected our legacy data pipelines and built a new platform on top of Apache Flink and Iceberg. The scale brought some interesting learnings and involved working closely with the Apache Flink and Kafka community to fix core issues. Thanks to the migration to V2, we were able to improve data availability and usability for our consumers significantly. The efficiency achieved in processing and storage layers brought us big savings on computing and storage costs. You can learn more about it from my [talk](https://www.slideshare.net/mobile/FlinkForward/massive-scale-data-processing-at-netflix-using-flink-snehal-nagmote-pallavi-phadnis) at the Flink forward conference. Over the last couple of years, we have been continuously enhancing the V2 platform to support more logging use cases beyond Netflix streaming apps and provide further analytics capabilities. For instance, I am recently working on a project to build a common analytics solution for 100s of Netflix studio and internal apps.

## How’s data engineering similar and different from software engineering?

The data engineering role at Netflix is similar to the software engineering role in many aspects. Both roles involve designing and developing large-scale solutions using various open-source technologies. In addition to the business logic, they need a good understanding of the framework internals and infrastructure in order to ensure production stability, for example, maintaining SLA to minimize the impact on the upstream and downstream applications. At Netflix, it is fairly common for data engineers and software engineers to collaborate on the same projects.

In addition, data engineers are responsible for designing data logging specifications and optimized data models to ensure that the desired business questions can be answered. Therefore, they have to understand both the product and the business use cases of the data in depth.

> **In other words, data engineers bridge the gap between data producers (such as client UI teams) and data consumers (such as data analysts and data scientists.)**

## Learning more

Interested in learning more about data roles at Netflix? You’re in the right place! Keep an eye out for our open roles in Data Science and Engineering by visiting our jobs site [here](https://jobs.netflix.com/search?team=Data+Science+and+Engineering). Our [culture](https://www.instagram.com/wearenetflix/?hl=en) is key to our impact and growth: read about it [here](https://jobs.netflix.com/culture). To learn more about our Data Engineers, check out our chats with [Dhevi Rajendran](https://netflixtechblog.medium.com/data-engineers-of-netflix-interview-with-dhevi-rajendran-a9ab7c7b36e5), [Samuel Setegne](https://netflixtechblog.medium.com/data-engineers-of-netflix-interview-with-samuel-setegne-f3027f58c2e2), and [Kevin Wylie](./data-engineers-of-netflix-interview-with-kevin-wylie-7fb9113a01ea.md).

---
**Tags:** Data Engineering · Big Data · Culture · Technology · Data
