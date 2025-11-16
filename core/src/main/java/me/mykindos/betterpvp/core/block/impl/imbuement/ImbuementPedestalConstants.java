package me.mykindos.betterpvp.core.block.impl.imbuement;

/**
 * Constants for ImbuementPedestal animations and behavior
 */
public final class ImbuementPedestalConstants {

    // Flying item animation constants
    public static final double FLYING_HEIGHT_MIN = 1.1;
    public static final double FLYING_HEIGHT_MAX = 1.8;
    public static final double FLYING_RADIUS_MIN = 0.8;
    public static final double FLYING_RADIUS_MAX = 1.2;
    public static final double HELIX_AMPLITUDE = 0.3;
    public static final double RADIUS_FLUCTUATION_AMPLITUDE = 0.2;
    public static final double ITEM_ROTATION_SPEED = 0.06;

    // Display constants
    public static final double TEXT_DISPLAY_HEIGHT_OFFSET = 2.2;
    public static final double GROUPING_HEIGHT = 2.0;

    // Recipe execution timing
    public static final long GROUPING_DURATION_MS = 1000L;
    public static final long EXPANSION_DURATION_MS = 2000L;
    public static final float EXPANSION_SCALE = 1.2f;

    // Gameplay constants
    public static final double FAILURE_CHANCE = 0.2;
    public static final int MAX_PEDESTAL_ITEMS = 10;

    private ImbuementPedestalConstants() {
        // Utility class
    }
} 