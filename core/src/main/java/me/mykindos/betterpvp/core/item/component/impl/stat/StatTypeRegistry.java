package me.mykindos.betterpvp.core.item.component.impl.stat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for all stat types in the system.
 * <p>
 * This singleton maintains a map of all registered stat types,
 * indexed by their NamespacedKey. Built-in types are registered
 * via StatTypes.registerAll(), and custom plugins can register
 * their own types at runtime.
 */
@Singleton
public class StatTypeRegistry {

    private final Map<NamespacedKey, StatType<?>> types = new HashMap<>();

    @Inject
    public StatTypeRegistry() {
        // Registry is populated by StatTypes static initializer
    }

    /**
     * Register a new stat type.
     *
     * @param type The stat type to register
     * @param <T> The value type for this stat
     * @throws IllegalArgumentException if a type with this key is already registered
     */
    public <T> void register(@NotNull StatType<T> type) {
        if (types.containsKey(type.getKey())) {
            throw new IllegalArgumentException("Stat type already registered: " + type.getKey());
        }
        types.put(type.getKey(), type);
    }

    /**
     * Get a stat type by its key.
     *
     * @param key The namespaced key of the stat type
     * @param <T> The value type for this stat
     * @return An Optional containing the stat type if found, empty otherwise
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<StatType<T>> getType(@NotNull NamespacedKey key) {
        return Optional.ofNullable((StatType<T>) types.get(key));
    }

    /**
     * Get all registered stat types.
     *
     * @return An unmodifiable copy of all registered types
     */
    public Map<NamespacedKey, StatType<?>> getAllTypes() {
        return Map.copyOf(types);
    }
}
