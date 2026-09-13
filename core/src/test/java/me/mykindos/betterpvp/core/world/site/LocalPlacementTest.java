package me.mykindos.betterpvp.core.world.site;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.bukkit.configuration.file.YamlConfiguration.loadConfiguration;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Placing a party on one server")
class LocalPlacementTest {

    private static final String CURRENT_SERVER = "current-server";

    private static final String CATALOGUE = """
            spawn:
              world:
                clone: "templates/spawn"
              admission: open
            elsewhere:
              world:
                clone: "templates/elsewhere"
              admission: open
              server: "other-server"
            """;

    @Mock
    private SiteInstances instances;

    private Realm originalRealm;
    private SiteRegistry registry;
    private LocalPlacement placement;

    @BeforeEach
    void setUp() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, CURRENT_SERVER), new Season(1, "test-season", LocalDate.now())));

        registry = new SiteRegistry(mock(Core.class));
        registry.load(loadConfiguration(new StringReader(CATALOGUE)));
        placement = new LocalPlacement(registry, instances);
    }

    @AfterEach
    void restoreRealm() {
        Core.setCurrentRealm(originalRealm);
    }

    private Site site(String id) {
        return registry.get(id).orElseThrow();
    }

    @Test
    @DisplayName("a site says nothing about hosting, so this server holds it")
    void unconfiguredSitesAreLocal() {
        assertEquals(CURRENT_SERVER, placement.hostFor(site("spawn")));
        assertTrue(placement.isLocal(site("spawn")));
    }

    @Test
    @DisplayName("a site naming another server is that server's")
    void configuredHostWins() {
        assertEquals("other-server", placement.hostFor(site("elsewhere")));
        assertFalse(placement.isLocal(site("elsewhere")));
    }

    @Test
    @DisplayName("locating a local site hands back a handle naming this server")
    void localSiteLocatesToAHandle() {
        final SiteKey key = SiteKey.of("spawn");
        final UUID instanceId = UUID.randomUUID();
        final SiteInstance instance = new SiteInstance(instanceId, key, "sites/spawn/abcd1234", SiteInstance.State.READY);
        when(instances.locate(any(), any())).thenReturn(CompletableFuture.completedFuture(instance));

        final SiteHandle handle = placement.locate(key, Party.solo(UUID.randomUUID())).join();

        assertEquals(instanceId, handle.getInstanceId());
        assertEquals(key, handle.getKey());
        assertEquals(CURRENT_SERVER, handle.getServer());
        assertEquals("sites/spawn/abcd1234", handle.getWorld());
    }

    @Test
    @DisplayName("a site held elsewhere is refused rather than quietly located here")
    void remoteSiteIsRefused() {
        final CompletableFuture<SiteHandle> located = placement.locate(SiteKey.of("elsewhere"), Party.solo(UUID.randomUUID()));

        final CompletionException thrown = assertThrows(CompletionException.class, located::join);
        assertInstanceOf(UnsupportedOperationException.class, thrown.getCause());
    }

    @Test
    @DisplayName("an unknown site is refused")
    void unknownSiteIsRefused() {
        final CompletableFuture<SiteHandle> located = placement.locate(SiteKey.of("nowhere"), Party.solo(UUID.randomUUID()));

        final CompletionException thrown = assertThrows(CompletionException.class, located::join);
        assertInstanceOf(IllegalArgumentException.class, thrown.getCause());
    }

    @Test
    @DisplayName("a handle on another server is refused rather than teleported to")
    void remoteHandleIsRefused() {
        final SiteHandle handle = new SiteHandle(UUID.randomUUID(), SiteKey.of("spawn"), "other-server", "sites/spawn/abcd1234");

        final CompletableFuture<Boolean> sent = placement.send(mock(Player.class), handle);

        final CompletionException thrown = assertThrows(CompletionException.class, sent::join);
        assertInstanceOf(UnsupportedOperationException.class, thrown.getCause());
    }
}
