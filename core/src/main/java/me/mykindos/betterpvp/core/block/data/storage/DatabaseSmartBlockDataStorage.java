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
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataSerializer;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.BlobStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

/**
 * Database storage implementation for SmartBlockData using optimized byte serialization.
 * Stores data efficiently in the database using dedicated SmartBlockDataSerializer byte methods.
 */
@CustomLog
@Singleton
public class DatabaseSmartBlockDataStorage implements SmartBlockDataStorage {

    private final Database database;
    private final String server;
    private final SmartBlockFactory smartBlockFactory;
    private final Provider<SmartBlockDataManager> dataManagerProvider;
    
    // Semaphore to limit concurrent chunk loading operations to prevent thread exhaustion
    private final Semaphore chunkLoadingSemaphore;

    @Inject
    public DatabaseSmartBlockDataStorage(Database database, @NotNull String server, SmartBlockFactory smartBlockFactory,
                                         Provider<SmartBlockDataManager> dataManagerProvider) {
        this.database = database;
        this.server = server;
        this.smartBlockFactory = smartBlockFactory;
        this.dataManagerProvider = dataManagerProvider;
        // Limit to 10 concurrent chunk loading operations to prevent thread pool exhaustion
        this.chunkLoadingSemaphore = new Semaphore(10);
    }

    @Override
    public boolean allowsAsynchronousLoading() {
        return true;
    }

    @Override
    public <T> void save(@NotNull SmartBlockInstance instance, @NotNull SmartBlockData<T> data) {
        if (!(instance.getType() instanceof DataHolder)) {
            throw new IllegalArgumentException("Instance must be a DataHolder to save data");
        }

        try {
            Chunk chunk = instance.getHandle().getChunk();
            long chunkKey = chunk.getChunkKey();
            int blockKey = UtilBlock.getBlockKey(instance.getHandle());
            
            // Get type information
            String blockType = instance.getType().getKey();
            String dataTypeClass = data.getDataType().getName();
            
            // Serialize data to bytes using existing serializer
            @SuppressWarnings("unchecked")
            DataHolder<T> dataHolder = (DataHolder<T>) instance.getType();
            SmartBlockDataSerializer<T> serializer = dataHolder.getDataSerializer();
            byte[] serializedData = serializer.serializeToBytes(data.get());

            String query = """
                INSERT INTO smart_block_data (server, chunk_key, block_key, block_type, data_type_class, data, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE 
                    block_type = VALUES(block_type),
                    data_type_class = VALUES(data_type_class), 
                    data = VALUES(data), 
                    last_updated = CURRENT_TIMESTAMP
                """;

            Statement statement = new Statement(query,
                new StringStatementValue(server),
                new LongStatementValue(chunkKey),
                new IntegerStatementValue(blockKey),
                new StringStatementValue(blockType),
                new StringStatementValue(dataTypeClass),
                new BlobStatementValue(serializedData)
            );

            database.executeUpdate(statement, TargetDatabase.GLOBAL).thenAccept(result -> {
//                log.info("Saved SmartBlock data for block at {}", instance.getHandle().getLocation()).submit();
            }).exceptionally(throwable -> {
                log.error("Failed to save SmartBlock data", throwable).submit();
                return null;
            });

        } catch (Exception e) {
            log.error("Error saving SmartBlock data", e).submit();
        }
    }

