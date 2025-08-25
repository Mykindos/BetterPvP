package me.mykindos.betterpvp.core.block.data.manager;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for handling serialization and deserialization of SmartBlock data.
 * Centralizes serialization logic and provides both sync and async options.
 */
@Singleton
@CustomLog
public class SmartBlockDataSerializationService {

    /**
     * Serializes data for a SmartBlock instance asynchronously.
     *
     * @param instance the block instance
     * @param data the data to serialize
     * @return CompletableFuture containing the serialized byte array
     * @param <T> the data type
     */
    public <T> CompletableFuture<byte[]> serialize(@NotNull SmartBlockInstance instance, @NotNull T data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return serializeSync(instance, data);
            } catch (Exception e) {
                log.error("Failed to serialize SmartBlock data for instance {}", instance.getHandle().getLocation(), e).submit();
                throw new RuntimeException("Serialization failed", e);
            }
        });
    }

    /**
     * Deserializes data for a SmartBlock instance asynchronously.
     *
     * @param instance the block instance
     * @param dataType the expected data type class
     * @param serializedData the serialized byte data
     * @return CompletableFuture containing the deserialized data object
     * @param <T> the data type
     */
    public <T> CompletableFuture<T> deserialize(@NotNull SmartBlockInstance instance,
                                                @NotNull Class<T> dataType,
                                                byte[] serializedData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return deserializeSync(instance, dataType, serializedData);
            } catch (Exception e) {
                log.error("Failed to deserialize SmartBlock data for instance {}", instance.getHandle().getLocation(), e).submit();
                throw new RuntimeException("Deserialization failed", e);
            }
        });
    }

    /**
     * Serializes data for a SmartBlock instance synchronously.
     * Use sparingly and avoid on main thread for better performance.
     *
     * @param instance the block instance
     * @param data the data to serialize
     * @return the serialized byte array
     * @param <T> the data type
     */
    @SuppressWarnings("unchecked")
    public <T> byte[] serializeSync(@NotNull SmartBlockInstance instance, @NotNull T data) {
        if (!(instance.getType() instanceof DataHolder)) {
            throw new IllegalArgumentException("Instance must be a DataHolder to serialize data");
        }

        try {
            DataHolder<T> dataHolder = (DataHolder<T>) instance.getType();
            SmartBlockDataSerializer<T> serializer = dataHolder.getDataSerializer();
            return serializer.serializeToBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize data for instance " + instance.getHandle().getLocation(), e);
        }
    }

    /**
     * Deserializes data for a SmartBlock instance synchronously.
     * Use sparingly and avoid on main thread for better performance.
     *
     * @param instance the block instance
     * @param dataType the expected data type class
     * @param serializedData the serialized byte data
     * @return the deserialized data object
     * @param <T> the data type
     */
    @SuppressWarnings("unchecked")
    public <T> T deserializeSync(@NotNull SmartBlockInstance instance,
                                 @NotNull Class<T> dataType,
                                 byte[] serializedData) {
        if (!(instance.getType() instanceof DataHolder)) {
            throw new IllegalArgumentException("Instance must be a DataHolder to deserialize data");
        }

        try {
            DataHolder<T> dataHolder = (DataHolder<T>) instance.getType();
            SmartBlockDataSerializer<T> serializer = dataHolder.getDataSerializer();
            return serializer.deserializeFromBytes(serializedData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize data for instance " + instance.getHandle().getLocation(), e);
        }
    }

    /**
     * Validates that an instance supports data storage.
     *
     * @param instance the block instance to validate
     * @return true if the instance supports data storage
     */
    public boolean supportsDataStorage(@NotNull SmartBlockInstance instance) {
        return instance.getType() instanceof DataHolder;
    }
}
