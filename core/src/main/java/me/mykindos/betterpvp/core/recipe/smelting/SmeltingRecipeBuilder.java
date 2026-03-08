package me.mykindos.betterpvp.core.recipe.smelting;

import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder class for creating smelting recipes in a fluent API style.
 */
public class SmeltingRecipeBuilder {
    
    private final Map<BaseItem, Integer> ingredients = new HashMap<>();
    private float minimumTemperature = 0.0f;
    private LiquidAlloy primaryResult;
    
    /**
     * Adds an ingredient to the recipe.
     * @param item The base item ingredient
     * @param amount The amount required
     * @return This builder instance
     */
    public SmeltingRecipeBuilder addIngredient(@NotNull BaseItem item, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        ingredients.put(item, amount);
        return this;
    }
    
    /**
     * Sets the primary result of the smelting recipe.
     * @param alloy The alloy type to produce
     * @param millibuckets The amount in millibuckets
     * @return This builder instance
     */
    public SmeltingRecipeBuilder setPrimaryResult(@NotNull Alloy alloy, int millibuckets) {
        if (millibuckets <= 0) {
            throw new IllegalArgumentException("Millibuckets must be positive");
        }
        this.primaryResult = new LiquidAlloy(alloy, millibuckets);
        return this;
    }
    
    /**
     * Sets the minimum temperature required for this recipe.
     * @param temperature The minimum temperature in Celsius
     * @return This builder instance
     */
    public SmeltingRecipeBuilder setMinimumTemperature(float temperature) {
        this.minimumTemperature = temperature;
        return this;
    }
    
    /**
     * Builds the smelting recipe.
     * @param itemFactory The item factory for recipe operations
     * @return The completed smelting recipe
     * @throws IllegalStateException if required fields are missing
     */
    public SmeltingRecipe build(@NotNull ItemFactory itemFactory) {
        if (ingredients.isEmpty()) {
            throw new IllegalStateException("Recipe must have at least one ingredient");
        }
        if (primaryResult == null) {
            throw new IllegalStateException("Recipe must have a primary result");
        }
        
        // Use the alloy's minimum temperature if we haven't set one explicitly
        float recipeTemperature = minimumTemperature > 0 ? minimumTemperature : primaryResult.getAlloyType().getMinimumTemperature();
        
        SmeltingResult smeltingResult = new SmeltingResult(primaryResult);
        return new SmeltingRecipe(ingredients, smeltingResult, recipeTemperature, itemFactory);
    }
    
    /**
     * Creates a new builder instance.
     * @return A new SmeltingRecipeBuilder
     */
    public static SmeltingRecipeBuilder create() {
        return new SmeltingRecipeBuilder();
    }
} 