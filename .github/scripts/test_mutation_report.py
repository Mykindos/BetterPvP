"""Tests for mutation_report.py. Run with `python -m unittest discover -s .github/scripts`."""
import os
import pathlib
import subprocess
import tempfile
import unittest

import mutation_report


def git(cwd, *args):
    subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True, check=True)


def head(cwd):
    return subprocess.run(["git", "rev-parse", "HEAD"], cwd=cwd, capture_output=True, text=True,
                          check=True).stdout.strip()


class ChangedClassesTest(unittest.TestCase):

    def setUp(self):
        self.repo = tempfile.TemporaryDirectory()
        self.addCleanup(self.repo.cleanup)
        root = pathlib.Path(self.repo.name)
        git(root, "init", "-q")
        git(root, "config", "user.email", "test@example.com")
        git(root, "config", "user.name", "Test")
        (root / "core/src/main/java/a").mkdir(parents=True)
        (root / "core/src/test/java/a").mkdir(parents=True)
        (root / "core/src/test/java/a/OldTest.java").write_text("package a;\nclass OldTest {}\n")
        self.lines = [f"    int field{i};" for i in range(10)]
        (root / "core/src/main/java/a/Old.java").write_text("package a;\nclass Old {\n" + "\n".join(self.lines)
                                                             + "\n}\n")
        git(root, "add", "-A")
        git(root, "commit", "-q", "-m", "base")
        self.root = root
        self.base = head(root)
        cwd = os.getcwd()
        os.chdir(root)
        self.addCleanup(os.chdir, cwd)

    def test_ac4_renamedAndChangedClassIsScored(self):
        git(self.root, "mv", "core/src/main/java/a/Old.java", "core/src/main/java/a/New.java")
        (self.root / "core/src/main/java/a/New.java").write_text(
            "package a;\nclass New {\n" + "\n".join(self.lines) + "\n    int added;\n}\n")
        git(self.root, "commit", "-q", "-am", "rename")

        classes = mutation_report.changed_classes(self.base, head(self.root))

        self.assertIn("a.New", classes.get("core", []))

    def test_changedClassIsScored(self):
        (self.root / "core/src/main/java/a/Old.java").write_text("package a;\nclass Old { int added; }\n")
        git(self.root, "commit", "-q", "-am", "change")

        classes = mutation_report.changed_classes(self.base, head(self.root))

        self.assertEqual(["a.Old", "a.Old$*"], classes.get("core"))


if __name__ == "__main__":
    unittest.main()
