package me.mykindos.betterpvp.core.item.component.serialization.impl;

import com.google.common.base.Preconditions;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.StatDeserializer;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.StatSerializationRegistry;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentDeserializer;
import me.mykindos.betterpvp.core.item.component.serialization.ComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serializer and deserializer for StatContainerComponent.
 * Uses the StatSerializationRegistry to handle individual stats generically.
 */
@CustomLog
public class StatContainerSerializer implements ComponentSerializer<StatContainerComponent>, ComponentDeserializer<StatContainerComponent> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "stats");
    
    private final StatSerializationRegistry statRegistry;

    public StatContainerSerializer(StatSerializationRegistry statRegistry) {
        this.statRegistry = statRegistry;
    }

    @Override
    @NotNull
    public Class<StatContainerComponent> getType() {
        return StatContainerComponent.class;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public void serialize(@NotNull StatContainerComponent instance, @NotNull PersistentDataContainer container) {
        List<PersistentDataContainer> collection = new ArrayList<>();
        
        // Serialize only modifier stats using their registered serializer
        for (ItemStat<?> stat : instance.getModifierStats()) {
            PersistentDataContainer statContainer = container.getAdapterContext().newPersistentDataContainer();
            serializeStat(stat, statContainer);
            collection.add(statContainer);
        }

        container.set(KEY, PersistentDataType.LIST.dataContainers(), collection);
    }

    @Override
    public void delete(@NotNull StatContainerComponent instance, @NotNull PersistentDataContainer container) {
        if (container.has(KEY, PersistentDataType.TAG_CONTAINER)) {
            container.remove(KEY);
        }
    }

    @Override
    public @NotNull StatContainerComponent deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        Preconditions.checkArgument(container.has(KEY, PersistentDataType.LIST.dataContainers()), "Container does not have stats data");
        List<PersistentDataContainer> collection = container.get(KEY, PersistentDataType.LIST.dataContainers());
        Preconditions.checkNotNull(collection, "Stats container is null");
        
        List<ItemStat<?>> modifierStats = new ArrayList<>();
        
        // Deserialize all registered stat types as modifiers
        for (PersistentDataContainer statContainer : collection) {
            for (Map.Entry<NamespacedKey, StatDeserializer<?>> entry : statRegistry.getAllDeserializers().entrySet()) {
                if (!entry.getValue().hasData(statContainer)) {
                    continue;
                }

                ItemStat<?> stat = entry.getValue().deserialize(item, statContainer);
                modifierStats.add(stat);
                break;
            }
        }

        // Check if the original base item has any base stats
        final List<ItemStat<?>> baseStats = item.getBaseItem().getComponent(StatContainerComponent.class)
                .map(StatContainerComponent::getBaseStats)
                .orElseGet(ArrayList::new);

        // Base stats should be loaded from the base item elsewhere
        return new StatContainerComponent(baseStats, modifierStats);
    }

    @SuppressWarnings("unchecked")
    private <T> void serializeStat(@NotNull ItemStat<T> stat, @NotNull PersistentDataContainer container) {
        try {
            statRegistry.getSerializer(stat).serialize(stat, container);
        } catch (IllegalArgumentException e) {
            log.error("No serializer found for stat with value type: " + stat.getValue().getClass()).submit();
            throw e;
        }
    }
} 