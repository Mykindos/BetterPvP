package me.mykindos.betterpvp.champions.item.component.storage;

import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ArmorStorageComponentSerializer implements ComponentSerializer<ArmorStorageComponent>, ComponentDeserializer<ArmorStorageComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "armor-storage");
    private static final NamespacedKey DATA = new NamespacedKey("betterpvp", "data");
    private static final NamespacedKey ROLE = new NamespacedKey("betterpvp", "role");
    private static final NamespacedKey EXCLUSIVE = new NamespacedKey("betterpvp", "exclusive");

    @Override
    @NotNull
    public Class<ArmorStorageComponent> getType() {
        return ArmorStorageComponent.class;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public void serialize(@NotNull ArmorStorageComponent instance, @NotNull PersistentDataContainer container) {
        final PersistentDataContainer parent = container.getAdapterContext().newPersistentDataContainer();

        // data
        final ItemStack[] contents = instance.getItems();
        byte[] bytes = ItemStack.serializeItemsAsBytes(contents);
        parent.set(DATA, PersistentDataType.BYTE_ARRAY, bytes);

        // role & exclusive
        parent.set(ROLE, PersistentDataType.STRING, instance.getRole().name());
        parent.set(EXCLUSIVE, PersistentDataType.BYTE, (byte) (instance.isExclusive() ? 1 : 0));

        container.set(KEY, PersistentDataType.TAG_CONTAINER, parent);
    }

    @Override
    public void delete(@NotNull ArmorStorageComponent instance, @NotNull PersistentDataContainer container) {
        container.remove(KEY);
    }

    @Override
    public @NotNull ArmorStorageComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        if (!container.has(KEY, PersistentDataType.TAG_CONTAINER)) {
            throw new IllegalStateException("ArmorStorageComponent data is missing");
        }

        // parent
        final PersistentDataContainer parent = Objects.requireNonNull(container.get(KEY, PersistentDataType.TAG_CONTAINER));

        // data
        final byte[] bytes = Objects.requireNonNull(parent.get(DATA, PersistentDataType.BYTE_ARRAY));
        final ItemStack[] items = ItemStack.deserializeItemsFromBytes(bytes);

        // role & exclusive
        final Role role = Role.valueOf(Objects.requireNonNull(parent.get(ROLE, PersistentDataType.STRING)));
        final boolean exclusive = Objects.requireNonNull(parent.get(EXCLUSIVE, PersistentDataType.BYTE)) == 1;

        return new ArmorStorageComponent(items, role, exclusive);
    }
}