package me.mykindos.betterpvp.core.item.impl.cannon.model;

/**
 * Where a cannon is in its load-fire-recover cycle. {@link #BOARDING} and {@link #TARGETING} apply only to the
 * passenger firing mode, whose sequence continues past the fuse.
 */
public enum CannonState {

    /** Nothing chambered. The cannon accepts a round. */
    IDLE,

    /** A round (or a rider) is chambered and the cannon is ready to fuse. */
    LOADED,

    /** The fuse is burning. It will fire on its own when the fuse runs out. */
    FUSING,

    /** A rider is aboard and the cannon is preparing to fuse. Passenger mode only. */
    BOARDING,

    /** The fuse has burned out and the rider is choosing where to land. Passenger mode only. */
    TARGETING,

    /** Recovering from a shot; cannot be loaded or fired. */
    COOLDOWN;

    /** Whether the cannon currently holds something it could fire. */
    public boolean isCharged() {
        return this != IDLE && this != COOLDOWN;
    }

    /** Whether the cannon is mid-sequence and should ignore aim and load input. */
    public boolean isBusy() {
        return this == FUSING || this == BOARDING || this == TARGETING;
    }
}
