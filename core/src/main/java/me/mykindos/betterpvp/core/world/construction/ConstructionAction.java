package me.mykindos.betterpvp.core.world.construction;

/** Something a player can do to a holding's structures, each checked against the owner's permissions. */
public enum ConstructionAction {
    BUILD,
    CANCEL,
    MOVE,
    UPGRADE,
    CLAIM,
    DEMOLISH,
    REPAIR
}
