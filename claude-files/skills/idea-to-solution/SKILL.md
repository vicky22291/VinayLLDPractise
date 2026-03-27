---
name: idea-to-solution
description: Autonomous experiment agent inspired by Karpathy's autoresearch. Iteratively improves a solution by forming hypotheses, editing code, testing, and keeping/discarding changes. Use when user says "run experiments", "optimize strategy", "idea to solution", or wants autonomous iterative improvement.
allowed-tools: "Read, Write, Edit, Bash, Grep, Glob, Agent"
metadata:
  version: 1.0.0
  category: automation
---

# Idea-to-Solution: Autonomous Experiment Agent

**When to Invoke**: User says "run experiments for {domain}", "optimize {solution}", "idea to solution", "/idea-to-solution {config-path}", or wants autonomous iterative improvement of any code.

**Inspiration**: Karpathy's autoresearch — hypothesis → edit → test → keep/discard → repeat.

---

## Core Concept

You are an autonomous experiment agent. Given:
1. A **config.yaml** pointing to solution file, run command, and metric
2. A **program.md** with domain-specific instructions
3. A **solution file** (the only file you edit)
4. **Infrastructure files** (read-only reference)

You run the experiment loop until stopped.

---

## Workflow

### Step 1: Setup
1. Read the config: `idea-to-solution/examples/{domain}/config.yaml`
2. Read the program.md for domain-specific instructions
3. Read infrastructure files for context
4. Read the current solution file
5. Run baseline: `python run.py` from the example directory
6. Record baseline metric in results.tsv

### Step 2: Experiment Loop (REPEAT)

For each experiment:

1. **Review state**: Read results.tsv to see what's been tried
2. **Form hypothesis**: One specific, testable change. Write it down.
3. **Edit solution file**: Make ONE change only (isolate variables)
4. **Commit**: `git add {solution_file} && git commit -m "exp-{N}: {hypothesis}"`
5. **Evaluate**: Run `python run.py` from the example directory
6. **Extract metric**: Parse the primary metric from output
7. **Decide**:
   - **KEEP** if metric improved → log as "keep", advance
   - **DISCARD** if metric worse → `git reset --hard HEAD~1`, log as "discard"
   - **CRASH** if run failed → read error, log as "crash", fix or skip
8. **Log**: Append row to results.tsv
9. **Report**: `EXP-{id}: {status} | metric: {before} → {after} | {hypothesis}`
10. **GOTO 1**

### Step 3: Summary
After every 5 experiments (or when stopped), print:
- Total experiments, kept, discarded, crashed
- Best metric achieved
- Top 3 improvements with their hypotheses
- Current solution state

---

## Rules

1. **ONE change per experiment.** Never combine multiple ideas.
2. **Log everything.** Even crashes and failures teach something.
3. **Read the log before each experiment.** Don't repeat failed approaches.
4. **Simplicity wins.** Removing code for equal performance = improvement.
5. **Watch for overfitting.** If train metric >> validation metric, discard.
6. **Stay in scope.** Only edit the solution file.
7. **Be honest.** Tiny improvement + lots of complexity = discard.

## Results.tsv Format

```
experiment_id	hypothesis	change_summary	metric_before	metric_after	delta	status	timestamp
```

## Directory Structure

```
idea-to-solution/
├── harness.py              # Python harness (optional, for programmatic use)
├── agent_loop.py           # Context builder and evaluation runner
├── program.md              # General agent instructions
├── examples/
│   └── trading-strategy/
│       ├── config.yaml     # Domain config
│       ├── program.md      # Trading-specific instructions
│       ├── backtest.py     # READ-ONLY: data, indicators, engine
│       ├── strategy.py     # AGENT EDITS THIS
│       ├── run.py          # READ-ONLY: evaluator
│       └── results.tsv     # Experiment log (created at runtime)
```

## Creating New Domains

To apply this pattern to a new problem:

1. Create `examples/{domain}/config.yaml` with:
   - `solution_file`: the file the agent edits
   - `run_command`: how to evaluate
   - `metric_name`: what to optimize
   - `metric_direction`: "maximize" or "minimize"
2. Create `examples/{domain}/program.md` with domain-specific guidance
3. Create the infrastructure file(s) (read-only)
4. Create the initial solution file (agent will improve it)
5. Create `run.py` that prints `metric_name: value`
