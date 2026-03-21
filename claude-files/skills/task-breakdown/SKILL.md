---
name: task-breakdown
description: Generates task breakdown files and task_runner V2 YAML from project design documents. Use when user says "break down tasks", "generate tasks for {project}", "create task files", or wants to produce task markdown files and YAML for task_runner.
allowed-tools: "Read, Write, Edit, Bash(mkdir *), Bash(PYTHONPATH=*), Bash(gh *), Grep, Glob, Agent"
metadata:
  version: 1.0.0
  category: project-management
---

# Task Breakdown & YAML Generation

**Use when**: User wants to generate task files and task_runner YAML from an existing project design
**Don't use for**: Project planning/design (use `project-planning`), running tasks (use `run-tasks`)

**Prerequisites**: Project must have a design document at `docs/projects/{project-name}/design.md` or `task-breakdown.md`

---

## Phase 1: Read & Understand Project

### Step 1: Locate Project Documents

Read ALL available project documents:

```
docs/projects/{project-name}/design.md          # Primary design doc (required)
docs/projects/{project-name}/task-breakdown.md   # May already have task specs
docs/projects/{project-name}/resources/*.md      # Research, requirements, proposals
```

Also read:
- `docs/LLD_Architecture.md` — System architecture context
- `docs/Projects.md` — Project registry for cross-project awareness

### Step 2: Extract Key Information

From the design document, identify:
- **Project name** and **phase** identifier
- **Module locations** — where code lives in `src/`
- **Dependencies** between components
- **External integrations** — APIs, databases, services
- **Data models** — classes, dataclasses, enums
- **Workflows** — sequence of operations

---

## Phase 2: Cross-Project Analysis (MANDATORY)

Before creating tasks, scan `docs/projects/` for:
- Similar task names across projects
- Common functionality that could be shared
- Overlapping file changes
- Reusable components

**Consolidation options**:
- **Option A**: Extend existing code
- **Option B**: Create shared components
- **Option C**: Generalize implementation
- **Option D**: Extract common logic

Document findings and present to user before proceeding.

---

## Phase 3: Break Down Into Tasks

### Task Sizing Rules

- Target **~100 lines of code** per task (implementation + tests)
- Larger is acceptable if task cannot be reasonably split
- Prioritize logical cohesion over strict line limits
- **Every task MUST include tests** — never create separate "test" tasks
- Minimum test coverage: 90%

### Dependency Graph

- Build a DAG of task dependencies
- Group independent tasks into **waves** (can run in parallel)
- Ensure no circular dependencies
- Use cross-project dependencies where needed: `{project-name}/T{id}`

### Task ID Convention

- Use `T1`, `T2`, `T3` for simple projects
- Use `{PHASE}-T1`, `{PHASE}-T2` for multi-phase projects (e.g., `P3B-T1`)
- Keep IDs short — they appear in branch names and PR titles

### Branch Naming

Branches follow: `{project-name}/{task-name}` (e.g., `my-project/add-data-models`)
- Task name in kebab-case
- The YAML `branch_prefix` handles the project prefix automatically

### Present Plan to User

Before generating files, present the task breakdown summary with LOC estimates:
```
Wave 1 (parallel):
  T1: Add Data Models          ~80 LOC (50 impl + 30 tests)
  T2: Add Config Loader        ~100 LOC (60 impl + 40 tests)
Wave 2 (depends on Wave 1):
  T3: Implement Processor      ~120 LOC (70 impl + 50 tests)
Wave 3 (depends on Wave 2):
  T4: Add CLI Entry Point      ~60 LOC (30 impl + 30 tests)

Total: 4 tasks in 3 waves, ~360 LOC
Code reuse: T3 extends existing BaseProcessor from src/common/
```

Wait for user approval before generating files.

---

## Phase 4: Generate Task Markdown Files

Create one file per task at: `docs/projects/{project-name}/tasks/{task-name}.md`

