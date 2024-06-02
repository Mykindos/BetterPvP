package me.mykindos.betterpvp.progression.profession.mining.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CustomLog
@Singleton
public class MiningRepository {

    private final Database database;

    @Inject
    public MiningRepository(Database database) {
        this.database = database;
    }

    public CompletableFuture<Long> getOresMinedForGamer(UUID player) {
        return CompletableFuture.supplyAsync(() -> {

            Statement statement = new Statement("SELECT Value FROM progression_properties WHERE Gamer = ? AND Property = ?",
                    new UuidStatementValue(player), new StringStatementValue("TOTAL_ORES_MINED"));
            try (CachedRowSet result = database.executeQuery(statement)) {

                if (result.next()) {
                    return result.getLong(1);
                }

            } catch (SQLException e) {
                log.error("Failed to load mining data for " + player, e).submit();
            }

            return 0L;
        });
    }

}
