package me.mykindos.betterpvp.core.block.data.storage;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataSerializationService;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * PDC (PersistentDataContainer) storage implementation for SmartBlockData.
 * This stores serialized byte data in PDC for compatibility with the new storage layer.
 */
@Singleton
@CustomLog
public class PDCSmartBlockDataStorage implements SmartBlockDataStorage {

    private static final NamespacedKey TYPE_KEY = new NamespacedKey("betterpvp", "block_type");
    private static final NamespacedKey DATA_KEY = new NamespacedKey("betterpvp", "block_data");
    private final SmartBlockFactory smartBlockFactory;
    private final SmartBlockRegistry smartBlockRegistry;
    private final Provider<SmartBlockDataManager> smartBlockDataManagerProvider;
    private final SmartBlockDataSerializationService serializationService;

    @Inject
    public PDCSmartBlockDataStorage(SmartBlockFactory smartBlockFactory, 
                                  SmartBlockRegistry smartBlockRegistry, 
                                  Provider<SmartBlockDataManager> smartBlockDataManagerProvider,
                                  SmartBlockDataSerializationService serializationService) {
        this.smartBlockFactory = smartBlockFactory;
        this.smartBlockRegistry = smartBlockRegistry;
        this.smartBlockDataManagerProvider = smartBlockDataManagerProvider;
        this.serializationService = serializationService;
    }

    @Override
    public <T> CompletableFuture<Void> save(@NotNull SmartBlockInstance instance, @NotNull SmartBlockData<T> data) {
        if (!serializationService.supportsDataStorage(instance)) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Instance must be a DataHolder to save data"));
        }

