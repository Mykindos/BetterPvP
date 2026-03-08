package me.mykindos.betterpvp.core.block;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.concurrent.CompletableFuture;

/**
 * Represents an instance of a {@link SmartBlock} in the game.
 */
@Getter
@EqualsAndHashCode
public final class SmartBlockInstance {

    private final SmartBlock type;
    private final Location location;
    private final SmartBlockDataManager dataManager;

    public SmartBlockInstance(SmartBlock type, Location location, SmartBlockDataManager dataManager) {
        this.type = type;
        this.location = location.clone();
        this.dataManager = dataManager;
    }

    public SmartBlockInstance(SmartBlock type, Block handle, SmartBlockDataManager dataManager) {
        this.type = type;
        this.location = handle.getLocation();
        this.dataManager = dataManager;
    }

    public Block getHandle() {
        return location.getBlock();
    }

    public Location getLocation() {
        return location.clone();
    }

    /**
     * Gets the block data for this instance synchronously.
     * Initializes with default data if not yet present.
     * The data type is automatically inferred from the SmartBlock's defined type.
     * 
     * <p><strong>WARNING:</strong> This method blocks the current thread until data is loaded.
     * Avoid calling on the main thread with slow storage implementations (e.g., database storage).
     * Consider using {@link #getBlockDataAsync()} for non-blocking operations.</p>
     * 
     * @return the block data if the block supports data, null otherwise
     * @param <T> the type of data stored in the block
     */
    @SuppressWarnings("unchecked")
    public <T> SmartBlockData<T> getBlockData() {
        return (SmartBlockData<T>) getBlockDataAsync().join();
    }

    /**
     * Gets the block data for this instance asynchronously.
     * Initializes with default data if not yet present.
     * The data type is automatically inferred from the SmartBlock's defined type.
     * 
     * <p>This is the preferred method for non-blocking data access.</p>
     * 
     * @return CompletableFuture containing the block data if the block supports data, or null
     * @param <T> the type of data stored in the block
     */
    public <T> CompletableFuture<SmartBlockData<T>> getBlockDataAsync() {
        return dataManager.getProvider().getOrCreateData(this);
    }

    /**
     * Gets read-only access to the block data synchronously.
     * This method returns the data object if it exists, or null if the block does not support data.
     * Changes to the data will not be saved automatically.
     * 
     * <p><strong>WARNING:</strong> This method blocks the current thread until data is loaded.
     * Avoid calling on the main thread with slow storage implementations.</p>
     * 
     * @return the data object, or null if not available
     * @param <T> the type of data stored in the block
     */
    public <T> T getData() {
        SmartBlockData<T> data = getBlockData();
        return data != null ? data.get() : null;
    }

    /**
     * Gets read-only access to the block data asynchronously.
     * This method returns the data object if it exists, or null if the block does not support data.
     * Changes to the data will not be saved automatically.
     * 
     * <p>This is the preferred method for non-blocking data access.</p>
     * 
     * @return CompletableFuture containing the data object, or null if not available
     * @param <T> the type of data stored in the block
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> getDataAsync() {
        return (CompletableFuture<T>) getBlockDataAsync().thenApply(data -> data != null ? data.get() : null);
    }

    /**
     * Checks if this block type supports data storage.
     * 
     * @return true if the block supports data, false otherwise
     */
    public boolean supportsData() {
        return type instanceof DataHolder;
    }
}
