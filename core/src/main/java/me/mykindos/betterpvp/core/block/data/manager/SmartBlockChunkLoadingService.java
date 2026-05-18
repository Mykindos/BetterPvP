package me.mykindos.betterpvp.core.block.data.manager;

import com.google.inject.Singleton;
import lombok.CustomLog;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Service responsible for coordinating chunk loading operations to prevent resource exhaustion.
 * Manages concurrent chunk loading with semaphore-based throttling and duplicate prevention.
 */
@Singleton
@CustomLog
public class SmartBlockChunkLoadingService {

    // Throttles concurrent chunk loading. Acquisition is non-blocking (returns a future) so that
    // no pooled thread is ever parked here while the permit-release path also needs that pool.
    private final AsyncSemaphore chunkLoadingSemaphore;

    // Track chunks currently being loaded to prevent duplicate loading operations
    private final Set<Long> loadingChunks = ConcurrentHashMap.newKeySet();

    // Track in-flight loading futures to avoid duplicate work
    private final Map<Long, CompletableFuture<?>> loadingFutures = new ConcurrentHashMap<>();

    public SmartBlockChunkLoadingService() {
        // Limit to 10 concurrent chunk loading operations to prevent thread pool exhaustion
        this.chunkLoadingSemaphore = new AsyncSemaphore(10);
    }

    /**
     * Loads chunk data using the provided loader function with proper coordination.
     * Prevents duplicate loading operations and limits concurrency.
     * 
     * @param chunk the chunk to load
     * @param loader the function that performs the actual loading
     * @return CompletableFuture containing the loaded data
     * @param <T> the return type of the loader
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> loadChunk(@NotNull Chunk chunk, 
                                             @NotNull Function<Chunk, CompletableFuture<T>> loader) {
        long chunkKey = chunk.getChunkKey();
        
        // Check if this chunk is already being loaded
        CompletableFuture<T> existingFuture = (CompletableFuture<T>) loadingFutures.get(chunkKey);
        if (existingFuture != null) {
            log.debug("Chunk {},{} is already being loaded, returning existing future", 
                chunk.getX(), chunk.getZ()).submit();
            return existingFuture;
        }
        
        // Create new loading future
        CompletableFuture<T> loadingFuture = acquirePermitAndLoad(chunk, loader);
        
        // Store the future to prevent duplicates
        loadingFutures.put(chunkKey, loadingFuture);
        
        // Clean up when done
        loadingFuture.whenComplete((result, throwable) -> {
            loadingFutures.remove(chunkKey);
            loadingChunks.remove(chunkKey);
        });
        
        return loadingFuture;
    }

    /**
     * Acquires a permit (without blocking any thread) and performs the chunk loading,
     * releasing the permit once the whole pipeline settles.
     *
     * @param chunk the chunk to load
     * @param loader the loader function
     * @return CompletableFuture containing the result
     * @param <T> the return type
     */
    private <T> CompletableFuture<T> acquirePermitAndLoad(@NotNull Chunk chunk,
                                                          @NotNull Function<Chunk, CompletableFuture<T>> loader) {
        final long chunkKey = chunk.getChunkKey();

        return chunkLoadingSemaphore.acquire()
            .thenCompose(ignored -> {
                loadingChunks.add(chunkKey);
                try {
                    return loader.apply(chunk);
                } catch (Exception e) {
                    return CompletableFuture.<T>failedFuture(e);
                }
            })
            .whenComplete((result, throwable) -> {
                chunkLoadingSemaphore.release();
                if (throwable != null) {
                    log.error("Failed to load chunk {},{}", chunk.getX(), chunk.getZ(), throwable).submit();
                } else {
                    log.debug("Successfully loaded chunk {},{}", chunk.getX(), chunk.getZ()).submit();
                }
            });
    }

    /**
     * Checks if a chunk is currently being loaded.
     * 
     * @param chunk the chunk to check
     * @return true if the chunk is currently being loaded
     */
    public boolean isChunkLoading(@NotNull Chunk chunk) {
        return loadingChunks.contains(chunk.getChunkKey());
    }

    /**
     * Gets the number of chunks currently being loaded.
     * 
     * @return the number of loading chunks
     */
    public int getLoadingChunkCount() {
        return loadingChunks.size();
    }

    /**
     * Gets the number of available permits for chunk loading.
     * 
     * @return the number of available permits
     */
    public int getAvailablePermits() {
        return chunkLoadingSemaphore.availablePermits();
    }

    /**
     * Waits for all currently loading chunks to finish.
     * Useful for shutdown or testing scenarios.
     * 
     * @return CompletableFuture that completes when all chunks are done loading
     */
    public CompletableFuture<Void> waitForAllLoading() {
        CompletableFuture<?>[] futures = loadingFutures.values().toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(futures);
    }

    /**
     * A counting semaphore whose {@link #acquire()} hands back a {@link CompletableFuture} instead
     * of blocking the caller. This is essential here: permits are released from pooled threads, and
     * blocking a pool thread inside {@code acquire()} while the release path also depends on that
     * pool produces a thread-starvation deadlock. A permit is transferred directly to the next
     * waiter on {@link #release()}, so a runnable waiter is never starved by the permit count.
     */
    private static final class AsyncSemaphore {

        private final int maxPermits;
        private final Deque<CompletableFuture<Void>> waiters = new ArrayDeque<>();
        private int permits;

        AsyncSemaphore(int permits) {
            this.maxPermits = permits;
            this.permits = permits;
        }

        CompletableFuture<Void> acquire() {
            synchronized (this) {
                if (permits > 0) {
                    permits--;
                    return CompletableFuture.completedFuture(null);
                }
                final CompletableFuture<Void> waiter = new CompletableFuture<>();
                waiters.addLast(waiter);
                return waiter;
            }
        }

        void release() {
            final CompletableFuture<Void> next;
            synchronized (this) {
                next = waiters.pollFirst();
                if (next == null) {
                    if (permits < maxPermits) {
                        permits++;
                    }
                    return;
                }
            }
            // Completed outside the lock so the waiter's continuation (the chunk loader) never
            // runs while this monitor is held.
            next.complete(null);
        }

        synchronized int availablePermits() {
            return permits;
        }
    }
}
