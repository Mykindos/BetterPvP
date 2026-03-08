package me.mykindos.betterpvp.core.interaction.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A type-safe key for storing and retrieving chain-scoped values from an {@link InteractionContext}.
 * Chain-scoped values persist across the entire chain execution.
 * <p>
 * Use this for data that needs to be shared between multiple interactions in a chain,
 * such as tracking state across a combo sequence.
 *
 * @param <T> the type of value this key represents
 */
public final class ChainKey<T> implements ScopedKey<T> {

    private final String name;
    private final Supplier<T> defaultSupplier;

    private ChainKey(@NotNull String name, @Nullable Supplier<T> defaultSupplier) {
        this.name = name;
        this.defaultSupplier = defaultSupplier;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public boolean hasDefault() {
        return defaultSupplier != null;
    }

    @Override
    @Nullable
    public T getDefault() {
        return defaultSupplier != null ? defaultSupplier.get() : null;
    }

    /**
     * Create a key with no default value.
     *
     * @param name the unique name for this key
     * @param <T>  the value type
     * @return a new chain key
     */
    public static <T> ChainKey<T> of(@NotNull String name) {
        return new ChainKey<>(name, null);
    }

    /**
     * Create a key with a constant default value.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @param <T>          the value type
     * @return a new chain key
     */
    public static <T> ChainKey<T> of(@NotNull String name, @NotNull T defaultValue) {
        return new ChainKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key with a lazily-computed default value.
     * The supplier is called each time the default is needed.
     *
     * @param name            the unique name for this key
     * @param defaultSupplier supplier for the default value
     * @param <T>             the value type
     * @return a new chain key
     */
    public static <T> ChainKey<T> of(@NotNull String name, @NotNull Supplier<T> defaultSupplier) {
        return new ChainKey<>(name, defaultSupplier);
    }

    /**
     * Create a key for a Set with an empty HashSet as default.
     *
     * @param name the unique name for this key
     * @param <E>  the element type
     * @return a new chain key for Set
     */
    public static <E> ChainKey<Set<E>> ofSet(@NotNull String name) {
        return new ChainKey<>(name, HashSet::new);
    }

    /**
     * Create a key for Long with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new chain key for Long
     */
    public static ChainKey<Long> ofLong(@NotNull String name) {
        return new ChainKey<>(name, null);
    }

    /**
     * Create a key for Long with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new chain key for Long
     */
    public static ChainKey<Long> ofLong(@NotNull String name, long defaultValue) {
        return new ChainKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key for Integer with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new chain key for Integer
     */
    public static ChainKey<Integer> ofInt(@NotNull String name) {
        return new ChainKey<>(name, null);
    }

    /**
     * Create a key for Integer with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new chain key for Integer
     */
    public static ChainKey<Integer> ofInt(@NotNull String name, int defaultValue) {
        return new ChainKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key for Double with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new chain key for Double
     */
    public static ChainKey<Double> ofDouble(@NotNull String name) {
        return new ChainKey<>(name, null);
    }

    /**
     * Create a key for Double with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new chain key for Double
     */
    public static ChainKey<Double> ofDouble(@NotNull String name, double defaultValue) {
        return new ChainKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key for Boolean with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new chain key for Boolean
     */
    public static ChainKey<Boolean> ofBoolean(@NotNull String name) {
        return new ChainKey<>(name, null);
    }

    /**
     * Create a key for Boolean with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new chain key for Boolean
     */
    public static ChainKey<Boolean> ofBoolean(@NotNull String name, boolean defaultValue) {
        return new ChainKey<>(name, () -> defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChainKey<?> that = (ChainKey<?>) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "ChainKey{" + name + "}";
    }
}
