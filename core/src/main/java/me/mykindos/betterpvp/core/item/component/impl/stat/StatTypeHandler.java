package me.mykindos.betterpvp.core.item.component.impl.stat;

import net.kyori.adventure.text.format.TextColor;

/**
 * Handles type-specific operations for stat values.
 * This interface abstracts away all type-specific logic (arithmetic, conversion, formatting)
 * to eliminate instanceof checks throughout the stat system.
 *
 * @param <T> The data type for stat values (Integer, Double, etc.)
 */
public interface StatTypeHandler<T> {

    /**
     * Gets the zero value for this type.
     *
     * @return The zero value (0 for Integer, 0.0 for Double)
     */
    T getZero();

    /**
     * Gets the one value for this type.
     *
     * @return The one value (1 for Integer, 1.0 for Double)
     */
    T getOne();

    /**
     * Adds two values of this type.
     *
     * @param a First value
     * @param b Second value
     * @return The sum
     */
    T add(T a, T b);

    /**
     * Subtracts one value from another.
     *
     * @param a Value to subtract from
     * @param b Value to subtract
     * @return The difference
     */
    T subtract(T a, T b);

    /**
     * Multiplies a value by a scalar.
     *
     * @param value The value to multiply
     * @param scalar The scalar multiplier
     * @return The product
     */
    T multiply(T value, double scalar);

    /**
     * Converts a double value to this type.
     * Used for converting stored double values (e.g., in reforges) to the appropriate type.
     *
     * @param value The double value to convert
     * @return The converted value
     */
    T fromDouble(double value);

    /**
     * Formats a value as a string with appropriate sign (+/-) prefix.
     *
     * @param value The value to format
     * @param isPercentage Whether this value represents a percentage
     * @return The formatted string
     */
    String formatValue(T value, boolean isPercentage);

    /**
     * Gets the default color for a value (green for positive, red for negative).
     *
     * @param value The value to get color for
     * @return The text color
     */
    TextColor getDefaultColor(T value);

    /**
     * Returns the smaller of two values.
     *
     * @param a First value
     * @param b Second value
     * @return The smaller value
     */
    T min(T a, T b);

    /**
     * Generates a random value between min (inclusive) and max (inclusive).
     *
     * @param min The minimum value
     * @param max The maximum value
     * @return A random value between min and max
     */
    T randomBetween(T min, T max);

    /**
     * Generates a biased random value between min (inclusive) and max (inclusive).
     * The bias parameter (0.0 to 1.0) determines where in the range the value will fall:
     * - 0.0 returns min
     * - 0.5 returns middle of range
     * - 1.0 returns max
     * <p>
     * This method is used with beta distribution output to create purity-based
     * stat randomization during reforging. The bias ratio is pre-calculated from
     * beta distribution and passed to this method for linear interpolation.
     *
     * @param min  The minimum value
     * @param max  The maximum value
     * @param bias A value from 0.0 to 1.0 representing the biased position in range
     * @return A biased value between min and max
     */
    T randomBetweenBiased(T min, T max, double bias);
}
