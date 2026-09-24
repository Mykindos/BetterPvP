"""Stop hook: compiles the Gradle modules whose Java or Kotlin sources changed, and hands errors back to Claude.

Skips when nothing changed since the last successful compile, so turns without code edits cost nothing.
"""
import hashlib
import json
import os
import pathlib
import subprocess
import sys

root = pathlib.Path(os.environ.get("CLAUDE_PROJECT_DIR", ".")).resolve()
stamp = root / ".claude" / ".compile-stamp"


def git(*args, cwd=root):
    return subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True).stdout


def changed_sources():
    files = set(git("diff", "--name-only", "HEAD").split())
    files |= set(git("ls-files", "--others", "--exclude-standard").split())
    return sorted(f for f in files if f.endswith((".java", ".kt")) and "/src/" in f)


def module_of(path):
    parts = path.split("/src/")[0].split("/")
    return ":" + ":".join(parts)


def main():
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    payload = json.load(sys.stdin)
    files = changed_sources()
    if not files:
        return 0
    digest = hashlib.sha256()
    for f in files:
        digest.update(f.encode())
        p = root / f
        if p.exists():
            digest.update(p.read_bytes())
    fingerprint = digest.hexdigest()
    if stamp.exists() and stamp.read_text() == fingerprint:
        return 0

    modules = sorted({module_of(f) for f in files})
    gradlew = str(root / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    result = subprocess.run([gradlew, *[f"{m}:classes" for m in modules], "-q", "--console=plain"],
                            cwd=root, capture_output=True, text=True)
    if result.returncode == 0:
        stamp.write_text(fingerprint)
        return 0
    if payload.get("stop_hook_active"):
        return 0
    output = (result.stdout + result.stderr).strip().splitlines()
    errors = []
    for i, line in enumerate(output):
        if ": error:" in line or line.startswith("e: "):
            errors.extend(output[i:i + 3])
    print(f"Compile failed for {', '.join(modules)}:", file=sys.stderr)
    print("\n".join((errors or [line for line in output if "warning:" not in line])[-60:]), file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main())
