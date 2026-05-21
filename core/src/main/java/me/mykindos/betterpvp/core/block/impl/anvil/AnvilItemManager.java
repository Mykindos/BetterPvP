package me.mykindos.betterpvp.core.block.impl.anvil;

import lombok.Getter;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.block.data.LoadHandler;
import me.mykindos.betterpvp.core.block.data.impl.StorageBlockData;
import me.mykindos.betterpvp.core.block.impl.anvil.operation.AnvilOperation;
import me.mykindos.betterpvp.core.block.impl.anvil.operation.AnvilOperationResolver;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import org.bukkit.Bukkit;
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
 * Manages item storage and resolves the active {@link AnvilOperation} (crafting or
 * repair) for an anvil.
 */
@Getter
public class AnvilItemManager implements RemovalHandler, LoadHandler {

    private final AnvilOperationResolver operationResolver;
    private final StorageBlockData anvilItems;
    private AnvilOperation currentOperation = null;

    public AnvilItemManager(@NotNull AnvilOperationResolver operationResolver) {
        this.operationResolver = operationResolver;
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
        resolveOperation();
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
            resolveOperation();
            return removedItem;
        }
        return null;
    }

    /**
     * Updates the items after operation completion, keeping remaining items.
     *
     * @param newItems The items remaining after the operation
     */
    public void updateItemsAfterRecipe(@NotNull List<ItemInstance> newItems) {
        anvilItems.setContent(newItems);
        resolveOperation();
    }

    /**
     * Re-resolves the active operation against the current items.
     */
    private void resolveOperation() {
        currentOperation = null;

        List<ItemInstance> items = anvilItems.getContent();
        if (items.isEmpty()) {
            return;
        }

        Map<Integer, ItemStack> stackMap = new HashMap<>();
        Map<Integer, ItemInstance> instanceMap = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            ItemInstance item = items.get(i);
            if (item != null) {
                stackMap.put(i, item.createItemStack());
                instanceMap.put(i, item);
            }
        }

        operationResolver.resolve(stackMap, instanceMap)
                .ifPresent(operation -> currentOperation = operation);
    }

    /**
     * Gets the items as a map for operation execution.
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

    public boolean hasOperation() {
        return currentOperation != null;
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
