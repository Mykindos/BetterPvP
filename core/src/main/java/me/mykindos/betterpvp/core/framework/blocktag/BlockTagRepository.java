package me.mykindos.betterpvp.core.framework.blocktag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.jooq.Query;
import org.jooq.impl.DSL;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CHUNK_BLOCK_TAGGING;

@Singleton
@CustomLog
public class BlockTagRepository {

    private final Core core;
    private final Database database;

    private final List<Query> pendingBlockTagUpdates = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public BlockTagRepository(Core core, Database database) {
        this.core = core;
        this.database = database;
        createPartitions();
    }

    public void createPartitions() {
        int realm = Core.getCurrentRealm().getId();
        String partitionTableName = "chunk_block_tagging_realm_" + realm;
        try {
            database.getDslContext().execute(DSL.sql(String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF chunk_block_tagging FOR VALUES IN (%d)",
                    partitionTableName, realm
            )));
            log.info("Created partition {} for realm {}", partitionTableName, realm).submit();
        } catch (Exception e) {
            log.info("Partition {} may already exist", partitionTableName).submit();
        }
    }

    public Map<Long, Map<String, BlockTag>> getBlockTagsForChunk(Chunk chunk) {
        Map<Long, Map<String, BlockTag>> blockTags = new HashMap<>();
        String chunkString = UtilWorld.chunkToFile(chunk);

        try {
            database.getDslContext()
                    .selectFrom(CHUNK_BLOCK_TAGGING)
                    .where(CHUNK_BLOCK_TAGGING.REALM.eq(Core.getCurrentRealm().getId()))
                    .and(CHUNK_BLOCK_TAGGING.CHUNK.eq(chunkString))
                    .fetch()
                    .forEach(record -> {
                        long blockKey = record.get(CHUNK_BLOCK_TAGGING.BLOCK_KEY);
                        String tag = record.get(CHUNK_BLOCK_TAGGING.TAG);
                        String value = record.get(CHUNK_BLOCK_TAGGING.VALUE);

                        blockTags.computeIfAbsent(blockKey, key -> new HashMap<>())
                                .put(tag, new BlockTag(tag, value));
                    });
        } catch (Exception e) {
            log.error("Failed to get block tags for chunk {}: {}", chunkString, e).submit();
        }

        return blockTags;
    }

    public void addBlockTag(Block block, BlockTag blockTag) {
        long blockKey = UtilBlock.getBlockKey(block);

        synchronized (pendingBlockTagUpdates) {
            pendingBlockTagUpdates.add(
                    database.getDslContext()
                            .insertInto(CHUNK_BLOCK_TAGGING)
                            .set(CHUNK_BLOCK_TAGGING.REALM, Core.getCurrentRealm().getId())
                            .set(CHUNK_BLOCK_TAGGING.CHUNK, UtilWorld.chunkToFile(block.getChunk()))
                            .set(CHUNK_BLOCK_TAGGING.BLOCK_KEY, blockKey)
                            .set(CHUNK_BLOCK_TAGGING.TAG, blockTag.getTag())
                            .set(CHUNK_BLOCK_TAGGING.VALUE, blockTag.getValue())
                            .set(CHUNK_BLOCK_TAGGING.LAST_UPDATED, System.currentTimeMillis())
                            .onConflict(CHUNK_BLOCK_TAGGING.REALM, CHUNK_BLOCK_TAGGING.CHUNK, CHUNK_BLOCK_TAGGING.BLOCK_KEY, CHUNK_BLOCK_TAGGING.TAG)
                            .doUpdate()
                            .set(CHUNK_BLOCK_TAGGING.VALUE, blockTag.getValue())
                            .set(CHUNK_BLOCK_TAGGING.LAST_UPDATED, System.currentTimeMillis())
            );
        }
    }

    public void removeBlockTag(Block block, String tag) {
        synchronized (pendingBlockTagUpdates) {
            pendingBlockTagUpdates.add(
                    database.getDslContext()
                            .deleteFrom(CHUNK_BLOCK_TAGGING)
                            .where(CHUNK_BLOCK_TAGGING.REALM.eq(Core.getCurrentRealm().getId()))
                            .and(CHUNK_BLOCK_TAGGING.CHUNK.eq(UtilWorld.chunkToFile(block.getChunk())))
                            .and(CHUNK_BLOCK_TAGGING.BLOCK_KEY.eq(UtilBlock.getBlockKey(block)))
                            .and(CHUNK_BLOCK_TAGGING.TAG.eq(tag))
            );
        }
    }

    // Purge player manipulated block tags older than 3 days
    public void purgeOldBlockTags() {
        Instant cutoff = Instant.now().minusSeconds(60 * 60 * 24 * 3);
        try {
            int deletedRows = database.getDslContext()
                    .deleteFrom(CHUNK_BLOCK_TAGGING)
                    .where(CHUNK_BLOCK_TAGGING.REALM.eq(Core.getCurrentRealm().getId()))
                    .and(CHUNK_BLOCK_TAGGING.LAST_UPDATED.lt(cutoff.toEpochMilli()))
                    .and(CHUNK_BLOCK_TAGGING.TAG.eq("PlayerManipulated"))
                    .execute();

            log.info("Purged {} old block tags", deletedRows).submit();
        } catch (Exception ex) {
            log.error("Failed to purge old block tags", ex).submit();
        }
    }

    public void processBlockTagUpdates() {
        synchronized (pendingBlockTagUpdates) {
            if(!pendingBlockTagUpdates.isEmpty()) {

                List<Query> temp = new ArrayList<>(pendingBlockTagUpdates);
                pendingBlockTagUpdates.clear();
                database.getDslContext().batch(temp).execute();
            }
        }
    }
}
