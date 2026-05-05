package me.mykindos.betterpvp.core.imbuement;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents the result of an imbuement recipe.
 * Holds the primary result as an {@link ItemInstance} (preview or live, depending
 * on which {@link ImbuementRecipe} method produced this wrapper) plus any secondary
 * base items that should be dropped alongside.
 */
@Getter
public class ImbuementRecipeResult {

    private final @NotNull ItemInstance primaryResult;
    private final @NotNull List<BaseItem> secondaryResults;

    /**
     * Creates a new imbuement recipe result with only a primary result.
     * @param primaryResult The main item produced by this recipe
     */
    public ImbuementRecipeResult(@NotNull ItemInstance primaryResult) {
        this.primaryResult = primaryResult;
        this.secondaryResults = Collections.emptyList();
    }

    /**
     * Creates a new imbuement recipe result with primary and secondary results.
     * @param primaryResult The main item produced by this recipe
     * @param secondaryResults Additional items that may be produced
     */
    public ImbuementRecipeResult(@NotNull ItemInstance primaryResult, @NotNull List<BaseItem> secondaryResults) {
        this.primaryResult = primaryResult;
        this.secondaryResults = List.copyOf(secondaryResults);
    }

    /** @return the BaseItem of the primary result, for matching/equality checks. */
    public @NotNull BaseItem getPrimaryBaseItem() {
        return primaryResult.getBaseItem();
    }
}
