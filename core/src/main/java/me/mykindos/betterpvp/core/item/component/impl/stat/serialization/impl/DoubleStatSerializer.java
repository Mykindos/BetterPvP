package me.mykindos.betterpvp.core.item.component.impl.stat.serialization.impl;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.DoubleItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.StatDeserializer;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.StatSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Generic serializer and deserializer for all DoubleItemStat types.
 * Uses a constructor function to create the appropriate stat instance.
 * 
 * @param <T> The specific DoubleItemStat type
 */
public class DoubleStatSerializer<T extends DoubleItemStat> implements StatSerializer<T>, StatDeserializer<T> {

    private final NamespacedKey key;
    private final Class<T> statType;
    private final Function<Double, T> constructor;

    /**
     * Create a serializer for a specific DoubleItemStat type.
     * 
     * @param key The namespaced key for this stat type
     * @param statType The stat class
     * @param constructor Function to create stat instances from double values
     */
    public DoubleStatSerializer(@NotNull NamespacedKey key, @NotNull Class<T> statType, @NotNull Function<Double, T> constructor) {
        this.key = key;
        this.statType = statType;
        this.constructor = constructor;
    }

    @Override
    @NotNull
    public Class<T> getType() {
        return statType;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return key;
    }

    @Override
    public void serialize(@NotNull T stat, @NotNull PersistentDataContainer container) {
        container.set(key, PersistentDataType.DOUBLE, stat.getValue());
    }

    @Override
    public void delete(@NotNull T stat, @NotNull PersistentDataContainer container) {
        if (container.has(key, PersistentDataType.DOUBLE)) {
            container.remove(key);
        }
    }

    @Override
    public @NotNull T deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        Preconditions.checkArgument(container.has(key, PersistentDataType.DOUBLE), "Container does not have double data");
        Double value = container.get(key, PersistentDataType.DOUBLE);
        Preconditions.checkNotNull(value, "Double data is null");
        return constructor.apply(value);
    }
} 