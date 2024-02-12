package me.mykindos.betterpvp.core.utilities.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a wrapper for the reason for an entity being removed
 */
@Getter
@AllArgsConstructor
public enum EntityRemovalReason {

    KILLED(true, false),

    DISCARDED(true, false),

    UNLOADED_TO_CHUNK(false, true),

    UNLOADED_TO_PLAYER(false, false),

    DIMENSION_CHANGED(false, false);

    private final boolean isDestroy;
    private final boolean isSave;

}
