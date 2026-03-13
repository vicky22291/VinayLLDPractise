---
title: "Effective Technical Writing"
author: "Vijay Seshadri"
date: "Dec 15, 2021"
url: "https://bytes.swiggy.com/effective-technical-writing-eb9679973c4c"
tags: ['Swiggy Engineering', 'Technical Writing', 'Programming', 'Documentation', 'Technology']
---

# Effective Technical Writing

![image](../images/dff5a5a75aa13b62.jpg)

As we continue to adjust ways of working due to the unprecedented COVID situation, we are faced with many challenges to make remote/hybrid models work. Prior to Covid, Swiggy’s Engineering team (for the most part) was based out of a single location. Most discussions happened during in-person meetings, hall way conversations and quick(adhoc) white boarding sessions. Covid changed all of that. Suddenly, we were faced with an all remote workforce and yet we attempted to keep up the pace of execution and decision-making. One tool that proved invaluable was the ability to produce _effective_ technical documents. While this sounds simple, putting it in practice is hard. Hence, I wanted to provide some insight into the approach and practices that hopefully help you all. Let’s face it — writing code is easier than writing documents :-)

By _effective, _I mean_ clear, concise and contextual. _If you need to make an important decision for your team, a well written document might win you half the battle. It allows team members to ask specific/relevant questions, make good use of combined team time and drive to a decision faster. While “writing good documents” might seem like a basic skill, there are subtle, but important aspects that improve the quality of documents. Further, this practice also helps new team members onboard faster and acts as a reference for the “whats” and “whys” for a technical decision. Last, but not least, the risk with “explaining during a meeting” is that we can walk out of the meeting having slightly different interpretations on what we agreed upon :-). Hence, I want to call out a five points that I strive to remember:

1. Purpose — Call out the purpose (or scope) of the doc upfront; are you planning to drive to a decision (e.g. I would like to finalize the call flow for use case X, design for reducing the P99 latency of my service by X%). Use this section also to focus on key questions/terms of alignment you need from the meeting.
2. Context — Oftentimes, the author has a lot of context in her head, but that might not reflect in the doc which leads to misinterpretation of a term, concept or a recommendation. If you are making assumptions about context, use an ‘intended audience’ section. A general best practice is to define a (non-obvious) term the first time you reference it.
3. Clarity versus length — Sometimes we tend to write too much to ensure that no little detail gets missed out; mistaking it as a means to improve clarity. This can cause more confusion if the reader gets distracted with irrelevant (to the purpose) details. Communicating complex concepts _concisely_ is hard. (something I struggle with). Suggestion — Pick a ‘take-away’ that you would like the audience to retain (for instance do not use caching layer as a persistent data store), back it up with simpler, structured data points (e.g. caching layer’s data durability design is not…) and use an appendix section or footnote to refer to more details in case the reader is interested (e.g. link to a paper on redis HA design…)
4. Picture is worth a thousand words — This is especially true for inter services/ components interaction. An API call flow diagram (with params/return values specifying important assumptions) has reduced the time taken to agree on complex flows. It is a much better alternative to verbal or verbosely written communication, both of which run the risk of being misinterpreted.
5. Sentence construction — Avoid using ambiguous phrases/words that dilute the intent or cause differing interpretations (e.g. “Using tech X _may_ under _some_ circumstances lead to _poor_ availability”). No decision can be made with such sentences. Short, simple sentences go a long way in improving the clarity.

---
**Tags:** Swiggy Engineering · Technical Writing · Programming · Documentation · Technology
