package me.mykindos.betterpvp.core.block.impl.imbuement;

import lombok.Getter;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.block.data.LoadHandler;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipe;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipeRegistry;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages item storage and recipe matching for ImbuementPedestal
 */
@Getter
public class ImbuementPedestalItemManager implements RemovalHandler, LoadHandler {

    private final ImbuementRecipeRegistry imbuementRecipeRegistry;
    private final StorageBlockData pedestalItems;
    private ImbuementRecipe currentRecipe = null;

    public ImbuementPedestalItemManager(@NotNull ImbuementRecipeRegistry imbuementRecipeRegistry) {
        this.imbuementRecipeRegistry = imbuementRecipeRegistry;
        this.pedestalItems = new StorageBlockData(ImbuementPedestalConstants.MAX_PEDESTAL_ITEMS);
    }

    /**
     * Adds an item to the pedestal.
     *
     * @param item The item to add
     * @return true if the item was added successfully
     */
    public boolean addItem(@NotNull ItemInstance item) {
        List<ItemInstance> currentItems = pedestalItems.getContent();
        if (isFull()) {
            return false;
        }

        currentItems.add(item);
        pedestalItems.setContent(currentItems);
        checkForRecipe();
        return true;
    }

    /**
     * Removes the last item from the pedestal.
     *
     * @return The removed item, or null if the pedestal is empty
     */
    @Nullable
    public ItemInstance removeLastItem() {
        List<ItemInstance> currentItems = pedestalItems.getContent();
        if (currentItems.isEmpty()) {
            return null;
        }

        ItemInstance removedItem = currentItems.removeLast();
        pedestalItems.setContent(currentItems);
        checkForRecipe();
        return removedItem;
    }

    /**
     * Clears all items from the pedestal.
     */
    public void clearAllItems() {
        pedestalItems.setContent(new ArrayList<>());
        currentRecipe = null;
    }

    /**
     * Checks for a matching recipe with current items.
     */
    private void checkForRecipe() {
        currentRecipe = null;

        if (pedestalItems.getContent().isEmpty()) {
            return;
        }

        // Convert items to the format expected by recipe matching
        Map<Integer, ItemStack> itemMap = new HashMap<>();
        List<ItemInstance> items = pedestalItems.getContent();
        for (int i = 0; i < items.size(); i++) {
            ItemInstance item = items.get(i);
            if (item != null) {
                itemMap.put(i, item.createItemStack());
            }
        }

        // Find a matching recipe through the registry
        Optional<ImbuementRecipe> recipeOpt = imbuementRecipeRegistry.findRecipe(itemMap);
        recipeOpt.ifPresent(recipe -> currentRecipe = recipe);
    }

    // Utility methods
    public int getItemCount() {
        return pedestalItems.getContent().size();
    }

    public boolean isFull() {
        return getItemCount() >= pedestalItems.maxSize();
    }

    public boolean hasItems() {
        return !pedestalItems.getContent().isEmpty();
    }

    public boolean hasRecipe() {
        return currentRecipe != null;
    }

    public List<ItemInstance> getItems() {
        return new ArrayList<>(pedestalItems.getContent());
    }

    @Override
    public void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        pedestalItems.onRemoval(instance, cause);
    }

    @Override
    public void onUnload(@NotNull SmartBlockInstance instance) {
        if (pedestalItems instanceof LoadHandler loadHandler) {
            loadHandler.onUnload(instance);
        }
    }
} 