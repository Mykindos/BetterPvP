package me.mykindos.betterpvp.core.item.component.impl.stat;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.DoubleStatTypeHandler;
import me.mykindos.betterpvp.core.item.component.impl.stat.type.IntegerStatTypeHandler;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Defines a type of statistic that can be applied to items.
 * This is a type definition, not a value holder.
 * <p>
 * StatType contains all the metadata and behavior for a stat type:
 * <ul>
 *     <li>Display information (name, description, formatting)</li>
 *     <li>Validation rules</li>
 *     <li>Merge behavior (how to combine stats)</li>
 *     <li>Lifecycle hooks (onApply/onRemove)</li>
 * </ul>
 *
 * @param <T> The data type for this stat (Integer, Double, etc.)
 */
@Getter
public class StatType<T> {

    private final NamespacedKey key;
    private final String name;
    private final TextColor displayColor;
    private final String shortName;
    private final String description;
    private final boolean isPercentage;

    // Formatting
    private final Function<T, TextColor> valueColorProvider;
    private final Function<T, String> stringValueProvider;

    // Validation
    private final Predicate<T> valuePredicate;

    // Merging (for combining base + modifier stats)
    private final BiFunction<T, T, T> mergeFunction;

    // Type-specific operations handler
    private final StatTypeHandler<T> typeHandler;

    // Lifecycle hooks
    private final ItemStatLifecycleHooks<T> lifecycleHooks;

    private StatType(Builder<T> builder) {
        this.key = builder.key;
        this.name = builder.name;
        this.shortName = builder.shortName;
        this.description = builder.description;
        this.isPercentage = builder.isPercentage;
        this.valueColorProvider = builder.valueColorProvider;
        this.stringValueProvider = builder.stringValueProvider;
        this.valuePredicate = builder.valuePredicate;
        this.mergeFunction = builder.mergeFunction;
        this.typeHandler = builder.typeHandler;
        this.lifecycleHooks = builder.lifecycleHooks;
        this.displayColor = builder.displayColor;
    }

    public TextColor getValueColor(T value) {
        return valueColorProvider.apply(value);
    }

    public String stringValue(T value) {
        return stringValueProvider.apply(value);
    }

    public boolean isValidValue(T value) {
        return value != null && valuePredicate.test(value);
    }

    public T merge(T base, T modifier) {
        return mergeFunction.apply(base, modifier);
    }

    /**
     * Gets the zero value for this stat type.
     */
    public T getZero() {
        return typeHandler.getZero();
    }

    /**
     * Gets the one value for this stat type.
     */
    public T getOne() {
        return typeHandler.getOne();
    }

    /**
     * Add two values of this stat type.
     */
    public T add(T a, T b) {
        return typeHandler.add(a, b);
    }

    /**
     * Subtract one value from another.
     */
    public T subtract(T a, T b) {
        return typeHandler.subtract(a, b);
    }

    /**
     * Multiply a value by a scalar.
     */
    public T multiply(T value, double scalar) {
        return typeHandler.multiply(value, scalar);
    }

    /**
     * Converts a double value to this stat type.
     */
    public T fromDouble(double value) {
        return typeHandler.fromDouble(value);
    }

    /**
     * Returns the smaller of two values.
     */
    public T min(T a, T b) {
        return typeHandler.min(a, b);
    }

    /**
     * Generates a random value between min and max.
     */
    public T randomBetween(T min, T max) {
        return typeHandler.randomBetween(min, max);
    }

    /**
     * Generates a biased random value between min and max using a bias ratio.
     */
    public T randomBetweenBiased(T min, T max, double bias) {
        return typeHandler.randomBetweenBiased(min, max, bias);
    }

    public void onApply(Item item, ItemStack stack, T value) {
        lifecycleHooks.onApply(item, stack, value);
    }

    public void onRemove(Item item, ItemStack stack, T value) {
        lifecycleHooks.onRemove(item, stack, value);
    }

    @FunctionalInterface
    public interface ItemStatLifecycleHooks<T> {
        void onApply(Item item, ItemStack stack, T value);

        default void onRemove(Item item, ItemStack stack, T value) {
            // Default: no-op
        }

        static <T> ItemStatLifecycleHooks<T> empty() {
            return (item, stack, value) -> {};
        }
    }

    public static <T> Builder<T> builder(NamespacedKey key, Class<T> valueType) {
        return new Builder<>(key, valueType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatType<?> statType = (StatType<?>) o;
        return Objects.equals(key, statType.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    public static class Builder<T> {
        private final NamespacedKey key;
        private final Class<T> valueType;
        private String name;
        private TextColor displayColor;
        private String shortName;
        private String description;
        private boolean isPercentage = false;
        private Function<T, TextColor> valueColorProvider;
        private Function<T, String> stringValueProvider;
        private Predicate<T> valuePredicate = value -> true;
        private BiFunction<T, T, T> mergeFunction;
        private StatTypeHandler<T> typeHandler;
        private ItemStatLifecycleHooks<T> lifecycleHooks = ItemStatLifecycleHooks.empty();

        private Builder(NamespacedKey key, Class<T> valueType) {
            this.key = key;
            this.valueType = valueType;
        }

        public Builder<T> name(String name) {
            this.name = name;
            this.shortName = name; // Default short name to name
            return this;
        }

        public Builder<T> displayColor(TextColor color) {
            this.displayColor = color;
            return this;
        }

        public Builder<T> shortName(String shortName) {
            this.shortName = shortName;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> percentage(boolean isPercentage) {
            this.isPercentage = isPercentage;
            return this;
        }

        public Builder<T> valueColorProvider(Function<T, TextColor> provider) {
            this.valueColorProvider = provider;
            return this;
        }

        public Builder<T> stringValueProvider(Function<T, String> provider) {
            this.stringValueProvider = provider;
            return this;
        }

        public Builder<T> valuePredicate(Predicate<T> predicate) {
            this.valuePredicate = predicate;
            return this;
        }

        public Builder<T> mergeFunction(BiFunction<T, T, T> function) {
            this.mergeFunction = function;
            return this;
        }

        public Builder<T> lifecycleHooks(ItemStatLifecycleHooks<T> hooks) {
            this.lifecycleHooks = hooks;
            return this;
        }

        public StatType<T> build() {
            // Apply defaults based on value type
            applyTypeDefaults();

            return new StatType<>(this);
        }

        @SuppressWarnings("unchecked")
        private void applyTypeDefaults() {
            // Select appropriate type handler based on value type
            if (typeHandler == null) {
                if (valueType == Double.class) {
                    typeHandler = (StatTypeHandler<T>) DoubleStatTypeHandler.getInstance();
                } else if (valueType == Integer.class) {
                    typeHandler = (StatTypeHandler<T>) IntegerStatTypeHandler.getInstance();
                } else {
                    throw new IllegalArgumentException("Unsupported value type: " + valueType);
                }
            }

            // Apply defaults using the type handler
            if (valueColorProvider == null) {
                valueColorProvider = typeHandler::getDefaultColor;
            }
            if (stringValueProvider == null) {
                stringValueProvider = v -> typeHandler.formatValue(v, isPercentage);
            }
            if (mergeFunction == null) {
                mergeFunction = typeHandler::add; // Default merge is addition
            }
        }

    }
}
