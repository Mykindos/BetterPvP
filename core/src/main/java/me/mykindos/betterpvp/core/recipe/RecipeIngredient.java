package me.mykindos.betterpvp.core.recipe;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Represents an ingredient in a recipe.
 * <p>
 * An ingredient may accept any one of several {@link BaseItem}s — this lets a single
 * recipe slot map onto vanilla item tags (e.g. {@code #minecraft:planks}). The first
 * item inserted is treated as the "primary" and is what menu viewers and recipe
 * resolvers see via {@link #getBaseItem()}.
 */
@Getter
public class RecipeIngredient {

    private final Set<BaseItem> baseItems;
    private final int amount;
    private boolean consumeOnCraft = true;

    public RecipeIngredient(@NotNull BaseItem baseItem, int amount) {
        this(Set.of(baseItem), amount);
    }

    public RecipeIngredient(@NotNull BaseItem baseItem, int amount, boolean consumeOnCraft) {
        this(Set.of(baseItem), amount, consumeOnCraft);
    }

    public RecipeIngredient(@NotNull Collection<BaseItem> baseItems, int amount) {
        if (baseItems.isEmpty()) {
            throw new IllegalArgumentException("RecipeIngredient requires at least one BaseItem");
        }
        this.baseItems = new LinkedHashSet<>(baseItems);
        this.amount = amount;
    }

    public RecipeIngredient(@NotNull Collection<BaseItem> baseItems, int amount, boolean consumeOnCraft) {
        this(baseItems, amount);
        this.consumeOnCraft = consumeOnCraft;
    }

    /**
     * @return the primary (first-inserted) base item — used for icons and equality-based lookups.
     */
    public @NotNull BaseItem getBaseItem() {
        return baseItems.iterator().next();
    }

    /**
     * @return {@code true} if this ingredient accepts the given base item.
     */
    public boolean accepts(@NotNull BaseItem baseItem) {
        return baseItems.contains(baseItem);
    }

    /**
     * Checks if the provided ItemStack matches this ingredient (sufficient amount and an accepted base item).
     */
    public boolean matches(@NotNull ItemStack stack, @NotNull ItemFactory itemFactory) {
        if (stack.getAmount() < amount) {
            return false;
        }

        Optional<ItemInstance> instance = itemFactory.fromItemStack(stack);
        return instance.map(i -> accepts(i.getBaseItem())).orElse(false);
    }
}
