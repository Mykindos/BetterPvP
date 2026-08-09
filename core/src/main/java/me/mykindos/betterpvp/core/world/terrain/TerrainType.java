package me.mykindos.betterpvp.core.world.terrain;

/**
 * How one surface column of a world is classified by a terrain scan, the unit a {@link TerrainMask} stores per column.
 * <p>
 * {@link #INTERIOR} is the implicit default — the claimable, unrestricted land inside an island — and is stored as the
 * absence of any other value, so a mask only pays for the columns it actually classifies. The remaining types map onto
 * capability zones: an island's central massif ({@link #MOUNTAIN}), its shoreline ring ({@link #BEACH}), and the open
 * water around it ({@link #OCEAN}).
 * <p>
 * Ordinals are persisted as single bytes, so the order is part of the on-disk format — append new types, never reorder.
 */
public enum TerrainType {

    /** Unrestricted, claimable land. The default when a column carries no classification. */
    INTERIOR,
    /** Open water connected to the world edge; the sea surrounding an island. */
    OCEAN,
    /** The shoreline band between the ocean and the interior. */
    BEACH,
    /** The contiguous raised massif grown from a peak seed. */
    MOUNTAIN;

    private static final TerrainType[] VALUES = values();

    /**
     * @param id a persisted ordinal byte
     * @return the matching type, or {@link #INTERIOR} for any unknown or out-of-range id
     */
    public static TerrainType byId(byte id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : INTERIOR;
    }
}
