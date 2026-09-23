package me.mykindos.betterpvp.clans.world.camp.settler;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.resource.CampResources;
import me.mykindos.betterpvp.clans.world.camp.settler.menu.SettlerCards;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampSettlersTest {

    private static final long CLAN = 42;
    private static final SiteKey SITE = Camps.keyFor(CLAN);

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final SettlerConfig config = mock(SettlerConfig.class);
    private final SettlerService service = mock(SettlerService.class);
    private final StructureShapes shapes = mock(StructureShapes.class);
    private final World world = mock(World.class);
    private final RegionIndex regions = mock(RegionIndex.class);
    private CampSettlers settlers;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(config.population(-1)).thenReturn(0);
        when(config.population(0)).thenReturn(6);
        when(config.population(1)).thenReturn(12);
        when(config.workingCap(CampProfessions.BUILDER)).thenReturn(Optional.of(
                new SettlerConfig.WorkingCap(List.of(2, 3, 4), Map.of(CampStructures.WORKSHOP, 1))));
        when(config.workingCap(CampProfessions.FARMER)).thenReturn(Optional.empty());
        settlers = new CampSettlers(store, config, service, mock(CampPermissions.class), shapes,
                mock(SettlerCards.class), mock(CampBuilders.class), mock(CampResources.class),
                mock(CampWageFund.class),
                new CampProfessions(new ProfessionRegistry()),
                new CampTraits(new TraitRegistry()));
    }

    private PlacedStructure place(String type, int stage, StructureCondition condition) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), type,
                new StructurePosition(0, 64, 0, 0), condition);
        structure.setStage(stage);
        camp.getHolding().getStructures().add(structure);
        return structure;
    }

    @Test
    void settlersGatherAtTheGreatHallsPointOrOnTopOfIt() {
        assertTrue(settlers.home(SITE, world, regions).isEmpty());

        final PlacedStructure hall = place(CampConstruction.GREAT_HALL, 0, StructureCondition.ACTIVE);
        when(shapes.point(world, hall, CampSettlers.HOME_POINT)).thenReturn(Optional.empty());
        assertEquals(new Location(world, 0.5, 65, 0.5), settlers.home(SITE, world, regions).orElseThrow());

        final Location marked = new Location(world, 4, 66, 4);
        when(shapes.point(world, hall, CampSettlers.HOME_POINT)).thenReturn(Optional.of(marked));
        assertEquals(marked, settlers.home(SITE, world, regions).orElseThrow());
    }

    @Test
    void aStructureWorkplaceIsItsWorkPoint() {
        final PlacedStructure workshop = place(CampStructures.WORKSHOP, 0, StructureCondition.ACTIVE);
        final Location marked = new Location(world, 9, 64, 2);
        when(shapes.point(world, workshop, CampSettlers.WORK_POINT)).thenReturn(Optional.of(marked));

        assertEquals(marked, settlers.workplace(SITE, world, regions, workshop.getId().toString()).orElseThrow());
        assertTrue(settlers.workplace(SITE, world, regions, UUID.randomUUID().toString()).isEmpty());
        assertTrue(settlers.workplace(SITE, world, regions, "nowhere").isEmpty());
    }

    @Test
    void itRegistersWithTheService() {
        verify(service).register(Camps.SITE_ID, settlers);
    }

    @Test
    void theGreatHallStageSetsThePopulation() {
        assertEquals(0, settlers.populationCap(SITE));

        place(CampConstruction.GREAT_HALL, 0, StructureCondition.UNDER_CONSTRUCTION);
        assertEquals(0, settlers.populationCap(SITE));

        camp.getHolding().getStructures().clear();
        place(CampConstruction.GREAT_HALL, 1, StructureCondition.ACTIVE);
        assertEquals(12, settlers.populationCap(SITE));
    }

    @Test
    void workshopStagesRaiseTheBuilderCap() {
        place(CampConstruction.GREAT_HALL, 0, StructureCondition.ACTIVE);
        assertEquals(OptionalInt.of(2), settlers.workingCap(SITE, CampProfessions.BUILDER));

        place(CampStructures.WORKSHOP, 1, StructureCondition.ACTIVE);
        assertEquals(OptionalInt.of(4), settlers.workingCap(SITE, CampProfessions.BUILDER));
        assertTrue(settlers.workingCap(SITE, CampProfessions.FARMER).isEmpty());
    }

    @Test
    void laterStagesKeepTheLastValue() {
        assertEquals(4, SettlerConfig.byStage(List.of(2, 3, 4), 5));
        assertEquals(0, SettlerConfig.byStage(List.of(2, 3, 4), -1));
        assertEquals(0, SettlerConfig.byStage(List.of(), 1));
    }

    @Test
    void theRosterRidesOnTheCampRecord() throws Exception {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName("Hal Two-Coats");
        settler.setRarity(SettlerRarity.LEGENDARY);
        settler.setProfession(CampProfessions.BUILDER);
        camp.getRoster().getSettlers().add(settler);

        final ObjectMapper mapper = new ObjectMapper();
        final Camp read = mapper.readValue(mapper.writeValueAsString(camp), Camp.class);
        assertEquals(settler, read.getRoster().find(settler.getId()).orElseThrow());

        // A record written before settlers existed reads with an empty roster.
        final Camp old = mapper.readValue("{\"resources\":{\"wood\":5}}", Camp.class);
        assertEquals(0, old.getRoster().size());
    }
}
