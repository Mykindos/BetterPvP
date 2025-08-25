package me.mykindos.betterpvp.core.block.data.manager;

import com.google.inject.Singleton;
import lombok.CustomLog;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * Service responsible for coordinating chunk loading operations to prevent resource exhaustion.
 * Manages concurrent chunk loading with semaphore-based throttling and duplicate prevention.
 */
@Singleton
@CustomLog
public class SmartBlockChunkLoadingService {

    // Semaphore to limit concurrent chunk loading operations to prevent thread exhaustion
    private final Semaphore chunkLoadingSemaphore;
    
    // Track chunks currently being loaded to prevent duplicate loading operations
    private final Set<Long> loadingChunks = ConcurrentHashMap.newKeySet();
    
    // Track in-flight loading futures to avoid duplicate work
    private final Map<Long, CompletableFuture<?>> loadingFutures = new ConcurrentHashMap<>();

    public SmartBlockChunkLoadingService() {
        // Limit to 10 concurrent chunk loading operations to prevent thread pool exhaustion
        this.chunkLoadingSemaphore = new Semaphore(10);
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
     * Acquires a semaphore permit and performs the chunk loading.
     * 
     * @param chunk the chunk to load
     * @param loader the loader function
     * @return CompletableFuture containing the result
     * @param <T> the return type
     */
    private <T> CompletableFuture<T> acquirePermitAndLoad(@NotNull Chunk chunk, 
                                                         @NotNull Function<Chunk, CompletableFuture<T>> loader) {
        long chunkKey = chunk.getChunkKey();
        
        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    chunkLoadingSemaphore.acquire();
                    loadingChunks.add(chunkKey);
                    return null;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for chunk loading permit", e);
                }
            })
            .thenCompose(ignored -> {
                try {
                    return loader.apply(chunk);
                } catch (Exception e) {
                    return CompletableFuture.failedFuture(e);
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
}
