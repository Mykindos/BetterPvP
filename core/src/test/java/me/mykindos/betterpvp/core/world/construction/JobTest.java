package me.mykindos.betterpvp.core.world.construction;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobTest {

    private static final long MINUTE = 60_000;

    private static Job tenMinuteBuild(long start) {
        return Job.start(JobKind.BUILD, Duration.ofMinutes(10), ResourceCost.NONE, 0, start);
    }

    @Test
    void progressRunsInRealTime() {
        final Job job = tenMinuteBuild(0);

        assertEquals(0.0, job.progress(0), 1e-9);
        assertEquals(0.5, job.progress(5 * MINUTE), 1e-9);
        assertEquals(1.0, job.progress(30 * MINUTE), 1e-9, "it never goes past done");
        assertTrue(job.isDone(10 * MINUTE));
    }

    @Test
    void holdingItStopsTheClockAndReleasingItCarriesOn() {
        final Job job = tenMinuteBuild(0);

        job.hold("siege", 2 * MINUTE);
        assertEquals(0.2, job.progress(7 * MINUTE), 1e-9, "nothing counts while it is held");
        assertTrue(job.isHeld());

        job.release("siege", 7 * MINUTE);
        assertFalse(job.isHeld());
        assertEquals(0.3, job.progress(8 * MINUTE), 1e-9);
    }

    @Test
    void itRunsOnlyOnceEveryHoldIsGone() {
        final Job job = tenMinuteBuild(0);
        job.hold("siege", MINUTE);
        job.hold("staffing", MINUTE);

        job.release("siege", 2 * MINUTE);

        assertTrue(job.isHeld());
        assertEquals(0.1, job.progress(5 * MINUTE), 1e-9);
    }

    @Test
    void aNewRateOnlyAppliesFromWhenItWasSet() {
        final Job job = tenMinuteBuild(0);

        job.setRate(2.0, 4 * MINUTE);

        assertEquals(0.4 + 0.2 * 2, job.progress(6 * MINUTE), 1e-9);
        assertEquals(MINUTE, job.remainingMillis(6 * MINUTE), "a fifth left, at double speed");
    }

    @Test
    void aStoppedJobNeverFinishes() {
        final Job job = tenMinuteBuild(0);

        job.setRate(0, MINUTE);

        assertEquals(Long.MAX_VALUE, job.remainingMillis(MINUTE));
    }

    @Test
    void anInstantJobIsDoneStraightAway() {
        final Job job = Job.start(JobKind.MOVE, Duration.ZERO, ResourceCost.NONE, 0, 0);

        assertTrue(job.isDone(0));
        assertEquals(0, job.remainingMillis(0));
    }
}
