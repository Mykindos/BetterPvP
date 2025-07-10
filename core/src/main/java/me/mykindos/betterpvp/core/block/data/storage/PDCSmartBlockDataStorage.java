package me.mykindos.betterpvp.core.block.data.storage;

import com.google.common.base.Preconditions;
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
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

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

    @Inject
    public PDCSmartBlockDataStorage(SmartBlockFactory smartBlockFactory, SmartBlockRegistry smartBlockRegistry, Provider<SmartBlockDataManager> smartBlockDataManagerProvider) {
        this.smartBlockFactory = smartBlockFactory;
        this.smartBlockRegistry = smartBlockRegistry;
        this.smartBlockDataManagerProvider = smartBlockDataManagerProvider;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void save(@NotNull SmartBlockInstance instance, @NotNull SmartBlockData<T> data) {
        Preconditions.checkArgument(instance.getType() instanceof DataHolder<?>, "Instance must be a DataHolder to save data");
        PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(instance.getHandle());

        final SmartBlockDataSerializer<T> serializer = ((DataHolder<T>) instance.getType()).getDataSerializer();
        
        try {
            byte[] serializedData = serializer.serializeToBytes(data.get());
            pdc.set(DATA_KEY, PersistentDataType.BYTE_ARRAY, serializedData);
            pdc.set(TYPE_KEY, PersistentDataType.STRING, instance.getType().getKey());
            UtilBlock.setPersistentDataContainer(instance.getHandle(), pdc);
        } catch (Exception e) {
            log.error("Failed to serialize SmartBlock data for instance {}: {}", instance, e.getMessage()).submit();
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<SmartBlockData<T>> load(@NotNull SmartBlockInstance instance) {
        final SmartBlock smartBlock = instance.getType();
        if (!(smartBlock instanceof DataHolder<?>)) {
            return Optional.empty(); // Not a DataHolder type
        }

        final Block handle = instance.getHandle();
        PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(handle);
        final DataHolder<T> holder = (DataHolder<T>) smartBlock;
        SmartBlockDataSerializer<T> serializer = holder.getDataSerializer();

        // Check if we have serialized data
        if (!pdc.has(DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
            return Optional.empty(); // No data available for this block
        }

        try {
            byte[] serializedData = pdc.get(DATA_KEY, PersistentDataType.BYTE_ARRAY);
            if (serializedData == null) {
                return Optional.empty();
            }
            
            T data = serializer.deserializeFromBytes(serializedData);
            final Class<T> expectedType = holder.getDataType();
            return Optional.of(new SmartBlockData<>(instance, expectedType, data, smartBlockDataManagerProvider.get()));
        } catch (Exception e) {
            log.error("Failed to deserialize SmartBlock data for instance {}: {}", instance, e.getMessage()).submit();
            return Optional.empty();
        }
    }
    
    @Override
    public void remove(@NotNull SmartBlockInstance instance) {
        if (!(instance.getType() instanceof DataHolder<?> holder)) {
            return;
        }
        PersistentDataContainer container = UtilBlock.getPersistentDataContainer(instance.getHandle());
        container.remove(DATA_KEY);
        container.remove(TYPE_KEY);
        UtilBlock.setPersistentDataContainer(instance.getHandle(), container);
    }
    
    @Override
    public @NotNull CompletableFuture<Map<Integer, SmartBlockData<?>>> loadChunk(@NotNull Chunk chunk) {
        Map<Integer, SmartBlockData<?>> chunkData = new HashMap<>();
        
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

                    getSmartBlock(instance.get(), dataHolder, container, block).ifPresent(smartBlockData -> {
                        // Use block's position as the key
                        int blockKey = UtilBlock.getBlockKey(block);
                        chunkData.put(blockKey, smartBlockData);
                        log.info("Loaded SmartBlock data for block at {}: {}", block.getLocation(), smartBlockData).submit();
                    });
                }
            }
        }

        // This can't be done async because it accesses the Bukkit API directly
        return CompletableFuture.completedFuture(chunkData);
    }

    private <T> Optional<SmartBlockData<T>> getSmartBlock(SmartBlockInstance instance, DataHolder<T> dataHolder, PersistentDataContainer container, Block block) {
        final SmartBlockDataSerializer<T> serializer = dataHolder.getDataSerializer();
        if (container.has(DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
            try {
                byte[] serializedData = container.get(DATA_KEY, PersistentDataType.BYTE_ARRAY);
                if (serializedData != null) {
                    T data = serializer.deserializeFromBytes(serializedData);
                    final Class<T> expectedType = dataHolder.getDataType();
                    return Optional.of(new SmartBlockData<>(instance, expectedType, data, smartBlockDataManagerProvider.get()));
                }
            } catch (Exception e) {
                log.error("Error deserializing SmartBlock data for block at {}: {}", block.getLocation(), e.getMessage()).submit();
            }
        }
        return Optional.empty();
    }

    @Override
    public void removeChunk(@NotNull Chunk chunk) {
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

                    if (!(smartBlock instanceof DataHolder<?> dataHolder)) {
                        continue; // Not a DataHolder type
                    }

                    if (container.has(DATA_KEY, PersistentDataType.BYTE_ARRAY)) {
                        container.remove(DATA_KEY); // Remove the specific SmartBlock data
                    }

                    // Update the block's PDC
                    UtilBlock.setPersistentDataContainer(block, container);
//                    log.info("Removed SmartBlock data from block at {}", block.getLocation()).submit();
                }
            }
        }
    }
} 