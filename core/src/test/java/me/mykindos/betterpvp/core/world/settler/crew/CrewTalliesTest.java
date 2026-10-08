package me.mykindos.betterpvp.core.world.settler.crew;

import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.StructureRemovedEvent;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static me.mykindos.betterpvp.core.world.settler.crew.CrewFixture.CAMP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CrewTalliesTest {

    private static final long HOUR = 3_600_000L;

    private final CrewFixture fixture = new CrewFixture();
    private CrewTallies tallies;

    @BeforeEach
    void setUp() {
        when(fixture.instances.all()).thenReturn(List.of(
                new SiteInstance(UUID.randomUUID(), CAMP, "camp_7", SiteInstance.State.READY)));
        tallies = new CrewTallies(fixture.sites, fixture.tracker, fixture.settlers, fixture.instances, fixture.rule);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    /** A hall advancing with a crew of 5 Workforce, so it runs. */
    private PlacedStructure staffed() {
        final PlacedStructure hall = fixture.advancing();
        fixture.enlist(hall, fixture.builder(5));
        fixture.applyRule(hall);
        return hall;
    }

    @Test
    void ac28_runningTalliesComeFirstThenTheLastFinishedOne() {
        final PlacedStructure done = staffed();
        tallies.sweep();
        fixture.now += HOUR;
        final PlacedStructure running = staffed();
        tallies.sweep();

        final List<CrewTally> listed = tallies.tallies(CAMP);
        assertEquals(2, listed.size());
        assertEquals(running.getId(), listed.get(0).getStructure());
        assertFalse(listed.get(0).isFinished());
        assertEquals(done.getId(), listed.get(1).getStructure());
        assertTrue(listed.get(1).isFinished());
    }

    @Test
    void ac28_aFinishedJobIsNeverTalliedAgain() {
        staffed();
        tallies.sweep();
        fixture.now += HOUR;
        tallies.sweep();
        final CrewTally last = tallies.tallies(CAMP).getFirst();
        fixture.now += HOUR;
        tallies.sweep();

        assertEquals(1, tallies.tallies(CAMP).size());
        assertSame(last, tallies.tallies(CAMP).getFirst());
    }

    @Test
    void ac28_aCancelledJobDropsItsRunningTally() {
        final PlacedStructure hall = staffed();
        tallies.sweep();
        hall.setJob(null);
        tallies.sweep();

        assertTrue(tallies.tallies(CAMP).isEmpty());
    }

    @Test
    void ac28_aRemovedStructureDropsBothTallies() {
        final PlacedStructure hall = staffed();
        tallies.sweep();
        fixture.now += HOUR;
        tallies.sweep();
        hall.setJob(Job.start(hall.getJob().getKind(), Duration.ofHours(1),
                hall.getJob().getSpent(), 1, fixture.now));
        tallies.sweep();
        assertEquals(2, tallies.tallies(CAMP).size());

        tallies.onRemoved(new StructureRemovedEvent(CAMP, hall, true));

        assertTrue(tallies.tallies(CAMP).isEmpty());
    }

    @Test
    void ac28_aRestartStartsEachTallyAgainFromWhereItsJobStands() {
        final PlacedStructure hall = staffed();
        tallies.sweep();
        fixture.now += HOUR / 2;
        tallies.sweep();
        final CrewTally before = tallies.tallies(CAMP).getFirst();

        final CrewTallies restarted = new CrewTallies(fixture.sites, fixture.tracker, fixture.settlers,
                fixture.instances, fixture.rule);
        restarted.sweep();
        final CrewTally after = restarted.tallies(CAMP).getFirst();
        fixture.now += HOUR / 2;
        restarted.sweep();

        assertNotSame(before, after);
        assertEquals(0.5, after.totalWork(), 1e-9, "only the half run since the restart is counted");
        assertEquals(hall.getId(), after.getStructure());
    }
}
