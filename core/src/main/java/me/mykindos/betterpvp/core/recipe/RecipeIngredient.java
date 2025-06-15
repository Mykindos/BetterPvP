package me.mykindos.betterpvp.core.recipe;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents an ingredient in a recipe, including the base item and amount required.
 */
@Getter
public class RecipeIngredient {
    
    private final BaseItem baseItem;
    private final int amount;
    
    /**
     * Creates a new recipe ingredient.
     * @param baseItem The base item required
     * @param amount The amount required
     */
    public RecipeIngredient(@NotNull BaseItem baseItem, int amount) {
        this.baseItem = baseItem;
        this.amount = amount;
    }
    
    /**
     * Checks if the provided ItemStack matches this ingredient.
     * @param stack The ItemStack to check
     * @param itemFactory The ItemFactory to use for conversion
     * @return true if the ItemStack matches this ingredient, false otherwise
     */
    public boolean matches(@NotNull ItemStack stack, @NotNull ItemFactory itemFactory) {
        if (stack.getAmount() < amount) {
            return false;
        }
        
        Optional<ItemInstance> instance = itemFactory.fromItemStack(stack);
        return instance.map(i -> i.getBaseItem().equals(baseItem)).orElse(false);
    }
} 