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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
@Singleton
@CustomLog
public class BlockTagManager {

    public static final Executor TAG_EXECUTOR = Executors.newSingleThreadExecutor();

    public static final Cache<String, Map<Integer, Map<String, BlockTag>>> BLOCKTAG_CACHE = Caffeine.newBuilder()
            .scheduler(Scheduler.systemScheduler())
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final BlockTagRepository blockTagRepository;

    @Inject
    public BlockTagManager(BlockTagRepository blockTagRepository) {
        this.blockTagRepository = blockTagRepository;
    }

    public CompletableFuture<Map<Integer, Map<String, BlockTag>>> getBlockTags(Chunk chunk) {
        return CompletableFuture.supplyAsync(() -> {
            String chunkIdentifier = UtilWorld.chunkToFile(chunk);
            Map<Integer, Map<String, BlockTag>> blockTags = BLOCKTAG_CACHE.getIfPresent(chunkIdentifier);
            if (blockTags != null) {
                // Manually update the entry to refresh the expiry
                BLOCKTAG_CACHE.put(chunkIdentifier, blockTags);
                return blockTags;
            }
            return BLOCKTAG_CACHE.get(chunkIdentifier, key -> blockTagRepository.getBlockTagsForChunk(chunk));
        }).exceptionally(e -> {
            log.error("Failed to get block tags for chunk {}: {}", UtilWorld.chunkToFile(chunk), e).submit();
            return new HashMap<>();
        });
    }

    public CompletableFuture<Boolean> isPlayerManipulated(Block block) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, Map<String, BlockTag>> blockTags = getBlockTags(block.getChunk()).join();
            return blockTags.computeIfAbsent(UtilBlock.getBlockKey(block), key -> new HashMap<>()).containsKey("PlayerManipulated");
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
        Map<Integer, Map<String, BlockTag>> blockTags = BLOCKTAG_CACHE.getIfPresent(chunk);
            if (blockTags == null) {
                CompletableFuture.runAsync(() -> {
                    Map<Integer, Map<String, BlockTag>> blockTagsForChunk = blockTagRepository.getBlockTagsForChunk(block.getChunk());
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
        Map<Integer, Map<String, BlockTag>> blockTags = BLOCKTAG_CACHE.getIfPresent(chunk);
        if (blockTags == null) {
            CompletableFuture.runAsync(() -> {
                Map<Integer, Map<String, BlockTag>> blockTagsForChunk = blockTagRepository.getBlockTagsForChunk(block.getChunk());
                BLOCKTAG_CACHE.put(chunk, blockTagsForChunk);
            });

            return null;
        }

        Map<String, BlockTag> stringBlockTagHashMap = blockTags.computeIfAbsent(UtilBlock.getBlockKey(block), key -> new HashMap<>());
        if(!stringBlockTagHashMap.containsKey(BlockTags.PLAYER_MANIPULATED.getTag())){
            return null;
        }

        String playerManipulated = stringBlockTagHashMap.get(BlockTags.PLAYER_MANIPULATED.getTag()).getValue();
        return UUID.fromString(playerManipulated);
    }

    /**
     * Loads the block tags for a specified chunk into the cache if they are not already present.
     * This method performs an asynchronous operation to retrieve the block tags from the repository
     * and stores them in the cache using the chunk's unique identifier.
     *
     * @param chunk The chunk whose block tags should be loaded into the cache if absent.
     */
    public void loadChunkIfAbsent(Chunk chunk) {
        String chunkIdentifier = UtilWorld.chunkToFile(chunk);
        if (BLOCKTAG_CACHE.getIfPresent(chunkIdentifier) == null) {
            CompletableFuture.runAsync(() -> {
                Map<Integer, Map<String, BlockTag>> blockTags = blockTagRepository.getBlockTagsForChunk(chunk);
                BLOCKTAG_CACHE.put(chunkIdentifier, blockTags);
            });
        }
    }

    /**
     * Adds a block tag to the specified block. The method asynchronously updates the cached list
     * of block tags for the chunk containing the block and persists it using the associated repository.
     *
     * @param block    The block to which the tag will be added.
     * @param blockTag The tag that will be associated with the block.
     */
    public void addBlockTag(Block block, BlockTag blockTag) {
        CompletableFuture.runAsync(() -> {
            Map<Integer, Map<String, BlockTag>> blockTags = getBlockTags(block.getChunk()).join();
            blockTags.computeIfAbsent(UtilBlock.getBlockKey(block), key -> new HashMap<>()).put(blockTag.getTag(), blockTag);

             blockTagRepository.addBlockTag(block, blockTag);
        }, TAG_EXECUTOR).exceptionally(ex -> {
            log.error("Failed to add block tag", ex).submit();
            return null;
        });
    }

    /**
     * Removes a specific tag associated with a block. This method operates asynchronously
     * to ensure non-blocking behavior. It updates the block tag data structure and removes
     * the tag from the repository that persists the block tags.
     *
     * @param block The block from which the tag should be removed.
     * @param tag The tag to remove from the specified block.
     */
    public void removeBlockTag(Block block, String tag) {
        CompletableFuture.runAsync(() -> {
            Map<Integer, Map<String, BlockTag>> blockTags = getBlockTags(block.getChunk()).join();
            blockTags.computeIfAbsent(UtilBlock.getBlockKey(block), key -> new HashMap<>()).remove(tag);
            blockTagRepository.removeBlockTag(block, tag);
        }, TAG_EXECUTOR).exceptionally(ex -> {
            log.error("Failed to remove block tag", ex).submit();
            return null;
        });
    }


}
