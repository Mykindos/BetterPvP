package me.mykindos.betterpvp.core.world.construction;

/** The state of the building itself, whatever job may be running on it. */
public enum StructureCondition {
    /** Not finished for the first time yet. */
    UNDER_CONSTRUCTION,
    ACTIVE,
    /** Knocked out, and needs a repair. */
    DISABLED,
    /** Broken from the start, and needs a repair. */
    NEEDS_REPAIR,
    /** Owned, but not standing anywhere in the world. */
    NOT_PLACED;

    /** Whether the building stands in the world, having been finished for the first time. */
    public boolean isStanding() {
        return this != UNDER_CONSTRUCTION && this != NOT_PLACED;
    }
}
