package me.mykindos.betterpvp.core.recipe.crafting.resolver;

import com.google.common.base.Preconditions;
import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.resolver.LookupParameter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

        // Map available items to their amounts
        final Map<ItemInstance, Integer> inventory = new HashMap<>();
        final List<ItemInstance> contents = itemFactory.fromArray(player.getInventory().getStorageContents());
        for (ItemInstance itemInstance : contents) {
            if (itemInstance != null) {
                inventory.put(itemInstance, itemInstance.getItemStack().getAmount());
            }
        }

        required.removeIf(ingredient -> {
            final BaseItem baseItem = ingredient.getBaseItem();
            int remainingAmount = ingredient.getAmount();
            if (remainingAmount <= 0) {
                return true; // Ignore zero amount ingredients
            }

            for (int i = 0; i < contents.size(); i++) {
                ItemInstance itemInstance = contents.get(i);
                if (itemInstance == null) {
                    continue; // Skip null items
                }
                int amountFound = inventory.getOrDefault(itemInstance, 0);
                if (amountFound <= 0 || !itemInstance.getBaseItem().equals(baseItem)) {
                    continue; // Not the right type or not enough of this ingredient
                }

                int amountToTake = Math.min(amountFound, remainingAmount);
                amountFound = Math.max(0, amountFound - remainingAmount);
                remainingAmount -= amountToTake;

                if (amountFound == 0) {
                    inventory.remove(itemInstance);
                } else {
                    inventory.replace(itemInstance, amountFound);
                }

                if (remainingAmount <= 0) {
                    return true; // Found enough of this ingredient
                }
            }
            return false;
        });

        return required.isEmpty();
    }

    /**
     * Calculates how many times a recipe can be crafted with the available items.
     * @param recipe The recipe to check against
     * @param contents The inventory contents to check
     * @return The maximum number of times the recipe can be crafted
     */
    public final int getMaxCraftableAmount(CraftingRecipe recipe, ItemStack[] contents) {
        Preconditions.checkNotNull(contents, "Contents must not be null");

        if (contents.length == 0) return 0;

        // copy the contents and iteratively remove items until we run out of ingredients
        final ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                copy[i] = contents[i].clone();
            } else {
                copy[i] = null;
            }
        }

        int amount = 0;
        while (removeMatching(recipe, copy)) {
            amount++;
        }

        // backpropagate the recipes until all stack sizes are respected
        final RecipeIngredient[] recipeContents = recipe.getIngredients().values().toArray(new RecipeIngredient[0]);
        outer:
        while (amount > 0) {
            for (RecipeIngredient ingredient : recipeContents) {
                final ItemStack itemStack = ingredient.getBaseItem().getModel();

                final Integer stackSize = itemStack.getData(DataComponentTypes.MAX_STACK_SIZE);
                if (stackSize != null && amount > stackSize) {
                    amount--;
                    continue outer;
                }
            }

            break;
        }
        return amount;
    }

    /**
     * Removes the matching items from the given contents array for multiple recipe crafts.
     * @param recipe The recipe to match against
     * @param contents The contents to remove matching items from
     * @param craftAmount The number of times to craft the recipe
     * @return True if the items were removed, false otherwise
     */
    public final boolean removeMatchingBulk(CraftingRecipe recipe, ItemStack[] contents, int craftAmount) {
        Preconditions.checkNotNull(contents, "Contents must not be null");
        Preconditions.checkArgument(contents.length > 0, "Contents must not be empty");
        Preconditions.checkArgument(craftAmount > 0, "Craft amount must be positive");

        // copy the contents and iteratively remove items until we run out of ingredients
        final ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                copy[i] = contents[i].clone();
            } else {
                copy[i] = null;
            }
        }

        int amount = 0;
        while (removeMatching(recipe, copy)) {
            amount++;
        }

        if (amount < craftAmount) {
            return false; // Not enough ingredients to craft the desired amount
        }

        // copy from copy to contents
        for (int i = 0; i < copy.length; i++) {
            if (copy[i] != null) {
                contents[i] = copy[i].clone();
            } else {
                contents[i] = null;
            }
        }

        return true;
    }

    /**
     * Removes the matching items from the given contents array.
     * If enough ingredients are found, the contents array will be modified
     * If not enough ingredients are found, the contents array will not be modified
     * @param recipe The recipe to match against
     * @param contents The contents to remove matching items from
     * @return true if the items were removed, false otherwise
     */
    public final boolean removeMatching(CraftingRecipe recipe, ItemStack[] contents) {
        Preconditions.checkNotNull(contents, "Contents must not be null");
        Preconditions.checkArgument(contents.length > 0, "Contents must not be empty");

        final List<ItemInstance> inventory = new ArrayList<>();
        for (ItemStack content : contents) {
            ItemStack toAdd = content == null ? ItemStack.of(Material.AIR) : content;
            inventory.add(itemFactory.fromItemStack(toAdd).orElse(null));
        }

        final Collection<RecipeIngredient> ingredients = recipe.getIngredients().values();
        
        // First check if we have enough ingredients without modifying anything
        final Map<ItemInstance, Integer> inventoryAmounts = new HashMap<>();
        for (ItemInstance itemInstance : inventory) {
            if (itemInstance != null) {
                inventoryAmounts.put(itemInstance, itemInstance.getItemStack().getAmount());
            }
        }
        
        final Collection<RecipeIngredient> required = new ArrayList<>(ingredients);
        final boolean hasEnoughIngredients = required.removeIf(ingredient -> {
            final BaseItem baseItem = ingredient.getBaseItem();
            int remainingAmount = ingredient.getAmount();
            if (remainingAmount <= 0) {
                return true; // Ignore zero amount ingredients
            }

            for (int i = 0; i < inventory.size(); i++) {
                ItemInstance itemInstance = inventory.get(i);
                if (itemInstance == null) {
                    continue; // Skip null items
                }
                int amountFound = inventoryAmounts.getOrDefault(itemInstance, 0);
                if (amountFound <= 0 || !itemInstance.getBaseItem().equals(baseItem)) {
                    continue; // Not the right type or not enough of this ingredient
                }

                int amountToTake = Math.min(amountFound, remainingAmount);
                amountFound = Math.max(0, amountFound - remainingAmount);
                remainingAmount -= amountToTake;

                if (amountFound == 0) {
                    inventoryAmounts.remove(itemInstance);
                } else {
                    inventoryAmounts.replace(itemInstance, amountFound);
                }

                if (remainingAmount <= 0) {
                    return true; // Found enough of this ingredient
                }
            }
            return false;
        }) && required.isEmpty();
        
        if (!hasEnoughIngredients) {
            return false; // Return false if not enough ingredients
        }
        
        // Now actually remove the items and track what was removed
        for (RecipeIngredient ingredient : ingredients) {
            final BaseItem baseItem = ingredient.getBaseItem();
            int remainingAmount = ingredient.getAmount();
            if (ingredient.getAmount() <= 0) {
                continue; // Ignore zero amount ingredients
            }

            for (int i = 0; i < contents.length; i++) {
                if (contents[i] == null) {
                    continue;
                }
                
                final ItemInstance itemInstance = inventory.get(i);
                if (!itemInstance.getBaseItem().equals(baseItem)) {
                    continue; // Not the right type
                }

                final ItemStack itemStack = contents[i];
                final int foundAmount = itemStack.getAmount();
                final int amountToTake = Math.min(foundAmount, remainingAmount);
                
                // Remove the items from contents
                
                remainingAmount -= amountToTake;
                final int newAmount = foundAmount - amountToTake;
                
                if (newAmount <= 0) {
                    contents[i] = null;
                } else {
                    itemStack.setAmount(newAmount);
                }
                
                if (remainingAmount <= 0) {
                    break; // Found enough of this ingredient
                }
            }
        }

        return true; // Successfully removed all required ingredients
    }
}
