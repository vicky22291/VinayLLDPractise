# Curiosity Stack — Functionality Guide

**What it is:** A Claude Cowork plugin that turns any technology, market signal, or global event into structured business insights through Socratic conversation. Built by Ameya Pimpalgaonkar for retail investors and enterprise professionals.

**What it is NOT:** A stock tips tool, a recommendation engine, or a chatbot giving generic answers. It never tells you what to buy or sell. Every output is framed as research, never advice.

---

## The Core Method: Six-Layer Decomposition

The plugin's central feature is a guided conversation that walks any topic through six layers of questioning. You don't get a wall of text — the plugin asks you questions at each layer, fills gaps with context, and builds understanding together. One layer at a time, never all at once.

**Layer 0 — The Signal:** You describe what caught your attention. The plugin asks: "What have you already heard about this? What made you stop and pay attention?" This establishes why the topic matters right now.

**Layer 1 — The Mechanics:** Goes past the headline into how things actually work. Not the press release version — the real mechanics. "What is this really? How does it work at a fundamental level? What does it actually do in the real economy?"

**Layer 2 — The Cause Tree:** Identifies three to five root causes driving this signal. Distinguishes between structural causes (permanent) and cyclical ones (temporary). At this point, the plugin generates the first visual checkpoint — a mini summary card you can share independently.

**Layer 3 — The Solution Space:** Each root cause creates a solution category, and each solution category maps to an industry. "For each cause — what are the solutions? Who needs to build them?" This is where abstract signals start becoming concrete business sectors.

**Layer 4 — Build Requirements:** What does each solution actually need to exist? What inputs, infrastructure, talent, and data are required? What is scarce? A second visual checkpoint card is generated here.

**Layer 5 — Value Chain Actors:** Named companies — not categories, actual businesses. Who are the global leaders? Which Indian companies sit in this chain? What is each company's specific function? The plugin searches its databases and surfaces companies with their exact role in the value chain.

**Layer 6 — Research Landscape:** How is this space typically accessed? Listed, unlisted, pre-IPO? What stage of development? A third visual checkpoint card is generated, and the decomposition is complete.

The entire process takes 8-10 minutes. This is intentional — the depth is the product. The plugin treats you as a thinking peer and expects you to engage at each layer rather than passively consume information.

---

## Personalization and Memory

Cowork has no memory between sessions, so the plugin uses a local configuration file (`curiosity-stack.local.md`) as a memory bridge. During a one-time setup (about 2 minutes), you answer questions about:

- Whether you're an investor, enterprise professional, or both
- Your geography focus (India, global, or both)
- What themes you're currently researching
- Companies or topics you're already tracking
- Which sectors you know well enough to skip the basics on
- Your preferred output format

Every subsequent session reads this file silently and personalizes the experience — skipping questions it already knows, adjusting depth, flagging connections to topics you're tracking, and surfacing your preferred sources. You never have to repeat yourself.

You can also connect up to 17 external sources: Google Drive, Gmail, Notion, Slack, OneDrive, Airtable, Dropbox, Box, Confluence, Jira, Asana, Linear, GitHub, HubSpot, Salesforce, Google Calendar, and Zapier. When connected, the plugin checks your existing notes at the start of every decomposition and asks if you want to factor them in — so you build on past work instead of starting from scratch.

---

## Output Options

After completing a decomposition, you choose one of four output formats:

**Value Chain Diagram:** An animated, interactive HTML visualization. Each layer appears one at a time with companies sliding into position. The diagram has two columns per layer — global companies on the left, Indian proxies on the right with their proxy pattern labeled. The whole thing scrolls vertically on a tall canvas. You can toggle text size (A / A+ / A++), download it, share it, or save it to your library.

**Interactive Mindmap:** A radial diagram centered on the topic with six branches, one per layer. Each branch expands on click. Company nodes at L5/L6 show hover cards with role in chain, proxy pattern, access method, and stage. India nodes pulse with a subtle animation.

**Research Note:** A structured markdown document — the traditional format. Clean, referenceable, downloadable.

**Research Brief:** A one-page PDF designed to be forwarded to someone. Contains an executive summary (3 sentences), a compressed value chain snapshot, global key players vs India proxies, the core assumption the thesis rests on, and three specific things to watch. Formatted for A4 with print CSS — you hit Cmd+P and get a clean PDF.

