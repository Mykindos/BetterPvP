package me.mykindos.betterpvp.core.item.serialization;

import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public interface CustomSerializer<T> {
    
    /**
     * Gets the type this serializer handles.
     *
     * @return The class
     */
    @NotNull Class<T> getType();

    /**
     * Serialize the into the persistent data container.
     *
     * @param instance  The instance to serialize
     * @param container The container to serialize into
     */
    void serialize(T instance, @NotNull PersistentDataContainer container);

    /**
     * Remove the instance's data from the persistent data container.
     *
     * @param instance  The instance whose data to remove
     * @param container The container to remove data from
     */
    void delete(T instance, @NotNull PersistentDataContainer container);
}
