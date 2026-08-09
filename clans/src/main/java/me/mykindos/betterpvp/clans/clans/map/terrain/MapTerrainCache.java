package me.mykindos.betterpvp.clans.clans.map.terrain;

import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.map.MapColorBlender;
import me.mykindos.betterpvp.clans.clans.map.data.MapFill;
import me.mykindos.betterpvp.clans.clans.map.data.MapFillStyle;
import me.mykindos.betterpvp.clans.clans.map.data.MapZoneEdges;
import me.mykindos.betterpvp.clans.clans.map.data.MapZoneStyle;
import net.minecraft.world.level.material.MapColor;

/**
 * The terrain layer of one world, held as flat primitive arrays over the bounded {@code radius} box. A column is
 * addressed by index arithmetic, so reading one costs no hashing, no boxing and no pointer chasing.
 * <p>
 * On top of the raw columns sit {@linkplain #mipAt mipmaps} — one per zoom level — holding the <em>finished</em>
 * packed map byte for every sampled column, with height shading and the zone tint already baked in. Drawing at any
 * zoom is then a copy out of the matching mip: no colour lookup, no brightness maths, no zone resolution, no blending.
 * That is what makes switching zoom free rather than merely fast.
 * <p>
 * Not internally synchronised. The owning {@link MapTerrainService} confines writes to its rebuild worker and treats
 * the arrays as safely-published, eventually-consistent reads elsewhere: a torn read costs at worst one stale pixel.
 */
public class MapTerrainCache {

    /** Zoom levels are powers of two from 1 to 16, so a scale is addressed by its exponent. */
    public static final int LEVELS = 5;

    /** {@link MapColor#NONE}'s id, which doubles as "this column has never been sampled". */
    private static final byte NO_DATA = 0;

    private static final byte STYLE_UNRESOLVED = -1;
    private static final byte STYLE_NONE = 0;

    /** How far a {@link MapFillStyle#CORNERS} bracket reaches along the outline from the corner it meets at. */
    private static final int CORNER_REACH = 2;

    @Getter
    private final String worldName;
    @Getter
    private final int radius;
    private final int width;

    private final byte[] colorId;
    private final short[] height;
    /** Resolved zone style per column: {@code -1} unresolved, {@code 0} none, otherwise style index + 1. */
    private final byte[] zoneStyle;

    private final byte[][] mips = new byte[LEVELS][];
    private final int[] mipRadius = new int[LEVELS];
    private final int[] mipWidth = new int[LEVELS];

    public MapTerrainCache(String worldName, int radius) {
        this.worldName = worldName;
        this.radius = radius;
        this.width = radius * 2 + 1;

        final int columns = width * width;
        this.colorId = new byte[columns];
        this.height = new short[columns];
        this.zoneStyle = new byte[columns];
        java.util.Arrays.fill(zoneStyle, STYLE_UNRESOLVED);

        for (int level = 0; level < LEVELS; level++) {
            mipRadius[level] = radius >> level;
            mipWidth[level] = mipRadius[level] * 2 + 1;
            mips[level] = new byte[mipWidth[level] * mipWidth[level]];
        }
    }

    // <editor-fold desc="Column storage">

    public boolean inBounds(int x, int z) {
        return x >= -radius && x <= radius && z >= -radius && z <= radius;
    }

    private int index(int x, int z) {
        return (x + radius) * width + (z + radius);
    }

    /**
     * @return whether this column has ever been sampled
     */
    public boolean hasColumn(int x, int z) {
        return inBounds(x, z) && colorId[index(x, z)] != NO_DATA;
    }

    /**
     * Records a sampled column and invalidates its cached zone style so the tint re-resolves on the next mip build.
     */
    public void setColumn(int x, int z, int color, short y) {
        if (!inBounds(x, z)) {
            return;
        }
        final int idx = index(x, z);
        colorId[idx] = (byte) color;
        height[idx] = y;
        zoneStyle[idx] = STYLE_UNRESOLVED;
    }

    // </editor-fold>

    // <editor-fold desc="Mipmaps">

