package me.mykindos.betterpvp.core.block.data.storage;

import me.mykindos.betterpvp.core.item.ItemInstance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Block data for storage functionality.
 * Replaces the old StorageBehavior system.
 */
public class StorageBlockData {
    
    private List<ItemInstance> content;
    
    public StorageBlockData() {
        this.content = new CopyOnWriteArrayList<>();
    }
    
    public StorageBlockData(List<ItemInstance> content) {
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
        return content.size();
    }
    
    /**
     * Checks if the storage is empty.
     * @return true if empty
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }
} 