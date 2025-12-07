package me.mykindos.betterpvp.core.imbuement;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents the result of an imbuement recipe.
 * Contains the primary result that is produced.
 */
@Getter
public class ImbuementRecipeResult {
    
    private final @NotNull BaseItem primaryResult;
    private final @NotNull List<BaseItem> secondaryResults;
    
    /**
     * Creates a new imbuement recipe result with only a primary result.
     * @param primaryResult The main item produced by this recipe
     */
    public ImbuementRecipeResult(@NotNull BaseItem primaryResult) {
        this.primaryResult = primaryResult;
        this.secondaryResults = Collections.emptyList();
    }
    
    /**
     * Creates a new imbuement recipe result with primary and secondary results.
     * @param primaryResult The main item produced by this recipe
     * @param secondaryResults Additional items that may be produced
     */
    public ImbuementRecipeResult(@NotNull BaseItem primaryResult, @NotNull List<BaseItem> secondaryResults) {
        this.primaryResult = primaryResult;
        this.secondaryResults = List.copyOf(secondaryResults);
    }
} 