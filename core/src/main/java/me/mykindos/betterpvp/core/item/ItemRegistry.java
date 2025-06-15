package me.mykindos.betterpvp.core.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.component.registry.ComponentRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry for managing modular items with components.
 * Handles registration, lookup, and tracking of all game items.
 */
@CustomLog
@Singleton
public class ItemRegistry {

    private static final String REGISTRATION_LOG_FORMAT = "Registered item: {}";

    @Getter
    private final ComponentRegistry componentRegistry;
    private final Map<NamespacedKey, BaseItem> items = new HashMap<>();
    private final Map<Material, BaseItem> fallbackItems = new HashMap<>();

    @Inject
    private ItemRegistry(@NotNull ComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    /**
     * Register an item with the registry
     *
     * @param key  The namespaced key for the item
     * @param item The item to register
     */
    public void registerItem(@NotNull NamespacedKey key, @NotNull BaseItem item) {
        items.put(key, item);
        log.info(REGISTRATION_LOG_FORMAT, key).submit();
    }

    /**
     * Register an item and mark it as a fallback item for a specific material.
     * These items are used when no specific item is defined for that material.
     *
     * @param key The namespaced key for the item
     * @param material The material to register the fallback for
     * @param item The fallback item to register
     */
    public void registerFallbackItem(@NotNull NamespacedKey key, @NotNull Material material, @NotNull BaseItem item) {
        this.registerItem(key, item);
        if (fallbackItems.containsKey(material)) {
            log.warn("Material {} already has a fallback item registered. Overwriting with new item: {}", material, key).submit();
        }
        fallbackItems.put(material, item);
        log.info("Registered fallback item for material {}: {}", material, key).submit();
    }

    /**
     * Get the fallback item for a specific material
     *
     * @param material The material to look up
     * @return The fallback item, or null if not found
     */
    @Nullable
    public BaseItem getFallbackItem(@NotNull Material material) {
        return fallbackItems.get(material);
    }

    /**
     * Get an item by its exact {@link NamespacedKey}
     *
     * @param key The namespaced key to look up
     * @return The corresponding item, or null if not found
     */
    @Nullable
    public BaseItem getItem(@NotNull NamespacedKey key) {
        return items.get(key);
    }

    /**
     * Get an item by its exact stringified {@link NamespacedKey}
     * @param namespacedKey The stringified namespaced key to look up (format: "namespace:key")
     * @return The corresponding item, or null if not found
     */
    @Nullable
    public BaseItem getItem(@NotNull String namespacedKey) {
        NamespacedKey namespacedKeyObj = NamespacedKey.fromString(namespacedKey);
        if (namespacedKeyObj == null) {
            log.warn("Invalid namespaced key format: {}", namespacedKey).submit();
            return null;
        }
        return getItem(namespacedKeyObj);
    }

    /**
     * Get an item by its exact class type (no superclasses)
     *
     * @param itemClass The class type of the item to look up
     * @return The first item found of the specified class, or null if not found
     */
    @Nullable
    public <T extends BaseItem> T getItemByClass(@NotNull Class<T> itemClass) {
        return items.values().stream()
                .filter(item -> item.getClass() == itemClass)
                .map(itemClass::cast)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all items matching a specific key string
     *
     * @param key The key string to match (without namespace)
     * @return A set of items with matching keys
     */
    @NotNull
    public Map<NamespacedKey, BaseItem> getItemsByKey(@NotNull String key) {
        return items.entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equalsIgnoreCase(key))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get all registered items
     *
     * @return An unmodifiable map of all registered items
     */
    @NotNull
    public Map<NamespacedKey, BaseItem> getItems() {
        return Collections.unmodifiableMap(items);
    }

    /**
     * Find the key associated with a specific item
     *
     * @param item The item to look up
     * @return The corresponding key, or null if the item is not registered
     */
    @Nullable
    public NamespacedKey getKey(@NotNull BaseItem item) {
        return items.entrySet().stream()
                .filter(entry -> entry.getValue().equals(item))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if an item is registered in the registry
     *
     * @param baseItem The item to check
     * @return True if the item is registered, false otherwise
     */
    public boolean isRegistered(@NotNull BaseItem baseItem) {
        return items.containsValue(baseItem) || fallbackItems.containsValue(baseItem);
    }
}