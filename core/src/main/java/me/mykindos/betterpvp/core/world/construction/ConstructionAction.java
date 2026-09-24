package me.mykindos.betterpvp.core.world.construction;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Something a player can do to a holding's structures, each checked against the owner's permissions. */
public enum ConstructionAction {
    BUILD,
    CANCEL,
    MOVE,
    @JsonAlias("UPGRADE")
    ADVANCE,
    CLAIM,
    DEMOLISH,
    REPAIR,
    PICK_UPGRADE
}
