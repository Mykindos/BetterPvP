package me.mykindos.betterpvp.core.item.component.impl.durability;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DurabilityComponentSerializer implements ComponentSerializer<DurabilityComponent>, ComponentDeserializer<DurabilityComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "durability");
    private static final NamespacedKey MAX_DAMAGE = new NamespacedKey("betterpvp", "max-damage");
    private static final NamespacedKey DAMAGE = new NamespacedKey("betterpvp", "damage");

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull Class<DurabilityComponent> getType() {
        return DurabilityComponent.class;
    }

    @Override
    public void serialize(DurabilityComponent instance, @NotNull PersistentDataContainer container) {
        PersistentDataContainer durabilityContainer = container.get(KEY, PersistentDataType.TAG_CONTAINER);
        if (durabilityContainer == null) {
            durabilityContainer = container.getAdapterContext().newPersistentDataContainer();
        }

        durabilityContainer.set(MAX_DAMAGE, PersistentDataType.INTEGER, instance.getMaxDamage());
        durabilityContainer.set(DAMAGE, PersistentDataType.INTEGER, instance.getDamage());
        container.set(KEY, PersistentDataType.TAG_CONTAINER, durabilityContainer);
    }

    @Override
    public DurabilityComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        PersistentDataContainer durabilityContainer = container.get(KEY, PersistentDataType.TAG_CONTAINER);
        if (durabilityContainer == null) {
            throw new IllegalArgumentException("Durability component not found in item");
        }

        int maxDamage = durabilityContainer.getOrDefault(MAX_DAMAGE, PersistentDataType.INTEGER, 0);
        final Optional<DurabilityComponent> component = item.getBaseItem().getComponent(DurabilityComponent.class);
        if (component.isPresent()) {
            maxDamage = component.get().getMaxDamage();
        }

        int damage = durabilityContainer.getOrDefault(DAMAGE, PersistentDataType.INTEGER, 0);
        if (damage > maxDamage) {
            damage = maxDamage;
        }

        final DurabilityComponent durabilityComponent = new DurabilityComponent(maxDamage);
        durabilityComponent.setDamage(damage);
        return durabilityComponent;
    }

    @Override
    public void delete(DurabilityComponent instance, @NotNull PersistentDataContainer container) {
        container.remove(KEY);
    }
}
