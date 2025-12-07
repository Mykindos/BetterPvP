package me.mykindos.betterpvp.core.block.data.storage;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockChunkLoadingService;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataSerializationService;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Record4;
import org.jooq.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static me.mykindos.betterpvp.core.database.jooq.Tables.SMART_BLOCK_DATA;

/**
 * Database storage implementation for SmartBlockData using optimized services.
 * Now much cleaner with separation of concerns through dedicated services.
 */
@CustomLog
@Singleton
public class DatabaseSmartBlockDataStorage implements SmartBlockDataStorage {

    private final Database database;
    private final SmartBlockFactory smartBlockFactory;
    private final Provider<SmartBlockDataManager> dataManagerProvider;
    private final SmartBlockDataSerializationService serializationService;
    private final SmartBlockChunkLoadingService chunkLoadingService;
    private final Core plugin;

    @Inject
    public DatabaseSmartBlockDataStorage(Database database, 
                                       SmartBlockFactory smartBlockFactory, 
                                       Provider<SmartBlockDataManager> dataManagerProvider,
                                       SmartBlockDataSerializationService serializationService,
                                       SmartBlockChunkLoadingService chunkLoadingService,
                                       Core plugin) {
        this.database = database;
        this.smartBlockFactory = smartBlockFactory;
        this.dataManagerProvider = dataManagerProvider;
        this.serializationService = serializationService;
        this.chunkLoadingService = chunkLoadingService;
        this.plugin = plugin;
    }

    @Override
    public boolean allowsAsynchronousLoading() {
        return true;
    }

    @Override
    public <T> CompletableFuture<Void> save(@NotNull SmartBlockInstance instance, @NotNull SmartBlockData<T> data) {
        if (!serializationService.supportsDataStorage(instance)) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Instance must be a DataHolder to save data"));
        }

