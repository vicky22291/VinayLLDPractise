---
name: run-tasks
description: Starts the task runner (v2) to automate task implementation for a project. Use when user says "run tasks for {project}", "automate {project}", "start implementing tasks", or wants to run background task automation.
allowed-tools: "Bash(PYTHONPATH=src:. python -m task_runner *), Bash(source *)"
metadata:
  version: 2.0.0
  category: automation
---

# Run Tasks

**When to Invoke**: User says "run tasks for {project}", "automate {project}", "start task runner for {project}", or "/run-tasks {project}"

**Purpose**: Launch task runner (v2) to autonomously implement project tasks using YAML-based task definitions

---

## Workflow (3-Step Process)

### Step 1: Validate Prerequisites and Project
- Check if virtualenv exists: `.venv/bin/activate`
- Check if task runner exists: `src/task_runner/`
- Check if YAML task file exists: `tasks/{project}.yaml`
- If any missing, inform user and exit

### Step 2: Start Task Runner
- Ensure virtualenv is activated: `source .venv/bin/activate`
- Run: `python -m task_runner tasks/{project}.yaml`
- For dry run: `python -m task_runner tasks/{project}.yaml --dry-run`
- For resume: `python -m task_runner tasks/{project}.yaml --resume`

### Step 3: Inform User
- Show concise confirmation with key commands only
- Keep output brief: status and confirmation

---

## Examples

### Example 1: Start Task Runner for Project
**User**: "Run tasks for my-awesome-feature"
**Action**: Validate YAML exists, start runner
**Output**:
```
Running task runner for: tasks/my-awesome-feature.yaml
```

### Example 2: Project Doesn't Exist
**User**: "Run tasks for nonexistent-project"
**Output**:
```
Project 'nonexistent-project' not found
Expected: tasks/nonexistent-project.yaml
```

### Example 3: Resume After Interruption
**User**: "Resume tasks for my-project"
**Output**:
```
Resuming task runner for: tasks/my-project.yaml (skipping completed tasks)
```

---

## Key Rules

**Do's**:
- Always validate YAML task file exists before starting
- Activate virtualenv before running
- Use `--dry-run` to preview execution plan first
- Use `--resume` to skip already-completed tasks

**Don'ts**:
- Don't skip project validation
- Don't forget to activate virtualenv
- Don't start multiple instances for same project

---

## Commands Reference

**Full pipeline**:
```bash
python -m task_runner tasks/{project}.yaml
```

**Dry run (preview)**:
```bash
python -m task_runner tasks/{project}.yaml --dry-run
```

**Single task**:
```bash
python -m task_runner tasks/{project}.yaml --task T1
```

**Resume (skip completed)**:
```bash
python -m task_runner tasks/{project}.yaml --resume
```

**Verbose logging**:
```bash
python -m task_runner tasks/{project}.yaml -v
```
