package me.mykindos.betterpvp.core.interaction.timing;

import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.supplier.ItemAttackSpeedSupplier;
import me.mykindos.betterpvp.core.item.config.ConfigEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.function.ToLongFunction;

/**
 * Represents a timing value (timeout, delay, etc.) that can be fixed or context-dependent.
 * <p>
 * Use the static factory methods for common cases:
 * <ul>
 *   <li>{@link #millis(long)} - fixed milliseconds</li>
 *   <li>{@link #seconds(double)} - fixed seconds (converted to millis)</li>
 *   <li>{@link #fromConfig(ConfigEntry)} - reads from config at runtime</li>
 *   <li>{@link #dynamic(ToLongFunction)} - fully dynamic based on context</li>
 *   <li>{@link #ZERO} - no delay/timeout</li>
 * </ul>
 */
public record Timing(
    @NotNull ToLongFunction<InteractionContext> supplier,
    @Nullable String debugDescription
) {

    /** Zero timing - no delay. */
    public static final Timing ZERO = new Timing(ctx -> 0L, "0ms");

    /**
     * Create a fixed timing in milliseconds.
     */
    public static Timing millis(long ms) {
        if (ms == 0) return ZERO;
        return new Timing(ctx -> ms, ms + "ms");
    }

    /**
     * Create a fixed timing in seconds.
     */
    public static Timing seconds(double seconds) {
        if (seconds == 0) return ZERO;
        long ms = (long) (seconds * 1000);
        return new Timing(ctx -> ms, seconds + "s");
    }

    /**
     * Create a timing from a Duration.
     */
    public static Timing of(@NotNull Duration duration) {
        if (duration.isZero()) return ZERO;
        long ms = duration.toMillis();
        return new Timing(ctx -> ms, duration.toString());
    }

    /**
     * Create a timing that reads from a config entry (in seconds).
     * The ConfigEntry reads from config on each get() call.
     */
    public static Timing fromConfig(@NotNull ConfigEntry<Double> configSeconds) {
        return new Timing(
            ctx -> (long) (configSeconds.get() * 1000),
            "config:" + configSeconds.getKey()
        );
    }

    /**
     * Create a timing that reads from a config entry (in milliseconds).
     */
    public static Timing fromConfigMillis(@NotNull ConfigEntry<Long> configMillis) {
        return new Timing(
            ctx -> configMillis.get(),
            "config:" + configMillis.getKey()
        );
    }

    /**
     * Create a fully dynamic timing that computes based on context.
     * Use this for complex cases like ItemAttackSpeedSupplier.
     */
    public static Timing dynamic(@NotNull ToLongFunction<InteractionContext> supplier) {
        return new Timing(supplier, "dynamic");
    }

    /**
     * Create a timing based on the item's attack speed.
     * Falls back to the given value if attack speed cannot be determined.
     */
    public static Timing fromAttackSpeed(long fallbackMillis) {
        return new Timing(
            new ItemAttackSpeedSupplier(fallbackMillis),
            "attackSpeed(fallback=" + fallbackMillis + "ms)"
        );
    }

    /**
     * Create a timing based on the item's attack speed with config fallback.
     */
    public static Timing fromAttackSpeed(@NotNull ConfigEntry<Long> fallbackConfig) {
        return new Timing(
            new ItemAttackSpeedSupplier(fallbackConfig),
            "attackSpeed(fallback=config:" + fallbackConfig.getKey() + ")"
        );
    }

    /**
     * Get the timing value in milliseconds.
     *
     * @param context the interaction context (may be null for fixed timings)
     * @return the timing in milliseconds
     */
    public long getMillis(@Nullable InteractionContext context) {
        return supplier.applyAsLong(context);
    }

    /**
     * Check if this is a zero timing.
     */
    public boolean isZero() {
        return this == ZERO;
    }

    /**
     * Check if this timing is zero or evaluates to zero for a null context.
     * Useful for checking fixed timings without a context.
     */
    public boolean isEffectivelyZero() {
        return isZero() || getMillis(null) == 0;
    }

    @Override
    public String toString() {
        return "Timing[" + debugDescription + "]";
    }
}
