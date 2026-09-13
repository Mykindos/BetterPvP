package me.mykindos.betterpvp.core.world.site;

import me.mykindos.betterpvp.core.Core;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Locating a site for a party")
class SiteInstancesTest {

    private static final String CATALOGUE = """
            hub:
              world:
                clone: "templates/hub"
              lifecycle: POOLED
              max: 2
              capacity: 2
              admission: open
              selection: fill-first
              fallback: overflow
            overflow:
              world:
                adopt: "Overflow"
              lifecycle: PERMANENT
              admission: open
            isle:
              world:
                clone: "templates/isle"
              lifecycle: ON_DEMAND
              min: 1
              admission: party
              dormancy-grace-seconds: 0
            wreck:
              world:
                clone: "templates/wreck"
              lifecycle: ON_DEMAND
              admission: party
              dormancy-grace-seconds: 0
            camp:
              world:
                own: "camps/"
              lifecycle: OWNED
              admission: open
              dormancy: UNLOAD_WHEN_EMPTY
              dormancy-grace-seconds: 0
            """;

    @Mock
    private SiteWorlds worlds;

    @Mock
    private SiteStore store;

    private SiteRegistry registry;
    private SiteInstances instances;
    private AtomicInteger worldCounter;

    private MockedStatic<Bukkit> bukkitStatic;