### Task File Template

```markdown
# Task: {TASK_ID} — {Task Name}

## Objective

{One clear sentence describing what this task accomplishes}

## Context

{Brief context: which module/component this belongs to, why it's needed, how it fits in the larger design}

Reference: `docs/projects/{project-name}/design.md` — Section {X}

## Requirements

1. {Specific, actionable requirement}
2. {Another requirement}
3. {Include file paths where code should be created/modified}
4. {Include test file paths}

### Implementation Details

- **Location**: `src/{module}/{file}.py`
- **Test Location**: `tests/{module}/test_{file}.py`
- **Dependencies**: {list any T{X} task outputs this depends on}

## Verification Criteria

### Acceptance Criteria
- [ ] {Criterion 1 — measurable/checkable}
- [ ] {Criterion 2}
- [ ] All tests pass: `PYTHONPATH=src:. pytest tests/{module}/test_{file}.py -v`
- [ ] Coverage > 90%

### Quality Checks
- [ ] {Code quality criterion}
- [ ] {Linting passes: `ruff check src/{module}/{file}.py`}
```

### Task File Rules

- **Objective**: One sentence. Clear what "done" looks like.
- **Context**: Brief. Link to design doc section.
- **Requirements**: Specific file paths, function signatures, behavior descriptions.
- **Verification**: Checkable criteria. Include exact test commands.
- Do NOT include actual code — task files describe WHAT, not HOW.
- Do NOT include Mermaid diagrams — those belong in the design doc.

---

## Phase 5: Generate task_runner YAML

Create: `tasks/{project-name}.yaml`

### YAML Template

```yaml
# {Project Name} — Phase {X}
# Usage:
#   PYTHONPATH=src:. python -m task_runner tasks/{project-name}.yaml --dry-run
#   PYTHONPATH=src:. python -m task_runner tasks/{project-name}.yaml
#   PYTHONPATH=src:. python -m task_runner tasks/{project-name}.yaml --task T1

project: {project-name}
phase: "{phase}"
working_directory: /Users/vicky/workspaces/TechnicalAnalysis/VQTS-Python
python_cmd: python3.13
venv_activate: "source .venv/bin/activate"
max_retries: 5

executor:
  agent: claude
  model: opus
verifier:
  agent: claude
  model: sonnet
  max_rounds: 3

git:
  base_branch: main
  manual_merge: true
  skip_pr: false
  branch_prefix: "{project-name}/"

lint: true

notifications:
  telegram:
    enabled: false

tasks:
  # ============================================================
  # Wave 1: {Description} (parallel, no dependencies)
  # ============================================================

  T1:
    name: {Task Name}
    task_file: docs/projects/{project-name}/tasks/{task-name}.md
    depends_on: []
    max_turns: 30

  T2:
    name: {Task Name}
    task_file: docs/projects/{project-name}/tasks/{task-name}.md
    depends_on: []
    max_turns: 30

  # ============================================================
  # Wave 2: {Description} (depends on Wave 1)
  # ============================================================

  T3:
    name: {Task Name}
    task_file: docs/projects/{project-name}/tasks/{task-name}.md
    depends_on:
      - T1
      - T2
    max_turns: 30
```

### YAML Configuration Rules

| Field | Default | When to Override |
|-------|---------|-----------------|
| `executor.model` | `opus` | Use `sonnet` for simpler tasks |
| `verifier.model` | `sonnet` | Use `haiku` for cost savings |
| `verifier.max_rounds` | `3` | Reduce to `2` for simple tasks |
| `max_turns` | `30` | Increase to `40-50` for complex tasks, reduce to `10-15` for trivial ones |
| `max_retries` | `5` | Reduce to `2-3` for simple projects |
| `skip_pr` | `false` | Set `true` for local-only / testing |
| `manual_merge` | `true` | Set `false` for auto-merge after approval |
| `branch_prefix` | `{project}/` | Match project conventions |
| `lint` | `true` | Set `false` only for non-Python tasks |

