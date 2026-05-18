package me.mykindos.betterpvp.core.block.data.storage;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Value;
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
import org.jooq.Condition;
import org.jooq.Record4;
import org.jooq.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static me.mykindos.betterpvp.core.database.jooq.Tables.SMART_BLOCK_DATA;

/**
 * Database-backed {@link SmartBlockDataStorage}.
 * <p>
 * Every read/write follows the same three-context discipline, and never more:
 * <ul>
 *     <li><b>DB executor</b> &mdash; SQL only.</li>
 *     <li><b>Main thread</b> &mdash; world/block access only, batched to a single hop per chunk.</li>
 *     <li><b>Common pool</b> &mdash; (de)serialization, done synchronously in one stage rather
 *         than fanned out into per-row futures.</li>
 * </ul>
 */
@CustomLog
@Singleton
public class DatabaseSmartBlockDataStorage implements SmartBlockDataStorage {

    /**
     * Dedicated pool for this storage's (de)serialization stages. Deliberately <b>not</b> the
     * shared {@code ForkJoinPool.commonPool}: chunk loading is throttled by a permit whose release
     * runs at the tail of this pipeline, so depending on a globally-contended pool here can
     * deadlock the throttle (and, transitively, anything else on commonPool such as client loads).
     */
    private static final AtomicInteger WORKER_ID = new AtomicInteger();
    private static final ExecutorService STORAGE_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            runnable -> {
                final Thread thread = new Thread(runnable, "BPvP-SmartBlockStorage-" + WORKER_ID.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });

    private final Database database;
    private final SmartBlockFactory smartBlockFactory;
    private final Provider<SmartBlockDataManager> dataManagerProvider;
    private final SmartBlockDataSerializationService serializationService;
    private final SmartBlockChunkLoadingService chunkLoadingService;
    private final Core plugin;

    /** Caches resolved data-type classes so repeated rows don't re-run reflective lookups. */
    private final Map<String, Class<?>> dataTypeClassCache = new ConcurrentHashMap<>();

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

    // Single-block operations

