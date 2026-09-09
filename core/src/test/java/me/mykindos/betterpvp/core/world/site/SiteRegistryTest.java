package me.mykindos.betterpvp.core.world.site;

import me.mykindos.betterpvp.core.Core;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Pins the shipped {@code sites.yml} to the catalogue in the Site Framework reference.
 */
@DisplayName("Site registry")
class SiteRegistryTest {

    private static SiteRegistry loadShippedCatalogue() {
        final SiteRegistry registry = new SiteRegistry(mock(Core.class));
        try (InputStream stream = SiteRegistryTest.class.getResourceAsStream("/configs/sites.yml")) {
            assertNotNull(stream, "sites.yml is missing from core resources");
            registry.load(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read sites.yml", ex);
        }
        return registry;
    }

    private static Site site(String id) {
        return loadShippedCatalogue().get(id).orElseThrow(() -> new AssertionError("No site '" + id + "' in sites.yml"));
    }

    @Test
    @DisplayName("ships every site in the catalogue")
    void shipsTheCatalogue() {
        assertEquals(Set.of("aldenmark", "spawn", "mining", "woodcutting", "fishing", "camp"),
                loadShippedCatalogue().all().stream().map(Site::getId).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("a permanent landmark adopts its world and is anchorable")
    void permanentLandmark() {
        final Site aldenmark = site("aldenmark");
        assertEquals(SitePolicy.Lifecycle.PERMANENT, aldenmark.getPolicy().getLifecycle());
        assertEquals(WorldSource.Kind.ADOPT, aldenmark.getWorldSource().getKind());
        assertEquals("Aldenmark", aldenmark.getWorldSource().getValue());
        assertTrue(aldenmark.getPolicy().isAnchorable());
        assertEquals(SitePolicy.Rejoin.RESUME, aldenmark.getPolicy().getRejoin());
        assertEquals(SitePolicy.RejoinAt.EXACT, aldenmark.getPolicy().getRejoinAt());
    }

    @Test
    @DisplayName("a hub keeps one instance warm and knows where to overflow to")
    void hub() {
        final Site spawn = site("spawn");
        assertEquals(WorldSource.Kind.ADOPT, spawn.getWorldSource().getKind());
        assertEquals(1, spawn.getPolicy().getMin());
        assertEquals("aldenmark", spawn.getPolicy().getFallbackSiteId());
        assertEquals(SitePolicy.RejoinAt.SPAWN_POINT, spawn.getPolicy().getRejoinAt());
        assertEquals(Selection.fillFirst().select(List.of(1, 5, 2)),
                spawn.getPolicy().getSelection().select(List.of(1, 5, 2)));
    }

    @Test
    @DisplayName("a party lands at the site's own marker, at the spot its distribution picks")
    void arrival() {
        assertEquals(ArrivalPoints.DEFAULT_MARKER, site("spawn").getArrivalMarker());
        assertEquals(0, site("spawn").getArrival().select(List.of("north", "south", "east")),
                "spawn has a front door, so everyone arrives at the same quay");
    }

    @Test
    @DisplayName("an expedition is party-only, reaped, and never an anchor")
    void expedition() {
        final Site isle = site("mining");
        assertEquals(SitePolicy.Lifecycle.ON_DEMAND, isle.getPolicy().getLifecycle());
        assertEquals(WorldSource.Kind.CLONE, isle.getWorldSource().getKind());
        assertFalse(isle.getPolicy().isAnchorable());
        assertEquals(SitePolicy.Rejoin.ANCHOR, isle.getPolicy().getRejoin());
        assertTrue(isle.getPolicy().reapsWhenEmpty(), "an on-demand site is reaped once empty");
    }

    @Test
    @DisplayName("a camp is owned, kept on disk, and goes dormant rather than being destroyed")
    void camp() {
        final Site camp = site("camp");
        assertEquals(SitePolicy.Lifecycle.OWNED, camp.getPolicy().getLifecycle());
        assertEquals(WorldSource.Kind.OWN, camp.getWorldSource().getKind());
        assertEquals(SitePolicy.Dormancy.UNLOAD_WHEN_EMPTY, camp.getPolicy().getDormancy());
        assertFalse(camp.getPolicy().reapsWhenEmpty(), "a camp is never destroyed");
        assertEquals(300, camp.getPolicy().getDormancyGraceSeconds());
        assertEquals(SiteKey.of("camp", 42L), camp.keyFor(42L));
        assertNotNull(camp.getWorldSource().getTemplate(), "a new camp is copied from something rather than generated");
    }

    @Test
    @DisplayName("crossing times come off the site, and an unset voyage block falls back to the default")
    void voyageTiming() {
        assertEquals(60, site("aldenmark").getTiming().getMinSeconds());
        assertEquals(120, site("aldenmark").getTiming().getMaxSeconds());
        assertEquals(VoyageTiming.DEFAULT, site("camp").getTiming());
    }

    @Test
    @DisplayName("an admission rule registered after config load still resolves")
    void lateBoundAdmission() {
        final Admission camp = site("camp").getPolicy().getAdmission();
        final SiteKey key = SiteKey.of("camp", 7L);
        final Party party = Party.solo(UUID.randomUUID());

        assertFalse(camp.admits(key, Set.of(), party), "an unregistered rule must not let anybody in");

        Admission.register("clan", (siteKey, occupants, arriving) -> siteKey.getOwnerId() == 7L);
        assertTrue(camp.admits(key, Set.of(), party));
    }
}
