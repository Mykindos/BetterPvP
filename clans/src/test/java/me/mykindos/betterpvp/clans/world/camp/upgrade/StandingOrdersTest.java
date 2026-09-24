package me.mykindos.betterpvp.clans.world.camp.upgrade;

import me.mykindos.betterpvp.clans.world.camp.Camp;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.settler.crew.CrewRule;
import me.mykindos.betterpvp.core.world.settler.crew.CrewService;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StandingOrdersTest {

    private static final long CLAN = 5;
    private static final SiteKey SITE = Camps.keyFor(CLAN);

    private final Camp camp = new Camp();
    private final CampStore store = mock(CampStore.class);
    private final SettlerService settlers = mock(SettlerService.class);
    private final CrewService crews = mock(CrewService.class);
    private final CrewRule rule = mock(CrewRule.class);
    private final ConstructionService construction = mock(ConstructionService.class);
    private final Map<Job, Integer> thresholds = new IdentityHashMap<>();
    private final List<String> enlisted = new ArrayList<>();
    private StandingOrders orders;
    private PlacedStructure hall;

    @BeforeEach
    void setUp() {
        when(store.cached(CLAN)).thenReturn(Optional.of(camp));
        when(settlers.roster(SITE)).thenReturn(Optional.of(camp.getRoster()));
        when(construction.now()).thenReturn(1_000L);
        when(crews.isFree(any())).thenAnswer(invocation -> {
            final Settler settler = invocation.getArgument(0);
            return settler.getAssignment() == null && "builder".equals(settler.getProfession());
        });
        when(rule.holds(eq(SITE), any(), any())).thenAnswer(invocation -> {
            final Job job = invocation.getArgument(2);
            return job.getStaff().size() < thresholds.getOrDefault(job, 0);
        });
        orders = new StandingOrders(new CampUpgrades(store), construction, settlers, crews, rule,
                mock(SiteInstances.class));
        hall = new PlacedStructure(UUID.randomUUID(), CampConstruction.GREAT_HALL, new StructurePosition(0, 64, 0, 0),
                StructureCondition.ACTIVE);
        hall.getUpgrades().put(0, StandingOrders.ID);
        camp.getHolding().getStructures().add(hall);
    }

    @Test
    void freeBuildersJoinTheLongestWaitingJobFirst() {
        final Settler first = builder("first");
        final Settler second = builder("second");
        final PlacedStructure newer = waiting(CampStructures.WORKSHOP, 500, 1);
        final PlacedStructure older = waiting(CampStructures.BARRACKS, 100, 1);

        orders.staff(SITE, camp.getHolding(), enlist());

        assertEquals(List.of(first.getId()), older.getJob().getStaff());
        assertEquals(List.of(second.getId()), newer.getJob().getStaff());
    }

    @Test
    void crewsStopGrowingOnceTheirJobCanRun() {
        builder("a");
        builder("b");
        builder("c");
        final PlacedStructure job = waiting(CampStructures.WORKSHOP, 100, 2);

        orders.staff(SITE, camp.getHolding(), enlist());

        assertEquals(2, job.getJob().getStaff().size());
        assertEquals(List.of("a", "b"), enlisted);
    }

    @Test
    void strikersAssignedAndOtherTradesStayPut() {
        builder("striker").setState(SettlerState.STRIKING);
        final Settler busy = builder("busy");
        busy.setAssignment(UUID.randomUUID().toString());
        busy.setState(SettlerState.WORKING);
        final Settler farmer = builder("farmer");
        farmer.setProfession("farmer");
        waiting(CampStructures.WORKSHOP, 100, 2);

        orders.staff(SITE, camp.getHolding(), enlist());

        assertTrue(enlisted.isEmpty());
    }

    @Test
    void aRefusedBuilderIsPassedOverForTheNext() {
        builder("rare");
        builder("common");
        final PlacedStructure job = waiting(CampStructures.WORKSHOP, 100, 1);

        orders.staff(SITE, camp.getHolding(), (structure, settler) -> {
            final Settler found = camp.getRoster().find(settler).orElseThrow();
            if (found.getName().equals("rare")) {
                return SettlerResult.refused("core.settler.crew.rarity_full");
            }
            return enlist().apply(structure, settler);
        });

        assertEquals(List.of("common"), enlisted);
        assertEquals(1, job.getJob().getStaff().size());
    }

    @Test
    void nothingHappensWithoutTheUpgrade() {
        hall.getUpgrades().clear();
        builder("a");
        waiting(CampStructures.WORKSHOP, 100, 1);

        orders.staff(SITE, camp.getHolding(), enlist());

        assertTrue(enlisted.isEmpty());
    }

    private BiFunction<UUID, UUID, SettlerResult> enlist() {
        return (structureId, settlerId) -> {
            final Settler settler = camp.getRoster().find(settlerId).orElseThrow();
            final Job job = camp.getHolding().find(structureId).orElseThrow().getJob();
            job.getStaff().add(settlerId);
            settler.setAssignment(structureId.toString());
            settler.setState(SettlerState.WORKING);
            enlisted.add(settler.getName());
            return SettlerResult.done(settler);
        };
    }

    private Settler builder(String name) {
        final Settler settler = new Settler();
        settler.setId(UUID.randomUUID());
        settler.setName(name);
        settler.setProfession("builder");
        camp.getRoster().getSettlers().add(settler);
        return settler;
    }

    private PlacedStructure waiting(String type, long heldSince, int threshold) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), type,
                new StructurePosition(10, 64, 10, 0), StructureCondition.UNDER_CONSTRUCTION);
        final Job job = Job.start(JobKind.BUILD, Duration.ofMinutes(10), ResourceCost.NONE, 0, heldSince);
        job.hold(CrewRule.ID, heldSince);
        structure.setJob(job);
        thresholds.put(job, threshold);
        camp.getHolding().getStructures().add(structure);
        return structure;
    }
}