        return serializationService.serialize(instance, data.get())
            .thenApply(serializedData -> {
                try {
                    PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(instance.getHandle());
                    pdc.set(DATA_KEY, PersistentDataType.BYTE_ARRAY, serializedData);
                    pdc.set(TYPE_KEY, PersistentDataType.STRING, instance.getType().getKey());
                    UtilBlock.setPersistentDataContainer(instance.getHandle(), pdc);
                    return (Void) null;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to save SmartBlock data to PDC", e);
                }
            })
            .exceptionally(throwable -> {
                log.error("Failed to save SmartBlock data for instance {}", instance.getHandle().getLocation(), throwable).submit();
                return null;
            });
    }
    
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<SmartBlockData<T>> load(@NotNull SmartBlockInstance instance) {
        if (!serializationService.supportsDataStorage(instance)) {
            return CompletableFuture.completedFuture(null);
        }

        final Block handle = instance.getHandle();
        PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(handle);

        // Check if we have serialized data
        if (!pdc.has(DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
            return CompletableFuture.completedFuture(null); // No data available for this block
        }

        try {
            byte[] serializedData = pdc.get(DATA_KEY, PersistentDataType.BYTE_ARRAY);
            if (serializedData == null) {
                return CompletableFuture.completedFuture(null);
            }

            final DataHolder<T> holder = (DataHolder<T>) instance.getType();
            final Class<T> expectedType = holder.getDataType();
            
            return serializationService.deserialize(instance, expectedType, serializedData)
                .thenApply(data -> new SmartBlockData<>(instance, expectedType, data, smartBlockDataManagerProvider.get()))
                .exceptionally(throwable -> {
                    log.error("Error deserializing SmartBlock data for instance {}", instance.getHandle().getLocation(), throwable).submit();
                    return null;
                });
        } catch (Exception e) {
            log.error("Error loading SmartBlock data for instance {}", instance.getHandle().getLocation(), e).submit();
            return CompletableFuture.completedFuture(null);
        }
    }
    
    @Override
    public CompletableFuture<Void> remove(@NotNull SmartBlockInstance instance) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!serializationService.supportsDataStorage(instance)) {
                    return (Void) null; // Nothing to remove
                }
                
                PersistentDataContainer container = UtilBlock.getPersistentDataContainer(instance.getHandle());
                container.remove(DATA_KEY);
                container.remove(TYPE_KEY);
                UtilBlock.setPersistentDataContainer(instance.getHandle(), container);
                return (Void) null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to remove SmartBlock data from PDC", e);
            }
        }).exceptionally(throwable -> {
            log.error("Error removing SmartBlock data for instance {}", instance.getHandle().getLocation(), throwable).submit();
            return null;
        });
    }
    
    @Override
    public @NotNull CompletableFuture<Map<Long, SmartBlockData<?>>> loadChunk(@NotNull Chunk chunk) {
        Map<Long, SmartBlockData<?>> chunkData = new HashMap<>();
        
        // For PDC storage, we need to scan all blocks in the chunk
        // This is less efficient than database storage but maintains compatibility
        for (int x = 0; x < 16; x++) {
            for (int y = chunk.getWorld().getMinHeight(); y <= chunk.getWorld().getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    PersistentDataContainer container = UtilBlock.getPersistentDataContainer(block);
                    final String typeKey = container.get(TYPE_KEY, PersistentDataType.STRING);
                    if (typeKey == null) {
                        continue; // No SmartBlock data on this block
                    }

                    SmartBlock smartBlock = smartBlockRegistry.getBlock(typeKey);
                    if (!(smartBlock instanceof DataHolder<?> dataHolder)) {
                        continue;
                    }

                    final Optional<SmartBlockInstance> instance = smartBlockFactory.from(block);
                    if (instance.isEmpty()) {
                        log.warn("Failed to create SmartBlockInstance for block at {}. Skipping.", block.getLocation()).submit();
                        continue;
                    }

                    SmartBlockData<?> smartBlockData = getSmartBlock(instance.get(), dataHolder, container, block);
                    if (smartBlockData != null) {
                        // Use block's position as the key
                        long blockKey = UtilBlock.getBlockKey(block);
                        chunkData.put(blockKey, smartBlockData);
                        log.info("Loaded SmartBlock data for block at {}: {}", block.getLocation(), smartBlockData).submit();
                    }
                }
            }
        }

        // This can't be done async because it accesses the Bukkit API directly
        return CompletableFuture.completedFuture(chunkData);
    }

    private <T> SmartBlockData<T> getSmartBlock(SmartBlockInstance instance, DataHolder<T> dataHolder, PersistentDataContainer container, Block block) {
        if (container.has(DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
            try {
                byte[] serializedData = container.get(DATA_KEY, PersistentDataType.BYTE_ARRAY);
                if (serializedData != null) {
                    final Class<T> expectedType = dataHolder.getDataType();
                    T data = serializationService.deserializeSync(instance, expectedType, serializedData);
                    return new SmartBlockData<>(instance, expectedType, data, smartBlockDataManagerProvider.get());
                }
            } catch (Exception e) {
                log.error("Error deserializing SmartBlock data for block at {}: {}", block.getLocation(), e.getMessage()).submit();
            }
        }
        return null;
    }

    @Override
    public CompletableFuture<Void> removeChunk(@NotNull Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // For PDC storage, data is stored directly on blocks
                // We can remove all data of this type from the chunk
                for (int x = 0; x < 16; x++) {
                    for (int y = chunk.getWorld().getMinHeight(); y <= chunk.getWorld().getMaxHeight(); y++) {
                        for (int z = 0; z < 16; z++) {
                            Block block = chunk.getBlock(x, y, z);
                            PersistentDataContainer container = UtilBlock.getPersistentDataContainer(block);

                            if (!container.has(TYPE_KEY, PersistentDataType.STRING)) {
                                continue; // No SmartBlock data on this block
                            }

                            // Remove the SmartBlock type key
                            String typeKey = container.get(TYPE_KEY, PersistentDataType.STRING);
                            SmartBlock smartBlock = smartBlockRegistry.getBlock(typeKey);
                            container.remove(TYPE_KEY);

                            if (!(smartBlock instanceof DataHolder<?>)) {
                                continue; // Not a DataHolder type
                            }

                            if (container.has(DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
                                container.remove(DATA_KEY); // Remove the specific SmartBlock data
                            }

                            // Update the block's PDC
                            UtilBlock.setPersistentDataContainer(block, container);
                        }
                    }
                }
                return (Void) null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to remove chunk SmartBlock data from PDC", e);
            }
        }).exceptionally(throwable -> {
            log.error("Error removing chunk SmartBlock data for chunk {},{}", 
                chunk.getX(), chunk.getZ(), throwable).submit();
            return null;
        });
    }
} 