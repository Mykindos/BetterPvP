package me.mykindos.betterpvp.core.item.component.impl.stat.serialization.impl;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatType;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypeHandler;
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

    // Static keys for delta serialization format
    private static final NamespacedKey TYPE_KEY = new NamespacedKey("betterpvp", "type");
    private static final NamespacedKey RELATIVE_VALUE_KEY = new NamespacedKey("betterpvp", "relative-value");
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
                && container.has(RELATIVE_VALUE_KEY, PersistentDataType.DOUBLE)
                && container.has(RANGE_MIN_FLAT_MODIFIER_KEY, PersistentDataType.DOUBLE);
    }

    @Override
    public void serialize(@NotNull ItemStat<Double> stat, @NotNull PersistentDataContainer container) {
        // Store type
        container.set(TYPE_KEY, PersistentDataType.STRING, stat.getType().getKey().toString());

        // Calculate and store relative value (position in range)
        double relativeValue = calculateRelativeValue(stat);
        container.set(RELATIVE_VALUE_KEY, PersistentDataType.DOUBLE, relativeValue);

        // Store augmentation modifier
        container.set(RANGE_MIN_FLAT_MODIFIER_KEY, PersistentDataType.DOUBLE, stat.getRangeMinFlatModifier());
    }

    private double calculateRelativeValue(ItemStat<Double> stat) {
        StatTypeHandler<Double> handler = stat.getType().getTypeHandler();
        Double effectiveMin = stat.getRangeMin(); // Already returns baseRangeMin + rangeMinFlatModifier
        Double range = handler.subtract(stat.getRangeMax(), effectiveMin);

        // Handle zero range edge case
        if (range.equals(0.0)) {
            return 0.5;
        }

        Double delta = handler.subtract(stat.getValue(), effectiveMin);
        double relativeValue = handler.toDouble(delta) / handler.toDouble(range);

        // Clamp to [0.0, 1.0] for safety
        return Math.max(0.0, Math.min(1.0, relativeValue));
    }

    @Override
    public void delete(@NotNull ItemStat<Double> stat, @NotNull PersistentDataContainer container) {
        // Remove all keys
        container.remove(TYPE_KEY);
        container.remove(RELATIVE_VALUE_KEY);
        container.remove(RANGE_MIN_FLAT_MODIFIER_KEY);
    }

    @Override
    public @NotNull ItemStat<Double> deserialize(@NotNull ItemInstance item, @NotNull PersistentDataContainer container) {
        String typeKeyStr = container.get(TYPE_KEY, PersistentDataType.STRING);
        Preconditions.checkNotNull(typeKeyStr, "Type key is null");

        NamespacedKey statTypeKey = Objects.requireNonNull(NamespacedKey.fromString(typeKeyStr));
        StatType<Double> type = registry.<Double>getType(statTypeKey)
                .orElseThrow(() -> new IllegalStateException("Unknown stat type: " + typeKeyStr));

        // Get base ranges from BaseItem (SINGLE SOURCE OF TRUTH)
        @SuppressWarnings("unchecked")
        ItemStat<Double> baseStat = (ItemStat<Double>) item.getBaseItem()
                .getComponent(StatContainerComponent.class)
                .stream()
                .flatMap(sc -> sc.getBaseStats().stream())
                .filter(stat -> stat.getType().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Base item missing stat: " + type));

        Double baseRangeMin = baseStat.getBaseRangeMin();
        Double rangeMax = baseStat.getRangeMax();

        // Read delta data
        Double relativeValue = container.get(RELATIVE_VALUE_KEY, PersistentDataType.DOUBLE);
        Double modifier = container.get(RANGE_MIN_FLAT_MODIFIER_KEY, PersistentDataType.DOUBLE);

        Preconditions.checkNotNull(relativeValue, "Relative value is null");
        Preconditions.checkNotNull(modifier, "Modifier is null");

        // Calculate absolute value from relative position
        Double value = calculateAbsoluteValue(relativeValue, baseRangeMin, rangeMax, modifier, type);

        return new ItemStat<>(type, value, baseRangeMin, rangeMax, modifier);
    }

    private Double calculateAbsoluteValue(double relativeValue, Double baseRangeMin,
                                         Double rangeMax, Double modifier, StatType<Double> type) {
        StatTypeHandler<Double> handler = type.getTypeHandler();
        Double effectiveMin = handler.add(baseRangeMin, modifier);
        Double range = handler.subtract(rangeMax, effectiveMin);

        // value = effectiveMin + (relativeValue * range)
        Double delta = handler.fromDouble(handler.toDouble(range) * relativeValue);
        return handler.add(effectiveMin, delta);
    }
} 