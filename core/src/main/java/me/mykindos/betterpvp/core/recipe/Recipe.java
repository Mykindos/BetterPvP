package me.mykindos.betterpvp.core.recipe;

import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Base interface for all recipe types in the system.
 * Recipes define how items can be combined to create new items.
 *
 * <p>Each recipe exposes its result through two methods with the same return type {@code T}:
 * a side-effect-free snapshot ({@link #previewResult()}) and a live, possibly side-effecting,
 * fresh production ({@link #createResult()}).</p>
 *
 * <p>If {@code T} contains an {@code ItemInstance}, the snapshot variant wraps a preview
 * instance (built via {@link ItemFactory#createPreview}) while the live variant wraps a
 * fully-built instance (built via {@link ItemFactory#create}). If {@code T} carries no
 * item content, both methods may return the same value.</p>
 */
public interface Recipe<T> {

    /**
     * Returns a snapshot of the recipe's result.
     * Implementations MUST be side-effect-free: no persistent builders,
     * no DB writes, no UUID generation. Safe to call freely from GUIs,
     * tooltips, recipe browsers, and matching/registry code.
     *
     * @return A snapshot of the result; equivalent in shape to {@link #createResult()}
     *         but with any embedded items built as previews.
     */
    @NotNull T previewResult();

    /**
     * Produces the live result of this recipe. May trigger persistent builders
     * (UUID assignment, DB writes, etc.) when the result contains an
     * {@code ItemInstance}. Use only when actually crafting / executing the recipe.
     *
     * @return The live result.
     */
    @NotNull T createResult();

    /**
     * Checks if the provided items match this recipe.
     * @param items A map of slot indices to ItemStacks
     * @return true if the items match this recipe, false otherwise
     */
    boolean matches(@NotNull Map<Integer, ItemStack> items);

    /**
     * Gets the ingredients required for this recipe.
     * @return A map of slot indices to ingredient requirements
     */
    @NotNull Map<Integer, RecipeIngredient> getIngredients();

    /**
     * Consumes the ingredients required for this recipe from the crafting matrix.
     *
     * @param ingredients The items in the crafting matrix
     * @param itemFactory The ItemFactory to use for item creation
     * @return A list of slots that were consumed from
     */
    @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> ingredients, @NotNull ItemFactory itemFactory);

    /**
     * Gets the type of this recipe.
     * @return The recipe type
     */
    @NotNull RecipeType getType();

    /**
     * Checks if this recipe can be crafted by the player in the current context.
     * This can be used to restrict recipes to specific conditions, such as requiring
     * a blueprint to be unlocked.
     *
     * @param player The player attempting to craft the recipe
     * @return true if the recipe can be crafted, false otherwise
     */
    default boolean canCraft(@Nullable Player player) {
        return true;
    }


}
