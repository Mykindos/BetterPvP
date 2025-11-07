package me.mykindos.betterpvp.progression.profession.fishing.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.progression.database.jooq.tables.records.GetBiggestFishCaughtByClientRecord;
import me.mykindos.betterpvp.progression.database.jooq.tables.records.GetBiggestFishCaughtRecord;
import me.mykindos.betterpvp.progression.profession.fishing.data.CaughtFish;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.GET_BIGGEST_FISH_CAUGHT;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.GET_BIGGEST_FISH_CAUGHT_BY_CLIENT;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.GET_TOP_FISHING_BY_COUNT;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.GET_TOP_FISHING_BY_WEIGHT;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.PROGRESSION_FISHING;

@CustomLog
@Singleton
public class FishingRepository {

    private final Database database;
    private final ProfessionProfileManager profileManager;
    private final List<Query> fishToSave = new ArrayList<>();

    @Inject
    public FishingRepository(Database database, ProfessionProfileManager profileManager) {
        this.database = database;
        this.profileManager = profileManager;

        createPartitions();
    }

    public void createPartitions() {
        int season = Core.getCurrentSeason();
        String partitionTableName = "progression_fishing_season_" + season;
        try {
            database.getDslContext().execute(DSL.sql(String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF progression_fishing FOR VALUES IN (%d)",
                    partitionTableName, season
            )));
            log.info("Created partition {} for season {}", partitionTableName, season).submit();
        } catch (Exception e) {
            log.info("Partition {} may already exist", partitionTableName).submit();
        }
    }

    public void saveFish(Client client, Fish fish) {

        fishToSave.add(database.getDslContext().insertInto(PROGRESSION_FISHING)
                .set(PROGRESSION_FISHING.CLIENT, client.getId())
                .set(PROGRESSION_FISHING.TYPE, fish.getType().getName())
                .set(PROGRESSION_FISHING.WEIGHT, fish.getWeight()));

    }

    public void saveAllFish(boolean async) {
        List<Query> statements = new ArrayList<>(fishToSave);
        if (statements.isEmpty()) return;
        fishToSave.clear();

        if (async) {
            database.getAsyncDslContext().executeAsyncVoid(ctx -> {
                ctx.batch(statements).execute();
            });
        } else {
            database.getDslContext().batch(statements).execute();
        }
    }


    public Long getCaughtCount(UUID player) {
        Optional<ProfessionProfile> professionProfileOptional = profileManager.getObject(player.toString());
        if (professionProfileOptional.isPresent()) {
            ProfessionProfile professionProfile = professionProfileOptional.get();
            return (long) professionProfile.getProfessionDataMap().get("Fishing").getProperty("TOTAL_FISH_CAUGHT").orElse(0L);
        }

        return 0L;
    }

    public Long getWeightCount(UUID player) {
        Optional<ProfessionProfile> professionProfileOptional = profileManager.getObject(player.toString());
        if (professionProfileOptional.isPresent()) {
            ProfessionProfile professionProfile = professionProfileOptional.get();
            return (long) professionProfile.getProfessionDataMap().get("Fishing").getProperty("TOTAL_WEIGHT_CAUGHT").orElse(0L);
        }

        return 0L;
    }

    public CompletableFuture<CaughtFish> getBiggestFishForGamer(UUID player, double days) {
        return database.getAsyncDslContext().executeAsync(ctx -> ctx.transactionResult(configuration -> {
            DSLContext ctxl = DSL.using(configuration);

            try {
                Long clientId = ctxl.select(CLIENTS.ID).from(CLIENTS).where(CLIENTS.UUID.eq(player.toString())).fetchOne(CLIENTS.ID);
                if (clientId == null) return null;

                GetBiggestFishCaughtByClientRecord result = ctxl.selectFrom(GET_BIGGEST_FISH_CAUGHT_BY_CLIENT.call(
                        clientId, Core.getCurrentSeason(), days, 1)).fetchOne();

                return result != null
                        ? new CaughtFish(player, result.getType(), result.getWeight())
                        : null;
            } catch (DataAccessException ex) {
                log.error("Failed to fetch biggest catch for {}", player.toString(), ex).submit();
            }

            return null;
        }));

    }

    public CompletableFuture<HashMap<UUID, CaughtFish>> getBiggestFishOverall(double days) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            HashMap<UUID, CaughtFish> leaderboard = new HashMap<>();
            try {
                Result<GetBiggestFishCaughtRecord> results = ctx.selectFrom(GET_BIGGEST_FISH_CAUGHT.call(
                        Core.getCurrentSeason(), days, 10)).fetch();

                results.forEach(fishRecord -> {
                    leaderboard.put(
                            UUID.randomUUID(),
                            new CaughtFish(
                                    UUID.fromString(fishRecord.getClientUuid()),
                                    fishRecord.getType(),
                                    fishRecord.getWeight()
                            )
                    );
                });
            } catch (DataAccessException ex) {
                log.error("Error fetching leaderboard data", ex).submit();
            }

            return leaderboard;
        });
    }

    public CompletableFuture<HashMap<UUID, Long>> getTopFishCaughtCount(double days) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            HashMap<UUID, Long> leaderboard = new HashMap<>();
            try {
                ctx.selectFrom(GET_TOP_FISHING_BY_COUNT.call(
                                Core.getCurrentSeason(),
                                days,
                                10))
                        .fetch()
                        .forEach(fishRecord -> {
                            leaderboard.put(
                                    UUID.fromString(fishRecord.getClientUuid()),
                                    fishRecord.getFishCount()
                            );
                        });
            } catch (DataAccessException ex) {
                log.error("Error fetching leaderboard data", ex).submit();
            }
            return leaderboard;
        });
    }

    public CompletableFuture<HashMap<UUID, Long>> getTopFishWeightSum(double days) {
        return database.getAsyncDslContext().executeAsync(ctx -> {
            HashMap<UUID, Long> leaderboard = new HashMap<>();
            try {
                ctx.selectFrom(GET_TOP_FISHING_BY_WEIGHT.call(
                                Core.getCurrentSeason(),
                                days,
                                10))
                        .fetch()
                        .forEach(fishRecord -> {
                            leaderboard.put(
                                    UUID.fromString(fishRecord.getClientUuid()),
                                    fishRecord.getTotalWeight()
                            );
                        });
            } catch (DataAccessException ex) {
                log.error("Error fetching leaderboard data", ex).submit();
            }
            return leaderboard;
        });
    }


}
