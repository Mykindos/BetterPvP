package me.mykindos.betterpvp.core.item.impl.cannon.model;

/**
 * Where a cannon is in its load-fire-recover cycle. {@link #TARGETING} applies only to the passenger firing mode,
 * whose sequence begins before the fuse rather than at it.
 */
public enum CannonState {

    /** Nothing chambered. The cannon accepts a round. */
    IDLE,

    /** A round (or a rider) is chambered and the cannon is ready to fuse. */
    LOADED,

    /** The fuse is burning. It will fire on its own when the fuse runs out. */
    FUSING,

    /** A rider is aboard and choosing where to land. Nothing is lit yet. Passenger mode only. */
    TARGETING,

    /** Recovering from a shot; cannot be loaded or fired. */
    COOLDOWN;

    /** Whether the cannon currently holds something it could fire. */
    public boolean isCharged() {
        return this != IDLE && this != COOLDOWN;
    }

    /** Whether the cannon is mid-sequence and should ignore aim and load input. */
    public boolean isBusy() {
        return this == FUSING || this == TARGETING;
    }
}
