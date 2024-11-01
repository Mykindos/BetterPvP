package me.mykindos.betterpvp.progression.profession;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@CustomLog
@Singleton
public class ProfessionRepository {

    private final Database database;

    @Inject
    public ProfessionRepository(Database database) {
        this.database = database;
    }

    public CompletableFuture<Long> getMostExperiencePerProfessionForGamer(UUID player, String profession) {
        return CompletableFuture.supplyAsync(() -> {

            AtomicLong exp = new AtomicLong(0);

            Statement statement = new Statement("SELECT * FROM progression_exp WHERE Profession = ? AND Gamer = ? ORDER BY Experience DESC LIMIT 10",
                    new StringStatementValue(profession),
                    new StringStatementValue(player.toString()));

            database.executeProcedure(statement, -1, result -> {
                try {
                    if (result.next()) {
                        long experience = result.getLong(3);
                        exp.set(experience);
                    }
                } catch (SQLException e) {
                    log.error("Error fetching leaderboard data", e).submit();
                }
            });

            return exp.get();
        });

    }

    public CompletableFuture<HashMap<UUID, Long>> getMostExperiencePerProfession(String profession) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<UUID, Long> leaderboard = new HashMap<>();
            Statement statement = new Statement("SELECT * FROM progression_exp WHERE Profession = ? ORDER BY Experience DESC LIMIT 10",
                    new StringStatementValue(profession));

            database.executeProcedure(statement, -1, result -> {
                try {
                    while (result.next()) {
                        UUID gamer = UUID.fromString(result.getString(1));
                        Long experience = result.getLong(3);
                        leaderboard.put(gamer, experience);
                    }
                } catch (SQLException e) {
                    log.error("Error fetching leaderboard data", e).submit();
                }
            });

            return leaderboard;
        });


    }

}
