package me.mykindos.betterpvp.core.tracking.model;

/**
 * Describes how active a grid cell is based on its current heat value.
 * Thresholds are configured in {@link me.mykindos.betterpvp.core.tracking.PlayerActivityService}.
 */
public enum ZoneClassification {
    HOTSPOT,
    ACTIVE,
    QUIET,
    EMPTY
}
