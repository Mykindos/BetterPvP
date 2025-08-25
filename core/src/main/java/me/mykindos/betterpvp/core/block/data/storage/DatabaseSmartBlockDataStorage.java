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
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.BlobStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Database storage implementation for SmartBlockData using optimized services.
 * Now much cleaner with separation of concerns through dedicated services.
 */
@CustomLog
@Singleton
public class DatabaseSmartBlockDataStorage implements SmartBlockDataStorage {

    private final Database database;
    private final String server;
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
        this.server = database.getCore().getConfig().getString("tab.server");
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
            long chunkKey = Chunk.getChunkKey(instance.getLocation());
            int blockKey = UtilBlock.getBlockKey(instance.getHandle());
            String world = instance.getLocation().getWorld().getName();
            String blockType = instance.getType().getKey();
            String dataTypeClass = data.getDataType().getName();

            String query = """
                INSERT INTO smart_block_data (server, world, chunk_key, block_key, block_type, data_type_class, data, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE 
                    block_type = VALUES(block_type),
                    data_type_class = VALUES(data_type_class), 
                    data = VALUES(data), 
                    last_updated = CURRENT_TIMESTAMP
                """;

            Statement statement = new Statement(query,
                new StringStatementValue(server),
                new StringStatementValue(world),
                new LongStatementValue(chunkKey),
                new IntegerStatementValue(blockKey),
                new StringStatementValue(blockType),
                new StringStatementValue(dataTypeClass),
                new BlobStatementValue(serializedData)
            );

