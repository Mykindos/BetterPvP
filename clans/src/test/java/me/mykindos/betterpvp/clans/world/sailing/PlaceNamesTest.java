package me.mykindos.betterpvp.clans.world.sailing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceNamesTest {

    @Test
    @DisplayName("generate always produces a non-blank two-word name")
    void generateIsNeverBlank() {
        for (int i = 0; i < 200; i++) {
            final String name = PlaceNames.generate();
            assertFalse(name.isBlank());
            assertTrue(name.contains(" "), "expected a two-word name, got: " + name);
        }
    }

    @Test
    @DisplayName("generate produces a reasonable variety of names across many calls")
    void generateHasVariety() {
        final Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(PlaceNames.generate());
        }
        assertTrue(seen.size() > 20, "expected varied names, got only " + seen.size() + " distinct values");
    }
}
