package me.mykindos.betterpvp.core.interaction.context;

import me.mykindos.betterpvp.core.item.ItemInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mutable context passed through an interaction chain for data sharing between nodes.
 * <p>
 * This context has two scopes:
 * <ul>
 *   <li><b>Chain scope</b>: Data that persists across the entire chain execution (use {@link ChainKey})</li>
 *   <li><b>Execution scope</b>: Data scoped to a single interaction execution (use {@link ExecutionKey})</li>
 * </ul>
 * <p>
 * The key type determines the scope - no separate methods needed:
 * <pre>
 * context.set(ChainKey.of("my_data"), value);     // Chain scope
 * context.set(ExecutionKey.of("my_data"), value); // Execution scope
 * </pre>
 */
public class InteractionContext {

    /**
     * Timestamp when the chain was started. Persists across the entire chain.
     */
    public static final ChainKey<Long> CHAIN_START_TIME = ChainKey.ofLong("chain_start_time");

    /**
     * Timestamp when the current interaction execution started.
     * Set at the beginning of each interaction execution (execution-scoped).
     */
    public static final ExecutionKey<Long> INTERACTION_START_TIME = ExecutionKey.ofLong("interaction_start_time");

    /**
     * The item currently held by the player.
     */
    public static final ChainKey<ItemInstance> HELD_ITEM = ChainKey.of("held_item");

    /**
     * Chain-scoped data that persists across the entire chain.
     */
    private final Map<ScopedKey<?>, Object> chainData = new HashMap<>();

    /**
     * Execution-scoped data that is cleared before each interaction execution.
     */
    private final Map<ScopedKey<?>, Object> executionData = new HashMap<>();

    /**
     * Create a new empty context.
     */
    public InteractionContext() {
        chainData.put(CHAIN_START_TIME, System.currentTimeMillis());
    }

    /**
     * Set a value in the chain-scoped context.
     * Chain-scoped values persist across the entire chain execution.
     *
     * @param key   the chain key
     * @param value the value
     * @param <T>   the value type
     */
    public <T> void set(@NotNull ChainKey<T> key, @NotNull T value) {
        chainData.put(key, value);
    }

    /**
     * Get a value from the chain-scoped context, returning the key's default if not present.
     *
     * @param key the chain key
     * @param <T> the value type
     * @return an Optional containing the value if present, or the default value if the key has one
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@NotNull ChainKey<T> key) {
        Object value = chainData.get(key);
        if (value != null) {
            return Optional.of((T) value);
        }
        if (key.hasDefault()) {
            return Optional.ofNullable(key.getDefault());
        }
        return Optional.empty();
    }

    /**
     * Get a value from the chain-scoped context without considering defaults.
     *
     * @param key the chain key
     * @param <T> the value type
     * @return an Optional containing the value only if explicitly set
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getExplicit(@NotNull ChainKey<T> key) {
        Object value = chainData.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    /**
     * Get a value from the chain-scoped context, or return a fallback if not present.
     *
     * @param key      the chain key
     * @param fallback the fallback value
     * @param <T>      the value type
     * @return the value, the key's default, or the fallback
     */
    public <T> T getOrDefault(@NotNull ChainKey<T> key, @NotNull T fallback) {
        return get(key).orElse(fallback);
    }

    /**
     * Get a value from the chain-scoped context, or null if not present.
     *
     * @param key the chain key
     * @param <T> the value type
     * @return the value, the key's default, or null
     */
    @Nullable
    public <T> T getOrNull(@NotNull ChainKey<T> key) {
        return get(key).orElse(null);
    }

    /**
     * Check if the chain-scoped context contains an explicitly set value for the given key.
     *
     * @param key the chain key
     * @return true if the key has an explicitly set value
     */
    public boolean has(@NotNull ChainKey<?> key) {
        return chainData.containsKey(key);
    }

