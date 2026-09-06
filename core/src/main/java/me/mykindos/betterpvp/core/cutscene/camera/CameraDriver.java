package me.mykindos.betterpvp.core.cutscene.camera;

import org.jetbrains.annotations.NotNull;

/**
 * The pose of one travelling shot at any tick within it.
 * <p>
 * Pure and stateless with respect to the world, so the curve a shot follows can be checked without a server: this is
 * where a transition is right or wrong, and everything around it is plumbing.
 */
public final class CameraDriver {

    private final CameraPose from;
    private final CameraPose to;
    private final int travelTicks;
    private final Easing easing;

    public CameraDriver(@NotNull CameraPose from, @NotNull CameraPose to, int travelTicks, @NotNull Easing easing) {
        this.from = from;
        this.to = to;
        this.travelTicks = Math.max(0, travelTicks);
        this.easing = easing;
    }

    /**
     * @param ticksInBeat ticks since this beat began, {@code 0} on its first tick
     * @return where the camera should be on that tick
     */
    public @NotNull CameraPose poseAt(int ticksInBeat) {
        if (hasArrived(ticksInBeat)) {
            return to;
        }
        // Progress is measured against the last travelling tick, so the eased curve reaches exactly 1 on arrival
        // rather than stopping just short of the destination and being snapped there.
        final double progress = (double) ticksInBeat / travelTicks;
        return from.interpolate(to, easing.apply(progress));
    }

    /** Whether the camera has finished travelling - the point from which a beat's advance condition is consulted. */
    public boolean hasArrived(int ticksInBeat) {
        return travelTicks == 0 || ticksInBeat >= travelTicks;
    }

    public int getTravelTicks() {
        return travelTicks;
    }
}
