package me.mykindos.betterpvp.conventions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Compares the source convention breaks found per rule and file against the counts recorded in the baseline.
 */
final class SourceBaseline {

    private SourceBaseline() {
    }

    /**
     * One failure message per rule and file whose break count differs from its baseline. Keys are
     * {@code RULE_NAME\tpath}, found values are the breaking lines.
     */
    static List<String> compare(Map<String, List<Integer>> found, Map<String, Integer> baseline) {
        TreeSet<String> keys = new TreeSet<>(found.keySet());
        keys.addAll(baseline.keySet());
        List<String> failures = new ArrayList<>();
        for (String key : keys) {
            List<Integer> lines = found.getOrDefault(key, List.of());
            int allowed = baseline.getOrDefault(key, 0);
            String[] parts = key.split("\t");
            String description = SourceRule.valueOf(parts[0]).description();
            if (lines.size() > allowed) {
                failures.add(parts[1] + " lines " + lines + ": " + description + " (" + lines.size() + " found, "
                        + allowed + " allowed by the baseline). Fix them, or for a genuine exception add "
                        + "// conventions:allow " + parts[0] + " on that line.");
            } else if (lines.size() < allowed) {
                failures.add(parts[1] + ": " + description + " (" + lines.size() + " found, " + allowed
                        + " in the baseline). Re-record the baseline with ./gradlew :conventions:test "
                        + "-PupdateBaseline so fixed breaks cannot come back.");
            }
        }
        return failures;
    }
}
