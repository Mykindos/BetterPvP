package me.mykindos.betterpvp.core.recipe.smelting;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A smelting recipe that combines metal ingredients to produce liquid alloys.
 * Unlike crafting recipes, smelting recipes are shapeless and require specific temperatures.
 */
@Getter
public class SmeltingRecipe implements Recipe<SmeltingResult, SmeltingResult> {
    
    private final @NotNull Map<BaseItem, Integer> ingredients;
    private final @NotNull SmeltingResult result;
    private final float minimumTemperature;
    private final @NotNull ItemFactory itemFactory;
    
    /**
     * Creates a new smelting recipe.
     * @param ingredients Map of base items to their required quantities
     * @param result The smelting result containing liquid alloys
     * @param minimumTemperature The minimum temperature required for this recipe
     * @param itemFactory The item factory for item operations
     */
    public SmeltingRecipe(@NotNull Map<BaseItem, Integer> ingredients,
                          @NotNull SmeltingResult result,
                          float minimumTemperature,
                          @NotNull ItemFactory itemFactory) {
        this.ingredients = new HashMap<>(ingredients);
        this.result = result;
        this.minimumTemperature = minimumTemperature;
        this.itemFactory = itemFactory;
    }
    
    @Override
    public @NotNull SmeltingResult getPrimaryResult() {
        return result;
    }
    
    @Override
    public @NotNull SmeltingResult createPrimaryResult() {
        return result;
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
        return RecipeType.SMELTING;
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
     * Gets the smelting result containing liquid alloys.
     * @return The smelting result
     */
    public @NotNull SmeltingResult getSmeltingResult() {
        return result;
    }
} 