package me.mykindos.betterpvp.core.block.data.impl;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.RemovalHandler;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Block data for storage functionality.
 * Replaces the old StorageBehavior system.
 */
@CustomLog
public class StorageBlockData implements RemovalHandler {
    
    private List<ItemInstance> content;
    private int size = -1; // Default size, can be overridden by subclasses if needed
    
    public StorageBlockData(int size) {
        Preconditions.checkArgument(size > 0, "Size must be greater than 0");
        this.content = new CopyOnWriteArrayList<>();
        this.size = size;
    }
    
    public StorageBlockData(List<ItemInstance> content, int size) {
        Preconditions.checkArgument(size > 0, "Size must be greater than 0");
        Preconditions.checkArgument(content.size() <= size, "Content size must not exceed the defined size");
        this.content = new CopyOnWriteArrayList<>(content);
        this.size = size;
    }

    public StorageBlockData() {
        this.content = new CopyOnWriteArrayList<>();
    }

    public StorageBlockData(List<ItemInstance> content) {
        Preconditions.checkNotNull(content, "Content cannot be null");
        this.content = new CopyOnWriteArrayList<>(content);
    }
    
    /**
     * Gets the storage content as a copy.
     * @return Copy of the storage content
     */
    public List<ItemInstance> getContent() {
        return new CopyOnWriteArrayList<>(content);
    }
    
    /**
     * Sets the storage content.
     * @param content The new content
     */
    public void setContent(List<ItemInstance> content) {
        this.content = new CopyOnWriteArrayList<>(content);
    }
    
    /**
     * Adds an item to the storage.
     * @param item The item to add
     */
    public void addItem(ItemInstance item) {
        content.add(item);
    }
    
    /**
     * Removes an item from the storage.
     * @param item The item to remove
     * @return true if the item was removed
     */
    public boolean removeItem(ItemInstance item) {
        return content.remove(item);
    }
    
    /**
     * Clears all content.
     */
    public void clear() {
        content.clear();
    }
    
    /**
     * Gets the number of items in storage.
     * @return The size
     */
    public int size() {
        return size == -1 ? content.size() : size;
    }

    /**
     * Gets the maximum size of the storage.
     * @return The maximum size or -1 if not defined
     */
    public int maxSize() {
        return size;
    }
    
    /**
     * Checks if the storage is empty.
     * @return true if empty
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }
    
    @Override
    public void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        final World world = instance.getHandle().getWorld();
        final Location dropLocation = instance.getHandle().getLocation().toCenterLocation();

        // Only drop items on natural removal (player breaking, explosion, etc.)
        // Don't drop items on forced removal
        if (cause == BlockRemovalCause.NATURAL) {
            // Drop all items at the block location
            for (ItemInstance item : content) {
                ItemStack itemStack = item.createItemStack();
                world.dropItemNaturally(dropLocation, itemStack);
            }
        }
        
        // Clear the content regardless of cause
        clear();
    }
} 