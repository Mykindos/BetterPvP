package me.mykindos.betterpvp.core.loot;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * A weighted loot entry. {@code defaultWeight} is the static value used for menu previews
 * and {@link LootTable#getChances()}; {@code weight} is the function used during rolls.
 */
@Value
public class WeightedEntry {

    @NotNull Loot<?, ?> loot;
    @NotNull WeightFunction weight;
    int defaultWeight;

    public static WeightedEntry of(@NotNull Loot<?, ?> loot, int weight) {
        return new WeightedEntry(loot, WeightFunction.constant(weight), weight);
    }

    public static WeightedEntry of(@NotNull Loot<?, ?> loot, @NotNull WeightFunction weight, int defaultWeight) {
        return new WeightedEntry(loot, weight, defaultWeight);
    }
}
