package me.mykindos.betterpvp.clans.world.camp.settler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
import dev.brauw.mapper.region.CuboidRegion;
import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampPermissions;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.protection.CampGrounds;
import me.mykindos.betterpvp.clans.world.camp.resource.CampResources;
import me.mykindos.betterpvp.clans.world.camp.settler.menu.SettlerCards;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.clans.world.camp.upgrade.ToolRack;
import me.mykindos.betterpvp.clans.world.camp.upgrade.WagePolicy;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureShapes;
import me.mykindos.betterpvp.core.world.construction.StructureStatusTracker;
import me.mykindos.betterpvp.core.world.mapper.RegionIndex;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.crew.BuilderStats;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampSettlersTest {

    private static final long CLAN = 42;
    private static final SiteKey SITE = Camps.keyFor(CLAN);

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final SettlerConfig config = ShippedSettlers.config();
    private final SettlerService service = mock(SettlerService.class);
    private final CampPermissions permissions = mock(CampPermissions.class);
    private final StructureShapes shapes = mock(StructureShapes.class);
    private final SettlerCards cards = mock(SettlerCards.class);
    private final CampBuilders builders = mock(CampBuilders.class);
    private final CampResources resources = mock(CampResources.class);
    private final World world = mock(World.class);
    private final RegionIndex regions = mock(RegionIndex.class);
    private final StructureStatusTracker construction = mock(StructureStatusTracker.class);
    private final AtomicLong now = new AtomicLong(1_000);
    private CampSettlers settlers;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(construction.now()).thenAnswer(invocation -> now.get());
        settlers = new CampSettlers(store, config, service, permissions, shapes, cards, builders, resources,
                mock(CampWageFund.class), mock(CampMorale.class), new ToolRack(new CampUpgrades(store)),
                mock(WagePolicy.class), construction);
    }

    private PlacedStructure place(String type, int stage, StructureCondition condition) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), type,
                new StructurePosition(0, 64, 0, 0), condition);
        structure.setStage(stage);
        camp.getHolding().getStructures().add(structure);
        return structure;
    }

    private static Settler settler(String profession, String... traits) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setRarity(SettlerRarity.COMMON);
        settler.setProfession(profession);
        settler.setTraits(new ArrayList<>(List.of(traits)));
        return settler;
    }

    @Test
    void ac1_theGreatHallStageSetsThePopulation() {
        assertEquals(0, settlers.populationCap(SITE), "no Great Hall");

        final PlacedStructure hall = place(CampConstruction.GREAT_HALL, 0, StructureCondition.UNDER_CONSTRUCTION);
        assertEquals(0, settlers.populationCap(SITE), "still under construction");
        hall.setCondition(StructureCondition.NOT_PLACED);
        assertEquals(0, settlers.populationCap(SITE), "not placed");

        hall.setCondition(StructureCondition.ACTIVE);
        assertEquals(6, settlers.populationCap(SITE));
        hall.setStage(1);
        assertEquals(12, settlers.populationCap(SITE));
        hall.setStage(2);
        assertEquals(20, settlers.populationCap(SITE));
        hall.setStage(5);
        assertEquals(20, settlers.populationCap(SITE), "later stages keep the last value");
    }

    @Test
    void ac1_laterStagesKeepTheLastValue() {
        assertEquals(4, SettlerConfig.byStage(List.of(2, 3, 4), 5));
        assertEquals(0, SettlerConfig.byStage(List.of(2, 3, 4), -1));
        assertEquals(0, SettlerConfig.byStage(List.of(), 1));
    }

    @Test
    void ac2_theBuilderCapFollowsTheGreatHallAndEachWorkshopStage() {
        final PlacedStructure hall = place(CampConstruction.GREAT_HALL, 0, StructureCondition.ACTIVE);
        assertEquals(OptionalInt.of(2), settlers.workingCap(SITE, CampProfessions.BUILDER));
        hall.setStage(1);
        assertEquals(OptionalInt.of(3), settlers.workingCap(SITE, CampProfessions.BUILDER));
        hall.setStage(2);
        assertEquals(OptionalInt.of(4), settlers.workingCap(SITE, CampProfessions.BUILDER));

        hall.setStage(0);
        final PlacedStructure workshop = place(CampStructures.WORKSHOP, 0, StructureCondition.UNDER_CONSTRUCTION);
        assertEquals(OptionalInt.of(2), settlers.workingCap(SITE, CampProfessions.BUILDER),
                "a Workshop under construction adds nothing");
        workshop.setCondition(StructureCondition.ACTIVE);
        assertEquals(OptionalInt.of(3), settlers.workingCap(SITE, CampProfessions.BUILDER));
        workshop.setStage(1);
        assertEquals(OptionalInt.of(4), settlers.workingCap(SITE, CampProfessions.BUILDER));
    }

    @Test
    void ac3_theFarmHasTwoFarmerSlotsAndOtherProfessionsNoCap() {
        final PlacedStructure hall = place(CampConstruction.GREAT_HALL, 0, StructureCondition.ACTIVE);
        for (int stage = 0; stage < 3; stage++) {
            hall.setStage(stage);
            assertEquals(OptionalInt.of(2), settlers.workingCap(SITE, CampProfessions.FARMER));
        }
        assertTrue(settlers.workingCap(SITE, "fisher").isEmpty());
    }

    @Test
    void ac4_onlyABuilderBringsStatsToACrew() {
        final PlacedStructure structure = underConstruction();
        final Job job = structure.getJob();
        final BuilderStats stats = new BuilderStats(2, 1.0, 0.5, null, Set.of());
        when(builders.stats(any(), any(), any())).thenReturn(stats);

        final Settler builder = settler(CampProfessions.BUILDER);
        assertEquals(Optional.of(stats), settlers.builderStats(SITE, builder, structure, job, List.of(builder)));
        final Settler farmer = settler(CampProfessions.FARMER);
        assertTrue(settlers.builderStats(SITE, farmer, structure, job, List.of(farmer)).isEmpty());
        final Settler wanderer = settler(null);
        assertTrue(settlers.builderStats(SITE, wanderer, structure, job, List.of(wanderer)).isEmpty());
    }

    @Test
    void ac11_aFinishedCrewsRefundGoesBackToTheCamp() {
        final PlacedStructure structure = underConstruction();
        final Job job = Job.start(JobKind.BUILD, Duration.ofMinutes(1), ResourceCost.of(Map.of("wood", 100)), 0, 0);
        final List<Settler> crew = List.of(settler(CampProfessions.BUILDER, CampTraits.FRUGAL));

        when(builders.refund(job, crew)).thenReturn(0.05);
        settlers.crewFinished(SITE, structure, job, crew);
        verify(resources).refund(SITE, ResourceCost.of(Map.of("wood", 5)));
    }

    @Test
    void ac11_aCrewWithNothingToRefundHandsNothingBack() {
        final PlacedStructure structure = underConstruction();
        final Job job = structure.getJob();
        when(builders.refund(any(), any())).thenReturn(0.0);
        settlers.crewFinished(SITE, structure, job, List.of(settler(CampProfessions.BUILDER)));
        verify(resources, never()).refund(any(), any());
    }

    @Test
    void ac17_greedySettlersAskAQuarterMoreAtAnyRarity() {
        final Settler greedy = settler(CampProfessions.BUILDER, CampTraits.GREEDY);
        assertEquals(1.25, settlers.wageMultiplier(SITE, greedy), 1e-9);
        greedy.setRarity(SettlerRarity.LEGENDARY);
        assertEquals(1.25, settlers.wageMultiplier(SITE, greedy), 1e-9);
        assertEquals(1.0, settlers.wageMultiplier(SITE, settler(CampProfessions.BUILDER)), 1e-9);
    }

    @Test
    void ac36_settlersGatherAtTheGreatHallsPointOrOnTopOfIt() {
        assertTrue(settlers.home(SITE, world, regions).isEmpty());

        final PlacedStructure hall = place(CampConstruction.GREAT_HALL, 0, StructureCondition.ACTIVE);
        when(shapes.point(world, hall, CampSettlers.HOME_POINT)).thenReturn(Optional.empty());
        assertEquals(new Location(world, 0.5, 65, 0.5), settlers.home(SITE, world, regions).orElseThrow());

        final Location marked = new Location(world, 4, 66, 4);
        when(shapes.point(world, hall, CampSettlers.HOME_POINT)).thenReturn(Optional.of(marked));
        assertEquals(marked, settlers.home(SITE, world, regions).orElseThrow());

        hall.setCondition(StructureCondition.NOT_PLACED);
        assertTrue(settlers.home(SITE, world, regions).isEmpty(), "a Great Hall not placed is no home");
    }

    @Test
    void ac37_aStructureWorkplaceIsItsWorkPointOrOnTopOfIt() {
        final PlacedStructure workshop = place(CampStructures.WORKSHOP, 0, StructureCondition.ACTIVE);
        when(shapes.point(world, workshop, CampSettlers.WORK_POINT)).thenReturn(Optional.empty());
        assertEquals(new Location(world, 0.5, 65, 0.5),
                settlers.workplace(SITE, world, regions, workshop.getId().toString()).orElseThrow());

        final Location marked = new Location(world, 9, 64, 2);
        when(shapes.point(world, workshop, CampSettlers.WORK_POINT)).thenReturn(Optional.of(marked));
        assertEquals(marked, settlers.workplace(SITE, world, regions, workshop.getId().toString()).orElseThrow());
        assertTrue(settlers.workplace(SITE, world, regions, UUID.randomUUID().toString()).isEmpty());
        assertTrue(settlers.workplace(SITE, world, regions, "nowhere").isEmpty());
    }

    @Test
    void ac37_farmersWorkAtTheMiddleOfTheFirstFarm() {
        final CuboidRegion farm;
        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getLogger).thenReturn(Logger.getLogger("test"));
            farm = new CuboidRegion(CampGrounds.FARM, new Location(world, 10, 64, 20), new Location(world, 20, 66, 30));
        }
        when(regions.find(CampGrounds.FARM, CuboidRegion.class)).thenReturn(List.of(farm));

        assertEquals(new Location(world, 15.5, 65, 25.5),
                settlers.workplace(SITE, world, regions, CampGrounds.FARM).orElseThrow());
    }

    private Settler builderOn(PlacedStructure structure) {
        final Settler builder = settler(CampProfessions.BUILDER);
        builder.setAssignment(structure.getId().toString());
        structure.getJob().getStaff().add(builder.getId());
        return builder;
    }

    private PlacedStructure underConstruction() {
        final PlacedStructure structure = place(CampStructures.WORKSHOP, 0, StructureCondition.UNDER_CONSTRUCTION);
        structure.setJob(Job.start(JobKind.BUILD, Duration.ofMinutes(10), ResourceCost.NONE, 0, 0));
        return structure;
    }

    @Test
    void ac38_aBuilderIsLockedWhileItsJobRunsByTheConstructionClock() {
        final PlacedStructure structure = underConstruction();
        final Settler builder = builderOn(structure);

        assertTrue(settlers.jobRunning(SITE, builder));

        now.set(Duration.ofMinutes(11).toMillis());
        assertFalse(settlers.jobRunning(SITE, builder), "a finished job no longer holds its crew");
    }

    @Test
    void ac38_aBuilderOnAPausedJobCanBeTakenOff() {
        final PlacedStructure structure = underConstruction();
        final Settler builder = builderOn(structure);

        structure.getJob().hold("crew", now.get());
        assertFalse(settlers.jobRunning(SITE, builder));

        structure.getJob().release("crew", now.get());
        assertTrue(settlers.jobRunning(SITE, builder));
    }

    @Test
    void ac38_onlyABuilderOnTheCrewIsLocked() {
        final PlacedStructure structure = underConstruction();
        final Settler builder = builderOn(structure);
        structure.getJob().getStaff().clear();
        assertFalse(settlers.jobRunning(SITE, builder), "not on the crew");

        assertFalse(settlers.jobRunning(SITE, settler(CampProfessions.BUILDER)), "no assignment");

        final Settler farmer = settler(CampProfessions.FARMER);
        farmer.setAssignment("farm");
        assertFalse(settlers.jobRunning(SITE, farmer), "not a Builder");
    }

    @Test
    void ac39_settlerActionsFollowTheCampsRankPermissions() {
        final Player player = mock(Player.class);
        when(permissions.allows(player, CLAN, SettlerAction.ASSIGN)).thenReturn(true);
        when(permissions.allows(player, CLAN, SettlerAction.DISMISS)).thenReturn(false);

        assertTrue(settlers.allows(player, SITE, SettlerAction.ASSIGN));
        assertFalse(settlers.allows(player, SITE, SettlerAction.DISMISS));
    }

    @Test
    void ac39_clickingASettlerOpensItsCard() {
        final Player player = mock(Player.class);
        final Settler settler = settler(null);
        settlers.interact(player, SITE, settler);
        verify(cards).open(player, SITE, settler.getId());
    }

    @Test
    void ac40_itRegistersWithTheServiceAndTheRosterIsTheCamps() {
        verify(service).register(Camps.SITE_ID, settlers);
        assertEquals(camp.getRoster(), settlers.roster(SITE).orElseThrow());

        settlers.changed(SITE);
        verify(store).changed(CLAN);
    }

    @Test
    void ac40_theRosterRidesOnTheCampRecord() throws Exception {
        final Settler settler = settler(CampProfessions.BUILDER);
        settler.setName("Hal Two-Coats");
        settler.setRarity(SettlerRarity.LEGENDARY);
        camp.getRoster().getSettlers().add(settler);

        final ObjectMapper mapper = new ObjectMapper();
        final Camp read = mapper.readValue(mapper.writeValueAsString(camp), Camp.class);
        assertEquals(settler, read.getRoster().find(settler.getId()).orElseThrow());

        final Camp old = mapper.readValue("{\"resources\":{\"wood\":5}}", Camp.class);
        assertEquals(0, old.getRoster().size(), "a record written before settlers existed has an empty roster");
    }

    @Test
    void ac44_aSettlerWhoseModelOrSkinIsMissingWearsTheDefaultLook() {
        final Settler builder = settler(CampProfessions.BUILDER);
        final SettlerLook own = config.look(CampProfessions.BUILDER, SettlerRarity.COMMON);
        try (MockedStatic<ModelEngineAPI> modelEngine = Mockito.mockStatic(ModelEngineAPI.class)) {
            modelEngine.when(() -> ModelEngineAPI.getBlueprint(anyString())).thenReturn(mock(ModelBlueprint.class));
            assertEquals(own, settlers.look(SITE, builder));

            modelEngine.when(() -> ModelEngineAPI.getBlueprint("settler_builder")).thenReturn(null);
            assertEquals(config.defaultLook(), settlers.look(SITE, builder), "skin missing");

            modelEngine.when(() -> ModelEngineAPI.getBlueprint("settler_builder")).thenReturn(mock(ModelBlueprint.class));
            modelEngine.when(() -> ModelEngineAPI.getBlueprint("scene_market_1")).thenReturn(null);
            assertEquals(config.defaultLook(), settlers.look(SITE, builder), "model missing");
        }
    }
}
