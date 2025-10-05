package me.mykindos.betterpvp.core.framework.blocktag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.TimestampStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@CustomLog
public class BlockTagRepository {

    private final Core core;
    private final Database database;

    private final List<Statement> pendingBlockTagUpdates = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public BlockTagRepository(Core core, Database database) {
        this.core = core;
        this.database = database;;
    }

    public Map<Integer, Map<String, BlockTag>> getBlockTagsForChunk(Chunk chunk) {
        Map<Integer, Map<String, BlockTag>> blockTags = new HashMap<>();
        String query = "SELECT * FROM chunk_block_tagging WHERE Server = ? AND Season = ? AND Chunk = ?";
        Statement statement = new Statement(query,
                StringStatementValue.of(Core.getCurrentServer()),
                StringStatementValue.of(Core.getCurrentSeason()),
                StringStatementValue.of(UtilWorld.chunkToFile(chunk))
        );

        try (CachedRowSet result = database.executeQuery(statement, TargetDatabase.GLOBAL).join()) {
            while(result.next()) {
                int blockKey = result.getInt(4);
                String tag = result.getString(5);
                String value = result.getString(6);
                blockTags.computeIfAbsent(blockKey, key -> new HashMap<>()).put(tag, new BlockTag(tag, value));
            }
        } catch (SQLException e) {
            log.error("Failed to get block tags for chunk {}: {}", UtilWorld.chunkToFile(chunk), e).submit();
        }

        return blockTags;
    }

    public void addBlockTag(Block block, BlockTag blockTag) {
        String query = "INSERT INTO chunk_block_tagging (Server, Season, Chunk, BlockKey, Tag, Value) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE Value = ?, LastUpdated = ?;";
        Statement statement = new Statement(query,
                StringStatementValue.of(Core.getCurrentServer()),
                StringStatementValue.of(Core.getCurrentSeason()),
                StringStatementValue.of(UtilWorld.chunkToFile(block.getChunk())),
                new IntegerStatementValue(UtilBlock.getBlockKey(block)),
                StringStatementValue.of(blockTag.getTag()),
                StringStatementValue.of(blockTag.getValue()),
                StringStatementValue.of(blockTag.getValue()),
                new TimestampStatementValue(Instant.now())
        );

        synchronized (pendingBlockTagUpdates) {
            pendingBlockTagUpdates.add(statement);
        }
    }

    public void removeBlockTag(Block block, String tag) {
        String query = "DELETE FROM chunk_block_tagging WHERE Server = ? AND Season = ? AND Chunk = ? AND BlockKey = ? AND Tag = ?;";
        Statement statement = new Statement(query,
                StringStatementValue.of(Core.getCurrentServer()),
                StringStatementValue.of(Core.getCurrentSeason()),
                StringStatementValue.of(UtilWorld.chunkToFile(block.getChunk())),
                new IntegerStatementValue(UtilBlock.getBlockKey(block)),
                StringStatementValue.of(tag)
        );

        synchronized (pendingBlockTagUpdates) {
            pendingBlockTagUpdates.add(statement);
        }
    }

    // Purge player manipulated block tags older than 3 days
    public void purgeOldBlockTags() {
        String query = "DELETE FROM chunk_block_tagging WHERE Server = ? AND Season = ? AND LastUpdated < ? AND Tag = ?";
        Statement statement = new Statement(query,
                StringStatementValue.of(Core.getCurrentServer()),
                StringStatementValue.of(Core.getCurrentSeason()),
                new TimestampStatementValue(Instant.now().minusSeconds(60 * 60 * 24 * 3)),
                StringStatementValue.of("PlayerManipulated")
        );

        database.executeUpdate(statement, TargetDatabase.GLOBAL);
    }

    public void processBlockTagUpdates() {
        synchronized (pendingBlockTagUpdates) {
            if(!pendingBlockTagUpdates.isEmpty()) {

                List<Statement> temp = new ArrayList<>(pendingBlockTagUpdates);
                pendingBlockTagUpdates.clear();
                database.executeBatch(temp, TargetDatabase.GLOBAL);
            }
        }
    }
}
