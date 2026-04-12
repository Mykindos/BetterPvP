package me.mykindos.betterpvp.core.npc.behavior;

/**
 * Defines how a {@link WaypointPatrolBehavior} cycles through its waypoint sequence
 * once the last point is reached.
 */
public enum PatrolMode {

    /**
     * Loops back to the first waypoint after the last.
     * A → B → C → A → B → …
     */
    CIRCULAR,

    /**
     * Reverses direction at each end of the sequence.
     * A → B → C → B → A → B → …
     */
    BACKTRACK

}
