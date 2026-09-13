package me.mykindos.betterpvp.clans.world.sailing;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.menu.navigation.Destination;
import me.mykindos.betterpvp.core.world.site.Placement;
import me.mykindos.betterpvp.core.world.site.Site;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.SiteOwners;
import me.mykindos.betterpvp.core.world.site.SiteRegistry;
import me.mykindos.betterpvp.core.world.site.crew.CrewService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("What a helm offers")
class ShipDestinationsTest {

    private static final String CATALOGUE = """
            aldenmark:
              display-name: "Aldenmark"
              world:
                adopt: "Aldenmark"
              lifecycle: PERMANENT
            spawn:
              display-name: "Spawn"
              world:
                adopt: "Spawn"
              lifecycle: PERMANENT
            mining:
              display-name: "Mining Isle"
              world:
                clone: "templates/mining"
              lifecycle: ON_DEMAND
              max: 1
            woodcutting:
              display-name: "Woodcutting Isle"
              world:
                clone: "templates/woodcutting"
              lifecycle: ON_DEMAND
            camp:
              display-name: "Camp"
              world:
                own: "camps/"
              lifecycle: OWNED
            """;

    @Mock
    private SiteInstances instances;

    @Mock
    private CrewService crewService;

    @Mock
    private VoyageService voyageService;

    @Mock
    private Placement placement;

    @Mock
    private Player player;

    private SiteRegistry registry;
    private SiteOwners owners;
    private ShipDestinations destinations;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        registry = new SiteRegistry(mock(Core.class));
        registry.load(YamlConfiguration.loadConfiguration(new StringReader(CATALOGUE)));
        owners = new SiteOwners();
        destinations = new ShipDestinations(registry, instances, crewService, voyageService, placement, owners);

        final Field count = ShipDestinations.class.getDeclaredField("expeditionCount");
        count.setAccessible(true);
        count.setInt(destinations, 3);

        when(placement.isLocal(any(Site.class))).thenReturn(true);
        when(instances.byWorld(anyString())).thenReturn(Optional.empty());
        when(instances.forKey(any(SiteKey.class))).thenReturn(List.of());
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        standingIn("Somewhere");
    }

    private void standingIn(String worldName) {
        final World world = mock(World.class);
        when(world.getName()).thenReturn(worldName);
        when(player.getWorld()).thenReturn(world);
    }

    private List<String> namesOffered() {
        return destinations.destinationsFor(player).stream()
                .map(destination -> ((TextComponent) destination.displayName()).content())
                .toList();
    }

    @Test
    @DisplayName("a port is offered under its own name and an expedition under a made-up one")
    void portsAndExpeditions() {
        final List<String> offered = namesOffered();

        assertTrue(offered.contains("Aldenmark"));
        assertTrue(offered.contains("Spawn"));
        assertFalse(offered.contains("Mining Isle"), "an uncharted island is not offered under its catalogue name");
        assertEquals(4, offered.size(), "two ports and both expeditions");
    }

    @Test
    @DisplayName("an owned site nobody has claimed for this player is not offered")
    void anUnownedSiteIsNotOffered() {
        assertFalse(namesOffered().contains("Camp"), "a player with no clan has no camp to sail to");
    }

    @Test
    @DisplayName("a place the player owns is offered, and only to them")
    void anOwnedSiteIsOfferedToItsOwner() {
        owners.register("camp", who -> OptionalLong.of(7L));

        assertTrue(namesOffered().contains("Camp"));
    }

    @Test
    @DisplayName("an owned site is sailed to under the owner's own key, not the site's")
    void anOwnedSiteCarriesItsOwner() {
        owners.register("camp", who -> OptionalLong.of(7L));

        final ShipDestination camp = (ShipDestination) destinations.destinationsFor(player).stream()
                .filter(destination -> ((TextComponent) destination.displayName()).content().equals("Camp"))
                .findFirst()
                .orElseThrow();

        assertEquals(SiteKey.of("camp", 7L), camp.getSiteKey());
    }

    @Test
    @DisplayName("where you already are is not a course you can set")
    void whereYouAreIsNotOffered() {
        final SiteInstance here = new SiteInstance(UUID.randomUUID(), SiteKey.of("spawn"), "Spawn", SiteInstance.State.READY);
        standingIn("Spawn");
        when(instances.byWorld("Spawn")).thenReturn(Optional.of(here));

        final List<String> offered = namesOffered();
        assertTrue(offered.contains("Aldenmark"));
        assertFalse(offered.contains("Spawn"));
    }

    @Test
    @DisplayName("a site that may not open another instance is left out rather than refused on click")
    void fullSitesAreNotOffered() {
        final SiteInstance mining = new SiteInstance(UUID.randomUUID(), SiteKey.of("mining"), "sites/mining/aaaa", SiteInstance.State.READY);
        when(instances.forKey(SiteKey.of("mining"))).thenReturn(List.of(mining));

        assertEquals(3, namesOffered().size(), "two ports and the one expedition with room left");
    }

    @Test
    @DisplayName("the offers a player is shown do not reshuffle while they are reading them")
    void offersAreHeldPerPlayer() {
        final List<String> first = namesOffered();
        assertEquals(first, namesOffered());
    }

    @Test
    @DisplayName("every destination carries the site it leads to")
    void destinationsCarryTheirSite() {
        for (Destination destination : destinations.destinationsFor(player)) {
            final ShipDestination course = (ShipDestination) destination;
            assertEquals(course.getSite().key(), course.getSiteKey());
            assertTrue(course.isReady());
        }
    }

    @Test
    @DisplayName("a display name is what the helm shows, whatever the site is called")
    void displayNameIsCarried() {
        final Site mining = registry.get("mining").orElseThrow();
        final ShipDestination course = new ShipDestination(mining, mining.key(), Component.text("Gullwind Cay"),
                crewService, voyageService, placement);

        assertEquals("Gullwind Cay", ((TextComponent) course.displayName()).content());
        assertEquals(mining.getTiming(), course.timing());
    }
}