    @BeforeEach
    void setUp() {
        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

        registry = new SiteRegistry(mock(Core.class));
        registry.load(YamlConfiguration.loadConfiguration(new StringReader(CATALOGUE)));
        instances = new SiteInstances(registry, worlds, store, new SiteOwners());
        worldCounter = new AtomicInteger();

        when(worlds.worldNameFor(any(), any(), any())).thenAnswer(call -> {
            final Site site = call.getArgument(0);
            return "worlds/" + site.getId() + "/" + worldCounter.incrementAndGet();
        });
        when(worlds.open(any(), anyString(), any())).thenAnswer(call -> {
            final World world = mock(World.class);
            when(world.getName()).thenReturn(call.getArgument(1));
            return CompletableFuture.completedFuture(world);
        });
        when(worlds.unload(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(worlds.destroy(anyString())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void tearDown() {
        bukkitStatic.close();
    }

    private SiteInstance locate(String siteId, Party party) {
        return instances.locate(SiteKey.of(siteId), party).join();
    }

    @Test
    @DisplayName("a whole crew is placed together by a single call")
    void crewLandsTogether() {
        final Party crew = Party.of(UUID.randomUUID(), Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        final SiteInstance instance = locate("wreck", crew);

        assertEquals(4, instance.getOccupants().size(), "everyone in the crew is counted before anybody moves");
        assertTrue(crew.getMembers().containsAll(instance.getOccupants()));
        assertEquals(1, instances.forKey(SiteKey.of("wreck")).size(), "one call makes one instance");
    }

    @Test
    @DisplayName("a stranger never lands in a crew's instance")
    void admissionKeepsStrangersOut() {
        final SiteInstance theirs = locate("wreck", Party.solo(UUID.randomUUID()));
        final SiteInstance mine = locate("wreck", Party.solo(UUID.randomUUID()));

        assertNotEquals(theirs.getId(), mine.getId());
    }

    @Test
    @DisplayName("fill-first packs a pooled site before opening another world")
    void fillFirstPacksBeforeGrowing() {
        final SiteInstance first = locate("hub", Party.solo(UUID.randomUUID()));
        final SiteInstance second = locate("hub", Party.solo(UUID.randomUUID()));

        assertSame(first, second, "the busiest instance with room takes them");
        assertEquals(2, first.getOccupants().size());

        final SiteInstance third = locate("hub", Party.solo(UUID.randomUUID()));
        assertNotEquals(first.getId(), third.getId(), "a full instance sends the next party to a new one");
    }

    @Test
    @DisplayName("a site at its ceiling overflows to its fallback")
    void fullSiteFallsBack() {
        for (int player = 0; player < 4; player++) {
            locate("hub", Party.solo(UUID.randomUUID()));
        }

        assertEquals(2, instances.forKey(SiteKey.of("hub")).size(), "the site stops growing at its maximum");

        final SiteInstance overflowed = locate("hub", Party.solo(UUID.randomUUID()));
        assertEquals("overflow", overflowed.getKey().getSiteId());
    }

    @Test
    @DisplayName("a dormant instance is woken rather than replaced")
    void dormantInstanceIsWoken() {
        final UUID owner = UUID.randomUUID();
        final SiteInstance camp = locate("camp", Party.solo(owner));
        instances.exit(camp, owner);
        instances.sleep(camp.getId()).join();

        assertEquals(SiteInstance.State.DORMANT, camp.getState());

        final SiteInstance returned = locate("camp", Party.solo(owner));

        assertSame(camp, returned);
        assertEquals(SiteInstance.State.READY, returned.getState());
        assertEquals(1, instances.forKey(SiteKey.of("camp")).size());
    }

    @Test
    @DisplayName("an owned site keeps one instance per owner")
    void ownedSitesAreKeyedByOwner() {
        final SiteInstance first = instances.locate(SiteKey.of("camp", 1L), Party.solo(UUID.randomUUID())).join();
        final SiteInstance second = instances.locate(SiteKey.of("camp", 2L), Party.solo(UUID.randomUUID())).join();

        assertNotEquals(first.getId(), second.getId());
        assertEquals(1L, first.getKey().getOwnerId());
        assertEquals(2L, second.getKey().getOwnerId());
    }

    @Test
    @DisplayName("an empty instance is destroyed if its world is disposable and unloaded if it is not")
    void emptyInstancesRetireByPolicy() {
        final UUID sailor = UUID.randomUUID();
        final SiteInstance wreck = locate("wreck", Party.solo(sailor));
        instances.exit(wreck, sailor);

        final UUID clanMember = UUID.randomUUID();
        final SiteInstance camp = instances.locate(SiteKey.of("camp", 3L), Party.solo(clanMember)).join();
        instances.exit(camp, clanMember);

        instances.retireEmptyInstances();

        verify(worlds).destroy(wreck.getWorldName());
        verify(worlds).unload(camp.getWorldName());
        assertEquals(SiteInstance.State.DORMANT, camp.getState());
    }

    @Test
    @DisplayName("an occupied instance is left alone however long it has been up")
    void occupiedInstancesAreNeverRetired() {
        final SiteInstance wreck = locate("wreck", Party.solo(UUID.randomUUID()));

        instances.retireEmptyInstances();

        verify(worlds, never()).destroy(wreck.getWorldName());
        assertEquals(SiteInstance.State.READY, wreck.getState());
    }

    @Test
    @DisplayName("a site holding instances ready keeps them provisioned")
    void warmInstancesAreKeptTopped() {
        instances.refill();

        final List<SiteInstance> warm = instances.forKey(SiteKey.of("isle"));
        assertEquals(1, warm.size(), "the site is topped up to its minimum");
        assertTrue(warm.getFirst().isEmpty(), "a warm instance nobody has claimed has no occupants");

        instances.refill();
        assertEquals(1, instances.forKey(SiteKey.of("isle")).size(), "a second pass does not double up");
    }

    @Test
    @DisplayName("a party claims the warm instance instead of waiting on a new world")
    void warmInstanceIsClaimedFirst() {
        instances.refill();
        final SiteInstance warm = instances.forKey(SiteKey.of("isle")).getFirst();

        final SiteInstance located = locate("isle", Party.solo(UUID.randomUUID()));

        assertSame(warm, located);
        verify(worlds, times(1)).open(any(), anyString(), any());
    }

    @Test
    @DisplayName("an instance with occupants refuses to be released without force")
    void releaseRefusesWhileOccupied() {
        final SiteInstance wreck = locate("wreck", Party.solo(UUID.randomUUID()));

        instances.release(wreck.getId()).join();
        verify(worlds, never()).destroy(wreck.getWorldName());
        assertTrue(instances.find(wreck.getId()).isPresent());

        instances.release(wreck.getId(), true).join();
        verify(worlds).destroy(wreck.getWorldName());
        assertFalse(instances.find(wreck.getId()).isPresent());
    }
}
