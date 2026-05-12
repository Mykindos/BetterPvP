package me.mykindos.betterpvp.core.recipe.crafting;

import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.access.ItemAccessService;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A shaped recipe that requires ingredients to be in specific positions.
 * The pattern can be placed anywhere within the crafting grid as long as the relative positions are maintained.
 */
@Getter
public class ShapedCraftingRecipe implements CraftingRecipe {

    private final BaseItem result;
    private final Consumer<ItemInstance> resultCustomizer;
    private final Map<Integer, RecipeIngredient> ingredients;
    private final ItemFactory itemFactory;
    private final int width;
    private final int height;
    private final boolean needsBlueprint;

    /**
     * The namespaced key under which this recipe is registered.
     * Set by {@link CraftingRecipeRegistry#registerRecipe(NamespacedKey, CraftingRecipe)} after registration.
     * Used by {@link #canCraft(Player)} to delegate to {@link ItemAccessService}.
     */
    @Nullable
    private NamespacedKey recipeKey;

    /**
     * Creates a new shaped recipe.
     *
     * @param result The base item produced by this recipe
     * @param resultCustomizer Side-effect-free customizer applied to both preview and live results
     *                         (e.g. setting amount). Must not trigger persistent state.
     * @param ingredients The ingredients and their positions (0-8 for a 3x3 grid)
     * @param itemFactory The ItemFactory to use for item matching
     */
    public ShapedCraftingRecipe(@NotNull BaseItem result,
                                @NotNull Consumer<ItemInstance> resultCustomizer,
                                @NotNull Map<Integer, RecipeIngredient> ingredients,
                                @NotNull ItemFactory itemFactory,
                                boolean needsBlueprint) {
        this.result = result;
        this.resultCustomizer = resultCustomizer;
        this.ingredients = new HashMap<>(ingredients);
        this.itemFactory = itemFactory;
        this.needsBlueprint = needsBlueprint;

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

    public ShapedCraftingRecipe(@NotNull BaseItem result,
                                @NotNull Map<Integer, RecipeIngredient> ingredients,
                                @NotNull ItemFactory itemFactory,
                                boolean needsBlueprint) {
        this(result, instance -> {}, ingredients, itemFactory, needsBlueprint);
    }

    @Override
    public @NotNull ItemInstance previewResult() {
        ItemInstance instance = itemFactory.createPreview(result);
        resultCustomizer.accept(instance);
        return instance;
    }

    @Override
    public @NotNull ItemInstance createResult() {
        ItemInstance instance = itemFactory.create(result);
        resultCustomizer.accept(instance);
        return instance;
    }

    @Override
    public boolean needsBlueprint() {
        return needsBlueprint;
    }

    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        for (int startRow = 0; startRow <= 3 - height; startRow++) {
            for (int startCol = 0; startCol <= 3 - width; startCol++) {
                if (matchesAt(items, startRow, startCol, false) || matchesAt(items, startRow, startCol, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAt(@NotNull Map<Integer, ItemStack> items, int startRow, int startCol, boolean mirrored) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int gridSlot = row * 3 + col;

                int recipeRow = row - startRow;
                int recipeCol = mirrored ? (width - 1) - (col - startCol) : (col - startCol);
                int recipeSlot = recipeRow * 3 + recipeCol;

                if (recipeRow < 0 || recipeRow >= height || recipeCol < 0 || recipeCol >= width) {
                    if (items.containsKey(gridSlot) && !items.get(gridSlot).getType().isAir()) {
                        return false;
                    }
                    continue;
                }

                RecipeIngredient ingredient = ingredients.get(recipeSlot);
                if (ingredient == null) {
                    if (items.containsKey(gridSlot) && !items.get(gridSlot).getType().isAir()) {
                        return false;
                    }
                } else {
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

    /** Called by {@link CraftingRecipeRegistry} to store the key after registration. */
    public void setRecipeKey(@NotNull NamespacedKey key) {
        this.recipeKey = key;
    }

    /**
     * Delegates to {@link ItemAccessService#isAllowed} with {@link AccessScope#CRAFT}.
     * The component check is performed inside the service — if the result item's
     * {@link me.mykindos.betterpvp.core.item.component.impl.access.RestrictedAccessComponent}
     * does not list CRAFT, the service returns {@code true} immediately.
     */
    @Override
    public boolean canCraft(@Nullable Player player) {
        if (player == null || recipeKey == null) return true;
        try {
            ItemAccessService service = JavaPlugin.getPlugin(Core.class)
                    .getInjector().getInstance(ItemAccessService.class);
            Key key = Key.key(recipeKey.namespace(), recipeKey.getKey());
            return service.isAllowed(player, result, key, AccessScope.CRAFT);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> ingredients, @NotNull ItemFactory itemFactory) {
        List<Integer> consumedSlots = new ArrayList<>();
        Map<Integer, ItemStack> itemStackMatrix = new HashMap<>();

        for (Map.Entry<Integer, ItemInstance> entry : ingredients.entrySet()) {
            if (entry.getValue() != null) {
                itemStackMatrix.put(entry.getKey(), entry.getValue().createItemStack());
            }
        }

        int startRow = 0, startCol = 0;
        boolean mirrored = false;
        boolean found = false;

        for (int row = 0; row <= 3 - height && !found; row++) {
            for (int col = 0; col <= 3 - width && !found; col++) {
                if (matchesAt(itemStackMatrix, row, col, false)) {
                    startRow = row;
                    startCol = col;
                    found = true;
                } else if (matchesAt(itemStackMatrix, row, col, true)) {
                    startRow = row;
                    startCol = col;
                    mirrored = true;
                    found = true;
                }
            }
        }

        if (!found) {
            return consumedSlots;
        }

        for (Map.Entry<Integer, RecipeIngredient> entry : this.ingredients.entrySet()) {
            int recipeSlot = entry.getKey();
            RecipeIngredient ingredient = entry.getValue();

            int recipeRow = recipeSlot / 3;
            int recipeCol = recipeSlot % 3;
            int gridRow = recipeRow + startRow;
            int gridCol = (mirrored ? (width - 1) - recipeCol : recipeCol) + startCol;
            int gridSlot = gridRow * 3 + gridCol;

            ItemInstance instance = ingredients.get(gridSlot);
            if (instance != null && ingredient.matches(instance.createItemStack(), itemFactory)) {
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
        private final BaseItem result;
        private final Consumer<ItemInstance> resultCustomizer;
        private final String[] pattern;
        private final Map<Character, RecipeIngredient> ingredients = new HashMap<>();
        private final ItemFactory itemFactory;
        private boolean needsBlueprint = false;

        public Builder(@NotNull BaseItem result, @NotNull String[] pattern, @NotNull ItemFactory itemFactory) {
            this(result, instance -> {}, pattern, itemFactory);
        }

        /**
         * Creates a new builder with a customizer applied to both preview and live results.
         * The customizer must be side-effect-free (e.g. set amount, set custom display) — do not
         * call {@link ItemFactory#create} or anything that triggers persistent state.
         */
        public Builder(@NotNull BaseItem result,
                       @NotNull Consumer<ItemInstance> resultCustomizer,
                       @NotNull String[] pattern,
                       @NotNull ItemFactory itemFactory) {
            this.result = result;
            this.resultCustomizer = resultCustomizer;
            this.pattern = pattern;
            this.itemFactory = itemFactory;

            if (pattern.length > 3) {
                throw new IllegalArgumentException("Pattern cannot have more than 3 rows");
            }

            for (String row : pattern) {
                if (row.length() > 3) {
                    throw new IllegalArgumentException("Pattern rows cannot be longer than 3 characters");
                }
            }
        }

        public Builder setIngredient(char key, @NotNull RecipeIngredient ingredient) {
            ingredients.put(key, ingredient);
            return this;
        }

        public Builder setIngredient(char key, @NotNull Material material, int amount) {
            final BaseItem item = itemFactory.fromItemStack(ItemStack.of(material)).orElseThrow().getBaseItem();
            return setIngredient(key, new RecipeIngredient(item, amount));
        }

        public Builder needsBlueprint() {
            this.needsBlueprint = true;
            return this;
        }

        public ShapedCraftingRecipe build() {
            Map<Integer, RecipeIngredient> recipeIngredients = new HashMap<>();

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

            return new ShapedCraftingRecipe(result, resultCustomizer, recipeIngredients, itemFactory, needsBlueprint);
        }
    }
}
