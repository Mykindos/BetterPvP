package me.mykindos.betterpvp.clans.world.discovery;

import lombok.Getter;
import me.mykindos.betterpvp.core.world.site.crew.Crew;
import me.mykindos.betterpvp.clans.world.island.IslandInstance;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

/**
 * A crew out on the open sea: who is aboard, the hull they are standing on, and the simulation that decides where the
 * sea thinks they are.
 * <p>
 * The vessel never moves. It sits at its mooring in a private limbo world for the whole expedition while
 * {@link ShipDynamics} and {@link Ocean} carry a virtual ship across an imaginary plane; everything the crew is
 * supposed to sail past is drawn near the real hull at the offset the plane says it should be. That is what
 * {@link #render(OceanOffset)} is for, and it is the join between the two halves of the feature.
 */
@Getter
public class Expedition {

    private final Crew crew;

    /** The private stretch of ocean they sail in, released when they come about. */
    private final IslandInstance limbo;

    private final ShipDynamics dynamics;
    private final Ocean ocean;

    /** What the vessel occupies in the limbo world — the bounds of the hull that was moored there. */
    private final BoundingBox hull;

    /** The berth marker the hull was pasted against; its yaw is the hull's real facing. */
    private final Location anchor;

    /** The surface the ship floats on, found once when the expedition opens rather than probed every tick. */
    private final double waterY;

    private final long startedAt;

    /** The real hull's axes, derived from the anchor once because they cannot change while the ship is moored. */
    private final ShipFrame frame;

    public Expedition(@NotNull Crew crew, @NotNull IslandInstance limbo, @NotNull ShipDynamics dynamics,
                      @NotNull Ocean ocean, @NotNull BoundingBox hull, @NotNull Location anchor, double waterY,
                      long startedAt) {
        this.crew = crew;
        this.limbo = limbo;
        this.dynamics = dynamics;
        this.ocean = ocean;
        this.hull = hull;
        this.anchor = anchor;
        this.waterY = waterY;
        this.startedAt = startedAt;
        this.frame = ShipFrame.of(anchor.getX(), anchor.getZ(), anchor.getYaw());
    }

    public long elapsedSeconds(long now) {
        return Math.max(0, (now - startedAt) / 1000L);
    }

    /**
     * Where a ship-local offset falls in the limbo world, on the water beside the moored hull.
     * <p>
     * The offset arrives with the virtual heading already divided out by {@link Ocean#localOf}, so nothing here reads
     * {@link ShipDynamics#getHeading()}: applying it a second time would spin everything around the deck as the crew
     * steered, instead of sliding past a hull that is pointing the way it always did.
     */
    public @NotNull Location render(@NotNull OceanOffset offset) {
        return new Location(anchor.getWorld(), frame.worldX(offset), waterY, frame.worldZ(offset));
    }

    /** The point currently lying at {@code bearing} degrees off the bow and {@code distance} blocks out. */
    public @NotNull Location renderAt(double bearing, double distance) {
        final double heading = dynamics.getHeading();
        return render(ocean.localOf(ocean.pointAt(heading, bearing, distance), heading));
    }

    public boolean isAboard(@NotNull Location location) {
        return location.getWorld() != null
                && location.getWorld().getName().equals(limbo.getWorldName())
                && hull.contains(location.getX(), location.getY(), location.getZ());
    }
}
