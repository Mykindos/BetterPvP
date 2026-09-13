package me.mykindos.betterpvp.core.world.site;

import me.mykindos.betterpvp.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Who a player can perceive")
class PresenceTest {

    private static final String CATALOGUE = """
            spawn:
              world:
                clone: "templates/spawn"
              lifecycle: POOLED
              max: 4
            """;

    private final List<Player> online = new ArrayList<>();

    private MockedStatic<Bukkit> bukkitStatic;
    private SiteInstances instances;
    private SiteRegistry registry;
    private Presence presence;

    @BeforeEach
    void setUp() {
        instances = mock(SiteInstances.class);
        when(instances.byWorld(anyString())).thenReturn(Optional.empty());
        when(instances.all()).thenReturn(List.of());

        registry = new SiteRegistry(mock(Core.class));
        registry.load(YamlConfiguration.loadConfiguration(new StringReader(CATALOGUE)));
        presence = new Presence(instances, registry);

        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(Bukkit::getOnlinePlayers).thenReturn(online);
    }

    @AfterEach
    void tearDown() {
        bukkitStatic.close();
    }

    private World world(String name) {
        final World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        when(world.getPlayers()).thenReturn(new ArrayList<>());
        bukkitStatic.when(() -> Bukkit.getWorld(name)).thenReturn(world);
        return world;
    }

    private Player playerIn(World world) {
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        world.getPlayers().add(player);
        online.add(player);
        return player;
    }

    private void liveInstance(String siteId, World world) {
        final SiteInstance instance = new SiteInstance(UUID.randomUUID(), SiteKey.of(siteId), world.getName(),
                SiteInstance.State.READY);
        when(instances.byWorld(world.getName())).thenReturn(Optional.of(instance));
        final List<SiteInstance> all = new ArrayList<>(instances.all());
        all.add(instance);
        when(instances.all()).thenReturn(all);
    }

    @Test
    @DisplayName("two players in one world perceive each other")
    void sameWorld() {
        final World world = world("sites/spawn/1a2b3c4d");
        final Player first = playerIn(world);
        final Player second = playerIn(world);

        assertTrue(presence.sees(first, second));
        assertTrue(presence.sees(second, first));
        assertEquals(List.of(first, second), List.copyOf(presence.around(first)));
    }

    @Test
    @DisplayName("two copies of one site are two groups, not one")
    void separateInstancesOfTheSameSite() {
        final World first = world("sites/spawn/1a2b3c4d");
        final World second = world("sites/spawn/9f8e7d6c");
        liveInstance("spawn", first);
        liveInstance("spawn", second);
        final Player here = playerIn(first);
        final Player there = playerIn(second);

        assertFalse(presence.sees(here, there));
        assertEquals(List.of(here), List.copyOf(presence.around(here)));
        assertEquals(List.of(there), List.copyOf(presence.around(there)));
    }

    @Test
    @DisplayName("a site lists everybody across all of its copies")
    void onSiteSpansInstances() {
        final World first = world("sites/spawn/1a2b3c4d");
        final World second = world("sites/spawn/9f8e7d6c");
        liveInstance("spawn", first);
        liveInstance("spawn", second);
        final Player here = playerIn(first);
        final Player there = playerIn(second);

        assertEquals(List.of(here, there), presence.onSite(registry.get("spawn").orElseThrow()));
    }

    @Test
    @DisplayName("anybody in a world no site owns is off site")
    void offSiteCoversWorldsWithoutAnInstance() {
        final World hub = world("world");
        final Player wanderer = playerIn(hub);

        assertEquals(List.of(wanderer), presence.offSite());
        assertTrue(presence.siteOf(wanderer).isEmpty());
    }

    @Test
    @DisplayName("an audience at a location is everybody in that world")
    void audienceAtALocation() {
        final World world = world("sites/spawn/1a2b3c4d");
        final Player present = playerIn(world);
        playerIn(world("elsewhere"));

        assertEquals(List.of(present), List.copyOf(presence.at(new Location(world, 0.0, 0.0, 0.0))));
    }
}
