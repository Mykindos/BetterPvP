package me.mykindos.betterpvp.core.item.purity.distribution;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.component.impl.purity.ItemPurity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a weighted distribution for randomly assigning purity levels to items.
 */
@Getter
public class PurityDistribution {

    private final String name;
    private final Map<ItemPurity, Integer> weights;
    private final int totalWeight;

    /**
     * Creates a new PurityDistribution with the specified name and weights.
     *
     * @param name    The name of this distribution
     * @param weights The weights for each purity level
     */
    public PurityDistribution(@NotNull String name, @NotNull Map<ItemPurity, Integer> weights) {
        this.name = name;
        this.weights = new HashMap<>(weights);
        this.totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Randomly selects a purity level based on the weighted distribution.
     *
     * @return A randomly selected ItemPurity based on weights
     */
    @NotNull
    public ItemPurity roll() {
        if (totalWeight == 0) {
            // Fallback to MODERATE if no weights configured
            return ItemPurity.MODERATE;
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        for (Map.Entry<ItemPurity, Integer> entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (random < cumulative) {
                return entry.getKey();
            }
        }

        // Fallback (should never reach here if weights are configured correctly)
        return ItemPurity.MODERATE;
    }

    /**
     * Gets the weight for a specific purity level.
     *
     * @param purity The purity level
     * @return The weight, or 0 if not configured
     */
    public int getWeight(@NotNull ItemPurity purity) {
        return weights.getOrDefault(purity, 0);
    }
}
