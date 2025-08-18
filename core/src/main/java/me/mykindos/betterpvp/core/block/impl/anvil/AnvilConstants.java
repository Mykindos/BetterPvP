package me.mykindos.betterpvp.core.block.impl.anvil;

/**
 * Constants for Anvil animations, timing, and behavior
 */
public final class AnvilConstants {

    // Timing constants
    public static final long SWING_COOLDOWN_MS = 1000L; // 1 second universal cooldown

    // Display positioning constants
    public static final double DISPLAY_HEIGHT_OFFSET = 1.005; // Height above anvil to show items
    public static final double STACK_HEIGHT_OFFSET = 0.03; // Height between stacked items
    public static final double TEXT_DISPLAY_HEIGHT_OFFSET = 1.5; // Height above anvil for text display

    // Item display constants
    public static final int MAX_STACK_DISPLAY_COUNT = 3; // Maximum visual stack count for stackable items
    public static final float BASE_ITEM_SCALE = 0.7f; // Base scale for displayed items
    public static final float SCALE_REDUCTION_PER_STACK = 0.01f; // Scale reduction per stacked item

    // Gameplay constants
    public static final int MAX_ANVIL_ITEMS = 10; // Maximum items on anvil

    private AnvilConstants() {
        // Utility class
    }
} 