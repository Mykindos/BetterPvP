package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.world.content.WorldContentScope;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerGenerator;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarterCrewTest {

    private static final long CLAN = 42;
    private static final SiteKey SITE = Camps.keyFor(CLAN);
    private static final String WORLD = "camp-42";

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final SettlerService service = mock(SettlerService.class);
    private final SiteInstances instances = mock(SiteInstances.class);
    private final World world = mock(World.class);
    private SettlerGenerator generator;

    @BeforeEach
    void setUp() {
        final ProfessionRegistry professions = new ProfessionRegistry();
        new CampProfessions(professions);
        final TraitRegistry traits = new TraitRegistry();
        new CampTraits(traits);
        generator = new SettlerGenerator(professions, traits);

        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(service.populationCap(SITE)).thenReturn(6);
        when(service.grant(eq(SITE), any())).thenAnswer(invocation -> SettlerResult.done(invocation.getArgument(1)));
        when(world.getName()).thenReturn(WORLD);
        when(instances.byWorld(WORLD)).thenReturn(Optional.of(
                new SiteInstance(UUID.randomUUID(), SITE, WORLD, SiteInstance.State.READY)));
    }

    private StarterCrew crew(SettlerConfig config) {
        return new StarterCrew(store, service, generator, config, instances);
    }

    private void open(StarterCrew crew) {
        crew.install(world, mock(RegionIndex.class), mock(WorldContentScope.class));
    }

    @Test
    void ac34_aNewCampIsGivenTheStartingSettlersOnce() {
        final StarterCrew crew = crew(ShippedSettlers.config());
        open(crew);

        final ArgumentCaptor<Settler> granted = ArgumentCaptor.forClass(Settler.class);
        verify(service, times(2)).grant(eq(SITE), granted.capture());
        for (Settler settler : granted.getAllValues()) {
            assertEquals(CampProfessions.BUILDER, settler.getProfession());
            assertEquals(SettlerRarity.COMMON, settler.getRarity());
            assertEquals("clans.settler.history.starting.1", settler.getHistory());
        }
        assertTrue(camp.isStartingSettlers());
        verify(store).changed(CLAN);

        open(crew);
        verify(service, times(2)).grant(eq(SITE), any());
    }

    @Test
    void ac34_aCampWithNoRoomYetIsGivenNoneAndNotMarked() {
        when(service.populationCap(SITE)).thenReturn(0);
        open(crew(ShippedSettlers.config()));

        verify(service, never()).grant(any(), any());
        assertFalse(camp.isStartingSettlers());
    }

    @Test
    void ac35_aRefusedStarterIsSkippedAndTheOthersStillJoin() {
        when(service.grant(eq(SITE), any()))
                .thenReturn(SettlerResult.refused("core.settler.full"))
                .thenAnswer(invocation -> SettlerResult.done(invocation.getArgument(1)));
        open(crew(ShippedSettlers.config()));

        verify(service, times(2)).grant(eq(SITE), any());
        assertTrue(camp.isStartingSettlers());
    }

    @Test
    void ac35_aStarterWithAProfessionTheCampLacksIsSkipped() {
        final StarterCrew crew = crew(ShippedSettlers.config(yaml -> yaml.set("starting-settlers", List.of(
                Map.of("profession", "fisher", "rarity", "common"),
                Map.of("profession", "builder", "rarity", "rare")))));

        assertDoesNotThrow(() -> open(crew));
        final ArgumentCaptor<Settler> granted = ArgumentCaptor.forClass(Settler.class);
        verify(service).grant(eq(SITE), granted.capture());
        assertEquals(CampProfessions.BUILDER, granted.getValue().getProfession());
        assertEquals(SettlerRarity.RARE, granted.getValue().getRarity());
        assertTrue(camp.isStartingSettlers());
    }
}
