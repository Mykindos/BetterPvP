package me.mykindos.betterpvp.core.item.component.impl.stat;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a statistical attribute value for an item.
 * This is a value holder that references a StatType for its definition.
 * <p>
 * ItemStats are immutable and contained within a {@link StatContainerComponent}.
 * Serialization is handled by the StatSerializationRegistry.
 *
 * @param <T> The data type for this stat (Integer, Double, etc.)
 */
@Getter
public class ItemStat<T> {

    public static final TextColor RED = TextColor.color(255, 0, 0);
    public static final TextColor GREEN = TextColor.color(0, 255, 0);
    public static final TextColor BLUE = TextColor.color(0, 0, 255);

    private final StatType<T> type;
    private final T value;
    private final T baseRangeMin;
    private final T rangeMax;
    private final T rangeMinFlatModifier;

    /**
     * Full constructor with all fields.
     */
    public ItemStat(@NotNull StatType<T> type, @NotNull T value, @NotNull T baseRangeMin, @NotNull T rangeMax, @NotNull T rangeMinFlatModifier) {
        Preconditions.checkNotNull(type, "type cannot be null");
        Preconditions.checkNotNull(value, "value cannot be null");
        Preconditions.checkArgument(type.isValidValue(value), "value is not valid for this stat type");

        this.type = type;
        this.value = value;
        this.baseRangeMin = baseRangeMin;
        this.rangeMax = rangeMax;
        this.rangeMinFlatModifier = rangeMinFlatModifier;
    }

    /**
     * Constructor with ranges, no modifier.
     */
    public ItemStat(@NotNull StatType<T> type, @NotNull T value, @NotNull T baseRangeMin, @NotNull T rangeMax) {
        this(type, value, baseRangeMin, rangeMax, type.getZero());
    }

    /**
     * Simple constructor with automatic ±1.0 ranges.
     */
    public ItemStat(@NotNull StatType<T> type, @NotNull T value) {
        this(type, value,
                type.subtract(value, type.getOne()),
                type.add(value, type.getOne()),
                type.getZero());
    }

    /**
     * Gets the calculated minimum range value.
     * This is the base range minimum plus any flat modifiers from reforging.
     *
     * @return The effective minimum range value
     */
    public T getRangeMin() {
        return type.add(baseRangeMin, rangeMinFlatModifier);
    }

    public String stringValue() {
        return type.stringValue(value);
    }

    public TextColor getValueColor() {
        return type.getValueColor(value);
    }

    public ItemStat<T> withValue(@NotNull T newValue) {
        return new ItemStat<>(type, newValue, baseRangeMin, rangeMax, rangeMinFlatModifier);
    }

    public ItemStat<T> withRanges(@NotNull T newBaseRangeMin, @NotNull T newRangeMax) {
        return new ItemStat<>(type, value, newBaseRangeMin, newRangeMax, rangeMinFlatModifier);
    }

    public ItemStat<T> withRangeMinFlatModifier(@NotNull T modifier) {
        return new ItemStat<>(type, value, baseRangeMin, rangeMax, modifier);
    }

    @SuppressWarnings("unchecked")
    public ItemStat<T> merge(@NotNull ItemStat<?> other) {
        Preconditions.checkArgument(type.equals(other.type), "Cannot merge stats of different types");
        T mergedValue = type.merge(this.value, (T) other.value);
        return new ItemStat<>(type, mergedValue, baseRangeMin, rangeMax, rangeMinFlatModifier);
    }

    public ItemStat<T> copy() {
        return new ItemStat<>(type, value, baseRangeMin, rangeMax, rangeMinFlatModifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemStat<?> itemStat = (ItemStat<?>) o;
        return Objects.equals(type, itemStat.type)
                && Objects.equals(value, itemStat.value)
                && Objects.equals(baseRangeMin, itemStat.baseRangeMin)
                && Objects.equals(rangeMax, itemStat.rangeMax)
                && Objects.equals(rangeMinFlatModifier, itemStat.rangeMinFlatModifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, baseRangeMin, rangeMax, rangeMinFlatModifier);
    }
}
