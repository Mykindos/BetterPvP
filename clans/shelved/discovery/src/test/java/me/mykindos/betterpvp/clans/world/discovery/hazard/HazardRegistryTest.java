package me.mykindos.betterpvp.clans.world.discovery.hazard;

import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HazardRegistryTest {

    @Test
    @DisplayName("an archetype is found under the name it claims")
    void archetypesAreFoundByTheirKey() {
        final HazardRegistry registry = new HazardRegistry();
        final HazardArchetype whirlpool = new StubHazard.StubArchetype("whirlpool", 5);
        registry.register(whirlpool);

        assertSame(whirlpool, registry.get("whirlpool"));
    }

    @Test
    @DisplayName("lookup ignores case, so a tag typed in any hand still resolves")
    void lookupIgnoresCase() {
        final HazardRegistry registry = new HazardRegistry();
        registry.register(new StubHazard.StubArchetype("IceBerg", 6));

        assertNotNull(registry.get("iceberg"));
        assertNotNull(registry.get("ICEBERG"));
    }

    @Test
    @DisplayName("an unknown key resolves to nothing rather than throwing")
    void unknownKeysAreNull() {
        final HazardRegistry registry = new HazardRegistry();
        registry.register(new StubHazard.StubArchetype("whirlpool", 5));

        assertNull(registry.get("kraken"));
        assertTrue(registry.keys().contains("whirlpool"));
    }

    @Test
    @DisplayName("nothing is registered until something registers it")
    void emptyByDefault() {
        final HazardRegistry registry = new HazardRegistry();

        assertTrue(registry.all().isEmpty());
        assertTrue(registry.keys().isEmpty());
        assertNull(registry.get("whirlpool"));
    }

    @Test
    @DisplayName("registering a name twice replaces the first, so a reload does not double it up")
    void reregisteringReplaces() {
        final HazardRegistry registry = new HazardRegistry();
        registry.register(new StubHazard.StubArchetype("whirlpool", 5));

        final HazardArchetype replacement = new StubHazard.StubArchetype("whirlpool", 9);
        registry.register(replacement);

        assertEquals(1, registry.all().size());
        assertSame(replacement, registry.get("whirlpool"));
    }

    @Test
    @DisplayName("everything registered is offered for the random draw, in the order it was claimed")
    void allIsOrderedByRegistration() {
        final HazardRegistry registry = new HazardRegistry();
        final HazardArchetype whirlpool = new StubHazard.StubArchetype("whirlpool", 5);
        final HazardArchetype iceberg = new StubHazard.StubArchetype("iceberg", 6);
        registry.register(whirlpool);
        registry.register(iceberg);

        assertEquals(List.of(whirlpool, iceberg), registry.all());
    }

    @Test
    @DisplayName("an archetype makes hazards that carry it and the point they were placed at")
    void createdHazardsCarryTheirArchetype() {
        final HazardArchetype archetype = new StubHazard.StubArchetype("iceberg", 6);
        final OceanPoint at = new OceanPoint(120, -40);

        final Hazard hazard = archetype.create(at);

        assertSame(archetype, hazard.getArchetype());
        assertEquals(at, hazard.getPoint());
        assertEquals(6, hazard.getArchetype().radius(), 1e-9);
    }
}
