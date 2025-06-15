package me.mykindos.betterpvp.core.recipe;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A shapeless recipe that requires specific ingredients but not in any particular arrangement.
 */
@Getter
public class ShapelessRecipe implements Recipe {

    private final BaseItem result;
    private final List<BaseItem> additionalResults;
    private final Map<Integer, RecipeIngredient> ingredients;
    private final ItemFactory itemFactory;
    private final boolean needsBlueprint;
    
    /**
     * Creates a new shapeless recipe with a single result.
     * 
     * @param result The result of the recipe
     * @param ingredients The ingredients required (slot positions are ignored for matching)
     * @param itemFactory The ItemFactory to use for item matching
     */
    public ShapelessRecipe(@NotNull BaseItem result, @NotNull Map<Integer, RecipeIngredient> ingredients, @NotNull ItemFactory itemFactory, boolean needsBlueprint) {
        this(result, Collections.emptyList(), ingredients, itemFactory, needsBlueprint);
    }
    
    /**
     * Creates a new shapeless recipe with multiple results.
     * 
     * @param primaryResult The primary result of the recipe
     * @param additionalResults Additional results of the recipe
     * @param ingredients The ingredients required (slot positions are ignored for matching)
     * @param itemFactory The ItemFactory to use for item matching
     */
    public ShapelessRecipe(@NotNull BaseItem primaryResult, @NotNull List<BaseItem> additionalResults,
                           @NotNull Map<Integer, RecipeIngredient> ingredients, @NotNull ItemFactory itemFactory, boolean needsBlueprint) {
        this.result = primaryResult;
        this.additionalResults = new ArrayList<>(additionalResults);
        this.ingredients = new HashMap<>(ingredients);
        this.itemFactory = itemFactory;
        this.needsBlueprint = needsBlueprint;
    }
    
    @Override
    public @NotNull BaseItem getPrimaryResult() {
        return result;
    }
    
    @Override
    public @NotNull List<BaseItem> getResults() {
        List<BaseItem> allResults = new ArrayList<>();
        allResults.add(result);
        allResults.addAll(additionalResults);
        return allResults;
    }

    @Override
    public @NotNull ItemInstance createPrimaryResult() {
        return itemFactory.create(result);
    }

    @Override
    public @NotNull List<ItemInstance> createResults() {
        List<ItemInstance> instances = new ArrayList<>();
        instances.add(itemFactory.create(result));
        for (BaseItem additional : additionalResults) {
            instances.add(itemFactory.create(additional));
        }
        return instances;
    }

    @Override
    public boolean needsBlueprint() {
        return needsBlueprint;
    }

    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        // Create a map of BaseItem to required amounts
        Map<BaseItem, Integer> requiredIngredients = new HashMap<>();
        for (RecipeIngredient ingredient : ingredients.values()) {
            requiredIngredients.merge(ingredient.getBaseItem(), ingredient.getAmount(), Integer::sum);
        }
        
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
        for (Map.Entry<BaseItem, Integer> entry : requiredIngredients.entrySet()) {
            BaseItem baseItem = entry.getKey();
            int requiredAmount = entry.getValue();
            
            int availableAmount = availableIngredients.getOrDefault(baseItem, 0);
            if (availableAmount < requiredAmount) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public @NotNull RecipeType getType() {
        return RecipeType.SHAPELESS_CRAFTING;
    }
    
    @Override
    public @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> craftingMatrix, @NotNull ItemFactory itemFactory) {
        List<Integer> consumedSlots = new ArrayList<>();

        // Create a map of BaseItem to required amounts
        Map<BaseItem, Integer> requiredIngredients = new HashMap<>();
        for (RecipeIngredient ingredient : getIngredients().values()) {
            requiredIngredients.merge(ingredient.getBaseItem(), ingredient.getAmount(), Integer::sum);
        }

        // Consume items from the matrix
        for (Map.Entry<BaseItem, Integer> entry : requiredIngredients.entrySet()) {
            BaseItem baseItem = entry.getKey();
            int amountToConsume = entry.getValue();

            // Find matching items in the matrix and consume them
            for (Map.Entry<Integer, ItemInstance> matrixEntry : new HashMap<>(craftingMatrix).entrySet()) {
                if (amountToConsume <= 0) {
                    break;
                }

                int slot = matrixEntry.getKey();
                ItemInstance instance = matrixEntry.getValue();

                if (instance == null) {
                    continue;
                }

                if (!instance.getBaseItem().equals(baseItem)) {
                    continue;
                }

                ItemStack stack = instance.createItemStack();
                // Consume from this stack
                int amountFromThisStack = Math.min(stack.getAmount(), amountToConsume);
                amountToConsume -= amountFromThisStack;

                int newAmount = stack.getAmount() - amountFromThisStack;
                if (newAmount <= 0) {
                    craftingMatrix.remove(slot);
                } else {
                    stack.setAmount(newAmount);
                    final ItemInstance result = itemFactory.fromItemStack(stack).orElseThrow();
                    craftingMatrix.put(slot, result);
                }

                consumedSlots.add(slot);
            }
        }
        
        return consumedSlots;
    }
} 