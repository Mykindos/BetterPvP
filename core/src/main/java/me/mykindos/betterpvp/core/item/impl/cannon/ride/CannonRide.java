package me.mykindos.betterpvp.core.item.impl.cannon.ride;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One player's flight, from the click that boards them to the moment they hit the ground.
 * <p>
 * Owned by {@link CannonRideService}, not by the cannon: a cannon can dematerialize or be destroyed while its rider is
 * still hundreds of blocks away, so the ride holds only a nullable reference back to it.
 */
@Getter
public class CannonRide {

    private final @NotNull UUID rider;
    private final @NotNull RideOrigin origin;
    private final @NotNull RideMannequin mannequin;

    /**
     * Whether this ride's own effects - the boarding thump, the shot, the trail - are the rider's alone. Captured from
     * the cannon at boarding rather than read back off it, since the cannon can be gone long before the rider lands.
     */
    private final boolean privateRide;

    /**
     * How long this rider's fuse burns on a {@link #privateRide}. Captured at boarding for the same reason as
     * {@link #privateRide}, and because a cannon reloaded from a changed config mid-flight must not move the deadline
     * of a countdown already running.
     */
    private final long fuseMillis;

    /** The cannon that launched this ride. Null once it has been destroyed or unloaded mid-flight. */
    @Setter private @Nullable CannonProp cannon;

    @Setter private @NotNull RidePhase phase = RidePhase.BOARDING;
    @Setter private long phaseSince = System.currentTimeMillis();

    /** Per-tick velocity while {@link RidePhase#FLYING}. */
    @Setter private @NotNull Vector velocity = new Vector();

    /** Where the rider aimed. Previewed during targeting, then solved for at launch. */
    @Setter private @Nullable Location target;

    public CannonRide(@NotNull UUID rider, @NotNull RideOrigin origin, @NotNull RideMannequin mannequin,
                      @NotNull CannonProp cannon) {
        this.rider = rider;
        this.origin = origin;
        this.mannequin = mannequin;
        this.cannon = cannon;
        this.privateRide = cannon.getProperties().isPrivateOperation();
        this.fuseMillis = cannon.fuseMillis();
    }

    public void enter(@NotNull RidePhase next) {
        this.phase = next;
        this.phaseSince = System.currentTimeMillis();
    }

    public long millisInPhase() {
        return System.currentTimeMillis() - phaseSince;
    }
}
