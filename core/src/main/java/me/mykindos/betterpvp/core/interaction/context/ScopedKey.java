package me.mykindos.betterpvp.core.interaction.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A type-safe key for storing and retrieving values from an {@link InteractionContext}.
 * The key type determines which scope the value is stored in:
 * <ul>
 *   <li>{@link ChainKey} - Chain scope: data persists across the entire chain execution</li>
 *   <li>{@link ExecutionKey} - Execution scope: data is cleared before each interaction execution</li>
 * </ul>
 * <p>
 * This sealed interface ensures compile-time safety - using the wrong scope is impossible
 * because the key type itself determines the scope.
 *
 * @param <T> the type of value this key represents
 */
public sealed interface ScopedKey<T> permits ChainKey, ExecutionKey {

    /**
     * Get the name of this key.
     *
     * @return the key name
     */
    @NotNull
    String getName();

    /**
     * Check if this key has a default value supplier.
     *
     * @return true if a default is set
     */
    boolean hasDefault();

    /**
     * Get the default value for this key.
     *
     * @return the default value, or null if no default is set
     */
    @Nullable
    T getDefault();
}
