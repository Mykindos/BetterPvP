package me.mykindos.betterpvp.core.cutscene.camera;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The camera curve is the part of a cutscene that is right or wrong on its own, independent of any server, so it is
 * checked here rather than by watching a shot and deciding whether it felt smooth.
 */
@DisplayName("Camera driver")
class CameraDriverTest {

    private static final double EPSILON = 1.0E-6;

    private static CameraPose at(double x) {
        return new CameraPose(x, 0, 0, 0, 0);
    }

    @Nested
    @DisplayName("travel")
    class Travel {

        @Test
        @DisplayName("lands exactly on the destination, not just near it")
        void landsExactly() {
            final CameraDriver driver = new CameraDriver(at(0), at(10), 20, Easing.EASE_IN_OUT);

            // A curve that only approaches its destination leaves a visible snap on the last tick.
            assertEquals(10, driver.poseAt(20).getX(), EPSILON);
            assertTrue(driver.hasArrived(20));
        }

        @Test
        @DisplayName("starts at the origin")
        void startsAtOrigin() {
            final CameraDriver driver = new CameraDriver(at(0), at(10), 20, Easing.LINEAR);
            assertEquals(0, driver.poseAt(0).getX(), EPSILON);
        }

        @Test
        @DisplayName("holds the destination past the end of the travel")
        void holdsAfterArrival() {
            final CameraDriver driver = new CameraDriver(at(0), at(10), 20, Easing.LINEAR);
            assertEquals(10, driver.poseAt(400).getX(), EPSILON);
        }

        @Test
        @DisplayName("a zero-tick travel has already arrived on its first tick")
        void zeroTickIsInstant() {
            final CameraDriver driver = new CameraDriver(at(0), at(10), 0, Easing.CUT);

            assertTrue(driver.hasArrived(0));
            assertEquals(10, driver.poseAt(0).getX(), EPSILON);
        }

        @Test
        @DisplayName("is still travelling before its last tick")
        void travellingBeforeEnd() {
            final CameraDriver driver = new CameraDriver(at(0), at(10), 20, Easing.LINEAR);
            assertFalse(driver.hasArrived(19));
            assertEquals(5, driver.poseAt(10).getX(), EPSILON);
        }
    }

    @Nested
    @DisplayName("easing")
    class Easings {

        @Test
        @DisplayName("every curve is anchored at both ends")
        void anchored() {
            for (Easing easing : Easing.values()) {
                assertEquals(1, easing.apply(1), EPSILON, easing + " must reach its destination");
                if (easing != Easing.CUT) {
                    assertEquals(0, easing.apply(0), EPSILON, easing + " must start where it started");
                }
            }
        }

        @Test
        @DisplayName("ease-in is behind linear and ease-out is ahead of it")
        void curveDirections() {
            // The whole point of the two: one leaves slowly, the other arrives slowly.
            assertTrue(Easing.EASE_IN.apply(0.5) < 0.5);
            assertTrue(Easing.EASE_OUT.apply(0.5) > 0.5);
            assertEquals(0.5, Easing.EASE_IN_OUT.apply(0.5), EPSILON);
        }

        @Test
        @DisplayName("every curve advances monotonically")
        void monotonic() {
            for (Easing easing : Easing.values()) {
                double previous = -1;
                for (int step = 0; step <= 100; step++) {
                    final double value = easing.apply(step / 100.0);
                    assertTrue(value >= previous, easing + " must never move backwards");
                    previous = value;
                }
            }
        }
    }

    @Nested
    @DisplayName("yaw")
    class Yaw {

        private static CameraPose facing(float yaw) {
            return new CameraPose(0, 0, 0, yaw, 0);
        }

        @Test
        @DisplayName("takes the short way round the wrap point")
        void shortestPath() {
            // 170 to -170 is 20 degrees of turn. Interpolating the raw numbers would swing 340 the other way, which
            // reads as the camera spinning almost all the way around to look 20 degrees further left.
            final CameraPose halfway = facing(170).interpolate(facing(-170), 0.5);
            assertEquals(180, Math.abs(halfway.getYaw()), EPSILON);
        }

        @Test
        @DisplayName("an ordinary pan is unaffected")
        void ordinaryPan() {
            assertEquals(45, facing(0).interpolate(facing(90), 0.5).getYaw(), EPSILON);
        }

        @Test
        @DisplayName("reaches the target angle, which need not be the target number")
        void reachesTarget() {
            // Turning right from 170 by 20 degrees lands on 190, not on -170. They are the same bearing and the client
            // accepts either, so the assertion is about the angle rather than about the number that expresses it.
            final float reached = facing(170).interpolate(facing(-170), 1).getYaw();
            assertEquals(0, degreesApart(reached, -170f), EPSILON);
        }

        /** How far apart two bearings are, in degrees, regardless of how each is written. */
        private static float degreesApart(float first, float second) {
            final float difference = ((first - second) % 360f + 360f) % 360f;
            return Math.min(difference, 360f - difference);
        }
    }
}
