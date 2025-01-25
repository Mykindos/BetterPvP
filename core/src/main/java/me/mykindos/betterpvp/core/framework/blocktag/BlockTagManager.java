package me.mykindos.betterpvp.core.framework.blocktag;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
@Singleton
@CustomLog
public class BlockTagManager {

    private static final Executor TAG_EXECUTOR = Executors.newSingleThreadExecutor();

    public static final Cache<String, HashMap<Integer, HashMap<String, BlockTag>>> BLOCKTAG_CACHE = Caffeine.newBuilder()
            .scheduler(Scheduler.systemScheduler())
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final BlockTagRepository blockTagRepository;

    @Inject
    public BlockTagManager(BlockTagRepository blockTagRepository) {
        this.blockTagRepository = blockTagRepository;
    }

    public CompletableFuture<HashMap<Integer, HashMap<String, BlockTag>>> getBlockTags(Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            String chunkIdentifier = UtilWorld.chunkToFile(chunk);
            HashMap<Integer, HashMap<String, BlockTag>> blockTags = BLOCKTAG_CACHE.getIfPresent(chunkIdentifier);
            if (blockTags != null) {
                // Manually update the entry to refresh the expiry
                BLOCKTAG_CACHE.put(chunkIdentifier, blockTags);
                return blockTags;
            }
            return BLOCKTAG_CACHE.get(chunkIdentifier, key -> blockTagRepository.getBlockTagsForChunk(chunk));
        }).exceptionally(e -> {
            log.error("Failed to get block tags for chunk {}", e, UtilWorld.chunkToFile(chunk)).submit();
            return new HashMap<>();
        });
    }

    public CompletableFuture<Boolean> isPlayerManipulated(Block block) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<Integer, HashMap<String, BlockTag>> blockTags = getBlockTags(block.getChunk()).join();
            return blockTags.computeIfAbsent(UtilBlock.getBlockKey(block), key -> new HashMap<>()).containsKey("PlayedManipulated");
        }).exceptionally(e -> {
            log.error("Failed to check if block is player manipulated", e).submit();
            return false;
        });
    }

    /**
     * Checks the cache to see if the block is player placed
     * Additionally loads the chunks block tags if not present
     *
     * @param block The block to check
     * @return True if the block is player placed, or not present in the cache
     */
    public boolean isPlayerPlaced(Block block) {

            String chunk = UtilWorld.chunkToFile(block.getChunk());
            HashMap<Integer, HashMap<String, BlockTag>> blockTags = BLOCKTAG_CACHE.getIfPresent(chunk);
            if (blockTags == null) {
                CompletableFuture.runAsync(() -> {
                    HashMap<Integer, HashMap<String, BlockTag>> blockTagsForChunk = blockTagRepository.getBlockTagsForChunk(block.getChunk());
                    BLOCKTAG_CACHE.put(chunk, blockTagsForChunk);
                });

                return true;
            }

            return blockTags.computeIfAbsent(UtilBlock.getBlockKey(block), key -> new HashMap<>()).containsKey(BlockTags.PLAYER_MANIPULATED.getTag());

    }

    /**
     * Gets the player who placed the block
     * Additionally loads the chunks block tags if not present
     *
     * @param block The block to check
     * @return The UUID of the player who placed the block, or null if not present
     */
    public UUID getPlayerPlaced(Block block) {
        String chunk = UtilWorld.chunkToFile(block.getChunk());
        HashMap<Integer, HashMap<String, BlockTag>> blockTags = BLOCKTAG_CACHE.getIfPresent(chunk);
        if (blockTags == null) {
            CompletableFuture.runAsync(() -> {
                HashMap<Integer, HashMap<String, BlockTag>> blockTagsForChunk = blockTagRepository.getBlockTagsForChunk(block.getChunk());
                BLOCKTAG_CACHE.put(chunk, blockTagsForChunk);
            });

            return null;
        }

        return UUID.fromString(blockTags.computeIfAbsent(UtilBlock.getBlockKey(block), key -> new HashMap<>()).get("PlayerManipulated").getValue());
    }

    public void loadChunkIfAbsent(Chunk chunk) {
        String chunkIdentifier = UtilWorld.chunkToFile(chunk);
        if (BLOCKTAG_CACHE.getIfPresent(chunkIdentifier) == null) {
            CompletableFuture.runAsync(() -> {
                HashMap<Integer, HashMap<String, BlockTag>> blockTags = blockTagRepository.getBlockTagsForChunk(chunk);
                BLOCKTAG_CACHE.put(chunkIdentifier, blockTags);
            });
        }
    }

    public void addBlockTag(Block block, BlockTag blockTag) {
        CompletableFuture.runAsync(() -> {
            HashMap<Integer, HashMap<String, BlockTag>> blockTags = getBlockTags(block.getChunk()).join();
            blockTags.computeIfAbsent(UtilBlock.getBlockKey(block), key -> new HashMap<>()).put(blockTag.getTag(), blockTag);

             blockTagRepository.addBlockTag(block, blockTag);
        }, TAG_EXECUTOR).exceptionally(ex -> {
            log.error("Failed to add block tag", ex).submit();
            return null;
        });
    }

    public void removeBlockTag(Block block, String tag) {
        CompletableFuture.runAsync(() -> {
            HashMap<Integer, HashMap<String, BlockTag>> blockTags = getBlockTags(block.getChunk()).join();
            blockTags.computeIfAbsent(UtilBlock.getBlockKey(block), key -> new HashMap<>()).remove(tag);
            blockTagRepository.removeBlockTag(block, tag);
        }, TAG_EXECUTOR).exceptionally(ex -> {
            log.error("Failed to remove block tag", ex).submit();
            return null;
        });
    }


}
