package me.mykindos.betterpvp.core.world.site;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import me.mykindos.betterpvp.core.world.WorldHandler;
import me.mykindos.betterpvp.core.world.travel.ServerLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Deciding where a player belongs")
class ResidencyTest {

    private static final String CATALOGUE = """
            spawn:
              world:
                adopt: "Spawn"
              lifecycle: PERMANENT
              anchorable: true
              rejoin: RESUME
              rejoin-at: SPAWN_POINT
            aldenmark:
              world:
                adopt: "Aldenmark"
              lifecycle: PERMANENT
              anchorable: true
              rejoin: RESUME
              rejoin-at: EXACT
            woodcutting:
              world:
                clone: "templates/woodcutting"
              lifecycle: ON_DEMAND
              admission: party
              anchorable: false
              rejoin: ANCHOR
            """;

    private static final UUID PLAYER = UUID.randomUUID();
    private static final long CLIENT_ID = 7L;

    private final Map<String, World> worlds = new HashMap<>();
    private final Map<ResidencyStore.Kind, Residence> stored = new EnumMap<>(ResidencyStore.Kind.class);

    private ClientManager clientManager;
    private ResidencyStore store;
    private SiteInstances instances;
    private WorldHandler worldHandler;
    private Client client;
    private Player player;

    private Realm originalRealm;
    private MockedStatic<Bukkit> bukkitStatic;
    private Residency residency;

