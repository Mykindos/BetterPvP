package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.Data;

/**
 * One viewer's map state: their zoom, where the map was last centred, and the composed frame waiting to be blitted.
 * <p>
 * The frame is built off the main thread and published here; {@code render()} does nothing but copy it onto the canvas.
 * The generation stamps let the frame builder tell whether terrain or claims moved under a settled viewer without
 * re-deriving anything.
 */
@Data
public class MapSettings {

    /** Pixels per side of a map canvas. */
    public static final int MAP_SIZE = 128;

    private Scale scale = Scale.CLOSEST;
    /** The block position the current frame is centred on. */
    private int mapX;
    private int mapZ;
    private String frameWorld;

    /** The composed 128×128 frame, or {@code null} until the first one is ready. */
    private volatile byte[] frame;
    /** Set when a new frame has been published and not yet drawn. */
    private volatile boolean frameDirty;
    /** Set while a build is in flight, so a slow build is not queued twice. */
    private volatile boolean building;

    /** Terrain and claim generations the current frame was built from. */
    private volatile int terrainGeneration = -1;
    private volatile int claimGeneration = -1;

    /**
     * Forces the next build regardless of movement, and makes the resulting frame skip the render interval so a zoom
     * or a redraw lands on the very next tick.
     */
    private volatile boolean forceRedraw = true;

    private int renderInterval;
    /** Guards against Bukkit firing two interact events for one click, without a time-based lockout. */
    private int lastZoomTick = -1;

    public MapSettings(int mapX, int mapZ) {
        this.mapX = mapX;
        this.mapZ = mapZ;
    }

    /**
     * @param tick the current server tick
     * @return whether a zoom input on this tick should be acted on
     */
    public boolean acceptZoom(int tick) {
        if (lastZoomTick == tick) {
            return false;
        }
        lastZoomTick = tick;
        return true;
    }

    public Scale setScale(Scale scale) {
        this.scale = scale;
        return this.scale;
    }

    public enum Scale {
        CLOSEST(0),
        CLOSE(1),
        NORMAL(2),
        FAR(3),
        FARTHEST(4);

        /** Zoom levels are powers of two, so a scale is stored as its exponent and the value derived from it. */
        private final int exponent;

        Scale(int exponent) {
            this.exponent = exponent;
        }

        public int getExponent() {
            return exponent;
        }

        public int getValue() {
            return 1 << exponent;
        }
    }
}
