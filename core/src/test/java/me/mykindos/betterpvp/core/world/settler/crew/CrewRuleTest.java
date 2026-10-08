package me.mykindos.betterpvp.core.world.settler.crew;

import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.mykindos.betterpvp.core.world.settler.crew.CrewFixture.CAMP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrewRuleTest {

    private final CrewFixture fixture = new CrewFixture();
    private final CrewRule rule = fixture.rule;

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private static Job job(JobKind kind, int targetStage) {
        return Job.start(kind, Duration.ofMinutes(5), ResourceCost.NONE, targetStage, 0);
    }

    @Test
    void ac8_aJobWaitsUntilItsCrewMeetsTheThreshold() {
        final PlacedStructure hall = fixture.advancing();
        final Job job = hall.getJob();

        assertEquals(5, rule.threshold(hall, job));
        assertTrue(rule.holds(CAMP, hall, job));

        job.getStaff().add(fixture.builder(3).getId());
        assertTrue(rule.holds(CAMP, hall, job));
        job.getStaff().add(fixture.builder(2).getId());
        assertFalse(rule.holds(CAMP, hall, job));
        assertEquals(5, rule.workforce(CAMP, hall, job));
    }

    @Test
    void ac9_buildingAndAdvancingNeedTheStageTheyWorkToward() {
        final PlacedStructure hall = fixture.hall(0);
        assertEquals(2, rule.threshold(hall, job(JobKind.BUILD, 0)));
        assertEquals(5, rule.threshold(hall, job(JobKind.ADVANCE, 1)));
    }

    @Test
    void ac9_repairsAndMovesNeedHalfTheStageTheyStandAtRoundedUp() {
        final PlacedStructure hall = fixture.hall(1);
        assertEquals(3, rule.threshold(hall, job(JobKind.REPAIR, 1)));
        assertEquals(3, rule.threshold(hall, job(JobKind.MOVE, 1)));
        assertEquals(1, rule.threshold(fixture.hall(0), job(JobKind.REPAIR, 0)));
    }

    @Test
    void ac9_fittingAnUpgradeNeedsItsOwnWorkforce() {
        final PlacedStructure hall = fixture.hall(0);
        final Job lantern = job(JobKind.FIT_UPGRADE, 0);
        lantern.setUpgrade("lantern");
        assertEquals(3, rule.threshold(hall, lantern));
    }

    @Test
    void ac9_whatTheCatalogueDoesNotKnowNeedsNothing() {
        final PlacedStructure hall = fixture.hall(0);
        final Job unknownUpgrade = job(JobKind.FIT_UPGRADE, 0);
        unknownUpgrade.setUpgrade("moat");
        assertEquals(0, rule.threshold(hall, unknownUpgrade));
        assertEquals(0, rule.threshold(hall, job(JobKind.FIT_UPGRADE, 0)), "no upgrade named");
        assertEquals(0, rule.threshold(hall, job(JobKind.ADVANCE, 5)), "a stage the type does not have");

        final PlacedStructure shed = new PlacedStructure(UUID.randomUUID(), "shed",
                new StructurePosition(0, 64, 0, 0), StructureCondition.ACTIVE);
        assertEquals(0, rule.threshold(shed, job(JobKind.BUILD, 0)));
    }

    @Test
    void ac10_aJobThatNeedsNoWorkforceRunsWithoutACrewAtItsListedTime() {
        final PlacedStructure hall = fixture.hall(0);
        final Job flag = job(JobKind.FIT_UPGRADE, 0);
        flag.setUpgrade("flag");
        hall.setJob(flag);

        assertFalse(rule.holds(CAMP, hall, flag));
        assertEquals(1.0, rule.rate(CAMP, hall, flag), 1e-9);
    }

    @Test
    void ac11_strikersLeaversAndBuildersWithoutStatsBringNothing() {
        final PlacedStructure hall = fixture.hall(0);
        final Job job = job(JobKind.BUILD, 0);
        hall.setJob(job);
        final Settler striker = fixture.builder(2);
        striker.changeState(SettlerState.STRIKING, 0);
        final Settler statless = fixture.builder(-1);
        job.getStaff().add(striker.getId());
        job.getStaff().add(statless.getId());
        job.getStaff().add(UUID.randomUUID());

        assertEquals(0, rule.workforce(CAMP, hall, job));
        assertTrue(rule.holds(CAMP, hall, job));
        assertEquals(List.of(), rule.stats(CAMP, hall, job));
    }

    @Test
    void ac11_theSiteIsAskedForEachBuilderBesideTheWorkingCrew() {
        final PlacedStructure hall = fixture.advancing();
        final Job job = hall.getJob();
        final Settler first = fixture.builder(3);
        final Settler second = fixture.builder(2);
        final Settler striker = fixture.builder(4);
        striker.changeState(SettlerState.STRIKING, 0);
        job.getStaff().addAll(List.of(first.getId(), second.getId(), striker.getId()));

        assertEquals(5, rule.workforce(CAMP, hall, job));
        assertEquals(2, fixture.site.statsCrews.size());
        fixture.site.statsCrews.forEach(crew -> assertEquals(List.of(first, second), crew));
    }

    @Test
    void ac12_aJobPausesWhenItsCrewFallsShortAndRunsAgainOnceItMeetsTheThreshold() {
        final PlacedStructure hall = fixture.advancing();
        final Job job = hall.getJob();
        final Settler first = fixture.builder(3);
        final Settler second = fixture.builder(2);
        job.getStaff().addAll(List.of(first.getId(), second.getId()));
        fixture.applyRule(hall);
        assertFalse(job.isHeld());

        first.changeState(SettlerState.STRIKING, 0);
        fixture.applyRule(hall);
        assertTrue(job.isHeld(), "a striker leaves the crew short");

        first.changeState(SettlerState.WORKING, 0);
        fixture.applyRule(hall);
        assertFalse(job.isHeld());

        fixture.settlers.remove(CAMP, second.getId(), SettlerLeaveReason.UNHAPPY);
        fixture.applyRule(hall);
        assertTrue(job.isHeld(), "a member that leaves the roster leaves the crew short");

        job.getStaff().add(fixture.builder(2).getId());
        fixture.applyRule(hall);
        assertFalse(job.isHeld());
    }

    @Test
    void ac15_withNoWorkingCrewTheCrewSpeedIsZero() {
        final PlacedStructure hall = fixture.advancing();
        assertEquals(0.0, rule.speed(CAMP, hall, hall.getJob()), 1e-9);
    }

    @Test
    void ac16_aJobWithAWorkingCrewRunsAtTheCrewSpeed() {
        final PlacedStructure hall = fixture.advancing();
        final Job job = hall.getJob();
        job.getStaff().add(fixture.builder(3, 2.0, 0.5, SettlerRarity.RARE).getId());
        job.getStaff().add(fixture.builder(2, 1.0, 0.5, SettlerRarity.COMMON).getId());

        assertEquals(2.5, rule.rate(CAMP, hall, job), 1e-9);
        assertEquals(2.5, rule.speed(CAMP, hall, job), 1e-9);
        fixture.applyRule(hall);
        assertEquals(2.5, job.getRate(), 1e-9);

        fixture.site.limits = new CrewLimits(5, 2.0, Map.of(), 0.1);
        assertEquals(2.0, rule.rate(CAMP, hall, job), 1e-9, "the site's maximum speed caps the pace");
    }
}