            return database.executeUpdate(statement, TargetDatabase.GLOBAL)
                .thenApply(result -> (Void) null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<SmartBlockData<T>> load(@NotNull SmartBlockInstance instance) {
        if (!serializationService.supportsDataStorage(instance)) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            Chunk chunk = instance.getHandle().getChunk();
            long chunkKey = chunk.getChunkKey();
            int blockKey = UtilBlock.getBlockKey(instance.getHandle());
            String world = instance.getLocation().getWorld().getName();

            String query = """
                SELECT data_type_class, data 
                FROM smart_block_data 
                WHERE server = ? AND world = ? AND chunk_key = ? AND block_key = ?
                """;

            Statement statement = new Statement(query,
                new StringStatementValue(server),
                new StringStatementValue(world),
                new LongStatementValue(chunkKey),
                new IntegerStatementValue(blockKey)
            );

            CompletableFuture<SmartBlockData<?>> rawFuture = database.executeQuery(statement, TargetDatabase.GLOBAL)
                .thenCompose(resultSet -> processLoadResultRaw(instance, resultSet))
                .exceptionally(throwable -> {
                    log.error("Failed to load SmartBlock data for {}", instance.getHandle().getLocation(), throwable).submit();
                    return null;
                });
            return (CompletableFuture<SmartBlockData<T>>) (CompletableFuture<?>) rawFuture;

        } catch (Exception e) {
            log.error("Error loading SmartBlock data for {}", instance.getHandle().getLocation(), e).submit();
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Processes the database result for a single block load.
     */
    private CompletableFuture<SmartBlockData<?>> processLoadResultRaw(@NotNull SmartBlockInstance instance, 
                                                                     CachedRowSet resultSet) {
        try (CachedRowSet rs = resultSet) {
            if (rs.next()) {
                String dataTypeClassName = rs.getString("data_type_class");
                byte[] serializedData = rs.getBytes("data");
                
                return deserializeSmartBlockDataRaw(instance, dataTypeClassName, serializedData);
            }
            return CompletableFuture.completedFuture(null);
        } catch (SQLException e) {
            log.error("Error processing result set for {}", instance.getHandle().getLocation(), e).submit();
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<Void> remove(@NotNull SmartBlockInstance instance) {
        try {
            Chunk chunk = instance.getHandle().getChunk();
            long chunkKey = chunk.getChunkKey();
            int blockKey = UtilBlock.getBlockKey(instance.getHandle());
            String world = instance.getLocation().getWorld().getName();

            String query = """
                DELETE FROM smart_block_data 
                WHERE server = ? AND world = ? AND chunk_key = ? AND block_key = ?
                """;

            Statement statement = new Statement(query,
                new StringStatementValue(server),
                new StringStatementValue(world),
                new LongStatementValue(chunkKey),
                new IntegerStatementValue(blockKey)
            );

            return database.executeUpdate(statement, TargetDatabase.GLOBAL)
                .thenApply(result -> (Void) null)
                .exceptionally(throwable -> {
                    log.error("Failed to remove SmartBlock data for {}", instance.getHandle().getLocation(), throwable).submit();
                    return null;
                });

        } catch (Exception e) {
            log.error("Error removing SmartBlock data for {}", instance.getHandle().getLocation(), e).submit();
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public @NotNull CompletableFuture<Map<Integer, SmartBlockData<?>>> loadChunk(@NotNull Chunk chunk) {
        return chunkLoadingService.loadChunk(chunk, this::loadChunkFromDatabase);
    }

    /**
     * Loads chunk data from the database. Used by the chunk loading service.
     */
    private CompletableFuture<Map<Integer, SmartBlockData<?>>> loadChunkFromDatabase(@NotNull Chunk chunk) {
        try {
            long chunkKey = chunk.getChunkKey();
            String world = chunk.getWorld().getName();

            String query = """
                SELECT block_key, block_type, data_type_class, data 
                FROM smart_block_data 
                WHERE server = ? AND world = ? AND chunk_key = ?
                ORDER BY block_key
                """;

            Statement statement = new Statement(query,
                new StringStatementValue(server),
                new StringStatementValue(world),
                new LongStatementValue(chunkKey)
            );

            return database.executeQuery(statement, TargetDatabase.GLOBAL)
                .thenCompose(resultSet -> processChunkResultSet(chunk, resultSet))
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
    private CompletableFuture<Map<Integer, SmartBlockData<?>>> processChunkResultSet(@NotNull Chunk chunk, CachedRowSet resultSet) {
        try (CachedRowSet rs = resultSet) {
            List<CompletableFuture<Map.Entry<Integer, SmartBlockData<?>>>> futures = new ArrayList<>();

            while (rs.next()) {
                int blockKey = rs.getInt("block_key");
                String blockType = rs.getString("block_type");
                String dataTypeClassName = rs.getString("data_type_class");
                byte[] serializedData = rs.getBytes("data");

                CompletableFuture<Map.Entry<Integer, SmartBlockData<?>>> reconstructFuture = 
                    reconstructSmartBlockData(chunk, blockKey, blockType, dataTypeClassName, serializedData)
                        .thenApply(data -> {
                            if (data != null) {
                                return Map.<Integer, SmartBlockData<?>>entry(blockKey, data);
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
                    Map<Integer, SmartBlockData<?>> result = new HashMap<>();
                    for (CompletableFuture<Map.Entry<Integer, SmartBlockData<?>>> future : futures) {
                        try {
                            Map.Entry<Integer, SmartBlockData<?>> entry = future.get();
                            if (entry != null) {
                                result.put(entry.getKey(), entry.getValue());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to get result from reconstruction future for chunk {},{}", 
                                chunk.getX(), chunk.getZ(), e).submit();
                        }
                    }
                    return result;
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
    private CompletableFuture<SmartBlockData<?>> reconstructSmartBlockData(
            Chunk chunk, int blockKey, String blockType,
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

            String query = """
                DELETE FROM smart_block_data 
                WHERE server = ? AND world = ? AND chunk_key = ?
                """;

            Statement statement = new Statement(query,
                new StringStatementValue(server),
                new StringStatementValue(chunk.getWorld().getName()),
                new LongStatementValue(chunkKey)
            );

            return database.executeUpdate(statement, TargetDatabase.GLOBAL)
                .thenApply(result -> (Void) null)
                .exceptionally(throwable -> {
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
    private CompletableFuture<SmartBlockData<?>> deserializeSmartBlockDataRaw(SmartBlockInstance instance, String dataTypeClassName, byte[] serializedData) {
        Class<?> dataType;
        try {
            dataType = Class.forName(dataTypeClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unknown data type class: " + dataTypeClassName, e);
        }
        return serializationService.deserialize(instance, dataType, serializedData).thenApply(dataObject -> {
            @SuppressWarnings("unchecked")
            Class<Object> objDataType = (Class<Object>) dataType;
            return new SmartBlockData<>(instance, objDataType, dataObject, dataManagerProvider.get());
        });
    }

    /**
     * Utility method to run a task on the main thread using UtilServer.
     */
    private <T> CompletableFuture<T> runOnMainThread(java.util.function.Supplier<T> task) {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
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