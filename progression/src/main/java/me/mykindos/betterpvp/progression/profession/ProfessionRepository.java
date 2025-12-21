package me.mykindos.betterpvp.progression.profession;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import org.jooq.impl.DSL;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.PROGRESSION_EXP;

@CustomLog
@Singleton
public class ProfessionRepository {

    private final Database database;

    @Inject
    public ProfessionRepository(Database database) {
        this.database = database;
        createPartitions();
    }

    public void createPartitions() {
        int season = Core.getCurrentRealm().getSeason().getId();
        String partitionTableName = "progression_exp_season_" + season;
        try {
            database.getDslContext().execute(DSL.sql(String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF progression_exp FOR VALUES IN (%d)",
                    partitionTableName, season
            )));
            log.info("Created partition {} for season {}", partitionTableName, season).submit();
        } catch (Exception e) {
            log.info("Partition {} may already exist", partitionTableName).submit();
        }
    }

    public CompletableFuture<Long> getMostExperiencePerProfessionForClient(UUID player, String profession) {
        try {
            return database.getAsyncDslContext().executeAsync(ctx -> {

                Long experience = ctx.select(PROGRESSION_EXP.EXPERIENCE)
                        .from(PROGRESSION_EXP)
                        .where(PROGRESSION_EXP.PROFESSION.eq(profession))
                        .and(PROGRESSION_EXP.CLIENT.eq(ctx.select(CLIENTS.ID)
                                        .from(CLIENTS)
                                        .where(CLIENTS.UUID.eq(player.toString()))))
                        .and(PROGRESSION_EXP.SEASON.eq(Core.getCurrentRealm().getSeason().getId()))
                        .orderBy(PROGRESSION_EXP.EXPERIENCE.desc())
                        .limit(1)
                        .fetchOne(PROGRESSION_EXP.EXPERIENCE);

                return experience != null ? experience : 0L;
            });
        } catch (Exception e) {
            log.error("Error fetching leaderboard data for player {}", player, e).submit();
            return CompletableFuture.completedFuture(0L);
        }

    }

    public CompletableFuture<HashMap<UUID, Long>> getMostExperiencePerProfession(String profession) {
        HashMap<UUID, Long> leaderboard = new HashMap<>();
        try {
            return database.getAsyncDslContext().executeAsync(ctx -> {
                ctx.select(CLIENTS.UUID, PROGRESSION_EXP.EXPERIENCE)
                        .from(PROGRESSION_EXP)
                        .join(CLIENTS).on(PROGRESSION_EXP.CLIENT.eq(CLIENTS.ID))
                        .where(PROGRESSION_EXP.PROFESSION.eq(profession))
                        .and(PROGRESSION_EXP.SEASON.eq(Core.getCurrentRealm().getSeason().getId()))
                        .orderBy(PROGRESSION_EXP.EXPERIENCE.desc())
                        .limit(10)
                        .fetch()
                        .forEach(expRecord -> {
                            UUID gamer = UUID.fromString(expRecord.get(CLIENTS.UUID));
                            Long experience = expRecord.get(PROGRESSION_EXP.EXPERIENCE);
                            leaderboard.put(gamer, experience);
                        });

                return leaderboard;
            });
        } catch (Exception e) {
            log.error("Error fetching leaderboard data", e).submit();
        }

        return CompletableFuture.completedFuture(leaderboard);


    }

}
