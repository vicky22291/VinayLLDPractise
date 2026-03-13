---
title: "Keeping Customers Streaming — The Centralized Site Reliability Practice at Netflix"
author: "Netflix Technology Blog"
date: "May 27, 2020"
url: "https://netflixtechblog.com/keeping-customers-streaming-the-centralized-site-reliability-practice-at-netflix-205cc37aa9fb"
tags: ['Site Reliability', 'Sre', 'Incident Response', 'Incident Management', 'Reliability']
---

# Keeping Customers Streaming — The Centralized Site Reliability Practice at Netflix

By [Hank Jacobs](https://www.linkedin.com/in/hajacobs/), Senior Site Reliability Engineer on CORE

We’re privileged to be in the business of bringing joy to our customers at Netflix. Whether it’s a compelling new series or an innovative product feature, we strive to provide a best-in-class service that people love and can enjoy anytime, anywhere. A key underpinning to keeping our customers happy and streaming is a strong focus on reliability.

**Reliability, formally speaking, is the ability of a system to function under stated conditions for a period of time.** Put simply, reliability means a system should work and continue working. From [failure injection testing](https://netflixtechblog.com/fit-failure-injection-testing-35d8e2a9bb2) to regularly exercising our [region evacuation](https://netflixtechblog.com/project-nimble-region-evacuation-reimagined-d0d0568254d4) abilities, Netflix engineers invest a lot in ensuring the services that comprise Netflix are [robust and reliable](https://www.youtube.com/watch?v=GFvgOumfuWc&t=241s). Many teams contribute to the reliability of Netflix and own the reliability of their service or area of expertise. The Critical Operations and Reliability Engineering team at Netflix (CORE) is responsible for the reliability of the Netflix service as a whole.

CORE is a team consisting of Site Reliability Engineers, Applied Resilience Engineers, and Performance Engineers. Our group is responsible for the reliability of business-critical operations. Unlike most SRE teams, we do not own or operate any customer-serving services nor do we routinely make production code changes, build infrastructure, or embed on service teams. Our primary focus is ensuring Netflix stays up. Practically speaking, this includes activities such as systemic risk identification, handling the lifecycle of an incident, and reliability consulting.

Teams at Netflix follow the [service ownership model](https://netflixtechblog.com/full-cycle-developers-at-netflix-a08c31f83249): they operate what they build. Most of the time, service owners catch issues before they impact customers. Things still occasionally go sideways and incidents happen that impact the customer experience. This is where the CORE team steps in: CORE configures, maintains, and responds to alerts that monitor high-level business KPIs ([stream starts per second](https://netflixtechblog.com/sps-the-pulse-of-netflix-streaming-ae4db0e05f8a), for instance). When one of those alerts fires, the CORE on-call engineer assesses the situation to determine the scope of impact, identify involved services, and engage service owners to assist with mitigation. From there, CORE begins to manage the incident.

Incident management at Netflix doesn’t follow common management practices like the [ITIL model](https://wiki.en.it-processmaps.com/images/7/73/Incident-management-itil.jpg). In an incident, the CORE on-call engineer generally operates as the Incident Manager. The Incident Manager is responsible for performing or delegating activities such as:

- Coordination — bringing in relevant service owners to help with the investigation and focus on mitigation
- Decision Making — making key choices to facilitate the mitigation and remediation of customer impact (e.g. deciding if we should evacuate a region)
- Scribe — keeping track of incident details such as involved teams, mitigation efforts, graphs of the current impact, etc.
- Technical Sleuthing — assisting the responding service owners with understanding what systems are contributing to the incident
- Liaison — communicating information about the incident across business functions with both internal and external teams as necessary

Once the customer impact is successfully mitigated, CORE is then responsible for coordinating the post-incident analysis. Analysis comes in many shapes and sizes depending on the impact and uniqueness of the incident, but most incidents go through what we call “memorialization”. This process includes a write-up of what happened, what mitigations took place, and what follow-up work was discussed. For particularly unique, interesting, or impactful incidents, CORE _may_ host an Incident Review or engage in a deeper, long-form investigation. Most post-incident analysis, especially for impactful incidents, is done in partnership with one of CORE’s Applied Resilience Engineers. A key point to emphasize is that all incident analysis work focuses on the [sociotechnical](https://en.wikipedia.org/wiki/Sociotechnical_system) aspects of an incident. Consequently, post-incident analysis tends to uncover many practical learnings and improvements for all involved. We frequently socialize these findings outside of those directly involved to help share learnings across the company.

So what happens when a CORE engineer is not on-call or doing incident analysis? Unsurprisingly, the response varies widely based on the skillset and interests of the individual team member. In broad strokes, examples include:

- Preserving operational visibility and response capabilities — fixing and improving our dashboards, alerts, and automation
- Reliability consulting — discussing various aspects including architectural decisions, systemic observability, application performance, and on-call health training
- Systematic risk identification and mitigation — partner with various teams to identify and fix systematic risks revealed by incidents
- Internal tooling — build and maintain tools that support and augment our incident response capabilities
- Learning and re-learning the changes to a complex, ever-moving system
- Building and maintaining relationships with other teams

Overall, we’ve found that this form of reliability work best suits the needs and goals of Netflix. Reliability being CORE’s primary focus affords us the bandwidth to both proactively explore potential business-critical risks as well as effectively respond to those risks. Additionally, having a broad view of the system allows us to spot systematic risks as they develop. By being a separate and central team, we can more efficiently share learnings across the larger engineering organization and more easily consult with teams on an ad hoc basis. Ultimately, CORE’s singular focus on reliability empowers us to reveal business-critical sociotechnical risks, facilitate effective responses to those risks and ensure Netflix continues to bring joy to our customers.

---
**Tags:** Site Reliability · Sre · Incident Response · Incident Management · Reliability
