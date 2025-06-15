package me.mykindos.betterpvp.core.recipe.crafting;

import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of a crafting operation.
 * This includes the primary result item, any additional results,
 * and the new crafting matrix that should be used after the crafting operation.
 */
public record CraftingResult(ItemInstance result, List<ItemInstance> additionalResults, Map<Integer, ItemInstance> newCraftingMatrix) {
}
