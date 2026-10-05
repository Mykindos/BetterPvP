package me.mykindos.betterpvp.conventions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks every source file against {@link SourceRule}. Breaks that already exist are counted per file and rule in
 * {@code source-baseline.tsv}, and any count that differs from its baseline fails. Run
 * {@code ./gradlew :conventions:test -PupdateBaseline} to rewrite the baseline after fixing old breaks.
 */
class SourceConventionsTest {

    private static final Path BASELINE = Path.of("src/test/resources/source-baseline.tsv");

    @Test
    void sourcesAddNoNewConventionBreaks() throws IOException {
        Map<String, List<Integer>> found = new TreeMap<>();
        for (Path file : Codebase.javaSources()) {
            JavaText text = new JavaText(Codebase.read(file));
            for (SourceRule rule : SourceRule.values()) {
                List<Integer> lines = rule.violations(text);
                if (!lines.isEmpty()) {
                    found.put(rule.name() + "\t" + Codebase.relative(file), lines);
                }
            }
        }

        if (Codebase.UPDATE_BASELINE) {
            List<String> rows = new ArrayList<>();
            found.forEach((key, lines) -> rows.add(key + "\t" + lines.size()));
            Files.write(BASELINE, rows);
            return;
        }

        List<String> failures = SourceBaseline.compare(found, readBaseline());
        if (!failures.isEmpty()) {
            fail("Source convention breaks differ from the baseline:\n" + String.join("\n", failures));
        }
    }

    private static Map<String, Integer> readBaseline() throws IOException {
        Map<String, Integer> baseline = new TreeMap<>();
        if (!Files.exists(BASELINE)) {
            return baseline;
        }
        for (String row : Files.readAllLines(BASELINE)) {
            if (row.isBlank()) {
                continue;
            }
            int last = row.lastIndexOf('\t');
            baseline.put(row.substring(0, last), Integer.parseInt(row.substring(last + 1)));
        }
        return baseline;
    }
}
