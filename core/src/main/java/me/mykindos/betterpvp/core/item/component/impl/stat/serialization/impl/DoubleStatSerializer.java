package me.mykindos.betterpvp.core.item.component.impl.stat.serialization.impl;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatType;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypeRegistry;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.StatDeserializer;
import me.mykindos.betterpvp.core.item.component.impl.stat.serialization.StatSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Serializer and deserializer for ItemStat<Double> types.
 */
public class DoubleStatSerializer implements StatSerializer<ItemStat<Double>>, StatDeserializer<ItemStat<Double>> {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "stat-double");

    // Static keys for serialization format
    private static final NamespacedKey TYPE_KEY = new NamespacedKey("betterpvp", "type");
    private static final NamespacedKey VALUE_KEY = new NamespacedKey("betterpvp", "value");
    private static final NamespacedKey RANGE_MIN_KEY = new NamespacedKey("betterpvp", "min");
    private static final NamespacedKey RANGE_MAX_KEY = new NamespacedKey("betterpvp", "max");
    private static final NamespacedKey RANGE_MIN_FLAT_MODIFIER_KEY = new NamespacedKey("betterpvp", "min-modifier");

    private final StatTypeRegistry registry;

    /**
     * Create a serializer for Double-based stats.
     *
     * @param registry The stat type registry for lookups
     */
    public DoubleStatSerializer(@NotNull StatTypeRegistry registry) {
        this.registry = registry;
    }

    @Override
    @NotNull
    public Class<ItemStat<Double>> getType() {
        @SuppressWarnings("unchecked")
        Class<ItemStat<Double>> clazz = (Class<ItemStat<Double>>) (Class<?>) ItemStat.class;
        return clazz;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public boolean hasData(@NotNull PersistentDataContainer container) {
        return container.has(TYPE_KEY, PersistentDataType.STRING)
                && container.has(VALUE_KEY, PersistentDataType.DOUBLE)
                && container.has(RANGE_MIN_KEY, PersistentDataType.DOUBLE)
                && container.has(RANGE_MAX_KEY, PersistentDataType.DOUBLE)
                && container.has(RANGE_MIN_FLAT_MODIFIER_KEY, PersistentDataType.DOUBLE);
    }

    @Override
    public void serialize(@NotNull ItemStat<Double> stat, @NotNull PersistentDataContainer container) {
        // Serialize all fields
        container.set(TYPE_KEY, PersistentDataType.STRING, stat.getType().getKey().toString());
        container.set(VALUE_KEY, PersistentDataType.DOUBLE, stat.getValue());
        container.set(RANGE_MIN_KEY, PersistentDataType.DOUBLE, stat.getBaseRangeMin());
        container.set(RANGE_MAX_KEY, PersistentDataType.DOUBLE, stat.getRangeMax());
        container.set(RANGE_MIN_FLAT_MODIFIER_KEY, PersistentDataType.DOUBLE, stat.getRangeMinFlatModifier());
    }

    @Override
    public void delete(@NotNull ItemStat<Double> stat, @NotNull PersistentDataContainer container) {
        // Remove all keys
        container.remove(TYPE_KEY);
        container.remove(VALUE_KEY);
        container.remove(RANGE_MIN_KEY);
        container.remove(RANGE_MAX_KEY);
        container.remove(RANGE_MIN_FLAT_MODIFIER_KEY);
    }

    @Override
    public @NotNull ItemStat<Double> deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        if (container.has(TYPE_KEY, PersistentDataType.STRING)) {
            String typeKeyStr = container.get(TYPE_KEY, PersistentDataType.STRING);
            Preconditions.checkNotNull(typeKeyStr, "Type key is null");

            NamespacedKey statTypeKey = Objects.requireNonNull(NamespacedKey.fromString(typeKeyStr));
            StatType<Double> type = registry.<Double>getType(statTypeKey)
                    .orElseThrow(() -> new IllegalStateException("Unknown stat type: " + typeKeyStr));

            Double value = container.get(VALUE_KEY, PersistentDataType.DOUBLE);
            Double rangeMin = container.get(RANGE_MIN_KEY, PersistentDataType.DOUBLE);
            Double rangeMax = container.get(RANGE_MAX_KEY, PersistentDataType.DOUBLE);
            Double modifier = container.get(RANGE_MIN_FLAT_MODIFIER_KEY, PersistentDataType.DOUBLE);

            Preconditions.checkNotNull(value, "Value is null");
            Preconditions.checkNotNull(rangeMin, "RangeMin is null");
            Preconditions.checkNotNull(rangeMax, "RangeMax is null");
            Preconditions.checkNotNull(modifier, "Modifier is null");

            return new ItemStat<>(type, value, rangeMin, rangeMax, modifier);
        } else {
            throw new IllegalArgumentException("Container does not have stat data for key: " + KEY);
        }
    }
} 