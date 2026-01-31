package me.mykindos.betterpvp.core.interaction.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A type-safe key for storing and retrieving values from an {@link InteractionContext}.
 * Supports default values that are computed lazily when the key is accessed.
 *
 * @param <T> the type of value this key represents
 */
public final class MetaKey<T> {

    private final String name;
    private final Supplier<T> defaultSupplier;

    private MetaKey(@NotNull String name, @Nullable Supplier<T> defaultSupplier) {
        this.name = name;
        this.defaultSupplier = defaultSupplier;
    }

    /**
     * Get the name of this key.
     */
    public String getName() {
        return name;
    }

    /**
     * Check if this key has a default value supplier.
     */
    public boolean hasDefault() {
        return defaultSupplier != null;
    }

    /**
     * Get the default value for this key.
     *
     * @return the default value, or null if no default is set
     */
    @Nullable
    public T getDefault() {
        return defaultSupplier != null ? defaultSupplier.get() : null;
    }

    // ==================== Factory Methods ====================

    /**
     * Create a key with no default value.
     *
     * @param name the unique name for this key
     * @param <T>  the value type
     * @return a new meta key
     */
    public static <T> MetaKey<T> of(@NotNull String name) {
        return new MetaKey<>(name, null);
    }

    /**
     * Create a key with a constant default value.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @param <T>          the value type
     * @return a new meta key
     */
    public static <T> MetaKey<T> of(@NotNull String name, @NotNull T defaultValue) {
        return new MetaKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key with a lazily-computed default value.
     * The supplier is called each time the default is needed.
     *
     * @param name            the unique name for this key
     * @param defaultSupplier supplier for the default value
     * @param <T>             the value type
     * @return a new meta key
     */
    public static <T> MetaKey<T> of(@NotNull String name, @NotNull Supplier<T> defaultSupplier) {
        return new MetaKey<>(name, defaultSupplier);
    }

    // ==================== Specialized Factory Methods ====================
    // These avoid the need for casting with generic types

    /**
     * Create a key for a Set with an empty HashSet as default.
     *
     * @param name the unique name for this key
     * @param <E>  the element type
     * @return a new meta key for Set
     */
    public static <E> MetaKey<Set<E>> ofSet(@NotNull String name) {
        return new MetaKey<>(name, HashSet::new);
    }

    /**
     * Create a key for Long with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new meta key for Long
     */
    public static MetaKey<Long> ofLong(@NotNull String name) {
        return new MetaKey<>(name, null);
    }

    /**
     * Create a key for Long with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new meta key for Long
     */
    public static MetaKey<Long> ofLong(@NotNull String name, long defaultValue) {
        return new MetaKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key for Integer with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new meta key for Integer
     */
    public static MetaKey<Integer> ofInt(@NotNull String name) {
        return new MetaKey<>(name, null);
    }

    /**
     * Create a key for Integer with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new meta key for Integer
     */
    public static MetaKey<Integer> ofInt(@NotNull String name, int defaultValue) {
        return new MetaKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key for Double with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new meta key for Double
     */
    public static MetaKey<Double> ofDouble(@NotNull String name) {
        return new MetaKey<>(name, null);
    }

    /**
     * Create a key for Double with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new meta key for Double
     */
    public static MetaKey<Double> ofDouble(@NotNull String name, double defaultValue) {
        return new MetaKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key for Boolean with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new meta key for Boolean
     */
    public static MetaKey<Boolean> ofBoolean(@NotNull String name) {
        return new MetaKey<>(name, null);
    }

    /**
     * Create a key for Boolean with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new meta key for Boolean
     */
    public static MetaKey<Boolean> ofBoolean(@NotNull String name, boolean defaultValue) {
        return new MetaKey<>(name, () -> defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetaKey<?> that = (MetaKey<?>) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "MetaKey{" + name + "}";
    }
}
