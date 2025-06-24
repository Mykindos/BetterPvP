package me.mykindos.betterpvp.core.block;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import org.bukkit.block.Block;

/**
 * Represents an instance of a {@link SmartBlock} in the game.
 */
@Getter
@EqualsAndHashCode
public final class SmartBlockInstance {

    private final SmartBlock type;
    private final Block handle;
    private final SmartBlockDataManager dataManager;

    public SmartBlockInstance(SmartBlock type, Block handle, SmartBlockDataManager dataManager) {
        this.type = type;
        this.handle = handle;
        this.dataManager = dataManager;
    }

    /**
     * Gets the block data for this instance, if the block supports data.
     * Initializes with default data if not yet present.
     * The data type is automatically inferred from the SmartBlock's defined type.
     * 
     * @return Optional containing the block data if the block supports data
     */
    public <T> SmartBlockData<T> getBlockData() {
        return dataManager.getOrCreateData(this);
    }

    /**
     * Gets read-only access to the block data.
     * This method returns the data object if it exists, or null if the block does not support data.
     * Changes to the data will not be saved automatically.
     * @return the data object, or null if not available
     * @param <T> the type of data stored in the block
     */
    public <T> T getData() {
        SmartBlockData<T> data = getBlockData();
        return data != null ? data.get() : null;
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
