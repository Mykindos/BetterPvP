package me.mykindos.betterpvp.core.interaction.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A type-safe key for storing and retrieving execution-scoped values from an {@link InteractionContext}.
 * Execution-scoped values are cleared before each interaction execution.
 * <p>
 * Use this for data that is specific to a single interaction execution,
 * such as the target entity, damage amount, or input metadata.
 *
 * @param <T> the type of value this key represents
 */
public final class ExecutionKey<T> implements ScopedKey<T> {

    private final String name;
    private final Supplier<T> defaultSupplier;

    private ExecutionKey(@NotNull String name, @Nullable Supplier<T> defaultSupplier) {
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
     * @return a new execution key
     */
    public static <T> ExecutionKey<T> of(@NotNull String name) {
        return new ExecutionKey<>(name, null);
    }

    /**
     * Create a key with a constant default value.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @param <T>          the value type
     * @return a new execution key
     */
    public static <T> ExecutionKey<T> of(@NotNull String name, @NotNull T defaultValue) {
        return new ExecutionKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key with a lazily-computed default value.
     * The supplier is called each time the default is needed.
     *
     * @param name            the unique name for this key
     * @param defaultSupplier supplier for the default value
     * @param <T>             the value type
     * @return a new execution key
     */
    public static <T> ExecutionKey<T> of(@NotNull String name, @NotNull Supplier<T> defaultSupplier) {
        return new ExecutionKey<>(name, defaultSupplier);
    }

    /**
     * Create a key for a Set with an empty HashSet as default.
     *
     * @param name the unique name for this key
     * @param <E>  the element type
     * @return a new execution key for Set
     */
    public static <E> ExecutionKey<Set<E>> ofSet(@NotNull String name) {
        return new ExecutionKey<>(name, HashSet::new);
    }

    /**
     * Create a key for Long with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new execution key for Long
     */
    public static ExecutionKey<Long> ofLong(@NotNull String name) {
        return new ExecutionKey<>(name, null);
    }

    /**
     * Create a key for Long with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new execution key for Long
     */
    public static ExecutionKey<Long> ofLong(@NotNull String name, long defaultValue) {
        return new ExecutionKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key for Integer with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new execution key for Integer
     */
    public static ExecutionKey<Integer> ofInt(@NotNull String name) {
        return new ExecutionKey<>(name, null);
    }

    /**
     * Create a key for Integer with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new execution key for Integer
     */
    public static ExecutionKey<Integer> ofInt(@NotNull String name, int defaultValue) {
        return new ExecutionKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key for Double with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new execution key for Double
     */
    public static ExecutionKey<Double> ofDouble(@NotNull String name) {
        return new ExecutionKey<>(name, null);
    }

    /**
     * Create a key for Double with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new execution key for Double
     */
    public static ExecutionKey<Double> ofDouble(@NotNull String name, double defaultValue) {
        return new ExecutionKey<>(name, () -> defaultValue);
    }

    /**
     * Create a key for Boolean with no default (returns null if not set).
     *
     * @param name the unique name for this key
     * @return a new execution key for Boolean
     */
    public static ExecutionKey<Boolean> ofBoolean(@NotNull String name) {
        return new ExecutionKey<>(name, null);
    }

    /**
     * Create a key for Boolean with a custom default.
     *
     * @param name         the unique name for this key
     * @param defaultValue the default value
     * @return a new execution key for Boolean
     */
    public static ExecutionKey<Boolean> ofBoolean(@NotNull String name, boolean defaultValue) {
        return new ExecutionKey<>(name, () -> defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecutionKey<?> that = (ExecutionKey<?>) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "ExecutionKey{" + name + "}";
    }
}
