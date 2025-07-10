package me.mykindos.betterpvp.core.anvil;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An anvil recipe that requires specific items and a number of hammer swings to produce results.
 * Unlike crafting recipes, anvil recipes are shapeless and require hammer swing progression.
 */
@Getter
public class AnvilRecipe implements Recipe<AnvilRecipeResult, ItemInstance> {
    
    private final @NotNull Map<BaseItem, Integer> ingredients;
    private final @NotNull AnvilRecipeResult result;
    private final int hammerSwings;
    private final @NotNull ItemFactory itemFactory;
    
    /**
     * Creates a new anvil recipe.
     * @param ingredients Map of base items to their required quantities
     * @param result The anvil recipe result containing primary and secondary outputs
     * @param hammerSwings The number of hammer swings required to complete this recipe
     * @param itemFactory The item factory for item operations
     */
    public AnvilRecipe(@NotNull Map<BaseItem, Integer> ingredients,
                       @NotNull AnvilRecipeResult result,
                       int hammerSwings,
                       @NotNull ItemFactory itemFactory) {
        this.ingredients = new HashMap<>(ingredients);
        this.result = result;
        this.hammerSwings = hammerSwings;
        this.itemFactory = itemFactory;
    }
    
    /**
     * Creates a new anvil recipe with only a primary result.
     * @param ingredients Map of base items to their required quantities
     * @param primaryResult The main item produced by this recipe
     * @param hammerSwings The number of hammer swings required to complete this recipe
     * @param itemFactory The item factory for item operations
     */
    public AnvilRecipe(@NotNull Map<BaseItem, Integer> ingredients,
                       @NotNull BaseItem primaryResult,
                       int hammerSwings,
                       @NotNull ItemFactory itemFactory) {
        this(ingredients, new AnvilRecipeResult(primaryResult), hammerSwings, itemFactory);
    }
    
    @Override
    public @NotNull AnvilRecipeResult getPrimaryResult() {
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
        
        // Check if all required ingredients are available in sufficient quantities
        for (Map.Entry<BaseItem, Integer> entry : ingredients.entrySet()) {
            BaseItem baseItem = entry.getKey();
            int requiredAmount = entry.getValue();
            
            int availableAmount = availableIngredients.getOrDefault(baseItem, 0);
            if (availableAmount < requiredAmount) {
                return false;
            }
        }
        
        // Check that no extra ingredients are present
        for (BaseItem availableItem : availableIngredients.keySet()) {
            if (!ingredients.containsKey(availableItem)) {
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
    
    @Override
    public @NotNull RecipeType getType() {
        return RecipeType.ANVIL_CRAFTING;
    }
    
    @Override
    public @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> ingredients, @NotNull ItemFactory itemFactory) {
        List<Integer> consumedSlots = new ArrayList<>();
        
        // Create a copy of required ingredients to track what we still need
        Map<BaseItem, Integer> remainingIngredients = new HashMap<>(this.ingredients);
        
        // Consume items from the matrix
        for (Map.Entry<Integer, ItemInstance> entry : new HashMap<>(ingredients).entrySet()) {
            if (entry.getValue() == null) continue;
            
            BaseItem baseItem = entry.getValue().getBaseItem();
            if (!remainingIngredients.containsKey(baseItem)) continue;
            
            int needed = remainingIngredients.get(baseItem);
            if (needed <= 0) continue;
            
            ItemStack stack = entry.getValue().createItemStack();
            int available = stack.getAmount();
            int toConsume = Math.min(available, needed);
            
            if (toConsume > 0) {
                if (available <= toConsume) {
                    ingredients.remove(entry.getKey());
                } else {
                    stack.setAmount(available - toConsume);
                    final ItemInstance newInstance = itemFactory.fromItemStack(stack).orElseThrow();
                    ingredients.put(entry.getKey(), newInstance);
                }
                
                remainingIngredients.put(baseItem, needed - toConsume);
                consumedSlots.add(entry.getKey());
            }
        }
        
        return consumedSlots;
    }
    
    /**
     * Gets the ingredient types used in this recipe (ignoring quantities).
     * Used for duplicate recipe detection.
     * @return A set of base items used as ingredients
     */
    public @NotNull Set<BaseItem> getIngredientTypes() {
        return ingredients.keySet();
    }
    
    /**
     * Gets the anvil recipe result containing all outputs.
     * @return The anvil recipe result
     */
    public @NotNull AnvilRecipeResult getAnvilResult() {
        return result;
    }
    
    /**
     * Gets the number of hammer swings required to complete this recipe.
     * @return The required hammer swing count
     */
    public int getRequiredHammerSwings() {
        return hammerSwings;
    }
} 