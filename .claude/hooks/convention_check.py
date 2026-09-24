"""PostToolUse hook: flags house-rule violations in the text an Edit/Write just added to a Java or Kotlin file.

Only the new text is checked, so existing code never triggers it. Exit code 2 sends the findings back to Claude.
"""
import json
import re
import sys

FQCN = re.compile(r"\b(?:java|javax|org|net|com|me|io|lombok|kotlin)\.(?:[a-z_][a-z0-9_]*\.)+[A-Z]\w*")
BANNER = re.compile(r"//\s*[─━═=\-~*#]{3,}")
RECORD = re.compile(r"^\s*(?:(?:public|private|protected|static|final)\s+)*record\s+\w+\s*[(<]", re.M)
ARRAY_LOOP = re.compile(r"for\s*\([^:;)]+:\s*new\s+\w+\s*\[\s*\]\s*\{")
LOG_START = re.compile(r"\blog\.(?:info|warn|error|debug|trace)\s*\(")
HARDCODED_MSG = re.compile(r"UtilMessage\.(?:simpleMessage|message)\(\s*\w+\s*,\s*\"[^\"]*\"\s*,\s*\"[A-Za-z]")
REFACTOR_NARRATIVE = re.compile(r"(?://|\*).*\b(?:[Rr]eplaces|[Uu]sed to|[Pp]reviously|no longer)\b")


def added_text(tool_input):
    if "new_string" in tool_input:
        return tool_input.get("new_string") or ""
    if "edits" in tool_input:
        return "\n".join(e.get("new_string", "") for e in tool_input["edits"])
    return tool_input.get("content") or ""


def code_lines(text):
    for number, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        if stripped.startswith(("import ", "package ")):
            continue
        yield number, line


def unsubmitted_logs(text):
    for match in LOG_START.finditer(text):
        depth, i = 0, match.end() - 1
        while i < len(text):
            if text[i] == "(":
                depth += 1
            elif text[i] == ")":
                depth -= 1
            elif text[i] == ";" and depth == 0:
                break
            i += 1
        if ".submit()" not in text[match.start():i + 1]:
            yield text[match.start():match.start() + 60].splitlines()[0]


def main():
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    payload = json.load(sys.stdin)
    tool_input = payload.get("tool_input", {})
    path = tool_input.get("file_path", "")
    if not path.endswith((".java", ".kt")):
        return 0
    text = added_text(tool_input)
    problems = []
    for number, line in code_lines(text):
        no_strings = re.sub(r'"(?:\\.|[^"\\])*"', '""', line)
        if not no_strings.strip().startswith(("//", "*", "/*")) and FQCN.search(no_strings):
            problems.append(f"fully qualified name, add an import instead: {line.strip()[:100]}")
        if BANNER.search(line):
            problems.append(f"banner/divider comment: {line.strip()[:80]}")
        if ARRAY_LOOP.search(line):
            problems.append(f"loop over a throwaway array, unroll it: {line.strip()[:100]}")
        if HARDCODED_MSG.search(line):
            problems.append(f"hardcoded player-facing text, use Translations.component: {line.strip()[:100]}")
        if REFACTOR_NARRATIVE.search(line):
            problems.append(f"refactor narrative in a comment, describe the end state: {line.strip()[:100]}")
    if RECORD.search(text):
        problems.append("Java record, use a Lombok @Value/@Data class")
    for snippet in unsubmitted_logs(text):
        problems.append(f"log call without .submit(), nothing will be emitted: {snippet}")
    if not problems:
        return 0
    print(f"Convention check on {path}:", file=sys.stderr)
    for problem in dict.fromkeys(problems):
        print(f"- {problem}", file=sys.stderr)
    print("Fix these unless one is a genuine exception (e.g. a real name collision).", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main())
