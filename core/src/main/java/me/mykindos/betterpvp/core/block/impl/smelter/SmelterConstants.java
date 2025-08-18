package me.mykindos.betterpvp.core.block.impl.smelter;

/**
 * Constants for Smelter operations, timing, and behavior
 */
public final class SmelterConstants {

    // Timing constants
    public static final long BURN_TIME_DECREASE_PER_TICK = 50L; // Milliseconds decreased per tick
    public static final float TEMPERATURE_DECREASE_PER_TICK = 1.0f; // Degrees decreased per tick when not burning
    public static final float TEMPERATURE_INCREASE_PER_TICK = 2.0f; // Degrees increased per tick when burning

    // Sound and particle timing
    public static final int SOUND_INTERVAL_TICKS = 20; // How often to play sounds (in ticks)
    public static final double LAVA_AMBIENT_SOUND_CHANCE = 0.4; // Chance to play lava ambient sound
    public static final double LAVA_POP_SOUND_CHANCE = 0.4; // Chance to play lava pop sound
    public static final int PARTICLE_INTERVAL_TICKS = 5; // How often to spawn particles (in ticks)

    // Sound parameters
    public static final float LAVA_AMBIENT_VOLUME = 0.5f;
    public static final float LAVA_AMBIENT_PITCH = 0.3f;
    public static final float LAVA_POP_VOLUME = 0.4f;
    public static final float LAVA_POP_PITCH = 0.5f;
    public static final float FUEL_CONSUME_VOLUME = 0.8f;
    public static final float FUEL_CONSUME_PITCH = 1.2f;
    public static final float CASTING_VOLUME = 0.8f;
    public static final float CASTING_PITCH = 1.5f;

    // Particle parameters
    public static final int SMOKE_PARTICLE_COUNT = 3;
    public static final double SMOKE_HEIGHT_OFFSET = 4.5;
    public static final double SMOKE_Y_OFFSET = 0.5;
    public static final int PARTICLE_RECEIVER_DISTANCE = 60;

    // Storage constants
    public static final int DEFAULT_CONTENT_SLOTS = 10;
    public static final int DEFAULT_RESULT_SLOTS = 1;
    public static final int DEFAULT_FUEL_SLOTS = 1;

    private SmelterConstants() {
        // Utility class
    }
} 