All outputs use a consistent design system: warm white background, soft charcoal text, deep teal accent used sparingly. The aesthetic is deliberately "senior analyst's research PDF" — not a startup dashboard. Every output includes a quality badge and an invisible HTML attribution comment that survives copy-paste.

---

## India Proxy Research

This is one of the plugin's strongest differentiators. You give it a global company or theme (e.g., "Nvidia", "grid-scale battery storage") and it autonomously researches the Indian market equivalents.

The agent runs a full research sequence without needing you to do any searching:

1. Clarifies which specific value chain layer you're interested in
2. Researches the global company's actual business model to establish what keywords and sub-segments to look for
3. Searches across Indian databases — Tracxn for startup profiles and funding data, Inc42 for news and founder interviews, NASSCOM for curated lists, Screener.in for listed company financials
4. Classifies each company it finds into one of five proxy patterns:
   - **Direct player** — does the same thing in India
   - **Infrastructure** — provides the underlying infrastructure the theme needs
   - **Supplier** — supplies inputs to companies driving the theme
   - **Beneficiary** — existing business that benefits as the theme grows
   - **Enabler** — product or service that becomes more valuable as the theme scales
5. Validates each candidate against red flags: Is the company actually in this business or just marketing it? Is the relevant segment more than 5% of revenue? Are founder claims verifiable? Is there a real client list?

The output is a structured shortlist showing each company's function in the value chain layer, the proxy pattern, how a researcher would investigate further (Screener.in link, Tracxn profile, Inc42 coverage), validation signals, and a specific watch trigger — what event would make this company more or less relevant.

If the agent finds nothing meaningful, it says so plainly. An empty result is itself a useful research finding.

---

## Thesis Stress Test

After a decomposition (or independently), you can stress test any research conclusion. The plugin produces a three-column animated visualization:

**Left column — Proponents:** 3-5 structural factors that support the thesis, with actual data points and named organizations. Not vague generalities.

**Center column — Core Assumption:** One sentence that captures the single assumption the entire thesis rests on. Framed as: "This thesis holds only if..."

**Right column — Critics:** 3-5 factors critics cite against the thesis. The plugin gives critics the strongest possible version of their argument — no strawmanning. If critics have a strong case, it says so honestly.

Below the columns: "How Wrong Can This Be?" — one specific paragraph on the biggest risk, not a generic warning. And 3-4 specific observable milestones worth tracking, with timeframes where possible.

---

## Decomposition Library

Every completed decomposition can be saved to a local library. Each saved session captures the topic, date, all six layer summaries, sources cited, tags (sector, geography, stage), and a conviction placeholder.

The library enables three powerful workflows:

**Retrieval:** When you start a new decomposition, the plugin checks your library for past sessions on the same or similar topic. If found, it offers to load that session as context — saving you ground you've already covered and letting you skip layers where your previous conclusions still hold.

**Layer Delta:** Revisit a topic months later and the plugin compares the new decomposition against your saved one — showing exactly what changed at each layer. This makes it easy to track how a space is evolving over time.

**Search and Browse:** Browse your full library, search by topic or sector tags, open past decompositions, delete old ones, or export the full library to Google Drive or Notion for backup and cross-device access.

---

## Watchlist Monitoring

You can set up a watchlist of topics and have the plugin monitor them automatically on a schedule you choose — daily, weekly, or fortnightly.

**Tracker Mode:** On each run, the plugin's monitoring agent autonomously sweeps the web for material developments across all your watchlist items. It searches for news, regulatory changes, new market entrants, and funding rounds. For each finding, it identifies which value chain layer is affected, scores materiality as HIGH (changes the thesis), MEDIUM (relevant but not thesis-changing), or LOW (noise), and explains why in 2-3 lines. It then compiles everything into a structured digest and emails it to you via Gmail, with a link to go deeper in Cowork.

**Trigger Mode:** For any topic on your watchlist, you can set specific per-layer conditions in plain language — for example, "Alert me when a new company enters Layer 4 of EV batteries" or "Alert me when there's a regulatory change at Layer 3 of semiconductor packaging." The agent checks these conditions on each run and fires alerts only when met, flagged prominently at the top of your digest.

The agent is honest about noise — it won't inflate the importance of findings to make the digest seem more valuable. Topics with no material developments are simply marked as such.

---

## Source Credibility

A personal source registry that builds intelligence over time about which publications, databases, and analysts work best for you — organized by sector and layer.

