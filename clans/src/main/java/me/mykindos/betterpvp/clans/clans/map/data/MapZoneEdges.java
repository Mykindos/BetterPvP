package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.Value;

/**
 * What one map pixel's neighbourhood says about it: which of its sides face something that is not the same zone, and
 * whether it sits on a bracket running up to a corner. This is the context an outline style needs and a positional
 * pattern ignores, supplied by whoever holds the resolved zone of each pixel so {@link MapFillStyle#paintsPixel} can
 * decide every style in one place.
 */
@Value
public class MapZoneEdges {

    /** Nothing exposed, for styles that never ask. */
    public static final MapZoneEdges NONE = new MapZoneEdges(false, false, false, false, false);

    boolean west;
    boolean east;
    boolean north;
    boolean south;
    /** Whether this pixel is on the short run of outline that meets at a corner. */
    boolean nearCorner;

    /**
     * @return the exposure of a pixel, before any corner has been looked for
     */
    public static MapZoneEdges of(boolean west, boolean east, boolean north, boolean south) {
        return new MapZoneEdges(west, east, north, south, false);
    }

    /**
     * @param value whether the pixel is within a corner's bracket
     * @return a copy carrying that answer
     */
    public MapZoneEdges withNearCorner(boolean value) {
        return new MapZoneEdges(west, east, north, south, value);
    }

    /**
     * @return whether any side is exposed, i.e. the pixel lies on the zone's outline
     */
    public boolean exposed() {
        return west || east || north || south;
    }

    /**
     * @return whether the outline turns here: exposed along one axis and the other, which is where two runs of outline
     * meet
     */
    public boolean corner() {
        return (west || east) && (north || south);
    }
}
