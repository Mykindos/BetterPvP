package me.mykindos.betterpvp.core.block.impl.smelter;

import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.block.data.storage.StorageBlockData;
import me.mykindos.betterpvp.core.block.data.storage.StorageBlockDataSerializer;
import me.mykindos.betterpvp.core.item.ItemFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SmelterDataSerializer implements SmartBlockDataSerializer<SmelterData> {

    private final StorageBlockDataSerializer<StorageBlockData> contentSerializer;
    private final StorageBlockDataSerializer<StorageBlockData> fuelSerializer;
    private final Smelter smelter;

    public SmelterDataSerializer(ItemFactory itemFactory, Smelter smelter) {
        this.contentSerializer = new StorageBlockDataSerializer<>("contents", StorageBlockData.class, itemFactory, StorageBlockData::new);
        this.fuelSerializer = new StorageBlockDataSerializer<>("fuel", StorageBlockData.class, itemFactory, StorageBlockData::new);
        this.smelter = smelter;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey("betterpvp", "smelter_data");
    }

    @Override
    public @NotNull Class<SmelterData> getType() {
        return SmelterData.class;
    }

    @Override
    public void serialize(@NotNull SmelterData data, @NotNull PersistentDataContainer container) {
        contentSerializer.serialize(data.getContentItems(), container);
        fuelSerializer.serialize(data.getFuelItems(), container);
        container.set(new NamespacedKey("betterpvp", "burn_time"), PersistentDataType.LONG, data.getBurnTime());
        container.set(new NamespacedKey("betterpvp", "temperature"), PersistentDataType.FLOAT, data.getTemperature());
    }

    @Override
    public @NotNull SmelterData deserialize(@NotNull PersistentDataContainer container) {
        StorageBlockData contentItems = contentSerializer.deserialize(container);
        StorageBlockData fuelItems = fuelSerializer.deserialize(container);
        long burnTime = Objects.requireNonNull(container.get(new NamespacedKey("betterpvp", "burn_time"), PersistentDataType.LONG));
        float temperature = Objects.requireNonNull(container.get(new NamespacedKey("betterpvp", "temperature"), PersistentDataType.FLOAT));

        final SmelterData defaultData = smelter.createDefaultData();
        defaultData.setContentItems(contentItems);
        defaultData.setFuelItems(fuelItems);
        defaultData.setBurnTime(burnTime);
        defaultData.setTemperature(temperature);
        return defaultData;
    }

    @Override
    public boolean hasData(@NotNull PersistentDataContainer container) {
        return container.has(new NamespacedKey("betterpvp", "burn_time"), PersistentDataType.LONG) &&
               container.has(new NamespacedKey("betterpvp", "temperature"), PersistentDataType.FLOAT) &&
               contentSerializer.hasData(container) &&
               fuelSerializer.hasData(container);
    }
}
