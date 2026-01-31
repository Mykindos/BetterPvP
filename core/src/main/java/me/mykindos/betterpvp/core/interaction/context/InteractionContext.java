package me.mykindos.betterpvp.core.interaction.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mutable context passed through an interaction chain for data sharing between nodes.
 * Allows interactions to pass data to subsequent interactions in the chain.
 */
public class InteractionContext {

    public static final MetaKey<Long> CHAIN_START_TIME = MetaKey.ofLong("chain_start_time");

    private final Map<MetaKey<?>, Object> data = new HashMap<>();

    /**
     * Create a new empty context.
     */
    public InteractionContext() {
        set(CHAIN_START_TIME, System.currentTimeMillis());
    }

    /**
     * Set a value in the context.
     *
     * @param key   the key
     * @param value the value
     * @param <T>   the value type
     */
    public <T> void set(@NotNull MetaKey<T> key, @NotNull T value) {
        data.put(key, value);
    }

    /**
     * Get a value from the context, returning the key's default if not present.
     *
     * @param key the key
     * @param <T> the value type
     * @return an Optional containing the value if present, or the default value if the key has one
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@NotNull MetaKey<T> key) {
        Object value = data.get(key);
        if (value != null) {
            return Optional.of((T) value);
        }
        // Check for default value
        if (key.hasDefault()) {
            return Optional.ofNullable(key.getDefault());
        }
        return Optional.empty();
    }

    /**
     * Get a value from the context without considering defaults.
     * Returns empty if the value was not explicitly set.
     *
     * @param key the key
     * @param <T> the value type
     * @return an Optional containing the value only if explicitly set
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getExplicit(@NotNull MetaKey<T> key) {
        Object value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    /**
     * Get a value from the context, or return a fallback if not present and no default.
     *
     * @param key      the key
     * @param fallback the fallback value if no value and no default
     * @param <T>      the value type
     * @return the value, the key's default, or the fallback
     */
    public <T> T getOrDefault(@NotNull MetaKey<T> key, @NotNull T fallback) {
        return get(key).orElse(fallback);
    }

    /**
     * Get a value from the context, or null if not present.
     *
     * @param key the key
     * @param <T> the value type
     * @return the value, the key's default, or null
     */
    @Nullable
    public <T> T getOrNull(@NotNull MetaKey<T> key) {
        return get(key).orElse(null);
    }

    /**
     * Check if the context contains an explicitly set value for the given key.
     *
     * @param key the key
     * @return true if the key has an explicitly set value
     */
    public boolean has(@NotNull MetaKey<?> key) {
        return data.containsKey(key);
    }

    /**
     * Remove a value from the context.
     *
     * @param key the key
     * @param <T> the value type
     * @return the removed value, or empty if not present
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> remove(@NotNull MetaKey<T> key) {
        Object value = data.remove(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    /**
     * Clear all values from the context.
     */
    public void clear() {
        data.clear();
    }

    /**
     * Increment a numeric value in the context.
     *
     * @param key       the key
     * @param increment the amount to increment
     * @return the new value
     */
    public int increment(@NotNull MetaKey<Integer> key, int increment) {
        int current = getOrDefault(key, 0);
        int newValue = current + increment;
        set(key, newValue);
        return newValue;
    }

    /**
     * Create a copy of this context.
     *
     * @return a new context with the same data
     */
    public InteractionContext copy() {
        InteractionContext copy = new InteractionContext();
        copy.data.putAll(this.data);
        return copy;
    }
}