    /**
     * @param level zoom exponent
     * @param gx    grid x (world x divided by the level's scale)
     * @param gz    grid z
     * @return the finished packed map colour, or {@code 0} when that column has no terrain data
     */
    public byte mipAt(int level, int gx, int gz) {
        final int r = mipRadius[level];
        if (gx < -r || gx > r || gz < -r || gz > r) {
            return NO_DATA;
        }
        return mips[level][(gx + r) * mipWidth[level] + (gz + r)];
    }

    /**
     * Rebuilds every zoom level across the whole world. Pure array work plus zone containment tests, so it runs off
     * the main thread.
     */
    public void rebuildAllMips(ZoneStyleSnapshot zones, MapColorBlender blender) {
        for (int level = 0; level < LEVELS; level++) {
            final int r = mipRadius[level];
            for (int gx = -r; gx <= r; gx++) {
                for (int gz = -r; gz <= r; gz++) {
                    writeMip(level, gx, gz, zones, blender);
                }
            }
        }
    }

    /**
     * Refreshes the mip entries a single changed column feeds into: its own cell at every zoom level, plus the
     * neighbours whose height shading is derived from it.
     */
    public void refreshColumn(int x, int z, ZoneStyleSnapshot zones, MapColorBlender blender) {
        for (int level = 0; level < LEVELS; level++) {
            final int gx = x >> level;
            final int gz = z >> level;
            writeMip(level, gx, gz, zones, blender);
            // Shading of a cell reads the column up-left of it, so the cells down-right of this one also change.
            writeMip(level, gx + 1, gz + 1, zones, blender);
            writeMip(level, gx + 1, gz - 1, zones, blender);
            writeMip(level, gx - 1, gz - 1, zones, blender);
        }
    }

    /**
     * Refreshes every mip cell overlapping a block area, plus a one-cell margin for the neighbours whose shading reads
     * into it. Used after a chunk is sampled, so a newly-seen chunk costs one bounded pass rather than a world rebuild.
     */
    public void refreshArea(int minX, int minZ, int maxX, int maxZ, ZoneStyleSnapshot zones, MapColorBlender blender) {
        for (int level = 0; level < LEVELS; level++) {
            final int minGx = (minX >> level) - 1;
            final int maxGx = (maxX >> level) + 1;
            final int minGz = (minZ >> level) - 1;
            final int maxGz = (maxZ >> level) + 1;
            for (int gx = minGx; gx <= maxGx; gx++) {
                for (int gz = minGz; gz <= maxGz; gz++) {
                    writeMip(level, gx, gz, zones, blender);
                }
            }
        }
    }

    private void writeMip(int level, int gx, int gz, ZoneStyleSnapshot zones, MapColorBlender blender) {
        final int r = mipRadius[level];
        if (gx < -r || gx > r || gz < -r || gz > r) {
            return;
        }
        mips[level][(gx + r) * mipWidth[level] + (gz + r)] = compute(level, gx, gz, zones, blender);
    }

    private byte compute(int level, int gx, int gz, ZoneStyleSnapshot zones, MapColorBlender blender) {
        final int scale = 1 << level;
        final int x = gx << level;
        final int z = gz << level;
        if (!hasColumn(x, z)) {
            return NO_DATA;
        }

        final int idx = index(x, z);
        final short currentY = height[idx];
        final double difference = (currentY - previousHeight(x, z, scale)) * 4.0D / (scale + 4)
                // Dither anchored to the sample grid, so the checker pattern stays put as the viewer moves.
                + (((gx + gz) & 1) - 0.5D) * 0.4D;

        final MapColor.Brightness brightness;
        if (difference > 0.6D) {
            brightness = MapColor.Brightness.HIGH;
        } else if (difference < -0.6D) {
            brightness = MapColor.Brightness.LOW;
        } else {
            brightness = MapColor.Brightness.NORMAL;
        }

        byte packed = MapColor.byId(colorId[idx] & 0xFF).getPackedId(brightness);
        return tint(level, gx, gz, packed, zones, blender);
    }

    /**
     * Mirrors the original neighbour probe: up-left, then down-left, then down-right, falling back to sea level 0.
     */
    private short previousHeight(int x, int z, int scale) {
        if (hasColumn(x - scale, z - scale)) {
            return height[index(x - scale, z - scale)];
        }
        if (hasColumn(x - scale, z + scale)) {
            return height[index(x - scale, z + scale)];
        }
        if (hasColumn(x + scale, z + scale)) {
            return height[index(x + scale, z + scale)];
        }
        return 0;
    }

