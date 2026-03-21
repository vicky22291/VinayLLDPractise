---
name: project-planning
description: Complete workflow for planning new projects - from requirements gathering to design documentation. Use when user wants to design a new project (not task breakdown or implementation).
allowed-tools: "Read, Write, Edit, Bash(gh *), Bash(git *), Bash(mkdir *), Grep, Glob, TodoWrite, Task"
metadata:
  version: 1.0.0
  category: project-management
---

# Project Planning Workflow

**Use when**: User wants to design a NEW project (requirements → design)
**Don't use for**: Task breakdown (use `task-breakdown`), implementation, bug fixes, or exploration

## Quick Start

**This skill handles new project design (requirements → design → final documentation).**

For other workflows:
- Task breakdown & YAML generation → `/task-breakdown`
- Implementation tasks (T1, T2) → CLAUDE.md
- Bug fixes, exploration, or docs → CLAUDE.md

**Use this skill when**: User wants to design a NEW project from scratch

---

## Phase 1: Requirements Gathering

**Setup**:
```bash
mkdir -p docs/projects/{project-name}/resources docs/projects/{project-name}/tasks
```

Create `resources/requirements-gathering.md`, use **question-by-question** approach.

**Questions** (organized by category):
1. **Scope & Context**: What "project approved" means, which repos/systems involved
2. **Automation Scope**: Which parts of development process to automate
3. **Human Involvement**: Level of oversight and approval gates
4. **Technical Approach**: Technology stack and tools to use
5. **Constraints**: Budget, security, performance, integration requirements
6. **Success Criteria**: Measurable outcomes that define project success
7. **Backward Compatibility**: Integration, rollback, stability considerations

---

## Phase 2: Design Discussion

Create `resources/design-proposal.md` - **iterative** approach with "Open Questions" section.

**Research** (if needed):
- Use `search-specialist` agent for external research via Task tool:
  ```json
  {
    "description": "web research for [topic]",
    "prompt": "Research [specific query]. Summarize key findings, sources, and relevant examples.",
    "subagent_type": "search-specialist"
  }
  ```
- Create `resources/research-findings.md` for general findings
- For detailed technical research, create separate files: `research-<topic>.md` (e.g., `research-github-api.md`)
- Include: research date, topic, purpose, findings, examples, references

**CRITICAL**: NO actual code - use Mermaid diagrams and pseudo code only!

**Required Diagrams**:

**HLD (High-Level Design)** — Reference `docs/hld-architecture-principles-reference.md` (sections: "System Design Principles" and "Anti-Patterns"):
- Component diagrams (system decomposition)
- Activity diagrams (workflow/process flow)
- Use case diagrams (if applicable)
- **System design principles**: Apply Separation of Concerns, SRP at system level, DRY, KISS, YAGNI from the reference
- **Anti-patterns**: Check design against documented anti-patterns (distributed monolith, golden hammer, over-engineering, etc.)

**LLD (Low-Level Design)**:
- Class diagrams (data structures, relationships)
- Sequence diagrams (component interactions)
- Entity-Relationship diagrams (data models)
- State diagrams (if applicable)
- **Design patterns**: Reference `docs/design-patterns-python-lld-reference.md` to identify applicable patterns (Strategy, Factory, Observer, etc.) and justify their use in the class diagrams

**Additional Content**:
- Design decisions with rationale
- Open Questions
- Performance/security/reliability considerations
- Pseudo code (only if diagrams insufficient for logic clarity)

**Process**: Propose diagrams/design → User feedback → Refine → Repeat until approved

---

## Phase 3: Project Naming & Documentation

- Agree on shortened name (e.g., `atr-trailing-stop` → `atr-ts`)
- Create final `design.md` with requirements, architecture, LLD
- Add **Cross-Project Dependencies** section if applicable

---

## Best Practices

**Session State** (multi-session projects):
Create `resources/SESSION_STATE.md` when Phase 1/2 spans multiple sessions:

```markdown
# Project Name - Session State
**Current Phase**: Phase X - [Name] (In Progress)

## Completed
- ✅ Step 1
- ✅ Step 2

## Current Task
⏳ What we're working on now

## Open Questions
[List questions awaiting user input]

## How to Resume
"Continue [project-name] from where we left off"

## Files Created
[List all project files with status]
```

Update at natural breakpoints (end of phase, before user break).

**Document Structure**:
- `design.md` - Final (after Phase 2)
- `resources/requirements-gathering.md` - Phase 1
- `resources/design-proposal.md` - Phase 2 draft
- `resources/research-*.md` - Research findings
- `resources/SESSION_STATE.md` - Resume info

**Document Lifecycle**:
1. **Phases 1-2 (Concurrent)**: Create intermediate docs in resources/ (`-proposal`, `-gathering`, `-findings`)
2. **After Approval**: Create final `design.md` at project root
3. **Throughout**: Maintain SESSION_STATE.md at phase breakpoints; keep intermediate docs for reference

**Research**:
Use `search-specialist` agent → Document in `resources/research-{topic}.md` with: research date, topic, purpose, executive summary, detailed findings, examples, references, quick reference section

**Backward Compatibility** - Apply to all phases:
- Integration strategy
- Rollback capability
- Stability guarantees
- API versioning
- Migration paths
- Prefer extending over modifying

---

## Key Rules

✅ Question-by-question in Phase 1
✅ Iterative design with open questions in Phase 2
✅ **NO actual code** - use Mermaid diagrams (Component, Activity, Sequence, Class, ER) and pseudo code only
✅ **HLD/LLD with diagrams** - Component/Activity for HLD, Class/Sequence/ER for LLD
✅ Use search-specialist for research

❌ Never write actual code in design documents
❌ Never skip diagrams in design docs
❌ Don't skip backward compatibility analysis
