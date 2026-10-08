package me.mykindos.betterpvp.core.world.construction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
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
    void ac21_aStructureWithNoJobShowsItsCondition() {
        assertEquals(StructureStatus.ACTIVE, structure(StructureCondition.ACTIVE).status(0));
        assertEquals(StructureStatus.NEEDS_REPAIR, structure(StructureCondition.NEEDS_REPAIR).status(0));
    }

    @Test
    void ac21_aBuildRisesThenWaitsToBeClaimed() {
        final PlacedStructure built = structure(StructureCondition.UNDER_CONSTRUCTION);
        built.setJob(job(JobKind.BUILD));

        assertEquals(StructureStatus.UNDER_CONSTRUCTION, built.status(MINUTE));
        assertEquals(StructureStatus.READY_TO_CLAIM, built.status(10 * MINUTE));
    }

    @Test
    void ac21_advancingLeavesItStandingButNotUsable() {
        final PlacedStructure advancing = structure(StructureCondition.ACTIVE);
        advancing.setJob(job(JobKind.ADVANCE));

        assertEquals(StructureStatus.ADVANCING, advancing.status(MINUTE));
        assertFalse(advancing.status(MINUTE).isUsable());
    }

    @Test
    void ac21_aRepairShowsWhatIsBeingRepaired() {
        final PlacedStructure broken = structure(StructureCondition.DISABLED);
        broken.setJob(job(JobKind.REPAIR));

        assertEquals(StructureStatus.DISABLED, broken.status(MINUTE));
    }

    @Test
    void ac21_aHeldJobIsPausedEvenOnceItsTimeIsUp() {
        final PlacedStructure built = structure(StructureCondition.UNDER_CONSTRUCTION);
        built.setJob(job(JobKind.BUILD));
        built.getJob().hold("siege", 0);

        assertEquals(StructureStatus.PAUSED, built.status(60 * MINUTE));
    }

    @Test
    void ac21_aStructurePutAwayIsNotPlacedWhateverElseIsGoingOn() {
        final PlacedStructure stored = structure(StructureCondition.NOT_PLACED);

        assertEquals(StructureStatus.NOT_PLACED, stored.status(0));
    }

    @Test
    void ac25_aHoldingSurvivesBeingWrittenDownAndReadBack() throws Exception {
        final Holding holding = new Holding();
        final PlacedStructure hall = structure(StructureCondition.UNDER_CONSTRUCTION);
        final Job job = Job.start(JobKind.BUILD, Duration.ofMinutes(10),
                ResourceCost.of(Map.of("wood", 40, "stone", 10)), 0, 5);
        job.hold("siege", 7);
        hall.setJob(job);
        hall.setStorage(Map.of("1,0,0", Arrays.asList("c3RvbmU=", null)));
        holding.getStructures().add(hall);

        final ObjectMapper mapper = new ObjectMapper();
        final Holding read = mapper.readValue(mapper.writeValueAsString(holding), Holding.class);

        assertEquals(holding, read);
        assertTrue(read.find(hall.getId()).orElseThrow().getJob().isHeld());
        assertEquals(40, read.getStructures().getFirst().getJob().getSpent().get("wood"));
    }

    @Test
    void ac25_aMoveJobKeepsItsPacePositionAndPicksThroughARoundTrip() throws Exception {
        final Holding holding = new Holding();
        final PlacedStructure hall = structure(StructureCondition.ACTIVE);
        hall.setStage(1);
        hall.getUpgrades().put(0, "lantern");
        final Job job = Job.start(JobKind.MOVE, Duration.ofMinutes(10), ResourceCost.of(Map.of("wood", 2)), 1, 0);
        job.setRate(2.0, MINUTE);
        job.setTarget(new StructurePosition(30, 64, 5, 3));
        hall.setJob(job);
        holding.getStructures().add(hall);

        final ObjectMapper mapper = new ObjectMapper();
        final PlacedStructure read = mapper.readValue(mapper.writeValueAsString(holding), Holding.class)
                .getStructures().getFirst();

        assertEquals(hall.getPosition(), read.getPosition());
        assertEquals(1, read.getStage());
        assertEquals(StructureCondition.ACTIVE, read.getCondition());
        assertEquals(Optional.of("lantern"), read.upgradeAt(0));
        assertEquals(new StructurePosition(30, 64, 5, 3), read.getJob().getTarget());
        assertEquals(job.progress(4 * MINUTE), read.getJob().progress(4 * MINUTE), 1e-9);
    }

    @Test
    void aRecordWrittenBeforeStagesStillReads() throws Exception {
        final String old = "{\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"hall\",\"version\":2,"
                + "\"condition\":\"ACTIVE\",\"job\":{\"kind\":\"UPGRADE\",\"targetVersion\":3}}";

        final PlacedStructure read = new ObjectMapper().readValue(old, PlacedStructure.class);

        assertEquals(2, read.getStage());
        assertEquals(JobKind.ADVANCE, read.getJob().getKind());
        assertEquals(3, read.getJob().getTargetStage());
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