    private byte tint(int level, int gx, int gz, byte packed, ZoneStyleSnapshot zones, MapColorBlender blender) {
        final byte style = styleAt(gx << level, gz << level, zones);
        if (style == STYLE_NONE) {
            return packed;
        }

        final MapZoneStyle resolved = zones.style(style);
        if (resolved == null) {
            return packed;
        }

        final MapFill fill = resolved.getFill();
        // Grid coordinates, not world ones: a cell is one pixel whatever the zoom, and the probe is one cell wide, so
        // patterns keep their period and outlines keep their width as the map zooms out.
        if (!fill.paintsPixel(gx, gz, fill.needsEdges() ? edges(style, level, gx, gz, zones) : MapZoneEdges.NONE)) {
            return packed;
        }
        return blender.blend(packed, resolved);
    }

    /**
     * What the neighbourhood of one cell says about the zone's outline. The corner search only runs for cells already
     * on the outline, so the interior of a zone costs four lookups.
     */
    private MapZoneEdges edges(byte style, int level, int gx, int gz, ZoneStyleSnapshot zones) {
        final MapZoneEdges edges = exposure(style, level, gx, gz, zones);
        if (!edges.exposed()) {
            return edges;
        }
        return edges.withNearCorner(edges.corner() || nearCorner(style, level, gx, gz, zones));
    }

    /**
     * Probes the four neighbouring cells against the style's own footprint, not against whatever wins the tint there —
     * see {@link ZoneStyleSnapshot#covers}. A neighbour off the edge of the mapped box counts as exposed, so a zone
     * running past the map's limit is still drawn as bounded rather than bleeding away.
     */
    private MapZoneEdges exposure(byte style, int level, int gx, int gz, ZoneStyleSnapshot zones) {
        final int scale = 1 << level;
        final int x = gx << level;
        final int z = gz << level;
        if (!covers(style, x, z, zones)) {
            return MapZoneEdges.NONE;
        }
        return MapZoneEdges.of(!covers(style, x - scale, z, zones),
                !covers(style, x + scale, z, zones),
                !covers(style, x, z - scale, zones),
                !covers(style, x, z + scale, zones));
    }

    private boolean covers(byte style, int x, int z, ZoneStyleSnapshot zones) {
        return inBounds(x, z) && zones.covers(style, x, height[index(x, z)], z);
    }

    /**
     * Whether a corner lies within {@link #CORNER_REACH} cells along the outline. Searched down the axes only, which is
     * all the outline runs along, so a bracket is the corner plus the arms leading into it.
     */
    private boolean nearCorner(byte style, int level, int gx, int gz, ZoneStyleSnapshot zones) {
        for (int step = 1; step <= CORNER_REACH; step++) {
            if (exposure(style, level, gx - step, gz, zones).corner()
                    || exposure(style, level, gx + step, gz, zones).corner()
                    || exposure(style, level, gx, gz - step, zones).corner()
                    || exposure(style, level, gx, gz + step, zones).corner()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The zone style covering a column, resolved once and cached. Columns outside the mapped box report
     * {@link #STYLE_NONE}, so a zone running off the edge of the map still gets an outline drawn along it.
     */
    private byte styleAt(int x, int z, ZoneStyleSnapshot zones) {
        if (!inBounds(x, z)) {
            return STYLE_NONE;
        }
        final int idx = index(x, z);
        byte style = zoneStyle[idx];
        if (style == STYLE_UNRESOLVED) {
            style = zones.resolve(x, height[idx], z);
            zoneStyle[idx] = style;
        }
        return style;
    }

    /** Drops every cached zone resolution, e.g. after the style registry or the zone set changed. */
    public void invalidateZoneStyles() {
        java.util.Arrays.fill(zoneStyle, STYLE_UNRESOLVED);
    }

    // </editor-fold>

    // <editor-fold desc="Persistence">

    /**
     * @return the raw colour bytes, one per column, for persistence. Live array — callers must not retain it.
     */
    public byte[] colorArray() {
        return colorId;
    }

    /**
     * @return the raw heights, one per column, for persistence. Live array — callers must not retain it.
     */
    public short[] heightArray() {
        return height;
    }

    public int columnCount() {
        return colorId.length;
    }

    // </editor-fold>
}
