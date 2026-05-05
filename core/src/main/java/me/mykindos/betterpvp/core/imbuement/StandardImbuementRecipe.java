package me.mykindos.betterpvp.core.imbuement;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard imbuement recipe that requires exact ingredients to produce results.
 * This is the traditional recipe type that matches ingredients exactly with no residuals.
 */
@Getter
public class StandardImbuementRecipe extends ImbuementRecipe {

    private final @NotNull Map<BaseItem, Integer> ingredients;
    private final @NotNull BaseItem primaryBaseItem;
    private final @NotNull List<BaseItem> secondaryBaseItems;

    public StandardImbuementRecipe(@NotNull Map<BaseItem, Integer> ingredients,
                                   @NotNull BaseItem primaryResult,
                                   @NotNull List<BaseItem> secondaryResults,
                                   @NotNull ItemFactory itemFactory) {
        super(itemFactory);
        this.ingredients = new HashMap<>(ingredients);
        this.primaryBaseItem = primaryResult;
        this.secondaryBaseItems = List.copyOf(secondaryResults);
    }

    public StandardImbuementRecipe(@NotNull Map<BaseItem, Integer> ingredients,
                                   @NotNull BaseItem primaryResult,
                                   @NotNull ItemFactory itemFactory) {
        this(ingredients, primaryResult, List.of(), itemFactory);
    }

    @Override
    public @NotNull ImbuementRecipeResult previewResult() {
        return new ImbuementRecipeResult(itemFactory.createPreview(primaryBaseItem), secondaryBaseItems);
    }

    @Override
    public @NotNull ImbuementRecipeResult createResult() {
        return new ImbuementRecipeResult(itemFactory.create(primaryBaseItem), secondaryBaseItems);
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

        if (availableIngredients.size() != ingredients.size()) {
            return false;
        }

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
        Map<Integer, RecipeIngredient> recipeIngredients = new HashMap<>();
        int index = 0;
        for (Map.Entry<BaseItem, Integer> entry : ingredients.entrySet()) {
            recipeIngredients.put(index++, new RecipeIngredient(entry.getKey(), entry.getValue()));
        }
        return recipeIngredients;
    }
}