### Optional Task Fields

Use these when needed:

```yaml
  T1:
    name: Task Name
    task_file: docs/projects/{project}/tasks/{task-name}.md
    depends_on: []
    max_turns: 30
    # Optional overrides:
    executor:
      model: sonnet          # Override project-level executor
    verifier:
      model: haiku           # Override project-level verifier
      max_rounds: 2
    lint: false               # Disable lint for this task
    condition: "T5.output_key.nested_key"  # Conditional execution
```

---

## Phase 6: Generate task-breakdown.md Summary

Create: `docs/projects/{project-name}/task-breakdown.md`

This is the master summary document listing all tasks with their metadata:

```markdown
# {Project Name} — Task Breakdown

## Overview
- **Project**: {project-name}
- **Phase**: {phase}
- **Total Tasks**: {N} in {M} waves
- **Estimated LOC**: ~{total} ({impl} impl + {tests} tests)

## Cross-Project Analysis
{Findings from Phase 2, or "No overlaps found."}

## Task Summary

### Wave 1: {Description}

#### T1: {Name}
- **Shortened Name**: {kebab-case}
- **Branch**: {project}/{task-name}
- **Estimated LOC**: ~{X} ({impl} + {tests})
- **Dependencies**: None
- **Code Reuse**: {Shared Component | Extends: {file} | New}
- **Task File**: `tasks/{task-name}.md`

#### T2: {Name}
...

### Wave 2: {Description}

#### T3: {Name}
- **Dependencies**: T1, T2
...
```

---

## Phase 7: Issue Creation (Optional)

Ask user if they want GitHub issues created. If yes:

```bash
gh label create "project:{name}" --color 0E8A16 2>/dev/null || true
```

For each task:
```bash
gh issue create \
  --title "T1: {Task Name}" \
  --label "enhancement,project:{name}" \
  --body "$(cat <<'EOF'
## Goal
{One sentence from task file Objective}

## Implementation
- **Location**: `src/{module}/{file}.py`
- **Test**: `tests/{module}/test_{file}.py`
- **Estimated LOC**: ~{X}

## Dependencies
{List or "None"}

## Task File
`docs/projects/{project-name}/tasks/{task-name}.md`

## Design Reference
`docs/projects/{project-name}/design.md` — Section {X}
EOF
)"
```

Update task files with issue numbers after creation.

---

## Phase 8: Validation

After generating all files:

1. **Dry run** the YAML to verify it parses correctly:
   ```bash
   PYTHONPATH=src:. python -m task_runner tasks/{project-name}.yaml --dry-run
   ```

2. **Verify all task files exist** at the paths referenced in YAML

3. **Check dependency graph** — no cycles, waves make sense

4. Present summary to user:
   ```
   Generated:
   - {N} task files in docs/projects/{project}/tasks/
   - tasks/{project}.yaml (N tasks in M waves)

   Dry run: OK
   ```

---

## Key Rules

**DO**:
- Read ALL project documents before breaking down tasks
- Check cross-project duplication BEFORE creating tasks
- Present task plan with LOC estimates and get user approval before generating files
- Include tests in every task (never separate test tasks)
- Target ~100 LOC per task
- Include LOC estimates and code reuse notes in task-breakdown.md
- Generate `task-breakdown.md` summary alongside individual task files
- Run `--dry-run` to validate YAML before finishing
- Use wave comments in YAML for readability
- Ask user about GitHub issue creation (optional)

**DON'T**:
- Skip cross-project analysis
- Include actual code in task files — describe WHAT, not HOW
- Create separate "test" tasks — tests are part of implementation
- Generate files without user approval of the breakdown plan
- Use relative paths in YAML `working_directory`
- Forget to set `PYTHONPATH=src:.` in verification commands
- Create GitHub issues without asking user first
