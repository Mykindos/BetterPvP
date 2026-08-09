package me.mykindos.betterpvp.clans.clans.map.data;

import java.util.Locale;

/**
 * How the map fills pixels that have no terrain data (beyond the mapped area, or over chunks not yet loaded).
 */
public enum MapBackground {

    /** A single flat colour ({@code clans.map.unknownColor}). */
    SOLID,
    /** Each empty pixel takes the colour of the nearest drawn pixel, so terrain bleeds outward into the edges. */
    CASCADE;

    /**
     * @param name     a config value (case-insensitive), possibly null
     * @param fallback the mode to use when {@code name} is null or unrecognised
     * @return the matching mode
     */
    public static MapBackground parse(String name, MapBackground fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