**During decompositions:** At Layer 1 (mechanics) and Layer 5 (companies), the plugin surfaces your top-rated sources for the current sector before you go looking. For example: "For fintech company research, your top sources are: Inc42 (strong for India funding rounds) and Tracxn (reliable across sectors)."

**After each session:** The plugin asks you to rate up to 3 sources used during the session — very useful, somewhat useful, or not useful. Takes about 60 seconds.

**After 5+ sessions:** The plugin starts suggesting ratings based on your usage patterns. For instance, if Inc42 appeared in 6 of your 8 India-focused sessions, it suggests rating it high for India L5. You confirm or override.

**After 10+ sessions:** The plugin surfaces cross-decomposition patterns — non-obvious connections across your research. For example: "3 of your recent decompositions — EV batteries, grid storage, and solar manufacturing — all converge at the power electronics layer. The same 2 companies appear across all three. Want to map this as a cross-cutting theme?"

---

## Scenario Library

For users who don't know where to start, the plugin offers 18 pre-built decomposition topics across six categories:

**India Themes:** Semiconductor packaging, defence manufacturing, GCC opportunity, pharma API supply chain.

**Geopolitics:** Middle East conflict and oil supply, Russia-Ukraine and energy transition, US-China tariffs and supply chains, Taiwan risk and semiconductors.

**Global Trends:** Space economy infrastructure, precision medicine data layer.

**AI — Global:** AI inference demand, AI data centre infrastructure.

**AI — India:** Indian IT services in the AI era, India AI data annotation layer.

**Cybersecurity:** Enterprise cybersecurity demand, OT security in critical infrastructure.

**Energy & Climate:** Grid-scale battery storage, green hydrogen economy.

Each scenario comes with a preview showing what each of the six layers will reveal for that specific topic and an estimated session time (8-10 minutes). Selecting one launches the full decomposition immediately — no topic description needed.

---

## Curated Reading List

After every decomposition, the plugin generates exactly 5 reading recommendations targeted at the specific topic and layers just explored. The mix is deliberate:

1. One foundational explainer deepening Layer 1 understanding
2. One practitioner piece — how someone actually built something in this space
3. One market/business analysis from a Layer 5/6 perspective
4. One India-specific piece (if India filter is active)
5. One "adjacent lens" — something approaching the topic from an unexpected angle

The plugin prefers original research papers, company filings, and long-form analysis from domain experts over generic content. If it's not confident a URL is accurate, it gives a search instruction instead of fabricating a link.

---

## SEBI Compliance

Because the plugin operates in investment-adjacent territory (identifying companies, mapping value chains, discussing market themes), it enforces strict compliance with Indian securities regulations:

- A mandatory disclaimer appears on every single research output — it cannot be suppressed
- The plugin will immediately and cleanly decline any question asking whether to buy, sell, or hold a specific security, what price to buy at, or whether something is a good investment
- Language is carefully controlled: "investment opportunity" becomes "research candidate", "entry point" becomes "how this is typically accessed", "upside potential" becomes "factors that proponents cite"
- When naming companies, they are always framed as research starting points, never targets or picks — "Companies that operate in this layer include..." not "You should look at..."

The compliance system is enforced both as an always-active skill (highest priority, cannot be overridden by other skills) and as an automated hook that reviews every response before delivery. The philosophy: "A sharp research tool that respects these boundaries is more valuable, not less."

---

## Feedback and Sharing

After every output, the plugin offers:

- A feedback prompt (thumbs up/down/comment linking to a Google Form)
- An option to publish the output publicly with branding and an install link
- An auto-generated shareable card formatted for X and LinkedIn — topic name, top 3 insights, India proxies highlighted, `#CuriosityStack` tag
- The option to set a watchlist trigger to revisit the topic when something changes

On the first-ever session, the feedback prompt is more prominent — encouraging early users to shape what gets built next.

---

## Planned Features

**Conviction Tracker (v3.3.0):** After a decomposition and stress test, the plugin will ask 5 structured questions and produce a conviction score from 1-10 with specific gaps listed. Not a buy signal — a research completeness score telling you how thoroughly you've investigated before forming a view.

**Cross-Decomposition Pattern Engine (v3.3.0):** After 5+ saved decompositions, a meta-analysis that surfaces non-obvious connections across your research — identifying convergence points across different topics that you might have missed.

**Web App (v4.0.0):** A standalone web application where anyone can paste a topic and get a structured decomposition without needing Cowork. Includes a community decomposition library where users share and build on each other's research.
