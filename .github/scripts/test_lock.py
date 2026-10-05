"""Fails when a commit after a PR's first `test:` commit changes src/test/ without its own `test:` prefix.

Tests are written and approved first, then the implementation has to pass them as they are. The recorded convention
baselines are the exception, since re-recording them is part of fixing an old break.

    python3 test_lock.py <base sha> <head sha>
"""
import subprocess
import sys


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout


def locked_files(files):
    """The test files among these paths. The recorded convention baselines are generated, so they stay editable."""
    baselines = ("conventions/src/test/resources/source-baseline.tsv", "conventions/src/test/resources/archunit_store/")
    return [f for f in files if "/src/test/" in f"/{f}" and not f.startswith(baselines)]


def main():
    base, head = sys.argv[1], sys.argv[2]
    fork_point = git("merge-base", base, head).strip()
    commits = [line.split("\t", 1) for line in
               git("log", "--reverse", "--no-merges", "--format=%H\t%s", f"{fork_point}..{head}").splitlines()]
    locked = False
    broken = []
    for sha, subject in commits:
        if subject.startswith("test:"):
            locked = True
            continue
        if not locked:
            continue
        files = git("diff-tree", "--no-commit-id", "--name-only", "-r", sha).split()
        tests = locked_files(files)
        if tests:
            broken.append(f"{sha[:8]} {subject}\n    " + "\n    ".join(tests))

    if not locked:
        print("No test: commit on this PR, nothing to check.")
        return 0
    if broken:
        print("These commits change tests after they were committed, without a test: prefix:\n")
        print("\n".join(broken))
        print("\nIf a test really was wrong, put that change in its own commit starting with test: so the reviewer "
              "sees it.")
        return 1
    print("Tests were not changed after their test: commit.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
