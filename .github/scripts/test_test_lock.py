"""Tests for test_lock.py. Run with `python -m unittest discover -s .github/scripts`."""
import unittest

import test_lock


class LockedFilesTest(unittest.TestCase):

    def test_testSourcesAreLocked(self):
        path = "core/src/test/java/a/ATest.java"
        self.assertEqual([path], test_lock.locked_files([path, "core/src/main/java/a/A.java"]))

    def test_recordedBaselinesAreNotLocked(self):
        self.assertEqual([], test_lock.locked_files([
            "conventions/src/test/resources/source-baseline.tsv",
            "conventions/src/test/resources/archunit_store/stored.rules",
        ]))


if __name__ == "__main__":
    unittest.main()
