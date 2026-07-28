package me.mykindos.betterpvp.core.item.impl.cannon.ride;

/**
 * Where a rider is in a human-cannonball flight. Distinct from
 * {@link me.mykindos.betterpvp.core.item.impl.cannon.model.CannonState} because the ride outlives the cannon's cycle:
 * the cannon is already cooling down while the rider is still in the air, and may be destroyed before they land.
 */
public enum RidePhase {

    /** Aboard with the camera settled on the cannon and nothing of the ride's own running - most of it spent waiting
     * on a shared emplacement's fuse, once a destination has been chosen. */
    BOARDING,

    /**
     * The rider's own fuse is burning towards the destination they picked. Private cannons only: they run one sequence
     * per rider rather than one for the emplacement, so the countdown belongs to the ride. On a shared cannon the cycle
     * owns it and the ride waits the fuse out in {@link #BOARDING}.
     */
    FUSING,

    /** In the air. */
    FLYING,

    /** Landed or aborted; the service cleans the ride up on the next tick. */
    FINISHED
}
