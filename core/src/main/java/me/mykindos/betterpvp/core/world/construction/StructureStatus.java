package me.mykindos.betterpvp.core.world.construction;

/**
 * What a player sees a structure as doing, worked out from its condition and whatever job is running on it. See
 * {@link PlacedStructure#status(long)}.
 */
public enum StructureStatus {
    /** Rising layer by layer. Not usable. */
    UNDER_CONSTRUCTION,
    /** Its job is done and it waits for someone to claim it. */
    READY_TO_CLAIM,
    ACTIVE,
    /** Still standing but not usable until the upgrade is claimed. */
    UPGRADING,
    /** Its job is held and waiting. */
    PAUSED,
    DISABLED,
    NEEDS_REPAIR,
    NOT_PLACED;

    /** Whether the structure's own features work. Storage stays reachable regardless. */
    public boolean isUsable() {
        return this == ACTIVE;
    }
}
