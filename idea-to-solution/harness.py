"""
Idea-to-Solution Harness
========================
Orchestrates the autonomous experiment loop:
  hypothesis → edit → test → keep/discard → repeat

Inspired by Karpathy's autoresearch, generalized to any domain.

Usage:
    python harness.py --config examples/trading-strategy/config.yaml

The harness:
  1. Reads config to find: solution file, run command, metric name, metric direction
  2. Creates a git branch for the experiment run
  3. Runs baseline evaluation
  4. Enters the experiment loop (driven by an AI agent or manual iteration)
"""

import argparse
import csv
import datetime
import json
import os
import re
import subprocess
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path

import yaml


@dataclass
class ExperimentConfig:
    """Configuration for an experiment domain."""

    name: str  # e.g., "trading-strategy"
    solution_file: str  # file the agent edits, e.g., "strategy.py"
    run_command: str  # e.g., "python run.py"
    metric_name: str  # e.g., "sharpe_ratio"
    metric_direction: str  # "maximize" or "minimize"
    timeout_seconds: int = 300  # max time per experiment run
    infrastructure_files: list[str] = field(
        default_factory=list
    )  # read-only reference files
    program_md: str = "program.md"  # agent instructions

    @classmethod
    def from_yaml(cls, path: str) -> "ExperimentConfig":
        with open(path) as f:
            data = yaml.safe_load(f)
        return cls(**data)


@dataclass
class ExperimentResult:
    experiment_id: int
    hypothesis: str
    change_summary: str
    metric_before: float | None
    metric_after: float | None
    delta: float | None
    status: str  # "keep", "discard", "crash"
    timestamp: str
    duration_seconds: float = 0.0


class ExperimentLog:
    """Manages results.tsv — the experiment journal."""

    FIELDS = [
        "experiment_id",
        "hypothesis",
        "change_summary",
        "metric_before",
        "metric_after",
        "delta",
        "status",
        "duration_seconds",
        "timestamp",
    ]

    def __init__(self, log_path: str):
        self.log_path = Path(log_path)
        if not self.log_path.exists():
            with open(self.log_path, "w", newline="") as f:
                writer = csv.writer(f, delimiter="\t")
                writer.writerow(self.FIELDS)

    def append(self, result: ExperimentResult):
        with open(self.log_path, "a", newline="") as f:
            writer = csv.writer(f, delimiter="\t")
            writer.writerow(
                [
                    result.experiment_id,
                    result.hypothesis,
                    result.change_summary,
                    result.metric_before,
                    result.metric_after,
                    result.delta,
                    result.status,
                    result.duration_seconds,
                    result.timestamp,
                ]
            )

    def read_all(self) -> list[dict]:
        if not self.log_path.exists():
            return []
        with open(self.log_path) as f:
            reader = csv.DictReader(f, delimiter="\t")
            return list(reader)

    def next_id(self) -> int:
        rows = self.read_all()
        if not rows:
            return 1
        return max(int(r["experiment_id"]) for r in rows) + 1

    def summary(self) -> dict:
        """Return a summary of experiments so far."""
        rows = self.read_all()
        total = len(rows)
        kept = sum(1 for r in rows if r["status"] == "keep")
        discarded = sum(1 for r in rows if r["status"] == "discard")
        crashed = sum(1 for r in rows if r["status"] == "crash")
        best_metric = None
        for r in rows:
            if r["status"] == "keep" and r["metric_after"]:
                val = float(r["metric_after"])
                if best_metric is None or val > best_metric:
                    best_metric = val
        return {
            "total": total,
            "kept": kept,
            "discarded": discarded,
            "crashed": crashed,
            "best_metric": best_metric,
        }


