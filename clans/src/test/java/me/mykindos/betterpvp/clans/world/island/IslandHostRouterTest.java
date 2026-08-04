package me.mykindos.betterpvp.clans.world.island;

import me.mykindos.betterpvp.clans.world.voyage.VoyageTiming;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IslandHostRouterTest {

    private static final String CURRENT_SERVER = "current-server";

    private Realm originalRealm;

    @BeforeEach
    void stubCurrentServer() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, CURRENT_SERVER), new Season(1, "test-season", LocalDate.now())));
    }

    @AfterEach
    void restoreCurrentServer() {
        Core.setCurrentRealm(originalRealm);
    }

    private static IslandTemplate template(String key) {
        return new IslandTemplate(key, Component.text(key), "islands/" + key, Material.GRASS_BLOCK, VoyageTiming.DEFAULT);
    }

    private static Clans clansWithHosting(ExtendedYamlConfiguration islandsConfig) {
        final Clans clans = mock(Clans.class);
        when(clans.getConfig("islands")).thenReturn(islandsConfig);
        return clans;
    }

    @Test
    @DisplayName("a template with no hosting entry defaults to the current server")
    void unconfiguredTemplateDefaultsToCurrentServer() {
        final ExtendedYamlConfiguration config = new ExtendedYamlConfiguration();
        final IslandHostRouter router = new IslandHostRouter(clansWithHosting(config));

        final IslandTemplate template = template("solo");
        assertEquals(CURRENT_SERVER, router.hostFor(template));
        assertTrue(router.isLocal(template));
        assertTrue(router.isServable(template));
    }

    @Test
    @DisplayName("a configured hosting entry routes the template to that server")
    void configuredHostingEntryRoutesElsewhere() {
        final ExtendedYamlConfiguration config = new ExtendedYamlConfiguration();
        config.set("hosting.solo", "other-server");
        final IslandHostRouter router = new IslandHostRouter(clansWithHosting(config));

        final IslandTemplate template = template("solo");
        assertEquals("other-server", router.hostFor(template));
        assertFalse(router.isLocal(template));
        assertFalse(router.isServable(template));
    }

    @Test
    @DisplayName("only templates listed under hosting are overridden; others still default locally")
    void hostingOverrideIsPerTemplate() {
        final ExtendedYamlConfiguration config = new ExtendedYamlConfiguration();
        config.set("hosting.solo", "other-server");
        final IslandHostRouter router = new IslandHostRouter(clansWithHosting(config));

        final IslandTemplate untouched = template("duo");
        assertEquals(CURRENT_SERVER, router.hostFor(untouched));
        assertTrue(router.isLocal(untouched));
    }
}
