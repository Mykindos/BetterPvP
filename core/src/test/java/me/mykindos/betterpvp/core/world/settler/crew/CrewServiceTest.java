package me.mykindos.betterpvp.core.world.settler.crew;

import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerLeaveReason;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static me.mykindos.betterpvp.core.world.settler.crew.CrewFixture.CAMP;
import static me.mykindos.betterpvp.core.world.settler.crew.CrewFixture.reason;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrewServiceTest {

    private final CrewFixture fixture = new CrewFixture();
    private final CrewService crews = fixture.crews;

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    /** A hall whose build ran out of time at {@code now}, waiting to be claimed. */
    private PlacedStructure finishedHall() {
        final PlacedStructure hall = fixture.hall(0);
        hall.setJob(Job.start(JobKind.BUILD, Duration.ofMillis(1_000), ResourceCost.NONE, 0, 0));
        return hall;
    }

    /** Puts {@code settler} on {@code structure}'s crew without the checks, as a crew saved before would be. */
    private void onCrew(PlacedStructure structure, Settler settler) {
        fixture.settlers.assign(CAMP, settler.getId(), structure.getId().toString());
        structure.getJob().getStaff().add(settler.getId());
    }

    @Test
    void ac1_aBuilderJoinsAJobWaitingForItsFirstCrewRunningOrPaused() {
        final PlacedStructure hall = fixture.advancing();
        fixture.applyRule(hall);
        assertTrue(hall.getJob().isHeld());
        assertEquals("done", reason(fixture.enlist(hall, fixture.builder(3))), "waiting for its first crew");

        assertEquals("done", reason(fixture.enlist(hall, fixture.builder(2))));
        fixture.applyRule(hall);
        assertFalse(hall.getJob().isHeld());
        assertEquals("done", reason(fixture.enlist(hall, fixture.builder(1))), "running");

        hall.getJob().hold("siege", fixture.now);
        assertEquals("done", reason(fixture.enlist(hall, fixture.builder(1))), "paused");
    }

    @Test
    void ac1_noJobOrAJobWhoseTimeIsUpIsRefused() {
        final Settler builder = fixture.builder(2);
        assertEquals("core.settler.crew.no_job", reason(fixture.enlist(fixture.hall(0), builder)));
        assertEquals("core.settler.crew.no_job", reason(fixture.enlist(finishedHall(), builder)));
        assertEquals("core.settler.crew.no_job",
                reason(crews.enlist(fixture.worksite, UUID.randomUUID(), builder.getId())));
        assertNull(builder.getAssignment());
    }

    @Test
    void ac2_onlyBuildersOnTheRosterCanJoin() {
        final PlacedStructure hall = fixture.advancing();
        assertEquals("core.settler.crew.not_builder", reason(fixture.enlist(hall, fixture.settler("farmer"))));
        assertEquals("core.settler.crew.not_builder", reason(fixture.enlist(hall, fixture.settler(null))));
        assertEquals("core.settler.not_found",
                reason(crews.enlist(fixture.worksite, hall.getId(), UUID.randomUUID())));
        assertTrue(hall.getJob().getStaff().isEmpty());
    }

    @Test
    void ac3_aBuilderAlreadyOnThisCrewOrBusyElsewhereIsRefused() {
        final PlacedStructure hall = fixture.advancing();
        final Settler builder = fixture.builder(2);
        fixture.enlist(hall, builder);
        assertEquals("core.settler.crew.already_on", reason(fixture.enlist(hall, builder)));

        final PlacedStructure other = fixture.advancing();
        fixture.applyRule(hall);
        assertTrue(hall.getJob().isHeld(), "its own job is paused");
        assertEquals("core.settler.crew.busy", reason(fixture.enlist(other, builder)));
        assertEquals(List.of(builder.getId()), hall.getJob().getStaff());
        assertTrue(other.getJob().getStaff().isEmpty());
    }

    @Test
    void ac4_aFullCrewIsRefused() {
        fixture.site.limits = new CrewLimits(2, 4.0, Map.of(), 0.1);
        final PlacedStructure hall = fixture.advancing();
        fixture.enlist(hall, fixture.builder(1));
        final Settler striker = fixture.builder(1);
        fixture.enlist(hall, striker);
        striker.changeState(SettlerState.STRIKING, 0);

        assertEquals("core.settler.crew.full", reason(fixture.enlist(hall, fixture.builder(1))),
                "a striker still takes a place");
        assertEquals(2, hall.getJob().getStaff().size());
    }

    @Test
    void ac4_aRarityIsRefusedOnceTheCrewHoldsItsLimit() {
        fixture.site.limits = new CrewLimits(5, 4.0, Map.of(SettlerRarity.RARE, 1), 0.1);
        final PlacedStructure hall = fixture.advancing();
        final Settler rare = fixture.builder(1, 1, 1, SettlerRarity.RARE);
        fixture.enlist(hall, rare);
        rare.changeState(SettlerState.STRIKING, 0);

        assertEquals("core.settler.crew.rarity_full",
                reason(fixture.enlist(hall, fixture.builder(1, 1, 1, SettlerRarity.RARE))), "a striker still counts");
        assertEquals("done", reason(fixture.enlist(hall, fixture.builder(1, 1, 1, SettlerRarity.UNCOMMON))));
        assertEquals("done", reason(fixture.enlist(hall, fixture.builder(1, 1, 1, SettlerRarity.UNCOMMON))),
                "a rarity with no limit has none");
    }

    @Test
    void ac4_aSiteWithNoLimitsHasNone() {
        final PlacedStructure hall = fixture.advancing();
        for (int i = 0; i < 10; i++) {
            assertEquals("done", reason(fixture.enlist(hall, fixture.builder(1, 1, 1, SettlerRarity.LEGENDARY))));
        }
        assertEquals(10, hall.getJob().getStaff().size());
    }

    @Test
    void ac5_theSettlerServiceRefusesStrikersAndAFullBuilderCap() {
        final PlacedStructure hall = fixture.advancing();
        final Settler striker = fixture.builder(2);
        striker.changeState(SettlerState.STRIKING, 0);
        assertEquals("core.settler.will_not_work", reason(fixture.enlist(hall, striker)));

        fixture.site.builderCap = OptionalInt.of(1);
        fixture.enlist(fixture.advancing(), fixture.builder(2));
        assertEquals("core.settler.working_full", reason(fixture.enlist(hall, fixture.builder(2))));
        assertTrue(hall.getJob().getStaff().isEmpty());
    }

    @Test
    void ac6_aBuilderThatJoinsIsWorkingOnTheStaffWrittenDownAndTheRulesApplied() {
        final PlacedStructure hall = fixture.advancing();
        final Settler builder = fixture.builder(5);
        final int before = fixture.site.changes;

        assertEquals("done", reason(fixture.enlist(hall, builder)));

        assertEquals(SettlerState.WORKING, builder.getState());
        assertEquals(hall.getId().toString(), builder.getAssignment());
        assertEquals(List.of(builder.getId()), hall.getJob().getStaff());
        assertTrue(fixture.site.changes > before);
        verify(fixture.constructionSite).changed(CAMP);
        verify(fixture.tracker).refresh(fixture.worksite);
    }

    @Test
    void ac7_joiningForAPlayerNeedsTheAssignPermission() {
        final PlacedStructure hall = fixture.advancing();
        final Player player = mock(Player.class);
        final Settler builder = fixture.builder(2);
        fixture.site.allowed = false;

        assertEquals("core.settler.not_allowed", reason(crews.join(player, fixture.world, hall.getId(), builder.getId())));
        assertTrue(hall.getJob().getStaff().isEmpty());
        assertEquals("done", reason(fixture.enlist(hall, builder)), "the site's own enlisting checks nothing");

        fixture.site.allowed = true;
        assertEquals("done", reason(crews.join(player, fixture.world, hall.getId(), fixture.builder(2).getId())));
    }

    @Test
    void ac7_joiningWhereNothingIsLoadedIsRefused() {
        final PlacedStructure hall = fixture.advancing();
        final Settler builder = fixture.builder(2);
        final World elsewhere = mock(World.class);
        when(fixture.sites.worksite(elsewhere)).thenReturn(Optional.empty());
        assertEquals("core.settler.not_loaded",
                reason(crews.join(mock(Player.class), elsewhere, hall.getId(), builder.getId())));
    }

    @Test
    void ac7_joiningWhereTheRosterIsNotLoadedIsRefusedAsNotLoaded() {
        final PlacedStructure hall = fixture.advancing();
        final Settler builder = fixture.builder(2);
        fixture.site.loaded = false;
        assertEquals("core.settler.not_loaded", reason(fixture.enlist(hall, builder)));
    }

    @Test
    void ac17_aBuilderOnARunningJobStaysUntilItEnds() {
        final PlacedStructure hall = fixture.advancing();
        final Settler first = fixture.builder(3);
        final Settler second = fixture.builder(2);
        fixture.enlist(hall, first);
        fixture.enlist(hall, second);
        fixture.applyRule(hall);
        assertFalse(hall.getJob().isHeld());

        assertEquals("core.settler.job_running", reason(fixture.settlers.unassign(CAMP, first.getId())));
        assertEquals("core.settler.job_running", reason(fixture.settlers.dismiss(CAMP, first.getId())));
        assertEquals(hall.getId().toString(), first.getAssignment());
    }

    @Test
    void ac17_aBuilderOnAPausedOrWaitingJobCanBeTakenOffOrDismissed() {
        final PlacedStructure hall = fixture.advancing();
        final Settler first = fixture.builder(3);
        final Settler second = fixture.builder(2);
        fixture.enlist(hall, first);
        fixture.applyRule(hall);
        assertTrue(hall.getJob().isHeld(), "waiting for enough Workforce");
        assertEquals("done", reason(fixture.settlers.dismiss(CAMP, first.getId())));

        fixture.enlist(hall, fixture.builder(3));
        fixture.enlist(hall, second);
        fixture.applyRule(hall);
        hall.getJob().hold("siege", fixture.now);
        assertEquals("done", reason(fixture.settlers.unassign(CAMP, second.getId())));
        assertNull(second.getAssignment());
    }

    @Test
    void ac18_takingABuilderOffAPausedJobTakesItOffTheCrew() {
        fixture.site.limits = new CrewLimits(2, 4.0, Map.of(), 0.1);
        final PlacedStructure hall = fixture.advancing();
        final Settler first = fixture.builder(3);
        final Settler second = fixture.builder(2);
        fixture.enlist(hall, first);
        fixture.enlist(hall, second);
        fixture.applyRule(hall);
        first.changeState(SettlerState.STRIKING, 0);
        fixture.applyRule(hall);
        assertTrue(hall.getJob().isHeld());

        assertEquals("done", reason(fixture.settlers.unassign(CAMP, second.getId())));

        final Job job = hall.getJob();
        assertFalse(job.getStaff().contains(second.getId()), "it leaves the job's staff at once");
        assertEquals(List.of(first), fixture.rule.crew(CAMP, job));
        assertEquals("done", reason(fixture.enlist(hall, fixture.builder(1))), "its place is free again");

        final PlacedStructure other = fixture.advancing();
        assertEquals("done", reason(fixture.enlist(other, second)));
        assertFalse(job.getStaff().contains(second.getId()), "it is on one crew only");
    }

    @Test
    void ac18_takingABuilderOffLeavesTheJobPausedWhenTheCrewFallsShort() {
        final PlacedStructure hall = fixture.advancing();
        final Settler first = fixture.builder(3);
        final Settler second = fixture.builder(2);
        fixture.enlist(hall, first);
        fixture.enlist(hall, second);
        hall.getJob().hold("siege", fixture.now);

        fixture.settlers.unassign(CAMP, second.getId());
        hall.getJob().release("siege", fixture.now);
        fixture.applyRule(hall);

        assertEquals(3, fixture.rule.workforce(CAMP, hall, hall.getJob()));
        assertTrue(hall.getJob().isHeld(), "3 of 5 Workforce left");
    }

    @Test
    void ac19_aMemberThatLeavesTheRosterIsDroppedFromTheStaff() {
        final PlacedStructure hall = fixture.advancing();
        final Settler first = fixture.builder(3);
        final Settler second = fixture.builder(2);
        fixture.enlist(hall, first);
        fixture.enlist(hall, second);
        fixture.settlers.remove(CAMP, second.getId(), SettlerLeaveReason.UNPAID);
        final int before = fixture.site.changes;

        crews.release(CAMP, fixture.holding);

        assertEquals(List.of(first.getId()), hall.getJob().getStaff());
        assertTrue(fixture.site.changes > before);
    }

    @Test
    void ac20_aJobWhoseTimeIsUpLetsItsCrewGoOnce() {
        final PlacedStructure hall = finishedHall();
        final Job job = hall.getJob();
        final Settler first = fixture.builder(2);
        final Settler striker = fixture.builder(2);
        onCrew(hall, first);
        onCrew(hall, striker);
        striker.changeState(SettlerState.STRIKING, 0);

        crews.release(CAMP, fixture.holding);
        crews.release(CAMP, fixture.holding);

        assertEquals(List.of(List.of(first, striker)), fixture.site.finished);
        assertEquals(1, first.getJobsFinished());
        assertEquals(1, striker.getJobsFinished(), "every member still on the roster is credited");
        assertTrue(job.getStaff().isEmpty());
        assertNull(first.getAssignment());
        assertEquals(SettlerState.IDLE, first.getState());
        assertNull(striker.getAssignment());
        assertNotNull(hall.getJob(), "before anyone claims it");
    }

    @Test
    void ac20_aRunningJobKeepsItsCrew() {
        final PlacedStructure hall = fixture.advancing();
        final Settler member = fixture.builder(5);
        fixture.enlist(hall, member);

        crews.release(CAMP, fixture.holding);

        assertEquals(hall.getId().toString(), member.getAssignment());
        assertEquals(List.of(member.getId()), hall.getJob().getStaff());
        assertTrue(fixture.site.finished.isEmpty());
    }

    @Test
    void ac21_aCancelledJobLetsItsCrewGoWithoutCountingIt() {
        final PlacedStructure hall = fixture.advancing();
        final Settler member = fixture.builder(5);
        fixture.enlist(hall, member);
        hall.setJob(null);

        crews.release(CAMP, fixture.holding);

        assertNull(member.getAssignment());
        assertEquals(SettlerState.IDLE, member.getState());
        assertEquals(0, member.getJobsFinished());
        assertTrue(fixture.site.finished.isEmpty());
    }

    @Test
    void ac21_aRemovedStructureLetsItsCrewGoWithoutCountingIt() {
        final PlacedStructure hall = fixture.advancing();
        final Settler member = fixture.builder(5);
        fixture.enlist(hall, member);
        fixture.holding.getStructures().remove(hall);

        crews.release(CAMP, fixture.holding);

        assertNull(member.getAssignment());
        assertEquals(0, member.getJobsFinished());
        assertTrue(fixture.site.finished.isEmpty());
    }

    @Test
    void ac22_theSweepLetsGoOfAJobThatFinishedWhileItsWorldWasClosed() {
        final PlacedStructure hall = finishedHall();
        final Settler member = fixture.builder(2);
        onCrew(hall, member);
        when(fixture.instances.all()).thenReturn(List.of(
                new SiteInstance(UUID.randomUUID(), CAMP, "camp_7", SiteInstance.State.READY)));
        fixture.bukkit.when(() -> Bukkit.getWorld("camp_7")).thenReturn(fixture.world);

        crews.sweep();

        assertNull(member.getAssignment());
        assertEquals(1, member.getJobsFinished());
    }

    @Test
    void ac22_theSweepUnassignsBuildersWhoseAssignmentPointsAtNoCrew() {
        final PlacedStructure hall = fixture.advancing();
        final Settler offStaff = fixture.builder(2);
        fixture.settlers.assign(CAMP, offStaff.getId(), hall.getId().toString());
        final Settler nowhere = fixture.builder(2);
        fixture.settlers.assign(CAMP, nowhere.getId(), UUID.randomUUID().toString());
        final Settler unreadable = fixture.builder(2);
        fixture.settlers.assign(CAMP, unreadable.getId(), "not-a-structure");
        final Settler farmer = fixture.settler("farmer");
        fixture.settlers.assign(CAMP, farmer.getId(), "farm");

        crews.release(CAMP, fixture.holding);

        assertNull(offStaff.getAssignment());
        assertNull(nowhere.getAssignment());
        assertNull(unreadable.getAssignment());
        assertEquals("farm", farmer.getAssignment(), "only Builders are let go");
    }
}
