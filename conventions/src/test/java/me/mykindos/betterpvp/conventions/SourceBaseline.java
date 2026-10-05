package me.mykindos.betterpvp.conventions;

import java.util.List;
import java.util.Map;

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
        throw new UnsupportedOperationException();
    }
}
