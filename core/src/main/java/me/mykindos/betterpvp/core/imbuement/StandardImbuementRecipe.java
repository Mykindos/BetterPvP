package me.mykindos.betterpvp.core.imbuement;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Standard imbuement recipe that requires exact ingredients to produce results.
 * This is the traditional recipe type that matches ingredients exactly with no residuals.
 */
@Getter
public class StandardImbuementRecipe extends ImbuementRecipe {
    
    private final @NotNull Map<BaseItem, Integer> ingredients;
    private final @NotNull ImbuementRecipeResult result;
    
    /**
     * Creates a new standard imbuement recipe.
     * @param ingredients Map of base items to their required quantities
     * @param result The imbuement recipe result containing primary and secondary outputs
     * @param itemFactory The item factory for item operations
     */
    public StandardImbuementRecipe(@NotNull Map<BaseItem, Integer> ingredients,
                                  @NotNull ImbuementRecipeResult result,
                                  @NotNull ItemFactory itemFactory) {
        super(itemFactory);
        this.ingredients = new HashMap<>(ingredients);
        this.result = result;
    }
    
    /**
     * Creates a new standard imbuement recipe with only a primary result.
     * @param ingredients Map of base items to their required quantities
     * @param primaryResult The main item produced by this recipe
     * @param itemFactory The item factory for item operations
     */
    public StandardImbuementRecipe(@NotNull Map<BaseItem, Integer> ingredients,
                                  @NotNull BaseItem primaryResult,
                                  @NotNull ItemFactory itemFactory) {
        this(ingredients, new ImbuementRecipeResult(primaryResult), itemFactory);
    }
    
    @Override
    public @NotNull ImbuementRecipeResult getPrimaryResult() {
        return result;
    }
    
    @Override
    public @NotNull ItemInstance createPrimaryResult() {
        return itemFactory.create(result.getPrimaryResult());
    }
    
    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        // Create a map of BaseItem to available amounts
        Map<BaseItem, Integer> availableIngredients = new HashMap<>();
        for (ItemStack stack : items.values()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            
            itemFactory.fromItemStack(stack).ifPresent(instance -> {
                BaseItem baseItem = instance.getBaseItem();
                availableIngredients.merge(baseItem, stack.getAmount(), Integer::sum);
            });
        }
        
        // Must match EXACTLY - no extra ingredients allowed and all required ingredients must be present
        if (availableIngredients.size() != ingredients.size()) {
            return false;
        }
        
        // Check if all required ingredients are available in exact quantities
        for (Map.Entry<BaseItem, Integer> entry : ingredients.entrySet()) {
            BaseItem baseItem = entry.getKey();
            int requiredAmount = entry.getValue();
            
            int availableAmount = availableIngredients.getOrDefault(baseItem, 0);
            if (availableAmount != requiredAmount) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public @NotNull Map<Integer, RecipeIngredient> getIngredients() {
        // Convert our ingredient map to the expected format
        Map<Integer, RecipeIngredient> recipeIngredients = new HashMap<>();
        int index = 0;
        for (Map.Entry<BaseItem, Integer> entry : ingredients.entrySet()) {
            recipeIngredients.put(index++, new RecipeIngredient(entry.getKey(), entry.getValue()));
        }
        return recipeIngredients;
    }
    
    /**
     * Gets the imbuement recipe result containing all outputs.
     * @return The imbuement recipe result
     */
    public @NotNull ImbuementRecipeResult getImbuementResult() {
        return result;
    }
} 