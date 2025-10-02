package me.mykindos.betterpvp.core.loot;

import lombok.Builder;
import lombok.Value;

/**
 * Configuration for progressive weight distribution.
 * Allows customization of how weights are adjusted.
 */
@Value
@Builder
public class ProgressiveWeightConfig {
    @Builder.Default
    int maxShift = 5; // Maximum weight adjustment per distribution

    @Builder.Default
    double shiftFactor = 0.5; // Fraction of variance used for shifting

    @Builder.Default
    boolean enableVarianceScaling = true; // Scale shift based on variance
}
