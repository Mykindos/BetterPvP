package me.mykindos.betterpvp.core.block.impl.anvil;

import lombok.Getter;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.anvil.AnvilRecipeRegistry;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.block.data.LoadHandler;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages item storage and recipe matching for Anvil
 */
@Getter
public class AnvilItemManager implements RemovalHandler, LoadHandler {

    private final AnvilRecipeRegistry anvilRecipeRegistry;
    private final StorageBlockData anvilItems;
    private AnvilRecipe currentRecipe = null;

    public AnvilItemManager(@NotNull AnvilRecipeRegistry anvilRecipeRegistry) {
        this.anvilRecipeRegistry = anvilRecipeRegistry;
        this.anvilItems = new StorageBlockData(AnvilConstants.MAX_ANVIL_ITEMS);
    }

    /**
     * Adds an item to the anvil.
     *
     * @param item The item to add
     * @return true if the item was added successfully
     */
    public boolean addItem(@NotNull ItemInstance item) {
        List<ItemInstance> currentItems = anvilItems.getContent();
        if (isFull()) {
            return false;
        }

        currentItems.add(item);
        anvilItems.setContent(currentItems);
        checkForRecipe();
        return true;
    }

    /**
     * Removes the last item from the anvil.
     *
     * @return The removed item, or null if the anvil is empty
     */
    @Nullable
    public ItemInstance removeLastItem() {
        List<ItemInstance> currentItems = anvilItems.getContent();
        if (currentItems.isEmpty()) {
            return null;
        }

        ItemInstance removedItem = currentItems.removeLast();
        if (removedItem != null) {
            anvilItems.setContent(currentItems);
            checkForRecipe();
            return removedItem;
        }
        return null;
    }

    /**
     * Updates the items after recipe execution, keeping remaining items.
     *
     * @param newItems The items remaining after recipe execution
     */
    public void updateItemsAfterRecipe(@NotNull List<ItemInstance> newItems) {
        anvilItems.setContent(newItems);
        checkForRecipe();
    }

    /**
     * Checks for a matching recipe with current items.
     */
    private void checkForRecipe() {
        currentRecipe = null;

        if (anvilItems.getContent().isEmpty()) {
            return;
        }

        // Convert items to the format expected by recipe matching
        Map<Integer, ItemStack> itemMap = new HashMap<>();
        List<ItemInstance> items = anvilItems.getContent();
        for (int i = 0; i < items.size(); i++) {
            ItemInstance item = items.get(i);
            if (item != null) {
                itemMap.put(i, item.createItemStack());
            }
        }

        // Find a matching recipe through the registry
        Optional<AnvilRecipe> recipeOpt = anvilRecipeRegistry.findRecipe(itemMap);
        recipeOpt.ifPresent(recipe -> currentRecipe = recipe);
    }

    /**
     * Gets the items as a map for recipe execution.
     */
    public Map<Integer, ItemInstance> getItemsAsMap() {
        Map<Integer, ItemInstance> itemInstanceMap = new HashMap<>();
        List<ItemInstance> items = anvilItems.getContent();
        for (int i = 0; i < items.size(); i++) {
            ItemInstance item = items.get(i);
            if (item != null) {
                itemInstanceMap.put(i, item);
            }
        }
        return itemInstanceMap;
    }

    // Utility methods
    public int getItemCount() {
        return anvilItems.getContent().stream().filter(Objects::nonNull).toList().size();
    }

    public boolean isFull() {
        return getItemCount() >= anvilItems.maxSize();
    }

    public boolean hasItems() {
        return getItemCount() > 0;
    }

    public boolean hasRecipe() {
        return currentRecipe != null;
    }

    public List<ItemInstance> getItems() {
        return new ArrayList<>(anvilItems.getContent());
    }

    @Override
    public void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        anvilItems.onRemoval(instance, cause);
    }

    @Override
    public void onUnload(@NotNull SmartBlockInstance instance) {
        if (anvilItems instanceof LoadHandler loadHandler) {
            loadHandler.onUnload(instance);
        }
    }
} 