    @Override
    public <T> Optional<SmartBlockData<T>> load(@NotNull SmartBlockInstance instance) {
        if (!(instance.getType() instanceof DataHolder)) {
            return Optional.empty();
        }

        try {
            Chunk chunk = instance.getHandle().getChunk();
            long chunkKey = chunk.getChunkKey();
            int blockKey = UtilBlock.getBlockKey(instance.getHandle());

            String query = """
                SELECT data_type_class, data 
                FROM smart_block_data 
                WHERE server = ? AND chunk_key = ? AND block_key = ?
                """;

            Statement statement = new Statement(query,
                new StringStatementValue(server),
                new LongStatementValue(chunkKey),
                new IntegerStatementValue(blockKey)
            );

            @SuppressWarnings("unchecked")
            DataHolder<T> dataHolder = (DataHolder<T>) instance.getType();

            return database.executeQuery(statement, TargetDatabase.GLOBAL).thenApply(resultSet -> {
                try (CachedRowSet rs = resultSet) {
                    if (rs.next()) {
                        String dataTypeClassName = rs.getString("data_type_class");
                        byte[] serializedData = rs.getBytes("data");
                        
                        return deserializeSmartBlockData(instance, dataHolder, dataTypeClassName, serializedData);
                    }
                } catch (SQLException e) {
                    log.error("Error processing result set", e).submit();
                }
                return Optional.<SmartBlockData<T>>empty();
            }).exceptionally(throwable -> {
                log.error("Failed to load SmartBlock data", throwable).submit();
                return Optional.empty();
            }).join();
        } catch (Exception e) {
            log.error("Error loading SmartBlock data", e).submit();
            return Optional.empty();
        }
    }

    @Override
    public void remove(@NotNull SmartBlockInstance instance) {
        try {
            Chunk chunk = instance.getHandle().getChunk();
            long chunkKey = chunk.getChunkKey();
            int blockKey = UtilBlock.getBlockKey(instance.getHandle());

            String query = """
                DELETE FROM smart_block_data 
                WHERE server = ? AND chunk_key = ? AND block_key = ?
                """;

            Statement statement = new Statement(query,
                new StringStatementValue(server),
                new LongStatementValue(chunkKey),
                new IntegerStatementValue(blockKey)
            );

            database.executeUpdate(statement, TargetDatabase.GLOBAL).thenAccept(result -> {
//                log.info("Removed SmartBlock data for block at {}", instance.getHandle().getLocation()).submit();
            }).exceptionally(throwable -> {
                log.error("Failed to remove SmartBlock data", throwable).submit();
                return null;
            });

        } catch (Exception e) {
            log.error("Error removing SmartBlock data", e).submit();
        }
    }

