package me.mykindos.betterpvp.core.item.component.impl.fuel;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class FuelComponentSerializer implements ComponentSerializer<FuelComponent>, ComponentDeserializer<FuelComponent> {

    private static final NamespacedKey BURN_TIME_KEY = new NamespacedKey("betterpvp", "fuel_burn_time");
    private static final NamespacedKey MAX_TEMPERATURE_KEY = new NamespacedKey("betterpvp", "fuel_max_temperature");

    @Override
    public @NotNull NamespacedKey getKey() {
        return FuelComponent.KEY;
    }

    @Override
    public @NotNull Class<FuelComponent> getType() {
        return FuelComponent.class;
    }

    @Override
    public void serialize(@NotNull FuelComponent component, @NotNull PersistentDataContainer container) {
        container.set(BURN_TIME_KEY, PersistentDataType.LONG, component.getBurnTime());
        container.set(MAX_TEMPERATURE_KEY, PersistentDataType.FLOAT, component.getMaxTemperature());
    }

    @Override
    public @NotNull FuelComponent deserialize(@NotNull ItemInstance itemInstance, @NotNull PersistentDataContainer container) {
        Long burnTime = container.get(BURN_TIME_KEY, PersistentDataType.LONG);
        Float maxTemperature = container.get(MAX_TEMPERATURE_KEY, PersistentDataType.FLOAT);

        if (burnTime == null || maxTemperature == null) {
            throw new IllegalArgumentException("Missing fuel component data in container");
        }

        return new FuelComponent(burnTime, maxTemperature);
    }

    @Override
    public boolean hasData(@NotNull PersistentDataContainer container) {
        return container.has(BURN_TIME_KEY, PersistentDataType.LONG) && 
               container.has(MAX_TEMPERATURE_KEY, PersistentDataType.FLOAT);
    }

    @Override
    public void delete(@NotNull FuelComponent component, @NotNull PersistentDataContainer container) {
        container.remove(BURN_TIME_KEY);
        container.remove(MAX_TEMPERATURE_KEY);
    }
} 