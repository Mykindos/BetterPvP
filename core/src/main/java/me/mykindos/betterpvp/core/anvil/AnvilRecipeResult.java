package me.mykindos.betterpvp.core.anvil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.item.BaseItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of an anvil crafting recipe.
 * Contains the primary result item and optional secondary result items.
 */
@Getter
public class AnvilRecipeResult {
    
    private final @NotNull BaseItem primaryResult;
    private final @NotNull List<BaseItem> secondaryResults;
    
    /**
     * Creates a new anvil recipe result with only a primary result.
     * @param primaryResult The main item produced by the recipe
     */
    public AnvilRecipeResult(@NotNull BaseItem primaryResult) {
        this.primaryResult = primaryResult;
        this.secondaryResults = new ArrayList<>();
    }
    
    /**
     * Creates a new anvil recipe result with a primary result and secondary results.
     * @param primaryResult The main item produced by the recipe
     * @param secondaryResults Additional items that may be produced
     */
    public AnvilRecipeResult(@NotNull BaseItem primaryResult, @NotNull List<BaseItem> secondaryResults) {
        this.primaryResult = primaryResult;
        this.secondaryResults = new ArrayList<>(secondaryResults);
    }
    
    /**
     * Gets all results (primary + secondary) as a single list.
     * @return A list containing the primary result followed by all secondary results
     */
    public @NotNull List<BaseItem> getAllResults() {
        List<BaseItem> allResults = new ArrayList<>();
        allResults.add(primaryResult);
        allResults.addAll(secondaryResults);
        return Collections.unmodifiableList(allResults);
    }
    
    /**
     * Checks if this result has any secondary results.
     * @return true if there are secondary results, false otherwise
     */
    public boolean hasSecondaryResults() {
        return !secondaryResults.isEmpty();
    }
    
    /**
     * Gets the number of secondary results.
     * @return The count of secondary result items
     */
    public int getSecondaryResultCount() {
        return secondaryResults.size();
    }
} 