        return serializationService.serialize(instance, data.get())
            .thenCompose(serializedData -> persistToDatabase(instance, data, serializedData))
            .exceptionally(throwable -> {
                log.error("Failed to save SmartBlock data for {}", instance.getHandle().getLocation(), throwable).submit();
                return null;
            });
    }

    /**
     * Persists serialized data to the database.
     */
    private <T> CompletableFuture<Void> persistToDatabase(@NotNull SmartBlockInstance instance, 
                                                         @NotNull SmartBlockData<T> data, 
                                                         byte[] serializedData) {
        try {
            return database.getAsyncDslContext().executeAsyncVoid(ctx -> {
                ctx.insertInto(SMART_BLOCK_DATA)
                        .set(SMART_BLOCK_DATA.REALM, Core.getCurrentRealm())
                        .set(SMART_BLOCK_DATA.WORLD, instance.getLocation().getWorld().getName())
                        .set(SMART_BLOCK_DATA.CHUNK_KEY, Chunk.getChunkKey(instance.getLocation()))
                        .set(SMART_BLOCK_DATA.BLOCK_KEY, UtilBlock.getBlockKey(instance.getHandle()))
                        .set(SMART_BLOCK_DATA.BLOCK_TYPE, instance.getType().getKey())
                        .set(SMART_BLOCK_DATA.DATA_TYPE_CLASS, data.getDataType().getName())
                        .set(SMART_BLOCK_DATA.DATA, serializedData)
                        .onConflict()
                        .doUpdate()
                        .set(SMART_BLOCK_DATA.BLOCK_TYPE, instance.getType().getKey())
                        .set(SMART_BLOCK_DATA.DATA_TYPE_CLASS, data.getDataType().getName())
                        .set(SMART_BLOCK_DATA.DATA, serializedData)
                        .execute();
            });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Nullable
    public <T> CompletableFuture<SmartBlockData<T>> load(@NotNull SmartBlockInstance instance) {
        if (!serializationService.supportsDataStorage(instance)) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            long chunkKey = Chunk.getChunkKey(instance.getLocation());
            long blockKey = UtilBlock.getBlockKey(instance.getHandle());
            String world = instance.getLocation().getWorld().getName();

            return database.getAsyncDslContext().executeAsync(ctx -> {
                return ctx.select(
                                SMART_BLOCK_DATA.DATA_TYPE_CLASS,
                                SMART_BLOCK_DATA.DATA)
                        .from(SMART_BLOCK_DATA)
                        .where(SMART_BLOCK_DATA.REALM.eq(Core.getCurrentRealm()))
                        .and(SMART_BLOCK_DATA.WORLD.eq(world))
                        .and(SMART_BLOCK_DATA.CHUNK_KEY.eq(chunkKey))
                        .and(SMART_BLOCK_DATA.BLOCK_KEY.eq(blockKey))
                        .fetchOptional();
            }).thenCompose(optional -> {
                if (optional.isPresent()) {
                    var record = optional.get();
                    return deserializeSmartBlockDataRaw(
                            instance,
                            record.get(SMART_BLOCK_DATA.DATA_TYPE_CLASS),
                            record.get(SMART_BLOCK_DATA.DATA));
                } else {
                    return CompletableFuture.completedFuture((SmartBlockData<T>) null);
                }
            }).exceptionally(throwable -> {
                log.error("Failed to load SmartBlock data for {}", instance.getHandle().getLocation(), throwable).submit();
                return null;
            });
        } catch (Exception e) {
            log.error("Error loading SmartBlock data for {}", instance.getHandle().getLocation(), e).submit();
            return CompletableFuture.completedFuture(null);
        }
    }


    @Override
    public CompletableFuture<Void> remove(@NotNull SmartBlockInstance instance) {
        try {
            Chunk chunk = instance.getHandle().getChunk();
            long chunkKey = chunk.getChunkKey();
            long blockKey = UtilBlock.getBlockKey(instance.getHandle());
            String world = instance.getLocation().getWorld().getName();

            return database.getAsyncDslContext().executeAsyncVoid(ctx -> {
                ctx.deleteFrom(SMART_BLOCK_DATA)
                        .where(SMART_BLOCK_DATA.REALM.eq(Core.getCurrentRealm()))
                        .and(SMART_BLOCK_DATA.WORLD.eq(world))
                        .and(SMART_BLOCK_DATA.CHUNK_KEY.eq(chunkKey))
                        .and(SMART_BLOCK_DATA.BLOCK_KEY.eq(blockKey))
                        .execute();
            }).exceptionally(throwable -> {
                log.error("Failed to remove SmartBlock data for {}", instance.getHandle().getLocation(), throwable).submit();
                return null;
            });

        } catch (Exception e) {
            log.error("Error removing SmartBlock data for {}", instance.getHandle().getLocation(), e).submit();
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public @NotNull CompletableFuture<Map<Long, SmartBlockData<?>>> loadChunk(@NotNull Chunk chunk) {
        return chunkLoadingService.loadChunk(chunk, this::loadChunkFromDatabase);
    }

    /**
     * Loads chunk data from the database. Used by the chunk loading service.
     */
    private CompletableFuture<Map<Long, SmartBlockData<?>>> loadChunkFromDatabase(@NotNull Chunk chunk) {
        try {
            long chunkKey = chunk.getChunkKey();
            String world = chunk.getWorld().getName();

            return database.getAsyncDslContext().executeAsync(ctx -> {
                return ctx.select(
                                SMART_BLOCK_DATA.BLOCK_KEY,
                                SMART_BLOCK_DATA.BLOCK_TYPE,
                                SMART_BLOCK_DATA.DATA_TYPE_CLASS,
                                SMART_BLOCK_DATA.DATA)
                        .from(SMART_BLOCK_DATA)
                        .where(SMART_BLOCK_DATA.REALM.eq(Core.getCurrentRealm()))
                        .and(SMART_BLOCK_DATA.WORLD.eq(world))
                        .and(SMART_BLOCK_DATA.CHUNK_KEY.eq(chunkKey))
                        .orderBy(SMART_BLOCK_DATA.BLOCK_KEY)
                        .fetch();
            }).thenCompose(result -> processChunkResultSet(chunk, result))
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Failed to load chunk SmartBlock data for chunk {},{}",
                                    chunk.getX(), chunk.getZ(), throwable).submit();
                        } else {
                            log.debug("Chunk {},{} loaded with {} SmartBlock entries",
                                    chunk.getX(), chunk.getZ(), result.size()).submit();
                        }
                    })
                    .exceptionally(throwable -> {
                        log.error("Error loading chunk SmartBlock data for chunk {},{}",
                                chunk.getX(), chunk.getZ(), throwable).submit();
                        return new HashMap<>();
                    });

        } catch (Exception e) {
            log.error("Error loading chunk SmartBlock data for chunk {},{}",
                    chunk.getX(), chunk.getZ(), e).submit();
            return CompletableFuture.completedFuture(new HashMap<>());
        }
    }

    /**
     * Processes database result set for chunk loading.
     */
    private CompletableFuture<Map<Long, SmartBlockData<?>>> processChunkResultSet(
            @NotNull Chunk chunk,
            Result<Record4<Long, String, String, byte[]>> result) {
        try {
            List<CompletableFuture<Map.Entry<Long, SmartBlockData<?>>>> futures = new ArrayList<>();

            for (Record4<Long, String, String, byte[]> record : result) {
                long blockKey = record.get(SMART_BLOCK_DATA.BLOCK_KEY);
                String blockType = record.get(SMART_BLOCK_DATA.BLOCK_TYPE);
                String dataTypeClassName = record.get(SMART_BLOCK_DATA.DATA_TYPE_CLASS);
                byte[] serializedData = record.get(SMART_BLOCK_DATA.DATA);

                CompletableFuture<Map.Entry<Long, SmartBlockData<?>>> reconstructFuture =
                    reconstructSmartBlockData(chunk, blockKey, blockType, dataTypeClassName, serializedData)
                        .thenApply(data -> {
                            if (data != null) {
                                return Map.<Long, SmartBlockData<?>>entry(blockKey, data);
                            }
                            return null;
                        })
                        .exceptionally(ex -> {
                            log.warn("Failed to reconstruct SmartBlockData for block_key {} in chunk {},{}: {}",
                                blockKey, chunk.getX(), chunk.getZ(), ex.getMessage()).submit();
                            return null;
                        });

                futures.add(reconstructFuture);
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<Long, SmartBlockData<?>> resultMap = new HashMap<>();
                    for (CompletableFuture<Map.Entry<Long, SmartBlockData<?>>> future : futures) {
                        try {
                            Map.Entry<Long, SmartBlockData<?>> entry = future.get();
                            if (entry != null) {
                                resultMap.put(entry.getKey(), entry.getValue());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to get result from reconstruction future for chunk {},{}",
                                chunk.getX(), chunk.getZ(), e).submit();
                        }
                    }
                    return resultMap;
                });

        } catch (Exception e) {
            log.error("Error processing chunk data result set for chunk {},{}",
                chunk.getX(), chunk.getZ(), e).submit();
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Reconstructs SmartBlockData asynchronously.
     * Uses thread service to safely access blocks on the main thread.
     */
    private <T> CompletableFuture<SmartBlockData<T>> reconstructSmartBlockData(
            Chunk chunk, long blockKey, String blockType,
            String dataTypeClassName, byte[] serializedData) {

        return runOnMainThread(() -> {
            final Block block = UtilBlock.getBlockByKey(blockKey, chunk);
            return smartBlockFactory.load(block)
                .orElseThrow(() -> new IllegalStateException(
                    "Could not find smart block at " + block.getLocation()));
        }).thenCompose(instance -> {
            final SmartBlock smartBlock = instance.getType();
            if (!Objects.equals(smartBlock.getKey(), blockType)) {
                log.warn("Block type mismatch: expected {}, got {} at {}",
                        blockType, smartBlock.getKey(), instance.getHandle().getLocation()).submit();
                return CompletableFuture.completedFuture(null);
            }

            if (!(smartBlock instanceof DataHolder<?>)) {
                log.warn("SmartBlock type {} does not support data, skipping at {}",
                        blockType, instance.getHandle().getLocation()).submit();
                return CompletableFuture.completedFuture(null);
            }

            // Deserialize data asynchronously
            return deserializeSmartBlockDataRaw(instance, dataTypeClassName, serializedData);
        });
    }

    @Override
    public CompletableFuture<Void> removeChunk(@NotNull Chunk chunk) {
        try {
            long chunkKey = chunk.getChunkKey();

            return database.getAsyncDslContext().executeAsyncVoid(ctx -> {
                ctx.deleteFrom(SMART_BLOCK_DATA)
                        .where(SMART_BLOCK_DATA.REALM.eq(Core.getCurrentRealm()))
                        .and(SMART_BLOCK_DATA.WORLD.eq(chunk.getWorld().getName()))
                        .and(SMART_BLOCK_DATA.CHUNK_KEY.eq(chunkKey))
                        .execute();
            }).exceptionally(throwable -> {
                log.error("Failed to remove chunk SmartBlock data for chunk {},{}",
                        chunk.getX(), chunk.getZ(), throwable).submit();
                return null;
            });

        } catch (Exception e) {
            log.error("Error removing chunk SmartBlock data for chunk {},{}",
                    chunk.getX(), chunk.getZ(), e).submit();
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Deserializes SmartBlockData from database components using the serialization service.
     */
    private <T> CompletableFuture<SmartBlockData<T>> deserializeSmartBlockDataRaw(SmartBlockInstance instance, String dataTypeClassName, byte[] serializedData) {
        Class<T> dataType;
        try {
            //noinspection unchecked
            dataType = (Class<T>) Class.forName(dataTypeClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unknown data type class: " + dataTypeClassName, e);
        }
        return serializationService.deserialize(instance, dataType, serializedData).thenApply(dataObject -> {
            return new SmartBlockData<>(instance, dataType, dataObject, dataManagerProvider.get());
        });
    }

    /**
     * Utility method to run a task on the main thread using UtilServer.
     */
    private <T> CompletableFuture<T> runOnMainThread(Supplier<T> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return CompletableFuture.completedFuture(task.get());
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }
        
        CompletableFuture<T> future = new CompletableFuture<>();
        UtilServer.runTask(plugin, () -> {
            try {
                T result = task.get();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}