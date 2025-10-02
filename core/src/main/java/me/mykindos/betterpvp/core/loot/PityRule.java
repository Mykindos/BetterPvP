package me.mykindos.betterpvp.core.loot;

import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * Represents a pity rule for a {@link LootBundle}.
 * <p>
 * Pity rules modify the weight of a loot entry based on the number of failed rolls.
 */
@Value
@Builder
public class PityRule {
    /**
     * The loot entry to apply the pity rule to.
     */
    @NotNull Loot<?, ?> loot;

    /**
     * The maximum number of failed rolls before the weight is shifted towards the center by
     * the {@link #weightIncrement}.
     */
    @Range(from = 1, to = Long.MAX_VALUE) int maxAttempts;

    /**
     * The weight increment to apply to the loot entry's weight every
     * {@link #maxAttempts} failed rolls.
     */
    @Range(from = 1, to = Long.MAX_VALUE) int weightIncrement;
}