class GitTracker:
    """Git-based experiment tracking: each experiment = a commit."""

    def __init__(self, work_dir: str):
        self.work_dir = work_dir

    def _run(self, *args: str) -> str:
        result = subprocess.run(
            ["git"] + list(args),
            cwd=self.work_dir,
            capture_output=True,
            text=True,
        )
        return result.stdout.strip()

    def create_branch(self, tag: str):
        branch = f"experiment/{tag}"
        self._run("checkout", "-b", branch)
        return branch

    def commit(self, message: str):
        self._run("add", "-A")
        self._run("commit", "-m", message)

    def get_current_commit(self) -> str:
        return self._run("rev-parse", "HEAD")

    def reset_to(self, commit_hash: str):
        self._run("reset", "--hard", commit_hash)

    def diff_summary(self) -> str:
        return self._run("diff", "--stat")


class ExperimentRunner:
    """Runs a single experiment: execute the solution and extract the metric."""

    def __init__(self, config: ExperimentConfig, work_dir: str):
        self.config = config
        self.work_dir = work_dir

    def run(self) -> tuple[float | None, str, bool]:
        """
        Run the experiment. Returns (metric_value, output_log, success).
        """
        start = time.time()
        try:
            result = subprocess.run(
                self.config.run_command.split(),
                cwd=self.work_dir,
                capture_output=True,
                text=True,
                timeout=self.config.timeout_seconds,
            )
            elapsed = time.time() - start
            output = result.stdout + "\n" + result.stderr

            if result.returncode != 0:
                return None, output, False

            metric = self._extract_metric(output)
            return metric, output, True

        except subprocess.TimeoutExpired:
            return None, f"TIMEOUT after {self.config.timeout_seconds}s", False
        except Exception as e:
            return None, str(e), False

    def _extract_metric(self, output: str) -> float | None:
        """Extract the metric value from run output.

        Looks for patterns like:
            metric_name: 1.234
            metric_name=1.234
            {"metric_name": 1.234}
        """
        patterns = [
            rf"{self.config.metric_name}\s*[:=]\s*([-+]?\d*\.?\d+)",
            rf'"{self.config.metric_name}"\s*:\s*([-+]?\d*\.?\d+)',
        ]
        for pattern in patterns:
            match = re.search(pattern, output)
            if match:
                return float(match.group(1))
        return None


