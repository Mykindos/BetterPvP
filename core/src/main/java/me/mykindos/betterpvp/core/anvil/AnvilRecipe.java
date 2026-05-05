package me.mykindos.betterpvp.core.anvil;

import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.access.ItemAccessService;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import net.kyori.adventure.key.Key;
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
import java.util.Set;

/**
 * An anvil recipe that requires specific items and a number of hammer swings to produce results.
 * Unlike crafting recipes, anvil recipes are shapeless and require hammer swing progression.
 */
@Getter
public class AnvilRecipe implements Recipe<AnvilRecipeResult> {

    private final @NotNull Map<BaseItem, Integer> ingredients;
    private final @NotNull BaseItem primaryBaseItem;
    private final @NotNull List<BaseItem> secondaryBaseItems;
    private final int hammerSwings;
    private final @NotNull ItemFactory itemFactory;

    @Nullable
    private NamespacedKey recipeKey;

    /**
     * Creates a new anvil recipe with primary and secondary results.
     */
    public AnvilRecipe(@NotNull Map<BaseItem, Integer> ingredients,
                       @NotNull BaseItem primaryResult,
                       @NotNull List<BaseItem> secondaryResults,
                       int hammerSwings,
                       @NotNull ItemFactory itemFactory) {
        this.ingredients = new HashMap<>(ingredients);
        this.primaryBaseItem = primaryResult;
        this.secondaryBaseItems = List.copyOf(secondaryResults);
        this.hammerSwings = hammerSwings;
        this.itemFactory = itemFactory;
    }

    /**
     * Creates a new anvil recipe with only a primary result.
     */
    public AnvilRecipe(@NotNull Map<BaseItem, Integer> ingredients,
                       @NotNull BaseItem primaryResult,
                       int hammerSwings,
                       @NotNull ItemFactory itemFactory) {
        this(ingredients, primaryResult, List.of(), hammerSwings, itemFactory);
    }

    @Override
    public @NotNull AnvilRecipeResult previewResult() {
        return new AnvilRecipeResult(itemFactory.createPreview(primaryBaseItem), secondaryBaseItems);
    }

    @Override
    public @NotNull AnvilRecipeResult createResult() {
        return new AnvilRecipeResult(itemFactory.create(primaryBaseItem), secondaryBaseItems);
    }

    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
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

        for (Map.Entry<BaseItem, Integer> entry : ingredients.entrySet()) {
            BaseItem baseItem = entry.getKey();
            int requiredAmount = entry.getValue();

            int availableAmount = availableIngredients.getOrDefault(baseItem, 0);
            if (availableAmount < requiredAmount) {
                return false;
            }
        }

        for (BaseItem availableItem : availableIngredients.keySet()) {
            if (!ingredients.containsKey(availableItem)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @NotNull Map<Integer, RecipeIngredient> getIngredients() {
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

        Map<BaseItem, Integer> remainingIngredients = new HashMap<>(this.ingredients);

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

    /** Called by {@link AnvilRecipeRegistry} to store the key after registration. */
    public void setRecipeKey(@NotNull NamespacedKey key) {
        this.recipeKey = key;
    }

    @Override
    public boolean canCraft(@Nullable Player player) {
        if (player == null || recipeKey == null) return true;
        try {
            ItemAccessService service = JavaPlugin.getPlugin(Core.class)
                    .getInjector().getInstance(ItemAccessService.class);
            Key key = Key.key(recipeKey.namespace(), recipeKey.getKey());
            return service.isAllowed(player, primaryBaseItem, key, AccessScope.CRAFT);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Gets the ingredient types used in this recipe (ignoring quantities).
     * Used for duplicate recipe detection.
     */
    public @NotNull Set<BaseItem> getIngredientTypes() {
        return ingredients.keySet();
    }

    /**
     * Gets the number of hammer swings required to complete this recipe.
     */
    public int getRequiredHammerSwings() {
        return hammerSwings;
    }
}