    @Override
    public <T> CompletableFuture<Void> save(@NotNull SmartBlockInstance instance, @NotNull SmartBlockData<T> data) {
        if (!serializationService.supportsDataStorage(instance)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Instance must be a DataHolder to save data"));
        }

        final BlockRef ref = BlockRef.of(instance);
        final String blockType = instance.getType().getKey();
        final String dataTypeName = data.getDataType().getName();
        final T value = data.get();

        return CompletableFuture
                .supplyAsync(() -> serializationService.serializeSync(instance, value), STORAGE_EXECUTOR)
                .thenCompose(bytes -> database.getAsyncDslContext().executeAsyncVoid(ctx ->
                        ctx.insertInto(SMART_BLOCK_DATA)
                                .set(SMART_BLOCK_DATA.REALM, Core.getCurrentRealm().getId())
                                .set(SMART_BLOCK_DATA.WORLD, ref.getWorld())
                                .set(SMART_BLOCK_DATA.CHUNK_KEY, ref.getChunkKey())
                                .set(SMART_BLOCK_DATA.BLOCK_KEY, ref.getBlockKey())
                                .set(SMART_BLOCK_DATA.BLOCK_TYPE, blockType)
                                .set(SMART_BLOCK_DATA.DATA_TYPE_CLASS, dataTypeName)
                                .set(SMART_BLOCK_DATA.DATA, bytes)
                                .onConflict(SMART_BLOCK_DATA.REALM, SMART_BLOCK_DATA.WORLD,
                                        SMART_BLOCK_DATA.CHUNK_KEY, SMART_BLOCK_DATA.BLOCK_KEY)
                                .doUpdate()
                                .set(SMART_BLOCK_DATA.BLOCK_TYPE, blockType)
                                .set(SMART_BLOCK_DATA.DATA_TYPE_CLASS, dataTypeName)
                                .set(SMART_BLOCK_DATA.DATA, bytes)
                                .execute()))
                .exceptionally(throwable -> {
                    log.error("Failed to save SmartBlock data for {}", instance.getLocation(), throwable).submit();
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

        final BlockRef ref = BlockRef.of(instance);

        return database.getAsyncDslContext().executeAsync(ctx ->
                        ctx.select(SMART_BLOCK_DATA.DATA_TYPE_CLASS, SMART_BLOCK_DATA.DATA)
                                .from(SMART_BLOCK_DATA)
                                .where(blockMatch(ref))
                                .fetchOptional())
                // Off the DB pool and off commonPool: deserialization is CPU work, not I/O.
                .thenApplyAsync(row -> row
                        .map(r -> (SmartBlockData<T>) deserialize(instance,
                                r.get(SMART_BLOCK_DATA.DATA_TYPE_CLASS),
                                r.get(SMART_BLOCK_DATA.DATA)))
                        .orElse(null), STORAGE_EXECUTOR)
                .exceptionally(throwable -> {
                    log.error("Failed to load SmartBlock data for {}", instance.getLocation(), throwable).submit();
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> remove(@NotNull SmartBlockInstance instance) {
        return delete(() -> blockMatch(BlockRef.of(instance)), "block at " + instance.getLocation());
    }

    // Chunk operations

    @Override
    public @NotNull CompletableFuture<Map<Long, SmartBlockData<?>>> loadChunk(@NotNull Chunk chunk) {
        return chunkLoadingService.loadChunk(chunk, this::loadChunkFromDatabase);
    }

    @Override
    public CompletableFuture<Void> removeChunk(@NotNull Chunk chunk) {
        return delete(() -> chunkMatch(chunk.getWorld().getName(), chunk.getChunkKey()),
                "chunk " + chunk.getX() + "," + chunk.getZ());
    }

    /**
     * The chunk read pipeline, one stage per thread context:
     * query (DB) &rarr; resolve instances (one main-thread hop) &rarr; deserialize (common pool).
     */
    private CompletableFuture<Map<Long, SmartBlockData<?>>> loadChunkFromDatabase(@NotNull Chunk chunk) {
        final String describe = "chunk " + chunk.getX() + "," + chunk.getZ();
        final Condition condition;
        try {
            condition = chunkMatch(chunk.getWorld().getName(), chunk.getChunkKey());
        } catch (Exception e) {
            log.error("Error loading SmartBlock data for {}", describe, e).submit();
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        return database.getAsyncDslContext().executeAsync(ctx ->
                        ctx.select(SMART_BLOCK_DATA.BLOCK_KEY,
                                        SMART_BLOCK_DATA.BLOCK_TYPE,
                                        SMART_BLOCK_DATA.DATA_TYPE_CLASS,
                                        SMART_BLOCK_DATA.DATA)
                                .from(SMART_BLOCK_DATA)
                                .where(condition)
                                .orderBy(SMART_BLOCK_DATA.BLOCK_KEY)
                                .fetch())
                .thenCompose(rows -> resolveInstancesOnMainThread(chunk, rows))
                .thenApplyAsync(resolved -> deserializeChunk(chunk, resolved), STORAGE_EXECUTOR)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.debug("Chunk {},{} loaded with {} SmartBlock entries",
                                chunk.getX(), chunk.getZ(), result.size()).submit();
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Error loading SmartBlock data for {}", describe, throwable).submit();
                    return new HashMap<>();
                });
    }

    /**
     * Resolves every row of a chunk to a live {@link SmartBlockInstance} in a single main-thread
     * task. A failure or mismatch on one row is logged and skipped without aborting the rest.
     */
    private CompletableFuture<List<ResolvedRow>> resolveInstancesOnMainThread(
            @NotNull Chunk chunk, @NotNull Result<Record4<Long, String, String, byte[]>> rows) {

        return runOnMainThread(() -> {
            final List<ResolvedRow> resolved = new ArrayList<>(rows.size());
            for (Record4<Long, String, String, byte[]> row : rows) {
                final long blockKey = row.get(SMART_BLOCK_DATA.BLOCK_KEY);
                try {
                    final ResolvedRow entry = resolveRow(chunk, row);
                    if (entry != null) {
                        resolved.add(entry);
                    }
                } catch (Exception e) {
                    log.warn("Failed to resolve SmartBlock at block_key {} in chunk {},{}: {}",
                            blockKey, chunk.getX(), chunk.getZ(), e).submit();
                }
            }
            return resolved;
        });
    }

    /** Resolves and validates a single row on the main thread, or {@code null} if it should be skipped. */
    @Nullable
    private ResolvedRow resolveRow(@NotNull Chunk chunk, @NotNull Record4<Long, String, String, byte[]> row) {
        final long blockKey = row.get(SMART_BLOCK_DATA.BLOCK_KEY);
        final String expectedType = row.get(SMART_BLOCK_DATA.BLOCK_TYPE);

        final Block block = UtilBlock.getBlockByKey(blockKey, chunk);
        final SmartBlockInstance instance = smartBlockFactory.load(block).orElse(null);
        if (instance == null) {
            log.warn("No SmartBlock found at block_key {} in chunk {},{}, skipping",
                    blockKey, chunk.getX(), chunk.getZ()).submit();
            return null;
        }

        final SmartBlock type = instance.getType();
        if (!Objects.equals(type.getKey(), expectedType)) {
            log.warn("Block type mismatch: expected {}, got {} at {}",
                    expectedType, type.getKey(), instance.getLocation()).submit();
            return null;
        }
        if (!(type instanceof DataHolder<?>)) {
            log.warn("SmartBlock type {} does not support data, skipping at {}",
                    expectedType, instance.getLocation()).submit();
            return null;
        }

        return new ResolvedRow(blockKey, instance,
                row.get(SMART_BLOCK_DATA.DATA_TYPE_CLASS), row.get(SMART_BLOCK_DATA.DATA));
    }

    /** Deserializes every resolved row synchronously off the main thread; bad rows are logged and skipped. */
    private Map<Long, SmartBlockData<?>> deserializeChunk(@NotNull Chunk chunk, @NotNull List<ResolvedRow> rows) {
        final Map<Long, SmartBlockData<?>> result = new HashMap<>(rows.size());
        for (ResolvedRow row : rows) {
            try {
                result.put(row.getBlockKey(),
                        deserialize(row.getInstance(), row.getDataTypeClassName(), row.getSerializedData()));
            } catch (Exception e) {
                log.warn("Failed to deserialize SmartBlockData for block_key {} in chunk {},{}: {}",
                        row.getBlockKey(), chunk.getX(), chunk.getZ(), e.getMessage()).submit();
            }
        }
        return result;
    }

    // Shared helpers

    /** Runs a {@code DELETE} guarded by {@code where}, with uniform error logging. */
    private CompletableFuture<Void> delete(@NotNull Supplier<Condition> where, @NotNull String describe) {
        final Condition condition;
        try {
            condition = where.get();
        } catch (Exception e) {
            log.error("Error removing SmartBlock data for {}", describe, e).submit();
            return CompletableFuture.failedFuture(e);
        }

        return database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.deleteFrom(SMART_BLOCK_DATA).where(condition).execute())
                .exceptionally(throwable -> {
                    log.error("Failed to remove SmartBlock data for {}", describe, throwable).submit();
                    return null;
                });
    }

    /** WHERE predicate matching a single block (realm + world + chunk + block). */
    private Condition blockMatch(@NotNull BlockRef ref) {
        return chunkMatch(ref.getWorld(), ref.getChunkKey())
                .and(SMART_BLOCK_DATA.BLOCK_KEY.eq(ref.getBlockKey()));
    }

    /** WHERE predicate matching every block in a chunk (realm + world + chunk). */
    private Condition chunkMatch(@NotNull String world, long chunkKey) {
        return SMART_BLOCK_DATA.REALM.eq(Core.getCurrentRealm().getId())
                .and(SMART_BLOCK_DATA.WORLD.eq(world))
                .and(SMART_BLOCK_DATA.CHUNK_KEY.eq(chunkKey));
    }

    /** Synchronously rebuilds a {@link SmartBlockData} from its stored components. */
    private SmartBlockData<?> deserialize(@NotNull SmartBlockInstance instance,
                                          @NotNull String dataTypeClassName, byte[] serializedData) {
        return deserializeTyped(instance, resolveDataType(dataTypeClassName), serializedData);
    }

    /** Captures the data-type wildcard so the {@code Class}/value generics line up without a cast. */
    private <V> SmartBlockData<V> deserializeTyped(@NotNull SmartBlockInstance instance,
                                                   @NotNull Class<V> dataType, byte[] serializedData) {
        final V value = serializationService.deserializeSync(instance, dataType, serializedData);
        return new SmartBlockData<>(instance, dataType, value, dataManagerProvider.get());
    }

    private Class<?> resolveDataType(@NotNull String className) {
        return dataTypeClassCache.computeIfAbsent(className, name -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unknown data type class: " + name, e);
            }
        });
    }

    /**
     * Runs a task on the Bukkit main thread, returning its result as a future. Executes inline
     * when already on the main thread to avoid an unnecessary scheduler round-trip.
     */
    private <T> CompletableFuture<T> runOnMainThread(@NotNull Supplier<T> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return CompletableFuture.completedFuture(task.get());
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        }

        final CompletableFuture<T> future = new CompletableFuture<>();
        UtilServer.runTask(plugin, () -> {
            try {
                future.complete(task.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /** Immutable identity of a single block row in {@code SMART_BLOCK_DATA}. */
    @Value
    private static class BlockRef {
        String world;
        long chunkKey;
        long blockKey;

        static BlockRef of(@NotNull SmartBlockInstance instance) {
            final var location = instance.getLocation();
            return new BlockRef(
                    location.getWorld().getName(),
                    Chunk.getChunkKey(location),
                    UtilBlock.getBlockKey(instance.getHandle()));
        }
    }

    /** A database row resolved to a live instance on the main thread, awaiting deserialization. */
    @Value
    private static class ResolvedRow {
        long blockKey;
        SmartBlockInstance instance;
        String dataTypeClassName;
        byte[] serializedData;
    }
}
