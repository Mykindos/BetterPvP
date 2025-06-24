package me.mykindos.betterpvp.core.block.data;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for serializing and deserializing complex block data to/from PDC.
 * Each SmartBlock can define its own serializer for custom data structures.
 */
public interface SmartBlockDataSerializer<T> {
    
    /**
     * Gets the namespaced key used to identify this data type in storage.
     * @return The namespaced key
     */
    @NotNull NamespacedKey getKey();
    
    /**
     * Gets the data type this serializer handles.
     * @return The data class
     */
    @NotNull Class<T> getType();
    
    /**
     * Serializes the data object to the persistent data container.
     * The implementation should store data using individual PDC key-value entries.
     * 
     * @param data The data object to serialize
     * @param container The container to serialize to
     */
    void serialize(@NotNull T data, @NotNull PersistentDataContainer container);
    
    /**
     * Deserializes data from the persistent data container.
     * 
     * @param container The container to deserialize from
     * @return The deserialized data object
     */
    @NotNull T deserialize(@NotNull PersistentDataContainer container);
    
    /**
     * Checks if the container has data for this serializer.
     * 
     * @param container The container to check
     * @return True if the data is present
     */
    boolean hasData(@NotNull PersistentDataContainer container);
} 