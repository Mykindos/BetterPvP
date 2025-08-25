package me.mykindos.betterpvp.core.recipe.crafting.resolver;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.resolver.LookupParameter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Matches a {@link me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe} if the player has all ingredients
 * in their inventory.
 */
public class HasIngredientsParameter implements LookupParameter<CraftingRecipe> {

    private final WeakReference<Player> player;
    private final ItemFactory itemFactory;

    public HasIngredientsParameter(Player player, ItemFactory itemFactory) {
        this.player = new WeakReference<>(Objects.requireNonNull(player, "Player must not be null"));
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean test(CraftingRecipe recipe) {
        final Collection<RecipeIngredient> required = new HashMap<>(recipe.getIngredients()).values();
        final Player player = this.player.get();
        if (player == null || required.isEmpty()) {
            return true; // No ingredients required
        }

        final List<ItemInstance> inventory = itemFactory.fromArray(player.getInventory().getStorageContents());
        required.removeIf(ingredient -> {
            final BaseItem baseItem = ingredient.getBaseItem();
            int remainingAmount = ingredient.getAmount();
            if (ingredient.getAmount() <= 0) {
                return true; // Ignore zero amount ingredients
            }

            for (ItemInstance itemInstance : inventory) {
                if (!itemInstance.getBaseItem().equals(baseItem)) {
                    continue; // Not the right type
                }

                remainingAmount -= itemInstance.getItemStack().getAmount();

                if (remainingAmount <= 0) {
                    return true; // Found enough of this ingredient
                }
            }

            return false;
        });

        return required.isEmpty();
    }

    /**
     * Removes the matching items from the given contents array.
     * If this method returns true, the contents array will be modified.
     * If this method returns false, the contents array will not be modified.
     * @param recipe The recipe to match against
     * @param contents The contents to remove matching items from
     * @return True if the contents array was modified, false otherwise
     */
    public final boolean removeMatching(CraftingRecipe recipe, ItemStack[] contents) {
        Preconditions.checkNotNull(contents, "Contents must not be null");
        Preconditions.checkArgument(contents.length > 0, "Contents must not be empty");

        final List<ItemInstance> inventory = itemFactory.fromArray(contents);
        final Collection<RecipeIngredient> ingredients = recipe.getIngredients().values();
        outer:
        for (RecipeIngredient ingredient : ingredients) {
            final BaseItem baseItem = ingredient.getBaseItem();
            int remainingAmount = ingredient.getAmount();
            if (ingredient.getAmount() <= 0) {
                continue; // Ignore zero amount ingredients
            }

            for (int i = 0; i < inventory.size(); i++) {
                final ItemInstance itemInstance = inventory.get(i);
                if (!itemInstance.getBaseItem().equals(baseItem)) {
                    continue; // Not the right type
                }

                final ItemStack itemStack = itemInstance.createItemStack();
                final int foundAmount = itemStack.getAmount();
                remainingAmount -= foundAmount;

                itemStack.setAmount(remainingAmount >= 0 ? 0 : -remainingAmount);
                contents[i] = itemStack;
                if (remainingAmount <= 0) {
                    continue outer;
                }
            }

            return false;
        }

        return true;
    }
}
