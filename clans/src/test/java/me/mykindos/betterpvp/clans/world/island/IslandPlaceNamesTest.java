package me.mykindos.betterpvp.clans.world.island;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IslandPlaceNamesTest {

    @Test
    @DisplayName("generate always produces a non-blank two-word name")
    void generateIsNeverBlank() {
        for (int i = 0; i < 200; i++) {
            final String name = IslandPlaceNames.generate();
            assertFalse(name.isBlank());
            assertTrue(name.contains(" "), "expected a two-word name, got: " + name);
        }
    }

    @Test
    @DisplayName("generate produces a reasonable variety of names across many calls")
    void generateHasVariety() {
        final Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(IslandPlaceNames.generate());
        }
        assertTrue(seen.size() > 20, "expected varied names, got only " + seen.size() + " distinct values");
    }
}
