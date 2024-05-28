package me.mykindos.betterpvp.progression.profession.fishing.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.data.CaughtFish;
import me.mykindos.betterpvp.progression.profession.fishing.fish.Fish;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

// All data for players should be loaded for as long as they are on, and saved when they log off
// The data should be saved as a fallback to the database every 5 minutes
// The data should be saved on shutdown
@CustomLog
@Singleton
public class FishingRepository {

    private final Database database;
    private final ProfessionProfileManager profileManager;
    private final List<Statement> fishToSave = new ArrayList<>();

    @Inject
    public FishingRepository(Database database, ProfessionProfileManager profileManager) {
        this.database = database;
        this.profileManager = profileManager;
    }

    public void saveFish(UUID player, Fish fish) {
        String query = "INSERT INTO progression_fishing (id, Gamer, Type, Weight) VALUES (?, ?, ?, ?);";
        Statement statement = new Statement(query,
                new UuidStatementValue(UUID.randomUUID()),
                new UuidStatementValue(player),
                new StringStatementValue(fish.getType().getName()),
                new IntegerStatementValue(fish.getWeight()));

        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Progression.class), () -> database.executeUpdate(statement));
    }

    public void saveAllFish(boolean async) {
        List<Statement> statements = new ArrayList<>(fishToSave);
        if (statements.isEmpty()) return;
        fishToSave.clear();
        database.executeBatch(statements, async);
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
        return CompletableFuture.supplyAsync(() -> {
            AtomicReference<CaughtFish> caughtFish = new AtomicReference<>();
            Statement statement = new Statement("CALL GetBiggestFishCaughtByGamer(?, ?, ?)",
                    new UuidStatementValue(player),
                    new DoubleStatementValue(days),
                    new IntegerStatementValue(1)); // Top 10
            database.executeProcedure(statement, -1, result -> {
                try {
                    if (result.next()) {
                        UUID gamer = UUID.fromString(result.getString(2));
                        String fishType = result.getString(3);
                        int fishWeight = result.getInt(4);
                        caughtFish.set(new CaughtFish(gamer, fishType, fishWeight));
                    }
                } catch (SQLException e) {
                    log.error("Error fetching leaderboard data", e).submit();
                }
            });

            return caughtFish.get();
        });

    }

    public CompletableFuture<HashMap<UUID, CaughtFish>> getBiggestFishOverall(double days) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<UUID, CaughtFish> leaderboard = new HashMap<>();
            Statement statement = new Statement("CALL GetBiggestFishCaught(?, ?)",
                    new DoubleStatementValue(days),
                    new IntegerStatementValue(10)); // Top 10
            database.executeProcedure(statement, -1, result -> {
                try {
                    while (result.next()) {
                        final UUID fishId = UUID.fromString(result.getString(1));
                        final UUID gamer = UUID.fromString(result.getString(2));
                        final String fishType = result.getString(3);
                        final int weight = result.getInt(4);
                        leaderboard.put(fishId, new CaughtFish(gamer, fishType, weight));

                    }
                } catch (SQLException e) {
                    log.error("Error fetching leaderboard data", e).submit();
                }
            });

            return leaderboard;

        });
    }

    public CompletableFuture<HashMap<UUID, Long>> getTopFishCaughtCount(double days) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<UUID, Long> leaderboard = new HashMap<>();
            Statement statement = new Statement("CALL GetTopFishingByCount(?, ?)",
                    new DoubleStatementValue(days),
                    new IntegerStatementValue(10)); // Top 10
            database.executeProcedure(statement, -1, result -> {
                try {
                    while (result.next()) {
                        final String gamer = result.getString(1);
                        final long count = result.getLong(2);
                        leaderboard.put(UUID.fromString(gamer), count);
                    }
                } catch (SQLException e) {
                    log.error("Error fetching leaderboard data", e).submit();
                }
            });

            return leaderboard;

        });
    }

    public CompletableFuture<HashMap<UUID, Long>> getTopFishWeightSum(double days) {
        return CompletableFuture.supplyAsync(() -> {
            HashMap<UUID, Long> leaderboard = new HashMap<>();
            Statement statement = new Statement("CALL GetTopFishingByWeight(?, ?)",
                    new DoubleStatementValue(days),
                    new IntegerStatementValue(10)); // Top 10
            database.executeProcedure(statement, -1, result -> {
                try {
                    while (result.next()) {
                        final String gamer = result.getString(1);
                        final long weight = result.getLong(2);
                        leaderboard.put(UUID.fromString(gamer), weight);
                    }
                } catch (SQLException e) {
                    log.error("Error fetching leaderboard data", e).submit();
                }
            });

            return leaderboard;

        });
    }


}
