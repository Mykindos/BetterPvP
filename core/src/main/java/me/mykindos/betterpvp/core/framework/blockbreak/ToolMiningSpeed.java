package me.mykindos.betterpvp.core.framework.blockbreak;

/**
 * Vanilla tool mining speeds, scaled by {@link #SCALE}.
 * <p>
 * Vanilla speeds (Minecraft Wiki, 1.21):
 * <pre>
 *   Hand     1.0
 *   Wood     2.0    Gold     12.0
 *   Stone    4.0
 *   Iron     6.0
 *   Diamond  8.0
 *   Netherite 9.0
 * </pre>
 * Scaled values are points of reference: a player using a diamond pickaxe
 * (scaled 120) on stone takes the same wall-clock time to break the block as
 * they would in vanilla — the formula in {@code BlockBreakProgressServiceImpl}
 * compensates for the scale factor.
 */
public final class ToolMiningSpeed {

    private ToolMiningSpeed() {}

    /** Multiplier from vanilla speed to framework speed. */
    public static final int SCALE = 15;

    public static final int HAND      = 1  * SCALE;  // 15
    public static final int WOOD      = 2  * SCALE;  // 30
    public static final int STONE     = 4  * SCALE;  // 60
    public static final int IRON      = 6  * SCALE;  // 90
    public static final int DIAMOND   = 8  * SCALE;  // 120
    public static final int NETHERITE = 9  * SCALE;  // 135
    public static final int GOLD      = 12 * SCALE;  // 180

    /**
     * Tick-progress divisor: {@code progressPerTick = scaledSpeed / (VANILLA_TICK_DIVISOR * hardness)}.
     * Derived from vanilla {@code ticks = 30 * hardness / vanillaSpeed}.
     */
    public static final double VANILLA_TICK_DIVISOR = 30.0 * SCALE; // 450
}
