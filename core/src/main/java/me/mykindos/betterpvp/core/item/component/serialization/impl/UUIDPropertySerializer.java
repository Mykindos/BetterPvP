package me.mykindos.betterpvp.core.item.component.serialization.impl;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.uuid.UUIDProperty;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Serializer and deserializer for UUIDProperty components.
 */
public class UUIDPropertySerializer implements ComponentSerializer<UUIDProperty>, ComponentDeserializer<UUIDProperty> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "uuid");

    @Override
    @NotNull
    public Class<UUIDProperty> getType() {
        return UUIDProperty.class;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public void serialize(@NotNull UUIDProperty instance, @NotNull PersistentDataContainer container) {
        container.set(KEY, CustomDataType.UUID, instance.getUniqueId());
    }

    @Override
    public void delete(@NotNull UUIDProperty instance, @NotNull PersistentDataContainer container) {
        if (container.has(KEY, CustomDataType.UUID)) {
            container.remove(KEY);
        }
    }

    @Override
    public @NotNull UUIDProperty deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        Preconditions.checkArgument(container.has(KEY, CustomDataType.UUID), "Container does not have uuid data");
        UUID uuid = container.get(KEY, CustomDataType.UUID);
        Preconditions.checkNotNull(uuid, "UUID data is null");
        return new UUIDProperty(uuid);
    }
} 