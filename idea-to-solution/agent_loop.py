"""
Agent Loop — The autonomous experiment driver.
================================================
This script runs the full autoresearch-style loop:
  1. Read current state (solution file, results log, git history)
  2. Generate context for the AI agent
  3. Execute experiments and track results

Can be used in two modes:
  - Interactive: human drives each experiment step
  - Autonomous: feeds context to an AI agent via stdin/stdout protocol

Usage:
    # Run the baseline and print context for the agent
    python agent_loop.py --config examples/trading-strategy/config.yaml --baseline

    # Run a single experiment (agent provides hypothesis + edits externally)
    python agent_loop.py --config examples/trading-strategy/config.yaml --evaluate

    # Print full context (solution + results + program) for the agent
    python agent_loop.py --config examples/trading-strategy/config.yaml --context
"""

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

import yaml


def load_config(config_path: str) -> dict:
    with open(config_path) as f:
        return yaml.safe_load(f)


def get_work_dir(config_path: str) -> str:
    return str(Path(config_path).parent)


def read_file(work_dir: str, filename: str) -> str:
    path = os.path.join(work_dir, filename)
    if os.path.exists(path):
        with open(path) as f:
            return f.read()
    return f"[File not found: {filename}]"


def read_results_log(work_dir: str) -> str:
    path = os.path.join(work_dir, "results.tsv")
    if os.path.exists(path):
        with open(path) as f:
            return f.read()
    return "[No experiments yet]"


def get_git_log(work_dir: str, n: int = 20) -> str:
    try:
        result = subprocess.run(
            ["git", "log", "--oneline", f"-{n}"],
            cwd=work_dir,
            capture_output=True,
            text=True,
        )
        return result.stdout.strip() or "[No git history]"
    except Exception:
        return "[Git not available]"


def run_evaluation(work_dir: str, run_command: str, timeout: int = 120) -> dict:
    """Execute the run command and parse output."""
    try:
        result = subprocess.run(
            run_command.split(),
            cwd=work_dir,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        output = result.stdout
        stderr = result.stderr

        if result.returncode != 0:
            return {
                "success": False,
                "error": stderr or output,
                "output": output,
            }

        # Try to extract JSON from output
        for line in output.split("\n"):
            line = line.strip()
            if line.startswith("{") and line.endswith("}"):
                try:
                    metrics = json.loads(line)
                    return {"success": True, "metrics": metrics, "output": output}
                except json.JSONDecodeError:
                    pass

        return {"success": True, "metrics": {}, "output": output}

    except subprocess.TimeoutExpired:
        return {"success": False, "error": f"Timeout after {timeout}s", "output": ""}
    except Exception as e:
        return {"success": False, "error": str(e), "output": ""}


def build_agent_context(config_path: str) -> str:
    """Build the full context string that an AI agent needs to run the next experiment."""
    config = load_config(config_path)
    work_dir = get_work_dir(config_path)

    sections = []

    # 1. Program instructions
    program = read_file(work_dir, config.get("program_md", "program.md"))
    sections.append(f"# AGENT INSTRUCTIONS\n\n{program}")

    # 2. Current solution
    solution = read_file(work_dir, config["solution_file"])
    sections.append(
        f"# CURRENT SOLUTION ({config['solution_file']})\n\n```python\n{solution}\n```"
    )

    # 3. Infrastructure files (read-only reference)
    for infra_file in config.get("infrastructure_files", []):
        content = read_file(work_dir, infra_file)
        sections.append(
            f"# INFRASTRUCTURE ({infra_file}) — READ ONLY\n\n```python\n{content}\n```"
        )

    # 4. Experiment log
    results = read_results_log(work_dir)
    sections.append(f"# EXPERIMENT LOG (results.tsv)\n\n```\n{results}\n```")

    # 5. Git history
    git_log = get_git_log(work_dir)
    sections.append(f"# GIT HISTORY (recent)\n\n```\n{git_log}\n```")

    # 6. Config summary
    sections.append(
        f"# CONFIG\n\n"
        f"- Metric: **{config['metric_name']}** ({config['metric_direction']})\n"
        f"- Run command: `{config['run_command']}`\n"
        f"- Timeout: {config.get('timeout_seconds', 120)}s\n"
        f"- Solution file: `{config['solution_file']}`\n"
    )

    return "\n\n---\n\n".join(sections)


def main():
    parser = argparse.ArgumentParser(description="Agent Loop — Experiment Driver")
    parser.add_argument("--config", required=True, help="Path to config.yaml")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument(
        "--baseline", action="store_true", help="Run baseline evaluation"
    )
    group.add_argument(
        "--evaluate", action="store_true", help="Evaluate current solution"
    )
    group.add_argument(
        "--context", action="store_true", help="Print full agent context"
    )
    args = parser.parse_args()

    config = load_config(args.config)
    work_dir = get_work_dir(args.config)

    if args.context:
        print(build_agent_context(args.config))

    elif args.baseline or args.evaluate:
        result = run_evaluation(
            work_dir,
            config["run_command"],
            config.get("timeout_seconds", 120),
        )
        if result["success"]:
            print(result["output"])
            if result["metrics"]:
                print(f"\nParsed metrics: {json.dumps(result['metrics'], indent=2)}")
        else:
            print(f"FAILED: {result['error']}", file=sys.stderr)
            if result["output"]:
                print(result["output"])
            sys.exit(1)


if __name__ == "__main__":
    main()
