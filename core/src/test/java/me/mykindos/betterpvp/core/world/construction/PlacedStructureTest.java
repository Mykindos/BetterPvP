package me.mykindos.betterpvp.core.world.construction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedStructureTest {

    private static final long MINUTE = 60_000;

    private static PlacedStructure structure(StructureCondition condition) {
        return new PlacedStructure(UUID.randomUUID(), "great_hall", new StructurePosition(1, 64, 2, 1), condition);
    }

    private static Job job(JobKind kind) {
        return Job.start(kind, Duration.ofMinutes(10), ResourceCost.NONE, 1, 0);
    }

    @Test
    void aStructureWithNoJobShowsItsCondition() {
        assertEquals(StructureStatus.ACTIVE, structure(StructureCondition.ACTIVE).status(0));
        assertEquals(StructureStatus.NEEDS_REPAIR, structure(StructureCondition.NEEDS_REPAIR).status(0));
    }

    @Test
    void aBuildRisesThenWaitsToBeClaimed() {
        final PlacedStructure built = structure(StructureCondition.UNDER_CONSTRUCTION);
        built.setJob(job(JobKind.BUILD));

        assertEquals(StructureStatus.UNDER_CONSTRUCTION, built.status(MINUTE));
        assertEquals(StructureStatus.READY_TO_CLAIM, built.status(10 * MINUTE));
    }

    @Test
    void anUpgradeLeavesItStandingButNotUsable() {
        final PlacedStructure upgraded = structure(StructureCondition.ACTIVE);
        upgraded.setJob(job(JobKind.UPGRADE));

        assertEquals(StructureStatus.UPGRADING, upgraded.status(MINUTE));
        assertFalse(upgraded.status(MINUTE).isUsable());
    }

    @Test
    void aRepairShowsWhatIsBeingRepaired() {
        final PlacedStructure broken = structure(StructureCondition.DISABLED);
        broken.setJob(job(JobKind.REPAIR));

        assertEquals(StructureStatus.DISABLED, broken.status(MINUTE));
    }

    @Test
    void aHeldJobIsPausedEvenOnceItsTimeIsUp() {
        final PlacedStructure built = structure(StructureCondition.UNDER_CONSTRUCTION);
        built.setJob(job(JobKind.BUILD));
        built.getJob().hold("siege", 0);

        assertEquals(StructureStatus.PAUSED, built.status(60 * MINUTE));
    }

    @Test
    void aStructurePutAwayIsNotPlacedWhateverElseIsGoingOn() {
        final PlacedStructure stored = structure(StructureCondition.NOT_PLACED);

        assertEquals(StructureStatus.NOT_PLACED, stored.status(0));
    }

    @Test
    void aHoldingSurvivesBeingWrittenDownAndReadBack() throws Exception {
        final Holding holding = new Holding();
        final PlacedStructure hall = structure(StructureCondition.UNDER_CONSTRUCTION);
        final Job job = Job.start(JobKind.BUILD, Duration.ofMinutes(10),
                ResourceCost.of(Map.of("wood", 40, "stone", 10)), 0, 5);
        job.hold("siege", 7);
        hall.setJob(job);
        holding.getStructures().add(hall);

        final ObjectMapper mapper = new ObjectMapper();
        final Holding read = mapper.readValue(mapper.writeValueAsString(holding), Holding.class);

        assertEquals(holding, read);
        assertTrue(read.find(hall.getId()).orElseThrow().getJob().isHeld());
        assertEquals(40, read.getStructures().getFirst().getJob().getSpent().get("wood"));
    }

    @Test
    void requirementsCountOnlyFinishedStructures() {
        final Holding holding = new Holding();
        holding.getStructures().add(structure(StructureCondition.UNDER_CONSTRUCTION));
        assertFalse(holding.hasBuilt("great_hall"));

        holding.getStructures().add(structure(StructureCondition.NEEDS_REPAIR));
        assertTrue(holding.hasBuilt("great_hall"));
    }

    @Test
    void aShareOfACostRoundsDown() {
        final ResourceCost cost = ResourceCost.of(Map.of("wood", 45, "iron", 3));

        final ResourceCost half = cost.share(0.5);

        assertEquals(22, half.get("wood"));
        assertEquals(1, half.get("iron"));
        assertTrue(ResourceCost.NONE.isFree());
        assertEquals(90, cost.plus(cost).get("wood"));
    }
}
