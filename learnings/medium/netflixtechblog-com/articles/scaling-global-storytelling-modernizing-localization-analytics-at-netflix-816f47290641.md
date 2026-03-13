---
title: "Scaling Global Storytelling: Modernizing Localization Analytics at Netflix"
author: "Netflix Technology Blog"
url: "https://netflixtechblog.com/scaling-global-storytelling-modernizing-localization-analytics-at-netflix-816f47290641"
tags: ['Analytics', 'Data', 'Localization', 'Tech Debt', 'Big Data']
---

# Scaling Global Storytelling: Modernizing Localization Analytics at Netflix

[Valentin Geffrier](https://www.linkedin.com/in/valentingeffrier/), [Tanguy Cornuau](https://www.linkedin.com/in/tanguycornuau/)

_Each year, we bring the Analytics Engineering community together for an Analytics Summit — a multi-day internal conference to share analytical deliverables across Netflix, discuss analytic practice, and build relationships within the community. This post is one of several topics presented at the Summit highlighting the breadth and impact of Analytics work across different areas of the business._

At Netflix, our goal is to entertain the world, which means we must speak the world’s languages. Given the company’s growth to serving 300 million+ members in more than 190+ countries and 50+ languages, the Localization team has had to scale rapidly in creating more dubs and subtitle assets than ever before. However, this growth created technical debt within our systems: a fragmented landscape of analytics workflows, duplicated pipelines, and siloed dashboards that we are now actively modernizing.

### The Challenge: “Who Made This Dub?”

Historically, business logic for localization metrics was replicated across isolated domains. A question as simple as “_Who made this dub/subtitle?”_ is actually complex — it requires mapping multiple data sources through intricate and constantly changing logic, which varies depending on the specific language asset type and creation workflow.

When this logic is copied into isolated pipelines for different use cases it creates two major risks: inconsistency in reporting and a massive maintenance burden whenever upstream logic changes. We realized we needed to move away from these vertical silos.

### Our Modernization Strategy

To address this, we defined a vision centered on consolidation, standardization, and trust, executed through three strategic pillars:

1. The Audit and Consolidation Playbook

We initiated a comprehensive audit of over 40 dashboards and tools to assess usage and code quality. Our focus has shifted from patching frontend visualizations to consolidating backend pipelines. For example, we are currently merging three legacy dashboards related to dubbing partner KPIs (around operational performance, capacity, and finances), focusing first on a unified data and backend layer that can support a variety of future frontend iterations.

2. Reducing “Not-So-Tech” Debt

Technical debt isn’t just about code; it is also about the user experience. We define “Not-So-Tech Debt” as the friction stakeholders feel when tools are hard to interpret or can benefit from better storytelling. To fix this, we revamped our Language Asset Consumption tool — instead of reporting dub and subtitle metrics independently, we combine audio and text languages into one consumption language that helps differentiate Original Language versus Localized Consumption and measure member preferences between subtitles, dubs, or a combination of both for a given language. This unlocks more intuitive insights based on actual recurring stakeholder use cases.

3. Investing in Core Building Blocks

We are shifting to a _write once, read many_ architecture. By centralizing business logic into unified tables — such as a “Language Asset Producer” table — we solve the “_Who made this dub?”_ problem once. This centralized source now feeds into multiple downstream domains, including our Dub Quality and Translation Quality metrics, ensuring that any logic update propagates instantly across the ecosystem.

### The Future: Event-Level Analytics

Looking ahead, we are moving beyond asset-level metrics to event-level analytics. We are building a generic data model to capture granular timed-text events, such as individual subtitle lines. This data helps us understand how subtitle characteristics (e.g. reading speed) affect member engagement and, in turn, refine the style guidelines we provide to our subtitle linguists to improve the member experience with localized content.

Ultimately, this modernization effort is about scaling our ability to measure and enhance the joy and entertainment we deliver to our diverse global audience, ensuring that every member, regardless of their language, has the best possible Netflix experience.

---
**Tags:** Analytics · Data · Localization · Tech Debt · Big Data
