package me.mykindos.betterpvp.conventions;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceBaselineTest {

    private static final String KEY = "BANNER_COMMENT\tcore/src/main/java/A.java";

    @Test
    void ac2_fewerBreaksThanTheBaselineFailsAndAsksToReRecord() {
        List<String> failures = SourceBaseline.compare(Map.of(KEY, List.of(3)), Map.of(KEY, 2));

        assertEquals(1, failures.size());
        assertTrue(failures.get(0).contains("-PupdateBaseline"), failures.get(0));
    }

    @Test
    void ac2_fileWithAllBreaksFixedButStillInTheBaselineFails() {
        List<String> failures = SourceBaseline.compare(Map.of(), Map.of(KEY, 1));

        assertEquals(1, failures.size());
        assertTrue(failures.get(0).contains("-PupdateBaseline"), failures.get(0));
    }

    @Test
    void baselineRowForAnUnknownRuleAsksToReRecord() {
        List<String> failures = SourceBaseline.compare(Map.of(), Map.of("RETIRED_RULE\tcore/src/main/java/A.java", 1));

        assertEquals(1, failures.size());
        assertTrue(failures.get(0).contains("-PupdateBaseline"), failures.get(0));
    }

    @Test
    void breaksMatchingTheBaselinePass() {
        assertEquals(List.of(), SourceBaseline.compare(Map.of(KEY, List.of(3, 7)), Map.of(KEY, 2)));
    }

    @Test
    void breaksAboveTheBaselineFail() {
        List<String> failures = SourceBaseline.compare(Map.of(KEY, List.of(3, 7)), Map.of(KEY, 1));

        assertEquals(1, failures.size());
        assertTrue(failures.get(0).contains("core/src/main/java/A.java"), failures.get(0));
    }
}