    @Override
    public @NotNull CompletableFuture<Map<Integer, SmartBlockData<?>>> loadChunk(@NotNull Chunk chunk) {
        CompletableFuture<Map<Integer, SmartBlockData<?>>> future = new CompletableFuture<>();
        
        // Acquire semaphore to limit concurrent operations
        CompletableFuture.runAsync(() -> {
            try {
                chunkLoadingSemaphore.acquire();
                
                try {
                    long chunkKey = chunk.getChunkKey();

                    String query = """
                        SELECT block_key, block_type, data_type_class, data 
                        FROM smart_block_data 
                        WHERE server = ? AND chunk_key = ?
                        ORDER BY block_key
                        """;

                    Statement statement = new Statement(query,
                        new StringStatementValue(server),
                        new LongStatementValue(chunkKey)
                    );

                    database.executeQuery(statement, TargetDatabase.GLOBAL)
                        .thenCompose(resultSet -> {
                            CompletableFuture<Map<Integer, SmartBlockData<?>>> processingFuture = new CompletableFuture<>();

                            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(Core.class), () -> {
                                try {
                                    Map<Integer, SmartBlockData<?>> result = processChunkResultSet(chunk, resultSet);
                                    processingFuture.complete(result);
                                } catch (Exception e) {
                                    processingFuture.completeExceptionally(e);
                                }
                            });

                            return processingFuture;
                        })
                        .whenComplete((result, throwable) -> {
                            // Always release the semaphore
                            chunkLoadingSemaphore.release();
                            
                            if (throwable != null) {
                                log.error("Failed to load chunk SmartBlock data", throwable).submit();
                                future.complete(new HashMap<>());
                            } else {
                                future.complete(result);
                            }
                        });
                        
                } catch (Exception e) {
                    chunkLoadingSemaphore.release();
                    log.error("Error loading chunk SmartBlock data", e).submit();
                    future.complete(new HashMap<>());
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for chunk loading semaphore", e).submit();
                future.complete(new HashMap<>());
            }
        });
        
        return future;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, SmartBlockData<?>> processChunkResultSet(@NotNull Chunk chunk, CachedRowSet resultSet) {
        Map<Integer, SmartBlockData<?>> result = new HashMap<>();

        try (CachedRowSet rs = resultSet) {
            while (rs.next()) {
                int blockKey = rs.getInt("block_key");
                String blockType = rs.getString("block_type");
                String dataTypeClassName = rs.getString("data_type_class");
                byte[] serializedData = rs.getBytes("data");

                try {
                    Optional<SmartBlockData<?>> smartBlockData = (Optional<SmartBlockData<?>>) reconstructSmartBlockData(chunk, blockKey, blockType, dataTypeClassName, serializedData);
                    smartBlockData.ifPresent(blockData -> result.put(blockKey, blockData));
                } catch (Exception e) {
                    log.warn("Failed to reconstruct SmartBlockData for block_key {}: {}",
                            blockKey, e.getMessage()).submit();
                }
            }
        } catch (SQLException e) {
            log.error("Error processing chunk data result set", e).submit();
        }

        return result;
    }

    @Override
    public void removeChunk(@NotNull Chunk chunk) {
        try {
            long chunkKey = chunk.getChunkKey();

            String query = """
                DELETE FROM smart_block_data 
                WHERE server = ? AND chunk_key = ?
                """;

            Statement statement = new Statement(query,
                new StringStatementValue(server),
                new LongStatementValue(chunkKey)
            );

            database.executeUpdate(statement).thenAccept(result -> {
//                log.info("Removed SmartBlock data entries for chunk at {},{}",
//                        chunk.getX(), chunk.getZ()).submit();
            }).exceptionally(throwable -> {
                log.error("Failed to remove chunk SmartBlock data", throwable).submit();
                return null;
            });

        } catch (Exception e) {
            log.error("Error removing chunk SmartBlock data", e).submit();
        }
    }

    /**
     * Deserializes SmartBlockData from database components using byte data.
     */
    @SuppressWarnings("unchecked")
    private <T> Optional<SmartBlockData<T>> deserializeSmartBlockData(
            SmartBlockInstance instance, DataHolder<T> dataHolder, 
            String dataTypeClassName, byte[] serializedData) {
        
        try {
            Class<T> dataType = (Class<T>) Class.forName(dataTypeClassName);
            SmartBlockDataSerializer<T> serializer = dataHolder.getDataSerializer();
            
            // Deserialize the data object from bytes
            T dataObject = serializer.deserializeFromBytes(serializedData);
            
            // Create and return the SmartBlockData wrapper
            SmartBlockData<T> smartBlockData = new SmartBlockData<>(
                instance, dataType, dataObject, dataManagerProvider.get());
            
            return Optional.of(smartBlockData);
            
        } catch (ClassNotFoundException e) {
            log.error("Unknown data type class: {}", dataTypeClassName, e).submit();
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error deserializing SmartBlockData", e).submit();
            return Optional.empty();
        }
    }

    /**
     * Reconstructs SmartBlockData for chunk loading (requires SmartBlock infrastructure).
     * This is a placeholder for chunk-based loading functionality.
     */
    private Optional<? extends SmartBlockData<?>> reconstructSmartBlockData(
            Chunk chunk, int blockKey, String blockType, 
            String dataTypeClassName, byte[] serializedData) {
        // Reconstruct the block instance
        final Block block = UtilBlock.getBlockByKey(blockKey, chunk);
        final SmartBlockInstance instance = smartBlockFactory.from(block)
                .orElseThrow(() -> new IllegalStateException("Failed to create SmartBlockInstance for block at " + block.getLocation()));
        final SmartBlock smartBlock = instance.getType();
        if (!Objects.equals(smartBlock.getKey(), blockType)) {
            log.warn("Block type mismatch: expected {}, got {}", blockType, smartBlock.getKey()).submit();
            return Optional.empty();
        }

        if (!(smartBlock instanceof DataHolder<?> dataHolder)) {
            log.warn("SmartBlock type {} does not support data, skipping", blockType).submit();
            return Optional.empty();
        }

        return deserializeSmartBlockData(instance, dataHolder, dataTypeClassName, serializedData);
    }
} 