class Harness:
    """The main experiment harness — orchestrates the full loop."""

    def __init__(self, config: ExperimentConfig, work_dir: str, run_tag: str):
        self.config = config
        self.work_dir = work_dir
        self.run_tag = run_tag
        self.log = ExperimentLog(os.path.join(work_dir, "results.tsv"))
        self.git = GitTracker(work_dir)
        self.runner = ExperimentRunner(config, work_dir)
        self.current_best: float | None = None

    def is_improvement(self, old: float | None, new: float | None) -> bool:
        if old is None or new is None:
            return new is not None
        if self.config.metric_direction == "maximize":
            return new > old
        return new < old

    def run_baseline(self) -> float | None:
        """Run the unmodified solution to establish a baseline."""
        print("=" * 60)
        print("BASELINE RUN")
        print("=" * 60)
        metric, output, success = self.runner.run()
        if success and metric is not None:
            self.current_best = metric
            self.log.append(
                ExperimentResult(
                    experiment_id=0,
                    hypothesis="baseline",
                    change_summary="unmodified solution",
                    metric_before=None,
                    metric_after=metric,
                    delta=None,
                    status="baseline",
                    timestamp=datetime.datetime.now().isoformat(),
                )
            )
            print(f"Baseline {self.config.metric_name}: {metric}")
        else:
            print(f"Baseline FAILED:\n{output}")
        return metric

    def run_experiment(self, hypothesis: str, change_summary: str) -> ExperimentResult:
        """Run one experiment iteration. The solution file should already be edited."""
        exp_id = self.log.next_id()
        timestamp = datetime.datetime.now().isoformat()
        before_commit = self.git.get_current_commit()
        metric_before = self.current_best

        # Commit the change
        self.git.commit(f"exp-{exp_id}: {hypothesis[:80]}")

        # Run evaluation
        start = time.time()
        metric_after, output, success = self.runner.run()
        duration = time.time() - start

        if not success or metric_after is None:
            # Crash — reset
            self.git.reset_to(before_commit)
            result = ExperimentResult(
                experiment_id=exp_id,
                hypothesis=hypothesis,
                change_summary=change_summary,
                metric_before=metric_before,
                metric_after=metric_after,
                delta=None,
                status="crash",
                timestamp=timestamp,
                duration_seconds=duration,
            )
            print(f"  EXP-{exp_id}: CRASH — {output[:200]}")

        elif self.is_improvement(metric_before, metric_after):
            # Improvement — keep
            delta = (
                (metric_after - metric_before) if metric_before is not None else None
            )
            self.current_best = metric_after
            result = ExperimentResult(
                experiment_id=exp_id,
                hypothesis=hypothesis,
                change_summary=change_summary,
                metric_before=metric_before,
                metric_after=metric_after,
                delta=delta,
                status="keep",
                timestamp=timestamp,
                duration_seconds=duration,
            )
            print(
                f"  EXP-{exp_id}: KEEP — {self.config.metric_name}: "
                f"{metric_before} → {metric_after} (delta: {delta:+.6f})"
            )

        else:
            # No improvement — discard
            delta = (
                (metric_after - metric_before) if metric_before is not None else None
            )
            self.git.reset_to(before_commit)
            result = ExperimentResult(
                experiment_id=exp_id,
                hypothesis=hypothesis,
                change_summary=change_summary,
                metric_before=metric_before,
                metric_after=metric_after,
                delta=delta,
                status="discard",
                timestamp=timestamp,
                duration_seconds=duration,
            )
            print(
                f"  EXP-{exp_id}: DISCARD — {self.config.metric_name}: "
                f"{metric_before} → {metric_after} (delta: {delta:+.6f})"
            )

        self.log.append(result)
        return result

    def print_summary(self):
        summary = self.log.summary()
        print("\n" + "=" * 60)
        print("EXPERIMENT SUMMARY")
        print("=" * 60)
        print(f"  Total experiments: {summary['total']}")
        print(f"  Kept:     {summary['kept']}")
        print(f"  Discarded: {summary['discarded']}")
        print(f"  Crashed:  {summary['crashed']}")
        print(f"  Best {self.config.metric_name}: {summary['best_metric']}")
        print(f"  Current best: {self.current_best}")
        print("=" * 60)


def main():
    parser = argparse.ArgumentParser(description="Idea-to-Solution Experiment Harness")
    parser.add_argument(
        "--config", required=True, help="Path to config.yaml for the domain"
    )
    parser.add_argument(
        "--tag", default=None, help="Run tag (default: timestamp-based)"
    )
    parser.add_argument(
        "--baseline-only", action="store_true", help="Only run the baseline, then exit"
    )
    args = parser.parse_args()

    config = ExperimentConfig.from_yaml(args.config)
    work_dir = str(Path(args.config).parent)
    tag = args.tag or datetime.datetime.now().strftime("%Y%m%d-%H%M")

    print(f"Domain: {config.name}")
    print(f"Solution file: {config.solution_file}")
    print(f"Metric: {config.metric_name} ({config.metric_direction})")
    print(f"Run command: {config.run_command}")
    print(f"Timeout: {config.timeout_seconds}s per experiment")
    print(f"Work dir: {work_dir}")
    print()

    harness = Harness(config, work_dir, tag)
    baseline = harness.run_baseline()

    if args.baseline_only:
        harness.print_summary()
        return

    # Interactive mode: the agent (or human) drives the loop
    print("\nHarness ready. The experiment loop is now controlled by the AI agent.")
    print("The agent should:")
    print(f"  1. Edit {config.solution_file}")
    print("  2. Call harness.run_experiment(hypothesis, change_summary)")
    print("  3. Review results.tsv")
    print("  4. Repeat")


if __name__ == "__main__":
    main()
