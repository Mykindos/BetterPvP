package me.mykindos.betterpvp.core.metal.casting;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
public class CastingMoldRecipe implements Recipe<BaseItem, ItemInstance> {
    
    private final @NotNull CastingMold baseMold;
    private final int requiredMillibuckets;
    private final @NotNull Alloy alloy;
    private final @NotNull BaseItem result;
    private final ItemFactory itemFactory;

    /**
     * Creates a new casting mold recipe.
     * @param baseMold The base casting mold that can be filled
     * @param requiredMillibuckets The amount of liquid alloy required to fill this mold
     * @param alloy The alloy type that can be used to fill this mold
     * @param result The filled casting mold that results from using this alloy
     */
    public CastingMoldRecipe(@NotNull CastingMold baseMold, int requiredMillibuckets, @NotNull Alloy alloy, @NotNull BaseItem result, ItemFactory itemFactory) {
        this.baseMold = baseMold;
        this.requiredMillibuckets = requiredMillibuckets;
        this.alloy = alloy;
        this.result = result;
        this.itemFactory = itemFactory;
    }
    
    /**
     * Checks if the given alloy can be used to fill this mold.
     * @param alloy The alloy to check
     * @return true if the alloy is accepted, false otherwise
     */
    public boolean acceptsAlloy(@NotNull Alloy alloy) {
        return this.alloy.equals(alloy);
    }
    
    /**
     * Checks if this recipe can be used with the given mold.
     * @param mold The mold to check
     * @return true if the mold matches this recipe's base mold
     */
    public boolean matches(@NotNull BaseItem mold) {
        return baseMold.equals(mold);
    }

    @Override
    public @NotNull BaseItem getPrimaryResult() {
        return result;
    }

    @Override
    public @NotNull ItemInstance createPrimaryResult() {
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
                continue; // notihng we can do with this item
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
        return List.of(); // No ingredients are consumed in this recipe
    }

    @Override
    public @NotNull RecipeType getType() {
        return RecipeType.SMELTING;
    }
}