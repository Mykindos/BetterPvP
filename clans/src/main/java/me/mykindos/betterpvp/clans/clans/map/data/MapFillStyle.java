package me.mykindos.betterpvp.clans.clans.map.data;

/**
 * One way of painting a zone or a clan claim onto the map. Styles combine — see {@link MapFill}, which is what config
 * and renderers actually hold — so this enum only has to answer for a single pattern at a time.
 */
public enum MapFillStyle {

    /** Every pixel of the region. */
    FILL,
    /** Only the exposed outline of the region. */
    BORDER,
    /** Diagonal hatching, one pixel in four. */
    DIAGONAL,
    /** Alternating checkerboard pixels. */
    CHECKER,
    /** Short brackets of outline at the region's corners. */
    CORNERS,
    /** A sparse dot grid. */
    DOTS;

    /**
     * Whether this style paints a given pixel.
     * <p>
     * Coordinates are the map's sample grid — one step per drawn pixel — not world blocks. A pattern measured in blocks
     * disappears the moment the map zooms out, because every drawn pixel then lands on a multiple of the pattern's
     * period and the style degrades into a solid fill.
     *
     * @param gridX the pixel's x on the sample grid
     * @param gridZ the pixel's z on the sample grid
     * @param edges which of the pixel's sides face something outside the zone; {@link MapZoneEdges#NONE} is enough for
     *              the styles {@link #needsEdges()} rejects
     * @return whether the pixel should be painted
     */
    public boolean paintsPixel(int gridX, int gridZ, MapZoneEdges edges) {
        return switch (this) {
            case FILL -> true;
            case BORDER -> edges.exposed();
            // The turning pixel alone is a single dot on a whole region; the bracket meeting there is what reads.
            case CORNERS -> edges.isNearCorner();
            case CHECKER -> ((gridX + gridZ) & 1) == 0;
            case DIAGONAL -> Math.floorMod(gridX + gridZ, 4) == 0;
            case DOTS -> Math.floorMod(gridX, 3) == 0 && Math.floorMod(gridZ, 3) == 0;
        };
    }

    /**
     * @return whether this style's answer depends on its neighbourhood, so a caller can skip probing for the positional
     * patterns that ignore it
     */
    public boolean needsEdges() {
        return this == BORDER || this == CORNERS;
    }
}
