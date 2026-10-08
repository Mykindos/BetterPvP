package me.mykindos.betterpvp.core.world.settler.crew;

import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrewTallyTest {

    private static final CrewLimits LIMITS = new CrewLimits(5, 4.0, Map.of(), 0.10);
    private static final long MINUTE = 60_000;

    private final UUID fast = UUID.randomUUID();
    private final UUID slow = UUID.randomUUID();
    private final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), "workshop",
            new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
    private final Job job = Job.start(JobKind.ADVANCE, Duration.ofMinutes(10), ResourceCost.NONE, 1, 0);

    private Map<UUID, BuilderStats> crew() {
        final Map<UUID, BuilderStats> crew = new LinkedHashMap<>();
        crew.put(fast, new BuilderStats(2, 2.0, 0.5, "mason", Set.of()));
        crew.put(slow, new BuilderStats(2, 1.0, 0.5, "laborer", Set.of()));
        return crew;
    }

    @Test
    void ac24_progressIsSplitByWhatEachAddsToTheCrew() {
        final CrewTally tally = new CrewTally(structure, job, 0);
        tally.sample(crew(), LIMITS, 0.25, false, MINUTE);

        assertEquals(0.2, tally.getShares().get(fast).getWork(), 1e-9);
        assertEquals(0.05, tally.getShares().get(slow).getWork(), 1e-9);
        assertEquals(0.8, tally.shareOf(fast), 1e-9);
        assertEquals(0.2, tally.shareOf(slow), 1e-9);
    }

    @Test
    void ac25_wastedSpeedAveragesOverTheTimeWorked() {
        final CrewTally tally = new CrewTally(structure, job, 0);
        tally.sample(crew(), LIMITS, 0.25, false, MINUTE);
        tally.sample(crew(), LIMITS, 0.5, false, 2 * MINUTE);

        final CrewTally.Share slower = tally.getShares().get(slow);
        assertEquals(0.5, slower.wastedSpeed(), 1e-9, "half of its Speed is lost to not being the fastest");
        assertEquals(2 * MINUTE, slower.getMillis());
        assertEquals(0.0, tally.getShares().get(fast).wastedSpeed(), 1e-9);
    }

    @Test
    void ac26_timeHeldCountsForNothing() {
        final CrewTally tally = new CrewTally(structure, job, 0);
        tally.sample(crew(), LIMITS, 0.25, false, MINUTE);
        tally.sample(crew(), LIMITS, 0.25, true, 5 * MINUTE);

        assertEquals(MINUTE, tally.getShares().get(slow).getMillis());
        assertEquals(0.25, tally.totalWork(), 1e-9);
    }

    @Test
    void ac27_progressAfterTheCrewIsLetGoGoesToThoseWhoWorked() {
        final CrewTally tally = new CrewTally(structure, job, 0);
        tally.sample(crew(), LIMITS, 0.25, false, MINUTE);
        assertFalse(tally.isFinished());
        tally.finish(Map.of(), LIMITS, 1.0, 2 * MINUTE);

        assertTrue(tally.isFinished());
        assertEquals(1.0, tally.totalWork(), 1e-9);
        assertEquals(0.8, tally.shareOf(fast), 1e-9);
    }

    @Test
    void ac23_onlyProgressAfterTheTallyStartsIsCounted() {
        final CrewTally tally = new CrewTally(structure, job, 5 * MINUTE);
        tally.finish(crew(), LIMITS, 1.0, 10 * MINUTE);

        assertEquals(0.5, tally.totalWork(), 1e-9);
        assertEquals(JobKind.ADVANCE, tally.getKind());
        assertEquals(1, tally.getTargetStage());
        assertEquals("workshop", tally.getStructureType());
        assertEquals(structure.getId(), tally.getStructure());
        assertNull(tally.getUpgrade());
    }

    @Test
    void ac23_aTallyRecordsTheUpgradeItIsFitting() {
        final Job fitting = Job.start(JobKind.FIT_UPGRADE, Duration.ofMinutes(5), ResourceCost.NONE, 0, 0);
        fitting.setUpgrade("lantern");
        final CrewTally tally = new CrewTally(structure, fitting, 0);

        assertEquals(JobKind.FIT_UPGRADE, tally.getKind());
        assertEquals("lantern", tally.getUpgrade());
    }

    @Test
    void ac24_sharesOfTheCountedWorkAddUpToOne() {
        final CrewTally tally = new CrewTally(structure, job, 0);
        tally.sample(crew(), LIMITS, 0.1, false, MINUTE);
        tally.sample(Map.of(fast, crew().get(fast)), LIMITS, 0.3, false, 2 * MINUTE);

        assertEquals(1.0, tally.shareOf(fast) + tally.shareOf(slow), 1e-9);
        assertEquals(0.0, tally.shareOf(UUID.randomUUID()));
    }

    @Test
    void ac25_aCompatibleBonusIsNeverWastedInATally() {
        final Map<UUID, BuilderStats> paired = new LinkedHashMap<>();
        paired.put(fast, new BuilderStats(2, 1.0, 0.5, "mason", Set.of("carpenter")));
        paired.put(slow, new BuilderStats(2, 1.0, 0.5, "carpenter", Set.of("mason")));
        final CrewTally tally = new CrewTally(structure, job, 0);
        tally.sample(paired, LIMITS, 0.2, false, MINUTE);

        assertEquals(0.0, tally.getShares().get(fast).wastedSpeed(), 1e-9);
        assertEquals(0.0, tally.getShares().get(slow).wastedSpeed(), 1e-9);
    }
}
