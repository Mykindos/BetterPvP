"""Runs mutation testing on the classes a PR changes, in modules that have tests, and writes the scores to the job
summary. Reports only, it never fails the build.

    python3 mutation_report.py <base sha> <head sha>
"""
import os
import pathlib
import subprocess
import sys
import xml.etree.ElementTree as ElementTree
from collections import defaultdict


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout


def changed_classes(base, head):
    fork_point = git("merge-base", base, head).strip()
    by_module = defaultdict(list)
    for path in git("diff", "--name-only", "--diff-filter=AM", fork_point, head).split():
        if not path.endswith(".java") or "/src/main/java/" not in path or "/database/jooq/" in path:
            continue
        module, source = path.split("/src/main/java/", 1)
        if pathlib.Path(module, "src/test/java").is_dir():
            by_module[module].append(source[:-len(".java")].replace("/", ".") + "*")
    return by_module


def scores(module):
    report = pathlib.Path(module, "build/reports/pitest/mutations.xml")
    if not report.exists():
        return {}
    totals = defaultdict(lambda: {"total": 0, "killed": 0, "uncovered": 0})
    for mutation in ElementTree.parse(report).getroot():
        name = mutation.findtext("mutatedClass").split("$")[0]
        status = mutation.get("status")
        totals[name]["total"] += 1
        totals[name]["killed"] += status in ("KILLED", "TIMED_OUT", "MEMORY_ERROR")
        totals[name]["uncovered"] += status == "NO_COVERAGE"
    return totals


def main():
    base, head = sys.argv[1], sys.argv[2]
    gradlew = "./gradlew" if os.name != "nt" else "gradlew.bat"
    lines = ["## Mutation testing", ""]
    targets = changed_classes(base, head)
    if not targets:
        lines.append("No changed classes in modules with tests.")
    rows = []
    for module, classes in sorted(targets.items()):
        run = subprocess.run([gradlew, f":{module.replace('/', ':')}:pitest",
                              f"-PpitTargetClasses={','.join(classes)}", "--console=plain", "-q"])
        if run.returncode != 0:
            lines.append(f"Mutation testing failed to run for `{module}`. See the job log.")
        for name, t in sorted(scores(module).items()):
            score = 100 * t["killed"] // t["total"] if t["total"] else 0
            rows.append(f"| `{name}` | {t['total']} | {t['killed']} | {t['uncovered']} | {score}% |")
    if rows:
        lines += ["Share of deliberate code changes that a test caught. Uncovered means no test runs that code.", "",
                  "| Class | Mutants | Caught | Uncovered | Score |", "|---|---|---|---|---|", *rows]
    summary = "\n".join(lines) + "\n"
    print(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as out:
            out.write(summary)
    return 0


if __name__ == "__main__":
    sys.exit(main())
