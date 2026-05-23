package me.mykindos.betterpvp.core.item.component.impl.repair;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class RepairableComponentSerializer implements ComponentSerializer<RepairableComponent>, ComponentDeserializer<RepairableComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "repairable");
    private static final NamespacedKey RESTORED_LIFETIME = new NamespacedKey("betterpvp", "restored-lifetime");

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull Class<RepairableComponent> getType() {
        return RepairableComponent.class;
    }

    @Override
    public void serialize(RepairableComponent instance, @NotNull PersistentDataContainer container) {
        PersistentDataContainer repairableContainer = container.get(KEY, PersistentDataType.TAG_CONTAINER);
        if (repairableContainer == null) {
            repairableContainer = container.getAdapterContext().newPersistentDataContainer();
        }

        repairableContainer.set(RESTORED_LIFETIME, PersistentDataType.INTEGER, instance.getRestoredLifetime());
        container.set(KEY, PersistentDataType.TAG_CONTAINER, repairableContainer);
    }

    @Override
    public RepairableComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        PersistentDataContainer repairableContainer = container.get(KEY, PersistentDataType.TAG_CONTAINER);
        if (repairableContainer == null) {
            throw new IllegalArgumentException("Repairable component not found in item");
        }

        final int restoredLifetime = repairableContainer.getOrDefault(RESTORED_LIFETIME, PersistentDataType.INTEGER, 0);

        final RepairableComponent component = new RepairableComponent();
        component.setRestoredLifetime(restoredLifetime);
        return component;
    }

    @Override
    public void delete(RepairableComponent instance, @NotNull PersistentDataContainer container) {
        container.remove(KEY);
    }
}
