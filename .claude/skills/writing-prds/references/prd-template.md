# PRD Template Reference

Complete template for generating Product Requirements Documents. Referenced from SKILL.md.

## Table of Contents

- [Standard PRD Template](#standard-prd-template)
- [Lean PRD Template](#lean-prd-template)
- [Executive One-Pager Template](#executive-one-pager-template)
- [Section Writing Guide](#section-writing-guide)

---

## Standard PRD Template

```markdown
# PRD: {Feature Name}

**Author:** {name}
**Date:** {date}
**Status:** Draft | In Review | Approved
**Version:** 1.0

---

## 1. Executive Summary

### Problem Statement
{1-2 sentences describing the user pain point or business gap}

### Why Now
{Why this is urgent — competitive pressure, user feedback volume, technical debt risk, or strategic timing}

### Proposed Solution
{1-2 sentences describing the approach at a high level}

### Success Criteria
| KPI | Target | Measurement Method |
|-----|--------|-------------------|
| {metric 1} | {target value} | {how measured} |
| {metric 2} | {target value} | {how measured} |
| {metric 3} | {target value} | {how measured} |

---

## 2. User Experience & Functionality

### User Personas

**Persona 1: {Name/Role}**
- Context: {when/where they encounter the problem}
- Pain point: {specific frustration}
- Goal: {what success looks like for them}

**Persona 2: {Name/Role}**
- Context: {when/where they encounter the problem}
- Pain point: {specific frustration}
- Goal: {what success looks like for them}

### User Stories

#### Story 1: {Title}
**As a** {persona}, **I want to** {action}, **so that** {benefit}.

**Acceptance Criteria:**
- [ ] {Verifiable criterion 1 — e.g., "Button displays confirmation dialog with cancel option"}
- [ ] {Verifiable criterion 2}
- [ ] {Verifiable criterion 3}

#### Story 2: {Title}
**As a** {persona}, **I want to** {action}, **so that** {benefit}.

**Acceptance Criteria:**
- [ ] {Verifiable criterion 1}
- [ ] {Verifiable criterion 2}

### Non-Goals
- {What this feature will NOT do — be specific}
- {Explicitly excluded functionality}
- {Future considerations parked here}

---

## 3. Quality Gates

Commands that must pass for every user story:

```
{e.g., npm test}
{e.g., npm run lint}
{e.g., npm run typecheck}
```

---

## 4. Technical Specifications

### Architecture Overview
{High-level description of how this fits into the existing system}

### Key Components
| Component | Responsibility | New/Modified |
|-----------|---------------|--------------|
| {component} | {what it does} | {New / Modified} |

### API Changes
{New endpoints, modified contracts, or "No API changes"}

### Data Model Changes
{New tables/fields, schema migrations, or "No data model changes"}

### Integration Points
- {External service 1}: {how it's used}
- {Internal system 1}: {how it's affected}

### Security & Privacy
- {Authentication/authorization requirements}
- {Data handling considerations}
- {Compliance requirements, if any}

---

## 5. Risks & Roadmap

### Phased Rollout

**MVP (Phase 1):**
- {Core functionality only}
- Timeline: {weeks}
- Exit criteria: {what must be true to move to Phase 2}

**v1.1 (Phase 2):**
- {Enhanced functionality}
- Timeline: {weeks after MVP}

**v2.0 (Phase 3):**
- {Full vision}
- Timeline: {weeks after v1.1}

### Technical Risks
| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| {risk 1} | High/Med/Low | High/Med/Low | {mitigation} |
| {risk 2} | High/Med/Low | High/Med/Low | {mitigation} |

### Dependencies
- {Team/system dependency 1}
- {Team/system dependency 2}

---

## 6. Open Questions

- [ ] {Unresolved question 1 — owner: @name}
- [ ] {Unresolved question 2 — owner: @name}
```

---

## Lean PRD Template

```markdown
# PRD: {Feature Name}

**Date:** {date} | **Status:** Draft

## Problem
{2-3 sentences: what's broken, who's affected, why it matters now}

## Solution
{2-3 sentences: what we're building}

## User Stories

### {Story 1 Title}
**As a** {user}, **I want to** {action}, **so that** {benefit}.
- [ ] {Acceptance criterion 1}
- [ ] {Acceptance criterion 2}

### {Story 2 Title}
**As a** {user}, **I want to** {action}, **so that** {benefit}.
- [ ] {Acceptance criterion 1}
- [ ] {Acceptance criterion 2}

## Non-Goals
- {excluded item 1}
- {excluded item 2}

## Success Metrics
| Metric | Target |
|--------|--------|
| {metric} | {value} |

## Open Questions
- {question 1}
```

---

## Executive One-Pager Template

```markdown
# {Feature Name}: Executive Summary

**Date:** {date} | **Author:** {name} | **Ask:** {what you need — approval, resources, budget}

## Business Case
{3-4 sentences: problem, opportunity size, competitive context}

## Proposed Solution
{2-3 sentences: what we'll build, key differentiator}

## Impact
| KPI | Current | Target | Timeline |
|-----|---------|--------|----------|
| {metric 1} | {baseline} | {goal} | {when} |
| {metric 2} | {baseline} | {goal} | {when} |
| {metric 3} | {baseline} | {goal} | {when} |

## Roadmap
| Phase | Scope | Timeline |
|-------|-------|----------|
| MVP | {core features} | {weeks} |
| v1.1 | {enhancements} | {weeks} |

## Risks
- {Top risk 1 + mitigation}
- {Top risk 2 + mitigation}

## Resource Requirements
- {Engineering: X engineers for Y weeks}
- {Design: X designers for Y weeks}
- {Other: dependencies}
```

---

## Section Writing Guide

### Writing Measurable Requirements

| Bad (Vague) | Good (Measurable) |
|-------------|-------------------|
| "Fast loading" | "Page loads in < 200ms at p95" |
| "Easy to use" | ">= 85% task completion rate in usability testing" |
| "Scalable" | "Handles 10K concurrent users with < 1% error rate" |
| "Secure" | "All PII encrypted at rest (AES-256) and in transit (TLS 1.3)" |
| "Reliable" | "99.9% uptime measured monthly" |
| "Works on mobile" | "100% Lighthouse Accessibility score, responsive at 320px-1440px" |

### Writing Verifiable Acceptance Criteria

| Bad | Good |
|-----|------|
| "Works correctly" | "Returns HTTP 200 with user object containing id, name, email" |
| "Handles errors" | "Displays inline error message when email format is invalid" |
| "Looks good" | "Matches Figma mockup at breakpoints 375px, 768px, 1440px" |

### Splitting Large User Stories

A story is too large if it can't be completed in one focused session. Split by:
- **User type**: "Admin can..." vs "Member can..."
- **Action**: "Create" vs "Edit" vs "Delete"
- **Scenario**: "Happy path" vs "Error handling" vs "Edge cases"
