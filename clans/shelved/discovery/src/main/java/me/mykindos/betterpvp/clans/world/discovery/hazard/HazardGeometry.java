package me.mykindos.betterpvp.clans.world.discovery.hazard;

import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

/**
 * The two questions asked of every hazard on every tick: has it gone, and has the hull hit it.
 * <p>
 * Pure numbers, so the answers are checkable without a sea to float them on.
 */
public final class HazardGeometry {

    private HazardGeometry() {
    }

    /**
     * Whether something that far from the ship has fallen off the back of the sea.
     * <p>
     * The radius has to sit clear of the range hazards are spawned at, or a hazard is culled on the tick it appears.
     */
    public static boolean beyond(double distance, double despawnRadius) {
        return distance > despawnRadius;
    }

    /**
     * Whether a hazard drawn at {@code (x, z)} is touching the hull.
     * <p>
     * Height is deliberately not part of it: the sea, the hull and everything drifting past it are all at water level,
     * and a vertical test would only ever compare a projected surface position against the deck it is level with.
     */
    public static boolean strikes(@NotNull BoundingBox hull, double x, double z, double radius) {
        final BoundingBox reach = hull.clone().expand(radius, 0, radius);
        return x >= reach.getMinX() && x <= reach.getMaxX() && z >= reach.getMinZ() && z <= reach.getMaxZ();
    }
}
