package me.mykindos.betterpvp.core.recipe.smelting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents the result of a smelting operation.
 * Contains the liquid alloys produced and any additional results.
 */
@Getter
@RequiredArgsConstructor
public class SmeltingResult {
    
    private final @NotNull LiquidAlloy primaryResult;
    
    /**
     * Gets the total millibuckets of liquid alloy produced.
     * @return The total mB across all results
     */
    public int getTotalMillibuckets() {
        return primaryResult.getMillibuckets();
    }
} 