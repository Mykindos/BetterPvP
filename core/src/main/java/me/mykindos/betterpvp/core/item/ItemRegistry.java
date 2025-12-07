package me.mykindos.betterpvp.core.item;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.adapter.BukkitMaterialAdapter;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Thread-safe registry for managing modular items with components.
 * Supports registration, lookup, and tracking of all game items.
 */
@CustomLog
@Singleton
public class ItemRegistry {

    private static final String REGISTRATION_LOG_FORMAT = "Registered item: {}";

    private final Map<NamespacedKey, BaseItem> items = new ConcurrentHashMap<>();
    private final Map<Material, BaseItem> fallbackItems = new ConcurrentHashMap<>();
    private final List<BiConsumer<NamespacedKey, BaseItem>> onRegisterCallbacks = new CopyOnWriteArrayList<>();

    // Sorted storage by NamespacedKey
    private final ConcurrentSkipListMap<NamespacedKey, BaseItem> sortedItems = new ConcurrentSkipListMap<>(
            Comparator.comparing(NamespacedKey::toString, String.CASE_INSENSITIVE_ORDER)
    );

    private final ExecutorService executor = Executors.newWorkStealingPool();

    @Inject
    private ItemRegistry() {
        // Kick off async registration at startup
        CompletableFuture.runAsync(() -> {
            BukkitMaterialAdapter.registerDefaults(fallbackItems, items, sortedItems);
        }, executor).whenComplete((v, ex) -> {
            if (ex != null) {
                log.error("Error during async material registration", ex).submit();
            } else {
                log.info("Finished async material fallback registration").submit();
            }
        });
    }

    public void registerItem(@NotNull NamespacedKey key, @NotNull BaseItem item) {
        final Material type = item.getModel().getType();
        Preconditions.checkArgument(type.isItem() && !type.isAir(), "Item must be an item");

        BaseItem prev = items.put(key, item);
        sortedItems.put(key, item); // keep sorted copy in sync

        if (prev != null) {
            log.warn("Item with key {} is already registered. Overwriting with new item.", key).submit();
        }

        for (BiConsumer<NamespacedKey, BaseItem> callback : onRegisterCallbacks) {
            callback.accept(key, item);
        }
        log.info(REGISTRATION_LOG_FORMAT, key).submit();
    }

    public void addRegisterCallback(@NotNull BiConsumer<NamespacedKey, BaseItem> callback) {
        onRegisterCallbacks.add(callback);
    }

    public void registerFallbackItem(@NotNull NamespacedKey key, @NotNull Material material, @NotNull BaseItem item) {
        final BaseItem existing = fallbackItems.get(material);
        if (existing instanceof VanillaItem) {
            // This was auto-generated above
            sortedItems.remove(material.getKey());
            items.remove(material.getKey());
            fallbackItems.remove(material);
            log.info("Removing auto-generated vanilla item fallback for material {}", material);
        }

        registerItem(key, item);
        BaseItem prev = fallbackItems.put(material, item);
        if (prev != null) {
            log.warn("Material {} already has a fallback item registered. Overwriting with new item: {}", material, key).submit();
        }
        log.info("Registered fallback item for material {}: {}", material, key).submit();
    }

    public BaseItem getFallbackItem(@NotNull Material material) {
        return fallbackItems.get(material);
    }

    public BaseItem getItem(@NotNull NamespacedKey key) {
        return items.get(key);
    }

    public BaseItem getItem(@NotNull String namespacedKey) {
        NamespacedKey namespacedKeyObj = NamespacedKey.fromString(namespacedKey);
        if (namespacedKeyObj == null) {
            log.warn("Invalid namespaced key format: {}", namespacedKey).submit();
            return null;
        }
        return getItem(namespacedKeyObj);
    }

    @Nullable
    public <T extends BaseItem> T getItemByClass(@NotNull Class<T> itemClass) {
        for (BaseItem item : items.values()) {
            if (item.getClass() == itemClass) {
                return itemClass.cast(item);
            }
        }
        return null;
    }

    @NotNull
    public Map<NamespacedKey, BaseItem> getItemsByKey(@NotNull String key) {
        return items.entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equalsIgnoreCase(key))
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @NotNull
    public Map<NamespacedKey, BaseItem> getItems() {
        return Map.copyOf(items);
    }

    /**
     * Get all items in sorted order by their {@link NamespacedKey}.
     * The returned list is a snapshot and will reflect current registry contents.
     */
    @NotNull
    public Map<NamespacedKey, BaseItem> getItemsSorted() {
        return Map.copyOf(sortedItems);
    }

    @Nullable
    public NamespacedKey getKey(@NotNull BaseItem item) {
        for (Map.Entry<NamespacedKey, BaseItem> entry : items.entrySet()) {
            if (entry.getValue().equals(item)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean isRegistered(@NotNull BaseItem baseItem) {
        return items.containsValue(baseItem) || fallbackItems.containsValue(baseItem);
    }
}