    @BeforeEach
    void setUp() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, "current-server"), new Season(1, "test", LocalDate.now())));

        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(() -> Bukkit.getWorld(anyString()))
                .thenAnswer(call -> worlds.get(call.getArgument(0, String.class)));

        clientManager = mock(ClientManager.class);
        store = mock(ResidencyStore.class);
        instances = mock(SiteInstances.class);
        worldHandler = mock(WorldHandler.class);
        client = mock(Client.class);
        player = mock(Player.class);

        final SiteRegistry registry = new SiteRegistry(mock(Core.class));
        registry.load(YamlConfiguration.loadConfiguration(new StringReader(CATALOGUE)));
        residency = new Residency(mock(Core.class), clientManager, store, registry, instances, mock(Placement.class),
                worldHandler);

        when(client.getId()).thenReturn(CLIENT_ID);
        when(clientManager.getStoredExact(PLAYER)).thenReturn(Optional.of(client));
        when(player.getUniqueId()).thenReturn(PLAYER);
        when(player.getName()).thenReturn("Tester");
        when(instances.byWorld(anyString())).thenReturn(Optional.empty());
        when(instances.find(any())).thenReturn(Optional.empty());
        when(store.load(CLIENT_ID)).thenReturn(stored);
        when(store.save(anyLong(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(store.clear(anyLong(), any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void tearDown() {
        Core.setCurrentRealm(originalRealm);
        bukkitStatic.close();
    }

    private World world(String name) {
        return worlds.computeIfAbsent(name, key -> {
            final World world = mock(World.class);
            when(world.getName()).thenReturn(key);
            when(world.getSpawnLocation()).thenReturn(new Location(world, 0.5, 70.0, 0.5));
            return world;
        });
    }

    /** A live instance of {@code siteId} in {@code world}, findable both by world and by id. */
    private SiteInstance instanceOf(String siteId, World world) {
        final SiteInstance instance = new SiteInstance(UUID.randomUUID(), SiteKey.of(siteId), world.getName(),
                SiteInstance.State.READY);
        when(instances.byWorld(world.getName())).thenReturn(Optional.of(instance));
        when(instances.find(instance.getId())).thenReturn(Optional.of(instance));
        return instance;
    }

    private Residence residenceAt(SiteInstance instance, Location location) {
        return new Residence(instance.getKey(), instance.getId(), new ServerLocation("current-server", location));
    }

    /** Runs a login, returning where the player materialises given what the server had chosen for them. */
    private Location login(Location serverChoice) {
        return residency.loginLocation(PLAYER).orElse(serverChoice);
    }

    private void quitFrom(World world, Location location) {
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        residency.onQuit(new ClientQuitEvent(client, player));
    }

    @Test
    @DisplayName("a player with no residence is left wherever the server put them")
    void loginWithoutARecord() {
        final Location serverChoice = new Location(world("world"), 1.0, 2.0, 3.0);

        assertEquals(serverChoice, login(serverChoice));
    }

    @Test
    @DisplayName("logging back into a live site that returns people to the spawn point lands there")
    void loginResumesAtSpawnPoint() {
        final World spawn = world("Spawn");
        final SiteInstance instance = instanceOf("spawn", spawn);
        stored.put(ResidencyStore.Kind.RESIDENCE, residenceAt(instance, new Location(spawn, 40.0, 65.0, 12.0)));

        assertEquals(spawn.getSpawnLocation(), login(new Location(world("world"), 0.0, 0.0, 0.0)));
    }

    @Test
    @DisplayName("logging back into a live site that returns people to where they were lands on the exact spot")
    void loginResumesAtExactSpot() {
        final World aldenmark = world("Aldenmark");
        final SiteInstance instance = instanceOf("aldenmark", aldenmark);
        final Location loggedOutAt = new Location(aldenmark, 40.0, 65.0, 12.0, 90.0f, 5.0f);
        stored.put(ResidencyStore.Kind.RESIDENCE, residenceAt(instance, loggedOutAt));

        assertEquals(loggedOutAt, login(new Location(world("world"), 0.0, 0.0, 0.0)));
    }

    @Test
    @DisplayName("logging back onto a released island falls back to the anchor")
    void loginOnAReleasedIslandFallsBackToTheAnchor() {
        final World spawn = world("Spawn");
        final SiteInstance spawnInstance = instanceOf("spawn", spawn);
        final Location anchored = new Location(spawn, 5.0, 6.0, 7.0);

        // The island's instance is not in the live set, which is what a released one looks like.
        stored.put(ResidencyStore.Kind.RESIDENCE, new Residence(SiteKey.of("woodcutting"), UUID.randomUUID(),
                new ServerLocation("current-server", new Location(world("sites/woodcutting/1a2b3c4d"), 1.0, 2.0, 3.0))));
        stored.put(ResidencyStore.Kind.ANCHOR, residenceAt(spawnInstance, anchored));

        assertEquals(anchored, login(new Location(world("world"), 0.0, 0.0, 0.0)));
    }

    @Test
    @DisplayName("a residence with nowhere left to resolve to sends the player to spawn rather than leaving them")
    void loginWithNoAnchorFallsBackToSpawn() {
        final Location spawnPoint = new Location(world("world"), 0.0, 64.0, 0.0);
        when(worldHandler.getSpawnLocation()).thenReturn(spawnPoint);
        stored.put(ResidencyStore.Kind.RESIDENCE, new Residence(SiteKey.of("woodcutting"), UUID.randomUUID(),
                new ServerLocation("current-server", new Location(world("sites/woodcutting/1a2b3c4d"), 1.0, 2.0, 3.0))));

        assertEquals(spawnPoint, login(new Location(world("world"), 9.0, 9.0, 9.0)));
    }

    @Test
    @DisplayName("quitting in an anchorable site records both the residence and the anchor")
    void quitInAnchorableSite() {
        final World spawn = world("Spawn");
        final SiteInstance instance = instanceOf("spawn", spawn);
        final Location standing = new Location(spawn, 1.0, 2.0, 3.0);

        quitFrom(spawn, standing);

        final Residence expected = Residence.of(instance, standing);
        verify(store).save(CLIENT_ID, ResidencyStore.Kind.RESIDENCE, expected);
        verify(store).save(CLIENT_ID, ResidencyStore.Kind.ANCHOR, expected);
    }

    @Test
    @DisplayName("quitting in a site nobody can be sent back to records the residence but not the anchor")
    void quitInDisposableSite() {
        final World isle = world("sites/woodcutting/1a2b3c4d");
        instanceOf("woodcutting", isle);

        quitFrom(isle, new Location(isle, 1.0, 2.0, 3.0));

        verify(store).save(eq(CLIENT_ID), eq(ResidencyStore.Kind.RESIDENCE), any());
        verify(store, never()).save(eq(CLIENT_ID), eq(ResidencyStore.Kind.ANCHOR), any());
    }

    @Test
    @DisplayName("quitting outside every site clears the residence, leaving the player's own position to stand")
    void quitOutsideAnySite() {
        final World overworld = world("world");

        quitFrom(overworld, new Location(overworld, 1.0, 2.0, 3.0));

        verify(store).clear(CLIENT_ID, ResidencyStore.Kind.RESIDENCE);
        verify(store, never()).save(eq(CLIENT_ID), eq(ResidencyStore.Kind.ANCHOR), any());
    }

    @Test
    @DisplayName("leaving an anchorable site moves the anchor to the spot left from")
    void anchorMovesOnLeavingAnchorableSite() {
        final World spawn = world("Spawn");
        final SiteInstance instance = instanceOf("spawn", spawn);
        final Location from = new Location(spawn, 5.0, 6.0, 7.0);

        residency.onTeleport(new PlayerTeleportEvent(player, from,
                new Location(world("sites/woodcutting/1a2b3c4d"), 0.0, 0.0, 0.0)));

        verify(store).save(CLIENT_ID, ResidencyStore.Kind.ANCHOR, Residence.of(instance, from));
    }

    @Test
    @DisplayName("leaving a site nobody can be sent back to leaves the anchor where it was")
    void anchorStaysOnLeavingDisposableSite() {
        final World isle = world("sites/woodcutting/1a2b3c4d");
        instanceOf("woodcutting", isle);

        residency.onTeleport(new PlayerTeleportEvent(player, new Location(isle, 5.0, 6.0, 7.0),
                new Location(world("sites/mining/9f8e7d6c"), 0.0, 0.0, 0.0)));

        verify(store, never()).save(anyLong(), eq(ResidencyStore.Kind.ANCHOR), any());
    }

    @Test
    @DisplayName("moving within one world is not leaving it")
    void anchorIgnoresSameWorldTeleport() {
        final World spawn = world("Spawn");
        instanceOf("spawn", spawn);

        residency.onTeleport(new PlayerTeleportEvent(player, new Location(spawn, 5.0, 6.0, 7.0),
                new Location(spawn, 8.0, 9.0, 10.0)));

        verify(store, never()).save(anyLong(), eq(ResidencyStore.Kind.ANCHOR), any());
    }

    @Test
    @DisplayName("the fallback is the anchor once the player's records have been read")
    void fallbackIsTheAnchor() {
        final World spawn = world("Spawn");
        final SiteInstance instance = instanceOf("spawn", spawn);
        final Location anchored = new Location(spawn, 5.0, 6.0, 7.0);
        stored.put(ResidencyStore.Kind.ANCHOR, residenceAt(instance, anchored));
        login(new Location(world("world"), 0.0, 0.0, 0.0));

        assertEquals(anchored, residency.fallback(player));
    }

    @Test
    @DisplayName("the fallback is spawn when there is no anchor to go back to")
    void fallbackIsSpawnWithoutAnAnchor() {
        final Location spawnPoint = new Location(world("world"), 0.0, 64.0, 0.0);
        when(worldHandler.getSpawnLocation()).thenReturn(spawnPoint);
        login(new Location(world("world"), 0.0, 0.0, 0.0));

        assertSame(spawnPoint, residency.fallback(player));
    }

    @Test
    @DisplayName("the anchor a teleport just recorded is available without going back to the table")
    void fallbackUsesTheAnchorRecordedInThisSession() {
        final World spawn = world("Spawn");
        instanceOf("spawn", spawn);
        final Location from = new Location(spawn, 5.0, 6.0, 7.0);
        login(new Location(world("world"), 0.0, 0.0, 0.0));

        residency.onTeleport(new PlayerTeleportEvent(player, from,
                new Location(world("sites/woodcutting/1a2b3c4d"), 0.0, 0.0, 0.0)));

        assertEquals(from, residency.fallback(player));
    }
}
