package me.mykindos.betterpvp.core.item.serialization;

import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public interface CustomDeserializer<T> {
    /**
     * Gets the namespaced key used to identify this type type in storage.
     *
     * @return The namespaced key
     */
    @NotNull NamespacedKey getKey();

    /**
     * Gets the type type this deserializer creates.
     *
     * @return The type class
     */
    @NotNull Class<T> getType();

    /**
     * Deserialize a type from the persistent data container.
     *
     * @param item The item instance to deserialize from
     * @param container The container to deserialize from
     * @return The deserialized type, or null if the data is not present or invalid
     */
    T deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container);

    /**
     * Check if the container has data for this type type.
     *
     * @param container The container to check
     * @return True if the type data is present
     */
    default boolean hasData(@NotNull PersistentDataContainer container) {
        return container.has(getKey());
    }
}
