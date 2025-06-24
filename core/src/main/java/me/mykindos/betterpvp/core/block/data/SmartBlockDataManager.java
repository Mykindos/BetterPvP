package me.mykindos.betterpvp.core.block.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Bukkit.getWorld;

/**
 * Manages SmartBlock data instances, including caching and persistence to PDC.
 */
@Singleton
@CustomLog
public class SmartBlockDataManager {

    private final Cache<String, SmartBlockData<?>> dataCache;
    
    @Inject
    public SmartBlockDataManager() {
        this.dataCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .removalListener(this::onCacheRemoval)
            .build();
    }
    
    /**
     * Gets or creates block data for the given instance.
     * The data type is inferred from the SmartBlock's defined data type.
     * 
     * @param instance The block instance
     * @return Optional containing the block data if the block supports data
     */
    @SuppressWarnings("unchecked")
    public <T> SmartBlockData<T> getOrCreateData(@NotNull SmartBlockInstance instance) {
        if (!instance.supportsData()) {
            return null; // Block does not support data
        }

        String cacheKey = getCacheKey(instance);
        SmartBlockData<?> cached = dataCache.getIfPresent(cacheKey);
        
        if (cached != null) {
            return (SmartBlockData<T>) cached;
        }
        
        // Load from PDC or create default
        return loadOrCreateData(instance);
    }

    /**
     * Internal method to load or create data with a specific type.
     */
    @SuppressWarnings("unchecked")
    private <T> SmartBlockData<T> loadOrCreateData(@NotNull SmartBlockInstance instance) {
        SmartBlock smartBlock = instance.getType();
        final DataHolder<T> dataHolder = (DataHolder<T>) smartBlock;

        SmartBlockDataSerializer<T> serializer = dataHolder.getDataSerializer();
        final Block handle = instance.getHandle();
        PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(handle);
        
        // Get or create the data container for this serializer
        PersistentDataContainer dataContainer;
        if (pdc.has(serializer.getKey(), PersistentDataType.TAG_CONTAINER)) {
            dataContainer = Objects.requireNonNull(pdc.get(serializer.getKey(), PersistentDataType.TAG_CONTAINER));
        } else {
            dataContainer = pdc.getAdapterContext().newPersistentDataContainer();
        }
        
        T data;
        if (serializer.hasData(dataContainer)) {
            // Load existing data
            data = serializer.deserialize(dataContainer);
            log.info("Loaded existing data for block {} at {}", smartBlock.getKey(), handle.getLocation()).submit();
        } else {
            // Create default data
            data = dataHolder.createDefaultData();
            // Save the default data immediately
            serializer.serialize(data, dataContainer);
            pdc.set(serializer.getKey(), PersistentDataType.TAG_CONTAINER, dataContainer);
            UtilBlock.setPersistentDataContainer(handle, pdc);
            log.info("Created default data for block {} at {}", smartBlock.getKey(), handle.getLocation()).submit();
        }

        final Class<T> expectedType = ((DataHolder<T>) smartBlock).getDataType();
        SmartBlockData<T> blockData = new SmartBlockData<>(instance, expectedType, data, this);
        dataCache.put(getCacheKey(instance), blockData);
        return blockData;
    }
    
        /**
     * Saves block data to the PDC.
     */
    public <T> void saveToContainer(@NotNull SmartBlockData<T> blockData) {
        SmartBlockInstance instance = blockData.getBlockInstance();
        SmartBlock smartBlock = instance.getType();

        @SuppressWarnings("unchecked")
        SmartBlockDataSerializer<T> serializer = ((DataHolder<T>) smartBlock).getDataSerializer();
        PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(instance.getHandle());
        
        // Get or create the data container for this serializer
        PersistentDataContainer dataContainer;
        if (pdc.has(serializer.getKey(), PersistentDataType.TAG_CONTAINER)) {
            dataContainer = Objects.requireNonNull(pdc.get(serializer.getKey(), PersistentDataType.TAG_CONTAINER));
        } else {
            dataContainer = pdc.getAdapterContext().newPersistentDataContainer();
        }
        
        serializer.serialize(blockData.get(), dataContainer);
        pdc.set(serializer.getKey(), PersistentDataType.TAG_CONTAINER, dataContainer);
        UtilBlock.setPersistentDataContainer(instance.getHandle(), pdc);
    }
    
    /**
     * Removes data from cache and PDC for a block instance.
     */
    public void removeData(@NotNull SmartBlockInstance instance) {
        String cacheKey = getCacheKey(instance);
        dataCache.invalidate(cacheKey);
        
        // Clear PDC data
        PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(instance.getHandle());
        
        // Get the serializer to know which keys to clear
        SmartBlock smartBlock = instance.getType();
        final SmartBlockDataSerializer<?> serializer = ((DataHolder<?>) smartBlock).getDataSerializer();

        // Remove the data container for this serializer
        pdc.remove(serializer.getKey());
        UtilBlock.setPersistentDataContainer(instance.getHandle(), pdc);
    }
    
    /**
     * Generates a cache key for a block instance.
     */
    private String getCacheKey(@NotNull SmartBlockInstance instance) {
        final Block handle = instance.getHandle();
        return String.format("%s_%d_%d_%d",
                handle.getWorld().getName(),
                handle.getX(),
                handle.getY(),
                handle.getZ());
    }
    
    /**
     * Handles cache removal events.
     */
    private void onCacheRemoval(String key, SmartBlockData<?> data, RemovalCause cause) {
        if (cause.wasEvicted()) {
            // Save to PDC before eviction
            log.info("Cache evicted data for {}, saving to PDC", key).submit();
            saveToContainer(data);
        }
    }
} 