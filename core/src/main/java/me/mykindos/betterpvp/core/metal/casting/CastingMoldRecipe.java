package me.mykindos.betterpvp.core.metal.casting;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import me.mykindos.betterpvp.core.recipe.smelting.Alloy;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a casting mold recipe that defines which alloys can be used to fill
 * a specific casting mold type and what the resulting filled mold should be.
 */
@Getter
public class CastingMoldRecipe implements Recipe<ItemInstance> {

    private final @NotNull CastingMold baseMold;
    private final int requiredMillibuckets;
    private final @NotNull Alloy alloy;
    private final @NotNull BaseItem result;
    private final ItemFactory itemFactory;

    public CastingMoldRecipe(@NotNull CastingMold baseMold, int requiredMillibuckets, @NotNull Alloy alloy, @NotNull BaseItem result, ItemFactory itemFactory) {
        this.baseMold = baseMold;
        this.requiredMillibuckets = requiredMillibuckets;
        this.alloy = alloy;
        this.result = result;
        this.itemFactory = itemFactory;
    }

    public boolean acceptsAlloy(@NotNull Alloy alloy) {
        return this.alloy.equals(alloy);
    }

    public boolean matches(@NotNull BaseItem mold) {
        return baseMold.equals(mold);
    }

    @Override
    public @NotNull ItemInstance previewResult() {
        return itemFactory.createPreview(result);
    }

    @Override
    public @NotNull ItemInstance createResult() {
        return itemFactory.create(result);
    }

    @Override
    public boolean matches(@NotNull Map<Integer, ItemStack> items) {
        final HashMap<RecipeIngredient, Integer> ingredients = new HashMap<>();
        for (RecipeIngredient value : getIngredients().values()) {
            ingredients.merge(value, value.getAmount(), Integer::sum);
        }

        for (ItemStack value : items.values()) {
            int amount = value.getAmount();
            final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(value);
            if (instanceOpt.isEmpty()) {
                continue;
            }

            final ItemInstance instance = instanceOpt.get();
            final BaseItem type = instance.getBaseItem();
            final Iterator<RecipeIngredient> iterator = ingredients.keySet().iterator();
            while (iterator.hasNext()) {
                final RecipeIngredient ingredient = iterator.next();
                if (ingredient.getBaseItem().equals(type)) {
                    int required = ingredients.get(ingredient);
                    required -= amount;
                    if (required <= 0) {
                        iterator.remove();
                    } else {
                        ingredients.put(ingredient, required);
                    }
                    break;
                }
            }
        }

        return ingredients.isEmpty();
    }

    @Override
    public @NotNull Map<Integer, RecipeIngredient> getIngredients() {
        return Map.of(
                0, new RecipeIngredient(baseMold, 1)
        );
    }

    @Override
    public @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> ingredients, @NotNull ItemFactory itemFactory) {
        return List.of();
    }

    @Override
    public @NotNull RecipeType getType() {
        return RecipeType.SMELTING;
    }
}
