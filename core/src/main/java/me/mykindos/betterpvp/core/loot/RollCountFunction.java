package me.mykindos.betterpvp.core.loot;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * Represents a function that determines the number of rolls to be made for a {@link LootBundle}.
 *
 * This functional interface allows for dynamic and flexible roll count generation based on the current {@link LootContext}.
 * It provides several pre-defined strategies for determining roll counts, including constant, progressive,
 * and context-dependent approaches.
 *
 * @see LootContext
 * @see LootBundle
 */
@FunctionalInterface
public interface RollCountFunction extends Function<LootContext, Integer> {

    /**
     * Creates a constant roll count function that always returns the same number of rolls.
     * <p>
     *     Example:
     * <pre>
     * // Always roll 1 time
     * RollCountFunction oneRoll = RollCountFunction.constant(1);
     *
     * // Always roll 3 times
     * RollCountFunction threeRolls = RollCountFunction.constant(3);
     * </pre>
     * @param count The fixed number of rolls to perform
     * @return A RollCountFunction that always returns the specified count
     */
    static RollCountFunction constant(int count) {
        return context -> count;
    }

    /**
     * Creates a progressive roll count function that increases rolls based on the {@link LootProgress} of the given
     * context.
     * <p>
     *     Example:
     * <pre>
     *     // Start with 1 roll, add 1 roll for every 5 failed attempts, max 3 rolls
     *      * RollCountFunction progressiveRolls = RollCountFunction.progressive(1, 1/5, 3);
     * </pre>
     * @param baseRolls The initial number of rolls
     * @param incrementPerProgress The number of additional rolls per {@link LootBundle} obtained in the
     *                              {@link LootProgress} of the given context.
     * @param maxRolls The maximum number of rolls allowed
     * @return A RollCountFunction that increases rolls progressively
     */
    static RollCountFunction progressive(int baseRolls, int incrementPerProgress, int maxRolls) {
        return context -> {
            LootProgress progress = context.getSession().getProgress();
            int additionalRolls = progress.getHistory().size() * incrementPerProgress;
            return Math.min(baseRolls + additionalRolls * incrementPerProgress, maxRolls);
        };
    }

    /**
     * Creates a randomized roll count function with a range of possible rolls.
     * <p>
     *     Example:
     * <pre>
     *     // Randomly roll between 1 and 3 times
     *      * RollCountFunction randomRolls = RollCountFunction.random(1, 3);
     * </pre>
     * @param minRolls The minimum number of rolls
     * @param maxRolls The maximum number of rolls
     * @return A RollCountFunction that returns a random number of rolls within the specified range
     */
    static RollCountFunction random(int minRolls, int maxRolls) {
        return context -> {
            // Use ThreadLocalRandom for thread-safe random number generation
            return ThreadLocalRandom.current().nextInt(minRolls, maxRolls + 1);
        };
    }

}