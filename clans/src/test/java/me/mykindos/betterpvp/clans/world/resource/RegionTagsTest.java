package me.mykindos.betterpvp.clans.world.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionTagsTest {

    @Test
    @DisplayName("bare markers are detected case-insensitively")
    void bareMarkers() {
        final RegionTags tags = new RegionTags(Set.of("Tree", "WILLOW"));
        assertTrue(tags.has("tree"));
        assertTrue(tags.has("willow"));
        assertFalse(tags.has("ore"));
        assertTrue(tags.markers().contains("tree"));
    }

    @Test
    @DisplayName("key:value pairs parse with case-insensitive keys and case-preserving values")
    void keyValuePairs() {
        final RegionTags tags = new RegionTags(Set.of("Level:67", "name:Willow Tree", "node:willow"));
        assertEquals(67, tags.getInt("level", 1));
        assertEquals("Willow Tree", tags.getString("name", "x"));
        assertEquals("willow", tags.get("node").orElse(null));
        assertTrue(tags.has("level"));
        assertTrue(tags.has("name"));
    }

    @Test
    @DisplayName("missing keys and malformed numbers fall back to defaults")
    void defaults() {
        final RegionTags tags = new RegionTags(Set.of("ore", "level:abc"));
        assertEquals(25, tags.getInt("level", 25));
        assertEquals(5.0, tags.getDouble("respawn", 5.0));
        assertEquals("none", tags.getString("name", "none"));
        assertTrue(tags.get("missing").isEmpty());
    }

    @Test
    @DisplayName("blank and null tags are ignored")
    void blankAndNull() {
        final java.util.Set<String> raw = new java.util.HashSet<>();
        raw.add("ore");
        raw.add("  ");
        raw.add(null);
        final RegionTags tags = new RegionTags(raw);
        assertTrue(tags.has("ore"));
        assertEquals(1, tags.markers().size());
    }

    @Test
    @DisplayName("double values parse")
    void doubles() {
        final RegionTags tags = new RegionTags(Set.of("respawn:12.5"));
        assertEquals(12.5, tags.getDouble("respawn", 1.0));
    }
}
