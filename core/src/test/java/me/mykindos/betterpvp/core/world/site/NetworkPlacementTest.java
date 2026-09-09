package me.mykindos.betterpvp.core.world.site;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.net.LocalSiteDirectory;
import me.mykindos.betterpvp.core.framework.net.PlayerTransfer;
import me.mykindos.betterpvp.core.framework.net.RemoteInstance;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Finding a party an instance anywhere on the network")
class NetworkPlacementTest {

    private static final String HERE = "Clans-1";
    private static final String THERE = "Clans-2";

    private static final String CATALOGUE = """
            hub:
              world:
                clone: "templates/hub"
              lifecycle: POOLED
              capacity: 4
            pinned:
              world:
                adopt: "Elsewhere"
              lifecycle: PERMANENT
              server: "Clans-2"
            camp:
              world:
                own: "camps/"
              lifecycle: OWNED
            """;

    private Realm originalRealm;
    private SiteInstances instances;
    private LocalSiteDirectory directory;
    private RecordingTransfer transfers;
    private NetworkPlacement placement;

    /** Stands in for whatever a network uses to move players, and remembers who was sent where. */
    private static class RecordingTransfer implements PlayerTransfer {
        private final List<String> sent = new ArrayList<>();

        @Override
        public CompletableFuture<Boolean> transfer(Player traveller, String server) {
            sent.add(traveller.getName() + " -> " + server);
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, HERE), new Season(1, "test", LocalDate.now())));

        final SiteRegistry registry = new SiteRegistry(mock(Core.class));
        registry.load(YamlConfiguration.loadConfiguration(new StringReader(CATALOGUE)));

        instances = mock(SiteInstances.class);
        directory = new LocalSiteDirectory();
        transfers = new RecordingTransfer();
        placement = new NetworkPlacement(mock(Core.class), registry, instances, directory, transfers,
                mock(LocalPlacement.class));
    }

    @AfterEach
    void tearDown() {
        Core.setCurrentRealm(originalRealm);
    }

    /** An instance this server holds, which locating will find and hand back. */
    private SiteInstance localInstance(String siteId) {
        final SiteInstance instance = new SiteInstance(UUID.randomUUID(), SiteKey.of(siteId),
                "sites/" + siteId + "/local", SiteInstance.State.READY);
        when(instances.locate(any(), any())).thenReturn(CompletableFuture.completedFuture(instance));
        return instance;
    }

    private RemoteInstance advertised(String siteId, String server, int occupants) {
        return new RemoteInstance(UUID.randomUUID(), siteId, SiteKey.NO_OWNER, server,
                "sites/" + siteId + "/" + server, true, occupants);
    }

    @Test
    @DisplayName("a party that could stay here stays here, rather than being put through a transfer")
    void prefersThisServer() {
        final SiteInstance here = localInstance("hub");
        directory.publish(List.of(advertised("hub", THERE, 0),
                new RemoteInstance(here.getId(), "hub", SiteKey.NO_OWNER, HERE, here.getWorldName(), true, 0)));

        final SiteHandle handle = placement.locate(SiteKey.of("hub"), Party.solo(UUID.randomUUID())).join();

        assertEquals(HERE, handle.getServer());
        assertEquals(here.getId(), handle.getInstanceId());
    }

    @Test
    @DisplayName("a party goes to another server's instance when this server has none")
    void fallsBackToAnotherServer() {
        final RemoteInstance remote = advertised("hub", THERE, 1);
        directory.publish(List.of(remote));

        final SiteHandle handle = placement.locate(SiteKey.of("hub"), Party.solo(UUID.randomUUID())).join();

        assertEquals(THERE, handle.getServer());
        assertEquals(remote.getId(), handle.getInstanceId());
        assertFalse(handle.isPending());
    }

    @Test
    @DisplayName("an instance that is full is passed over, and a new one is made instead")
    void fullRemoteInstanceIsPassedOver() {
        directory.publish(List.of(advertised("hub", THERE, 4)));
        final SiteInstance made = localInstance("hub");

        final SiteHandle handle = placement.locate(SiteKey.of("hub"), Party.solo(UUID.randomUUID())).join();

        assertEquals(HERE, handle.getServer());
        assertEquals(made.getId(), handle.getInstanceId());
    }

    @Test
    @DisplayName("seats taken for a party still travelling count against the next party's room")
    void reservedSeatsCountAgainstCapacity() {
        final RemoteInstance remote = advertised("hub", THERE, 2);
        directory.publish(List.of(remote));

        final SiteHandle first = placement.locate(SiteKey.of("hub"),
                Party.of(UUID.randomUUID(), java.util.Set.of(UUID.randomUUID()))).join();
        assertEquals(THERE, first.getServer());

        // Two seats were taken and two were already occupied, so the four-seat instance is now full.
        final SiteInstance made = localInstance("hub");
        final SiteHandle second = placement.locate(SiteKey.of("hub"), Party.solo(UUID.randomUUID())).join();

        assertEquals(HERE, second.getServer());
        assertEquals(made.getId(), second.getInstanceId());
    }

    @Test
    @DisplayName("a site pinned to a server is located there, whatever else exists")
    void pinnedSiteGoesToItsServer() {
        localInstance("pinned");

        final SiteHandle handle = placement.locate(SiteKey.of("pinned"), Party.solo(UUID.randomUUID())).join();

        assertEquals(THERE, handle.getServer());
    }

    @Test
    @DisplayName("a pinned server with nothing up yet is still the answer, and the instance follows on arrival")
    void pinnedSiteWithNothingRunningIsStillTheAnswer() {
        final SiteHandle handle = placement.locate(SiteKey.of("pinned"), Party.solo(UUID.randomUUID())).join();

        assertEquals(THERE, handle.getServer());
        assertTrue(handle.isPending());
    }

    @Test
    @DisplayName("an owned site is located on whichever server claimed it")
    void ownedSiteFollowsItsStickyHost() {
        final SiteInstance camp = localInstance("camp");

        final SiteHandle handle = placement.locate(SiteKey.of("camp", 42L), Party.solo(UUID.randomUUID())).join();

        assertEquals(HERE, handle.getServer());
        assertEquals(camp.getId(), handle.getInstanceId());
    }

    @Test
    @DisplayName("an unknown site fails rather than being invented somewhere")
    void unknownSiteFails() {
        final CompletableFuture<SiteHandle> located =
                placement.locate(SiteKey.of("nowhere"), Party.solo(UUID.randomUUID()));

        assertTrue(located.isCompletedExceptionally());
    }

    @Test
    @DisplayName("a player sent to another server is expected there, so it knows what to do with them")
    void arrivalIsRecordedBeforeTheTransfer() {
        final UUID player = UUID.randomUUID();
        final RemoteInstance remote = advertised("hub", THERE, 0);
        directory.expect(player, remote);

        final Optional<RemoteInstance> arrival = directory.claimArrival(player).join();

        assertTrue(arrival.isPresent());
        assertEquals(remote.getId(), arrival.get().getId());
        assertTrue(directory.claimArrival(player).join().isEmpty(), "reading an arrival should consume it");
    }

    @Test
    @DisplayName("crossing servers goes through the transfer seam, whatever the network uses")
    void sendingAcrossServersUsesTheTransferSeam() {
        final Player traveller = mock(Player.class);
        when(traveller.getUniqueId()).thenReturn(UUID.randomUUID());
        when(traveller.getName()).thenReturn("Tester");
        final RemoteInstance remote = advertised("hub", THERE, 0);

        final boolean sent = placement.sendAll(List.of(traveller),
                new SiteHandle(remote.getId(), SiteKey.of("hub"), THERE, remote.getWorld())).join();

        assertTrue(sent);
        assertEquals(List.of("Tester -> " + THERE), transfers.sent);
        assertTrue(directory.claimArrival(traveller.getUniqueId()).join().isPresent(),
                "the server they land on has to be told why they came");
    }

    @Test
    @DisplayName("hostFor names this server unless the site says otherwise")
    void hostForReadsThePolicy() {
        final SiteRegistry registry = new SiteRegistry(mock(Core.class));
        registry.load(YamlConfiguration.loadConfiguration(new StringReader(CATALOGUE)));

        assertEquals(HERE, placement.hostFor(registry.get("hub").orElseThrow()));
        assertEquals(THERE, placement.hostFor(registry.get("pinned").orElseThrow()));
        assertTrue(placement.isLocal(registry.get("hub").orElseThrow()));
        assertFalse(placement.isLocal(registry.get("pinned").orElseThrow()));
        assertNotEquals(HERE, placement.hostFor(registry.get("pinned").orElseThrow()));
    }
}
