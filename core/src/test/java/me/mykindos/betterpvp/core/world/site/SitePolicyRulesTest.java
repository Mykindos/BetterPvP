package me.mykindos.betterpvp.core.world.site;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks admission and selection on their own, without any instances involved.
 */
@DisplayName("Site admission and selection")
class SitePolicyRulesTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final SiteKey KEY = SiteKey.of("spawn");

    @Test
    @DisplayName("party admission lets a crewmate follow you in but keeps strangers out")
    void partyAdmission() {
        final Admission admission = Admission.party();
        final Party crew = Party.of(ALICE, Set.of(BOB));

        assertTrue(admission.admits(KEY, Set.of(), crew), "an empty instance takes anybody");
        assertTrue(admission.admits(KEY, Set.of(ALICE), crew), "a crewmate is already aboard");
        assertFalse(admission.admits(KEY, Set.of(UUID.randomUUID()), crew), "a stranger's instance is not ours");
    }

    @Test
    @DisplayName("fill-first packs the busiest instance, spread takes the emptiest")
    void selectionRules() {
        final List<Integer> counts = List.of(3, 9, 1);

        assertEquals(1, Selection.fillFirst().select(counts));
        assertEquals(2, Selection.spread().select(counts));
    }

    @Test
    @DisplayName("a party always contains its leader, and a site key round-trips through storage")
    void valueTypes() {
        assertTrue(Party.of(ALICE, Set.of(BOB)).has(ALICE));
        assertEquals(2, Party.of(ALICE, Set.of(BOB)).size());
        assertEquals(SiteKey.of("camp", 12L), SiteKey.parse(SiteKey.of("camp", 12L).toString()));
        assertEquals(SiteKey.of("spawn"), SiteKey.parse("spawn"));
    }
}
