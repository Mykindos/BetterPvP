package me.mykindos.betterpvp.core.block.data;

import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Container for SmartBlock data instances. Each SmartBlockInstance with data
 * will have an associated SmartBlockData containing the actual data object.
 */
public final class SmartBlockData<T> {

    private final transient SmartBlockInstance blockInstance;
    private final Class<T> dataType;
    private final transient SmartBlockDataManager dataManager;
    private T data;
    private transient long lastAccessed;
    
    public SmartBlockData(@NotNull SmartBlockInstance blockInstance,
                   @NotNull Class<T> dataType,
                   @NotNull T initialData,
                   @NotNull SmartBlockDataManager dataManager) {
        this.blockInstance = blockInstance;
        this.dataType = dataType;
        this.data = initialData;
        this.dataManager = dataManager;
        this.lastAccessed = System.currentTimeMillis();
    }
    
    /**
     * Gets the current data object and updates the last accessed time.
     * @return The data object
     */
    @NotNull
    public T get() {
        this.lastAccessed = System.currentTimeMillis();
        return data;
    }
    
    /**
     * Sets new data and automatically saves to PDC.
     * @param newData The new data to set
     */
    public void set(@NotNull T newData) {
        this.data = newData;
        this.lastAccessed = System.currentTimeMillis();
        save();
    }
    
    /**
     * Updates the data using a mutator function and automatically saves to PDC.
     * @param mutator Function that modifies the data
     */
    public void update(@NotNull Consumer<T> mutator) {
        mutator.accept(data);
        this.lastAccessed = System.currentTimeMillis();
        save();
    }

    /**
     * Gets the block instance this data belongs to.
     * @return The block instance
     */
    @NotNull
    public SmartBlockInstance getBlockInstance() {
        return blockInstance;
    }
    
    /**
     * Gets the data type class.
     * @return The data type class
     */
    @NotNull
    public Class<T> getDataType() {
        return dataType;
    }
    
    /**
     * Gets the last accessed timestamp.
     * @return Last accessed time in milliseconds
     */
    public long getLastAccessed() {
        return lastAccessed;
    }
    
    /**
     * Saves the current data to PDC.
     */
    private void save() {
        dataManager.save(this);
    }
} 