    /**
     * Remove a value from the chain-scoped context.
     *
     * @param key the chain key
     * @param <T> the value type
     * @return the removed value, or empty if not present
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> remove(@NotNull ChainKey<T> key) {
        Object value = chainData.remove(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    /**
     * Increment a numeric value in the chain-scoped context.
     *
     * @param key       the chain key
     * @param increment the amount to increment
     * @return the new value
     */
    public int increment(@NotNull ChainKey<Integer> key, int increment) {
        int current = getOrDefault(key, 0);
        int newValue = current + increment;
        set(key, newValue);
        return newValue;
    }

    /**
     * Set a value in the execution-scoped context.
     * Execution-scoped values are cleared before each interaction execution.
     *
     * @param key   the execution key
     * @param value the value
     * @param <T>   the value type
     */
    public <T> void set(@NotNull ExecutionKey<T> key, @NotNull T value) {
        executionData.put(key, value);
    }

    /**
     * Get a value from the execution-scoped context, returning the key's default if not present.
     *
     * @param key the execution key
     * @param <T> the value type
     * @return an Optional containing the value if present, or the default value if the key has one
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@NotNull ExecutionKey<T> key) {
        Object value = executionData.get(key);
        if (value != null) {
            return Optional.of((T) value);
        }
        if (key.hasDefault()) {
            return Optional.ofNullable(key.getDefault());
        }
        return Optional.empty();
    }

    /**
     * Get a value from the execution-scoped context without considering defaults.
     *
     * @param key the execution key
     * @param <T> the value type
     * @return an Optional containing the value only if explicitly set
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getExplicit(@NotNull ExecutionKey<T> key) {
        Object value = executionData.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    /**
     * Get a value from the execution-scoped context, or return a fallback.
     *
     * @param key      the execution key
     * @param fallback the fallback value
     * @param <T>      the value type
     * @return the value, the key's default, or the fallback
     */
    public <T> T getOrDefault(@NotNull ExecutionKey<T> key, @NotNull T fallback) {
        return get(key).orElse(fallback);
    }

    /**
     * Get a value from the execution-scoped context, or null if not present.
     *
     * @param key the execution key
     * @param <T> the value type
     * @return the value, the key's default, or null
     */
    @Nullable
    public <T> T getOrNull(@NotNull ExecutionKey<T> key) {
        return get(key).orElse(null);
    }

    /**
     * Check if the execution-scoped context contains an explicitly set value for the given key.
     *
     * @param key the execution key
     * @return true if the key has an explicitly set value
     */
    public boolean has(@NotNull ExecutionKey<?> key) {
        return executionData.containsKey(key);
    }

    /**
     * Remove a value from the execution-scoped context.
     *
     * @param key the execution key
     * @param <T> the value type
     * @return the removed value, or empty if not present
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> remove(@NotNull ExecutionKey<T> key) {
        Object value = executionData.remove(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    /**
     * Start a new interaction execution.
     * This clears execution-scoped data and sets the interaction start time.
     */
    public void startExecution() {
        executionData.clear();
        executionData.put(INTERACTION_START_TIME, System.currentTimeMillis());
    }

    /**
     * Clear execution-scoped data only.
     * Chain-scoped data is preserved.
     */
    public void clearExecution() {
        executionData.clear();
    }

    /**
     * Clear all values from both chain and execution scopes.
     */
    public void clear() {
        chainData.clear();
        executionData.clear();
    }

    /**
     * Reset the context for a new chain.
     * Clears all data and reinitializes chain start time.
     */
    public void reset() {
        chainData.clear();
        executionData.clear();
        chainData.put(CHAIN_START_TIME, System.currentTimeMillis());
    }

    /**
     * Reset only the chain-scoped data for a new chain execution.
     * Clears chain data and reinitializes chain start time.
     * Execution data is preserved (use {@link #startExecution()} to clear it).
     */
    public void resetChain() {
        chainData.clear();
        chainData.put(CHAIN_START_TIME, System.currentTimeMillis());
    }

    /**
     * Create a copy of this context.
     *
     * @return a new context with the same data in both scopes
     */
    public InteractionContext copy() {
        InteractionContext copy = new InteractionContext();
        copy.chainData.clear(); // Remove the default CHAIN_START_TIME
        copy.chainData.putAll(this.chainData);
        copy.executionData.putAll(this.executionData);
        return copy;
    }
}
