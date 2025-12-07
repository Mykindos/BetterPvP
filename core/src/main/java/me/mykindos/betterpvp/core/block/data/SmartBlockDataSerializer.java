package me.mykindos.betterpvp.core.block.data;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Interface for serializing and deserializing complex block data to/from bytes.
 * Each SmartBlock can define its own serializer for custom data structures.
 */
public interface SmartBlockDataSerializer<T> {

    /**
     * Gets the data type this serializer handles.
     * @return The data class
     */
    @NotNull Class<T> getType();
    
    /**
     * Serializes the data object directly to bytes for storage.
     * This method should be optimized for performance and storage efficiency.
     * 
     * @param data The data object to serialize
     * @return The serialized data as bytes
     * @throws IOException if serialization fails
     */
    byte[] serializeToBytes(@NotNull T data) throws IOException;
    
    /**
     * Deserializes data directly from bytes for storage.
     * This method should be optimized for performance.
     * 
     * @param bytes The serialized data bytes
     * @return The deserialized data object
     * @throws IOException if deserialization fails
     */
    @NotNull T deserializeFromBytes(byte[] bytes) throws IOException;
} 