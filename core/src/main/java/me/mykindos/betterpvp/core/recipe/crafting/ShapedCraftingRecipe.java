package me.mykindos.betterpvp.core.recipe.crafting;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A shaped recipe that requires ingredients to be in specific positions.
 * The pattern can be placed anywhere within the crafting grid as long as the relative positions are maintained.
 */
@Getter
public class ShapedCraftingRecipe implements CraftingRecipe {

    private final Supplier<ItemInstance> resultSupplier;
    private final Map<Integer, RecipeIngredient> ingredients;
    private final ItemFactory itemFactory;
    private final int width;
    private final int height;
    private final boolean needsBlueprint;
    
    /**
     * Creates a new shaped recipe with a single resultSupplier.
     * 
     * @param resultSupplier The resultSupplier of the recipe
     * @param ingredients The ingredients and their positions (0-8 for a 3x3 grid)
     * @param itemFactory The ItemFactory to use for item matching
     */
    public ShapedCraftingRecipe(@NotNull Supplier<ItemInstance> resultSupplier, @NotNull Map<Integer, RecipeIngredient> ingredients, @NotNull ItemFactory itemFactory, boolean needsBlueprint) {
        this.resultSupplier = resultSupplier;
        this.ingredients = new HashMap<>(ingredients);
        this.itemFactory = itemFactory;
        this.needsBlueprint = needsBlueprint;

        // Calculate width and height of the pattern
        int minX = 3, minY = 3, maxX = 0, maxY = 0;
        for (int slot : ingredients.keySet()) {
            int x = slot % 3;
            int y = slot / 3;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        this.width = maxX - minX + 1;
        this.height = maxY - minY + 1;
    }

    @Override
    public @NotNull ItemInstance getPrimaryResult() {
        return resultSupplier.get();
    }

    @Override
    public boolean needsBlueprint() {
        return needsBlueprint;
    }

    @Override
    public @NotNull ItemInstance createPrimaryResult() {
        return resultSupplier.get();
    }

    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        // Try to match the pattern at each possible position in the grid
        for (int startRow = 0; startRow <= 3 - height; startRow++) {
            for (int startCol = 0; startCol <= 3 - width; startCol++) {
                if (matchesAt(items, startRow, startCol)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Checks if the recipe matches at the specified position in the grid.
     * 
     * @param items The items in the grid
     * @param startRow The starting row
     * @param startCol The starting column
     * @return true if the recipe matches, false otherwise
     */
    private boolean matchesAt(@NotNull Map<Integer, ItemStack> items, int startRow, int startCol) {
        // For each slot in the grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int gridSlot = row * 3 + col;
                
                // Calculate the corresponding slot in the recipe
                int recipeRow = row - startRow;
                int recipeCol = col - startCol;
                int recipeSlot = recipeRow * 3 + recipeCol;
                
                // If outside the recipe pattern, there should be no item
                if (recipeRow < 0 || recipeRow >= height || recipeCol < 0 || recipeCol >= width) {
                    if (items.containsKey(gridSlot) && !items.get(gridSlot).getType().isAir()) {
                        return false;
                    }
                    continue;
                }
                
                // Check if the ingredient matches
                RecipeIngredient ingredient = ingredients.get(recipeSlot);
                if (ingredient == null) {
                    // No ingredient required at this position
                    if (items.containsKey(gridSlot) && !items.get(gridSlot).getType().isAir()) {
                        return false;
                    }
                } else {
                    // Ingredient required at this position
                    if (!items.containsKey(gridSlot) || items.get(gridSlot).getType().isAir() || 
                            !ingredient.matches(items.get(gridSlot), itemFactory)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

    @Override
    public @NotNull RecipeType getType() {
        return RecipeType.SHAPED_CRAFTING;
    }
    
    @Override
    public @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> ingredients, @NotNull ItemFactory itemFactory) {
        List<Integer> consumedSlots = new ArrayList<>();
        Map<Integer, ItemStack> itemStackMatrix = new HashMap<>();
        
        // Convert to ItemStacks for matching
        for (Map.Entry<Integer, ItemInstance> entry : ingredients.entrySet()) {
            if (entry.getValue() != null) {
                itemStackMatrix.put(entry.getKey(), entry.getValue().createItemStack());
            }
        }
        
        // Find the position where the recipe matches
        int startRow = 0, startCol = 0;
        boolean found = false;
        
        // Try each possible position
        for (int row = 0; row <= 3 - height; row++) {
            for (int col = 0; col <= 3 - width; col++) {
                if (matchesAt(itemStackMatrix, row, col)) {
                    startRow = row;
                    startCol = col;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }
        
        if (!found) {
            return consumedSlots;
        }
        
        // Consume ingredients at the matched position
        for (Map.Entry<Integer, RecipeIngredient> entry : this.ingredients.entrySet()) {
            int recipeSlot = entry.getKey();
            RecipeIngredient ingredient = entry.getValue();

            // Calculate the corresponding grid slot
            int recipeRow = recipeSlot / 3;
            int recipeCol = recipeSlot % 3;
            int gridRow = recipeRow + startRow;
            int gridCol = recipeCol + startCol;
            int gridSlot = gridRow * 3 + gridCol;

            ItemInstance instance = ingredients.get(gridSlot);
            if (instance != null && ingredient.matches(instance.createItemStack(), itemFactory)) {
                // Only consume if the ingredient should be consumed on craft
                if (ingredient.isConsumeOnCraft()) {
                    ItemStack stack = instance.createItemStack();
                    int newAmount = stack.getAmount() - ingredient.getAmount();

                    if (newAmount <= 0) {
                        ingredients.remove(gridSlot);
                    } else {
                        stack.setAmount(newAmount);
                        final ItemInstance newInstance = itemFactory.fromItemStack(stack).orElseThrow();
                        ingredients.put(gridSlot, newInstance);
                    }

                    consumedSlots.add(gridSlot);
                }
            }
        }
        
        return consumedSlots;
    }
    
    /**
     * A builder for creating shaped recipes using a character-based pattern.
     */
    public static class Builder {
        private final Supplier<ItemInstance> resultSupplier;
        private final String[] pattern;
        private final Map<Character, RecipeIngredient> ingredients = new HashMap<>();
        private final ItemFactory itemFactory;
        private boolean needsBlueprint = false;
        
        /**
         * Creates a new builder for a shaped recipe.
         * 
         * @param resultSupplier The resultSupplier of the recipe
         * @param pattern The pattern of the recipe (up to 3 rows of up to 3 characters each)
         * @param itemFactory The ItemFactory to use for item matching
         */
        public Builder(@NotNull Supplier<ItemInstance> resultSupplier, @NotNull String[] pattern, @NotNull ItemFactory itemFactory) {
            this.resultSupplier = resultSupplier;
            this.pattern = pattern;
            this.itemFactory = itemFactory;
            
            // Validate pattern
            if (pattern.length > 3) {
                throw new IllegalArgumentException("Pattern cannot have more than 3 rows");
            }
            
            for (String row : pattern) {
                if (row.length() > 3) {
                    throw new IllegalArgumentException("Pattern rows cannot be longer than 3 characters");
                }
            }
        }

        public Builder(@NotNull BaseItem result, @NotNull String[] pattern, @NotNull ItemFactory itemFactory) {
            this(() -> itemFactory.create(result), pattern, itemFactory);
        }

        /**
         * Sets an ingredient for a character in the pattern.
         * 
         * @param key The character in the pattern
         * @param ingredient The ingredient to use for this character
         * @return This builder
         */
        public Builder setIngredient(char key, @NotNull RecipeIngredient ingredient) {
            ingredients.put(key, ingredient);
            return this;
        }

        /**
         * Sets an ingredient for a character in the pattern using a Material and amount.
         * @param key The character in the pattern
         * @param material The material to use for this character
         * @param amount The amount of the material required
         * @return This builder
         */
        public Builder setIngredient(char key, @NotNull Material material, int amount) {
            final BaseItem item = itemFactory.fromItemStack(ItemStack.of(material)).orElseThrow().getBaseItem();
            return setIngredient(key, new RecipeIngredient(item, amount));
        }

        /**
         * Marks the recipe as requiring a blueprint to craft.
         * @return This builder
         */
        public Builder needsBlueprint() {
            this.needsBlueprint = true;
            return this;
        }
        
        /**
         * Builds the shaped recipe.
         * 
         * @return The built recipe
         */
        public ShapedCraftingRecipe build() {
            Map<Integer, RecipeIngredient> recipeIngredients = new HashMap<>();
            
            // Convert pattern to ingredient map
            for (int y = 0; y < pattern.length; y++) {
                String row = pattern[y];
                for (int x = 0; x < row.length(); x++) {
                    char key = row.charAt(x);
                    if (key != ' ') {
                        RecipeIngredient ingredient = ingredients.get(key);
                        if (ingredient != null) {
                            int slot = y * 3 + x;
                            recipeIngredients.put(slot, ingredient);
                        }
                    }
                }
            }
            
            return new ShapedCraftingRecipe(resultSupplier, recipeIngredients, itemFactory, needsBlueprint);
        }
    }
} 