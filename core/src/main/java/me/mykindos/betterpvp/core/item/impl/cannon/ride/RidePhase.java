package me.mykindos.betterpvp.core.item.impl.cannon.ride;

/**
 * Where a rider is in a human-cannonball flight. Distinct from
 * {@link me.mykindos.betterpvp.core.item.impl.cannon.model.CannonState} because the ride outlives the cannon's cycle:
 * the cannon is already cooling down while the rider is still in the air, and may be destroyed before they land.
 */
public enum RidePhase {

    /** Aboard, camera settled on the cannon, waiting on the emplacement's own fuse. */
    BOARDING,

    /**
     * The rider's own fuse is burning. Private cannons only: they run one sequence per rider rather than one for the
     * emplacement, so the countdown belongs to the ride. On a shared cannon the cycle owns it and the ride waits out
     * the fuse in {@link #BOARDING}.
     */
    FUSING,

    /** Fuse spent; the rider is choosing where to land. */
    TARGETING,

    /** In the air. */
    FLYING,

    /** Landed or aborted; the service cleans the ride up on the next tick. */
    FINISHED
}
