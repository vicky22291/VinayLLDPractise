---
name: writing-prds
description: "Generates comprehensive Product Requirements Documents through a structured discovery-to-drafting workflow. Use when user asks to 'write a PRD', 'create a PRD', 'generate PRD', 'product requirements document', 'feature spec', 'define requirements', or 'scope a feature'."
metadata:
  author: vinay
  version: 1.0.0
  category: product
  tags: [prd, product, requirements, planning, specifications]
---

# Writing PRDs

Generates production-grade Product Requirements Documents by combining problem-first discovery, measurable success criteria, and AI-agent-friendly user stories.

For the full PRD template and section details: see [references/prd-template.md](references/prd-template.md)

## Instructions

### Step 1: Discovery (Never Skip)

Before writing anything, ask **3-5 clarifying questions** (one set at a time) using AskUserQuestion:

**Mandatory questions:**
1. What problem does this solve, and why is it urgent now?
2. Who are the target users? (personas or roles)
3. How will you measure success? (specific metrics, not "it works")

**Follow-up questions** (pick 2 based on answers):
- What is explicitly out of scope?
- What are the constraints? (timeline, tech stack, budget)
- Are there existing systems this integrates with?
- Could a prototype replace parts of this document?

If the user's answers reveal complexity, ask one more round of questions before proceeding.

### Step 2: Choose PRD Format

Based on discovery, recommend one format:

| Format | When to Use |
|--------|-------------|
| **Standard** | New features, multi-sprint work |
| **Lean** | Small enhancements, single-sprint work |
| **Executive One-Pager** | Stakeholder alignment, funding requests |

Default to **Standard** unless the user specifies otherwise.

### Step 3: Draft the PRD

Generate the PRD following the template in [references/prd-template.md](references/prd-template.md).

**Writing rules:**
- Every requirement must be **measurable** — replace "fast" with "< 200ms", "intuitive" with ">= 85% task completion rate"
- User stories must be **completable in one focused session** — split large stories
- Mark unknowns as **"TBD - [what needs answering]"** instead of guessing
- Write for clarity — include file paths, API endpoints, and code references where known
- Non-goals are mandatory — explicitly state what this feature will NOT do

**Quality gates** (if the project has a build/test system):
- Ask what commands must pass (e.g., `npm test`, `pnpm typecheck`, `make lint`)
- List them once in the Quality Gates section; they apply to all user stories

### Step 4: Select Success Metrics Framework

Choose the framework that best fits the product context:

| Framework | Best For |
|-----------|----------|
| **North Star** | Single key metric driving the product |
| **HEART** | UX-focused features (Happiness, Engagement, Adoption, Retention, Task Success) |
| **AARRR** | Growth features (Acquisition, Activation, Retention, Revenue, Referral) |
| **OKRs** | Goal-oriented teams with quarterly planning |

Include 3-5 concrete KPIs with target values and measurement methods.

### Step 5: Present and Iterate

1. Present the draft PRD to the user
2. Ask for feedback on **specific sections** (don't just say "looks good?")
3. Suggest: "Which sections need refinement: scope, user stories, technical specs, or metrics?"
4. Iterate until the user approves

### Step 6: Save the PRD

Save the final PRD to: `./docs/prd-{feature-name}.md`

If a `./tasks/` directory exists, also save there for task runner compatibility.

## Examples

### Example 1: New Feature PRD

**Input**: "Write a PRD for adding dark mode to our app"

**Output**: Standard PRD with:
- Problem: user eye strain, competitive gap
- "Why Now" justification
- User personas (night-time users, accessibility needs)
- User stories with verifiable acceptance criteria
- Technical specs (CSS variables, theme provider, storage)
- Success metrics: adoption rate > 30% in 90 days

### Example 2: Lean PRD

**Input**: "Quick PRD for adding a CSV export button"

**Output**: Lean PRD with:
- One-paragraph problem statement
- 3 user stories with acceptance criteria
- Non-goals (no scheduling, no PDF export)
- Success metric: 15% of users export within first month

### Example 3: Executive One-Pager

**Input**: "I need a one-pager to pitch real-time notifications to leadership"

**Output**: Executive summary with:
- Business case and revenue impact
- Competitive landscape
- 3 KPIs with targets
- Phased roadmap (MVP in 4 weeks, v1.1 in 8 weeks)
- Resource ask

## Troubleshooting

| Issue | Fix |
|-------|-----|
| PRD is too vague | Re-run discovery; add measurable acceptance criteria |
| Scope is too large | Split into multiple PRDs; move items to non-goals |
| Stakeholders disagree on priority | Add "Why Now" section with data |
| User stories are too big | Each story should be completable in one session; split further |
| Missing technical details | Mark as "TBD" and list in Open Questions |
| Metrics feel arbitrary | Use HEART/AARRR framework to ground them |
