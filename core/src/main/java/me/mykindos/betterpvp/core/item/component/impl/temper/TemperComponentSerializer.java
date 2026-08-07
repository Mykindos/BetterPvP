package me.mykindos.betterpvp.core.item.component.impl.temper;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import me.mykindos.betterpvp.core.item.temper.TemperProfile;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TemperComponentSerializer implements ComponentSerializer<TemperComponent>, ComponentDeserializer<TemperComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "temper");
    private static final NamespacedKey TEMPER = new NamespacedKey("betterpvp", "temper-value");
    private static final NamespacedKey TEMPER_TIME = new NamespacedKey("betterpvp", "temper-time");
    private static final NamespacedKey LAST_DRAIN = new NamespacedKey("betterpvp", "temper-last-drain");

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull Class<TemperComponent> getType() {
        return TemperComponent.class;
    }

    @Override
    public void serialize(TemperComponent instance, @NotNull PersistentDataContainer container) {
        PersistentDataContainer temperContainer = container.get(KEY, PersistentDataType.TAG_CONTAINER);
        if (temperContainer == null) {
            temperContainer = container.getAdapterContext().newPersistentDataContainer();
        }

        temperContainer.set(TEMPER, PersistentDataType.DOUBLE, instance.getTemper());
        temperContainer.set(TEMPER_TIME, PersistentDataType.LONG, instance.getTemperTime());
        temperContainer.set(LAST_DRAIN, PersistentDataType.LONG, instance.getLastDrainTime());
        container.set(KEY, PersistentDataType.TAG_CONTAINER, temperContainer);
    }

    @Override
    public TemperComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        final PersistentDataContainer temperContainer = container.get(KEY, PersistentDataType.TAG_CONTAINER);
        if (temperContainer == null) {
            throw new IllegalArgumentException("Temper component not found in item");
        }

        // The profile is tuning, not instance state: always take the item's current one so a config
        // reload retunes stacks that were written before the change.
        final Optional<TemperComponent> baseComponent = item.getBaseItem().getComponent(TemperComponent.class);
        final TemperProfile profile = baseComponent
                .map(TemperComponent::getProfile)
                .orElseGet(TemperProfile::medium);

        final TemperComponent component = new TemperComponent(profile);
        component.setTemper(temperContainer.getOrDefault(TEMPER, PersistentDataType.DOUBLE, 1d));
        component.setTemperTime(temperContainer.getOrDefault(TEMPER_TIME, PersistentDataType.LONG, 0L));
        component.setLastDrainTime(temperContainer.getOrDefault(LAST_DRAIN, PersistentDataType.LONG, 0L));
        return component;
    }

    @Override
    public void delete(TemperComponent instance, @NotNull PersistentDataContainer container) {
        container.remove(KEY);
    }
}
